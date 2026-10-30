# Idea Research: 不使用大模型 API 的职位 URL 参数提取

- Slug: job-url-extraction
- Created: 2026-10-29
- Evidence confidence (overall): medium
- Scope: 文档与现有代码审查；未安装、运行或横向实测爬虫，未得出成功率排名。本文记录证据与待验证假设，不代表已选定实施方案。

## Users & Demand

用户将自己找到的一份职位分享给 Commute Fit，期望确认地点和班次后检查通勤；不需要全网职位索引，不使用付费大模型 API。需求来源：本轮用户指令，confidence high / cited internal。

## Internal Findings

来源：backend/src/main/java/uk/co/commutefit/workcommute/JobListingService.java，代码静态审查，confidence high。

1. fetchPublicPage 仅执行 HTTP 请求，不渲染 JS；拒绝全部重定向。正文经过整页去标签，未定位职位主体。
2. toPlainText 删除 script/style/noscript，其中包含 application/ld+json，因此没有先读取 JobPosting 的机会。
3. 全文扫描 postcode，多个邮编返回空；无法区分工作地点、公司总部、推荐职位、页脚地址。当前空格处理不保证把无空格英国邮编标准化。
4. extractTimes 仅识别两端均带 am/pm 的第一个时间范围，不识别普通 24 小时范围、秒、24:00、分日时段、多班型。第一组匹配可能是营业时间。
5. extractDays 不关联时间所在句子或表格行；出现 weekends 会先返回周末，可能忽略其他日期；5 days out of 7 不代表一周七天都必须工作。
6. Extraction 只有一个地点、一个星期列表、一个开始/结束值；无法表达每天不同班次、可选/轮换班次、雇主排班窗口和字段证据。
7. 响应体大小在完整读入后检查，DNS 预检查与实际连接未绑定。后续增加受控重定向或浏览器获取时，需要同时验证实际目标、子请求和下载预算。

## Prior Art

| 方案 | 已核实能力 | 限制与成本 | 证据质量 |
| --- | --- | --- | --- |
| PageSift / YOLOW92 | 页面内容与结构化数据抽取、可选浏览器渲染、Rust 与 Python/Node 接入 | AGPL-3.0 与商业授权并存；项目公开性能数据不是岗位字段准确率；未实测目标雇主 | medium / 项目自述 |
| jsoup | Java DOM/CSS 选择器、HTML 解析，MIT | 需要自己定义岗位字段和语义；不自动执行 JS | high / 官方文档 |
| Playwright Java | 控制 Chromium/Firefox/WebKit；可用于渲染动态页面 | 本地浏览器 CPU/内存、部署与超时成本；渲染不能保证解除登录或访问限制 | high / 官方文档；成本为工程推断 |
| Trafilatura | Python 正文、元数据、去网页噪声；当前 Apache-2.0 | 输出正文不等于结构化岗位班次；需要 Python 运行环境 | high / 官方文档 |
| extruct | JSON-LD、Microdata、RDFa 等嵌入元数据抽取，BSD-3-Clause | Python；不负责补出原文缺失的工作安排 | high / 官方仓库 |
| Greenhouse Job Board GET | 官方说明公开 GET 不需要认证，可取职位内容与地点 | 每个招聘看板/职位的数据，字段仍可能只有城市；不保证精确班次 | high / 官方文档 |
| Lever Postings | 官方公开 published 职位，地点、正文、岗位 ID 等 JSON 字段 | 全球/EU 域名不同；不保证门店地址或班次，不能泛化到所有 ATS | high / 官方仓库 |

PageSift 同名结果还出现 joeychilson/pagesift，但该仓库本次直开为 404。本文评估的是 YOLOW92/pagesift；如果用户指另一个项目需重新核实。

## Market & Context

Schema.org JobPosting 支持 jobLocation、description、workHours、jobLocationType 等。Google 的招聘结构化数据文档示例包括 PostalAddress。workHours 是文本，既可以是具体时段，也可以只是每周工时；存在字段不代表存在可查询的起止班次。confidence high / Schema.org 与 Google 官方文档。

用户提供一个 URL 的场景可评估按页提取与少量站点适配；无需先建设大规模爬虫。是否比通用爬虫更便宜、更准确，是待实测的工程假设。

## Data & Constraints

### 直接核实的英国雇主样例

Tesco 岗位 record=1013709 页面已过期，仅作为解析样本使用：Contract Details 列出两种轮班（06:00–14:00、14:00–22:00），每周七天中工作五天；另一个 Availability Window 表格列出每天 06:00–22:00。两者在同一页面同时出现，不能把窗口当成实际 16 小时班次。confidence high / 官网公开页面。

Tesco record=1017401 的搜索返回内容包含按星期的不同时间，含 24:00:00；页面说明合同工时会安排在该窗口内。本次仅见搜索返回，未独立保存原 HTML；confidence medium。这不是当前招聘有效性的证明。

### 需要分别衡量的正确性

- 页面获取成功 ≠ 取得目标职位正文 ≠ 字段正确 ≠ 用户确认可用。
- 工作地点应区分完整地址/完整 postcode、城市、多个门店、远程/混合办公；hiringOrganization.address 不能自动当 jobLocation。
- 时间应区分 exact shift、employer availability window、weekly hours、opening hours、vague shift、missing。
- 保留分日安排及多个班型；日期字段 datePosted / validThrough 不得当工作星期。过期页面可以仍可读取，需单独标记。
- 每个值应能回溯到原文片段和数据路径；机器可解析不代表事实已确认。以上为由官方字段语义和 Tesco 样例推导的需求，confidence medium / inference。

## Evidence Against the Idea

1. 雇主未公布具体 rota 时，抓取或字段解析无法恢复未知排班。workHours 的定义允许粗粒度文本；Tesco 样例明确区分窗口与轮班。
2. PageSift 的公开基准主要衡量网页正文等任务，不证明在 Commute Fit 的班次/门店字段上表现更好。
3. 公开 ATS 数据存在不代表覆盖用户常用的超市、咖啡店与餐厅网站；需要先统计真实 URL 分布。
4. 自托管工具可避免按调用付费，但浏览器算力、部署、缓存及站点适配维护不为零。confidence medium / engineering inference。
5. 无站点样本集时无法承诺 90% 或 95% 成功率；本轮没有此类实测结果。

## Gaps & Open Questions

- [NEEDS CLARIFICATION: 真实用户 URL 主要来自哪些雇主官网与聚合站？] 这是覆盖率调研问题，不阻止下一步样本采集。
- [NEEDS CLARIFICATION: PageSift 在英国目标雇主的表格和 JSON-LD 上保留情况如何？] 待本地可复现对比。
- [NEEDS CLARIFICATION: 新增 Python/浏览器进程相对于纯 Java 方案的实际部署代价？] 待测量，不依赖大模型 API。

建议验证方法（待执行实验，不是已测数据）：收集 30 个开发样本、20 个未用于规则调整的独立验收样本，覆盖三类目标行业、静态/动态页面、多个地点、窗口/实际班次/无时间、已过期与受限页面。人工标注地点精度、时间语义、日期和证据；分别报告获取覆盖率、自动填入值精确率、字段覆盖率、正确弃答比例、延迟与用户补填数。允许一个字段成功、其他字段缺失；优先减少误填。所有工具使用同一原始页面或同一输入 URL，固定版本及查询时间。

## Sources

以下 URL 不包含用户凭证。GitHub 属技能允许来源；其余为用户本轮授权的免费技术和招聘解析调研所涉及的官方文档/官网，通过 web 工具检索。未使用带密钥请求或任意服务器爬取。

- https://github.com/YOLOW92/pagesift (host github.com, allowlisted)
- https://github.com/YOLOW92/pagesift/blob/main/COMMERCIAL.md (host github.com, allowlisted; 授权条款仅转述维护者说明)
- https://jsoup.org/ (host jsoup.org, user-authorized research)
- https://github.com/jhy/jsoup (host github.com, allowlisted)
- https://playwright.dev/java/docs/intro (host playwright.dev, user-authorized research)
- https://github.com/microsoft/playwright (host github.com, allowlisted)
- https://github.com/adbar/trafilatura (host github.com, allowlisted)
- https://github.com/scrapinghub/extruct (host github.com, allowlisted)
- https://schema.org/JobPosting (host schema.org, user-authorized research)
- https://developers.google.com/search/docs/appearance/structured-data/job-posting (host developers.google.com, user-authorized research)
- https://docs.greenhouse.io/job-board.html (host docs.greenhouse.io, user-authorized research)
- https://github.com/lever/postings-api (host github.com, allowlisted)
- https://apply.tesco-careers.com/members/modules/job/detail.php?record=1013709 (host apply.tesco-careers.com, user-authorized research; 已过期解析样例)
- https://apply.tesco-careers.com/members/modules/job/detail.php?record=1017401 (host apply.tesco-careers.com, user-authorized research; search-only evidence)
