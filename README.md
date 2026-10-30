# Commute Fit — 通勤初筛

当前首页为006简化MVP：输入出发和工作区域的完整 postcode、单程上限（默认60分钟），选择日间/早班/晚班，即可初筛是否值得尝试投递。默认预选日间，个人时间和准确门店均不必填。首屏只给一个结论及方向原因，日期和路线折叠查看。原详细工作检查保留在 /advanced，独立路线查询保留在 /journey。

Enter two postcodes, a one-way limit and your chosen daytime/early/late references. Dates and routes sit behind one main verdict. Exact shifts, job extraction and availability are optional; no fares are used. Detailed job checks remain at /advanced and journey lookup at /journey.

## Simple check / 新版初筛（006）

- 从伦敦今天起建立七份日程。早班去程02:30→03:30→04:30→05:30→06:30；日间06:30→07:30；晚班22:30→21:30→20:30，均为到达目标，首个符合上限的路线即停止。
- 上午回程06:10→07:10→08:10→09:10→10:10；日间回程17:10。早班回程用当天上午，晚班用次日上午；第二天早班复用第一天晚班已查到的上午结果。第七日晚班使用第八天回程节点，仍只有七份日程。
- 只查用户选中的类型；日间成功不跳过已选早晚。相同日期、方向及时间节点不重复查询，只保留最优合格路线。不设置最少工时，参考去回窗口不构造实际班表。
- 已选参考有可行日期/时段时给“可以通勤，可以尝试投递”，并说明其他不符合方向；未知不判失败。明确工作日的必需班次另按全部要求检查，未明确工作日则七天是可用日期参考。
- 选填区可粘贴招聘正文或URL、确认多个实际班次、输入明确时间或限制窗口。无完整postcode时补工作区域，不强迫地图找门店。双周排班需确认第一周周一，取消某周选项保留原周期长度。
- 后台执行并显示进度，可取消。任务仅在内存中暂存，完成后两分钟过期、每30秒清理，取消即移除；无数据库/历史记录。页面输入变动会使旧任务失效，不显示旧输入的结果。
- 规格、运行和验收：[006 spec](specs/006-simple-commute-check/spec.md)、[quickstart](specs/006-simple-commute-check/quickstart.md)、[CHANGELOG](specs/006-simple-commute-check/CHANGELOG.md)。后端新增接口需重启加载，不能仅刷新前端。

## Run locally / 本地启动

需要 Java 21、Node.js 24/npm，以及可连接 api.tfl.gov.uk 的网络。当前 MVP 不需要 Docker 或 PostgreSQL。

推荐统一启动（在项目根目录）：

```sh
bash start-local.sh
```

若当前位于 backend 或 frontend 目录，运行 `bash ../start-local.sh`。脚本自行定位项目，构建并启动后端，检查接口就绪，再启动连接同一端口的前端；打开它实际输出的网址并保持终端开启。后端失败日志保存在 `.local/journey-backend.log`，会直接输出末尾内容。后端运行独立的 JAR 副本，开发期间再次构建不会覆盖该运行文件；加载新后端代码仍需重启。

也可以分开启动：

后端（终端 1）：

```sh
export JAVA_HOME='/Users/jdm/Library/Java/JavaVirtualMachines/jdk-21.0.13+9/Contents/Home'
export PATH="$JAVA_HOME/bin:$PATH"
cd backend
./mvnw spring-boot:run
```

后端只监听 127.0.0.1:8080。可选 TFL_APP_KEY 仅在后端设置。

前端（终端 2）：

```sh
cd frontend
npm install
VITE_API_PROXY_TARGET=http://127.0.0.1:8080 npm run dev
```

打开 Vite 输出的本地网址，通常是 http://127.0.0.1:5173。

## Advanced work commute check / 高级工作检查（/advanced）

- 用户填写起点、单程通勤上限，以及 URL、职位描述或工作地点 postcode；个人可上班星期和时间可选。
- URL/文本规则提取职位地点、中文/英文星期和明确的 12/24 小时制班次；结果作为建议，用户需确认或修改。读取失败时可粘贴文本或手动补充。
- 实际班次优先于雇主可排班窗口；支持多选。星期不明确则覆盖七天查询，不将“每周五天、包括周末”推断为只查周末。双周 rota 需要填写第一周周一的实际日期，再查询对应的两周。
- 去程按上班时间到达，回程按下班时间出发；展示路线预计时长、换乘数和交通段。
- 只要已确认的班次不兼容或通勤超过上限，就判为“不太合适”；信息或路线不足时判为“可能合适／待确认”。
- 本流程无票价预算、不显示或使用票价、不调用 FareTo。
- 详细规格：[specs/003-work-commute-check/spec.md](specs/003-work-commute-check/spec.md)。

## Standalone journey lookup / 单独路线查询

- 两个 postcode；默认现在出发，也可选择指定日期与时间的「出发」或「到达」。所有时间按伦敦当地时间处理。
- 每条路线的出发/到达日期时间、全程耗时、交通段、站点和换乘。
- 只使用 TfL Journey Planner 返回的路线级 `journey.fare.totalCost`；若缺失或无效，显示“票价暂不可用”，不查询未列入公开 Swagger 的 FareTo，也不拼加分段票价。
- 全程总价旁显示 TfL 返回的高峰/非高峰票价标注；若未返回该标注，则不自行判断。
- TfL 未返回总价时显示“票价暂不可用”，而非免费或不完整总额。
- 支持中英文、交换起终点、地点消歧与查询错误重试。
- 时间模式保留三个本地化并排选项；切换模式或修改日期、时间后自动查询，加载时保留当前路线结果。指定时间采用紧凑时间/日期栏：左侧时间菜单按 15 分钟间隔选择，框内箭头可微调；右侧日期框包含日历与前后日期按钮，月历以浮层打开。
- 计划班次不保证实时到达；成人 PAYG 报价不是最终扣款，不应用优惠或全天封顶。
- 两个 postcode 会发送给 TfL；本应用不持久保存输入或结果。

路线查询页不包含交互地图绘制或逐步导航。求职通勤检查不包含账号、职位申请资格判断、复杂交通偏好、优惠或票价预算。

## Verify / 验证

```sh
cd backend
./mvnw --batch-mode --no-transfer-progress verify
```

```sh
cd frontend
npm test
npm run build
```

当前工作通勤规格：[specs/003-work-commute-check/spec.md](specs/003-work-commute-check/spec.md)。路线查询规格：[specs/002-postcode-journey/spec.md](specs/002-postcode-journey/spec.md)。
TfL API 在线规格及本项目端点/字段参考：[docs/tfl-unified-api-reference.md](docs/tfl-unified-api-reference.md)。
旧方案保存在 [specs/001-commute-fit-assessment/](specs/001-commute-fit-assessment/)；不再代表当前 MVP。
旧档案与班次后端仅在显式启用 legacy Spring profile 时加载；数据库和已有档案未删除。默认启动不连接或迁移数据库。

Powered by the Transport for London Journey Planner API. This is an independent app, not endorsed by TfL.
# Job extraction module (004)

The work commute page can read a public job URL or pasted description without a paid model API. It presents workplace candidates, schedule evidence and editable suggestions. Select the intended workplace and one or more actual shifts before checking the commute. Actual shifts take precedence over employer availability windows; opening hours and weekly hours are not converted into shifts. Chinese weekdays and two-week rota rows are supported; a rota anchor date still needs user confirmation.

The standalone endpoint is `POST /api/job-extractions` with `{ "sourceType": "url" | "description" | "work-postcode", "content": "..." }`. The existing `/api/work-commute/extract` delegates to the same module. See [004 contract](specs/004-job-extraction/contracts/extraction.md) and [validation guide](specs/004-job-extraction/quickstart.md).

Run `bash start-local.sh` from the project root. If an older backend is already running, stop it in its terminal first. Restart after backend changes; the frontend updates automatically during development.

First release: bounded public static-page fetching, JSON-LD JobPosting, postcode/address candidates, weekday-linked 12/24-hour time ranges and basic HTML tables. Dynamic browser rendering, login-only pages, generic ATS adapters and CAPTCHA handling are not included. Parsing fixtures passing does not establish coverage of all recruitment sites; information not published by the employer remains unknown. Failed URL reading can be replaced with pasted text or manual entry.

## Workplace location selection / 工作地点确认（005）

门店名称不等于可直接使用的精确地址。提取地点后点击“使用这个工作地点”，或在地点框输入名称/地址后点击“搜索并确认位置”。查看候选地址、postcode（若有）、来源和地图，再明确确认。门店和交通站点分开标注；不能用附近车站自动替代工作地点。只返回城市/街道时需细化，不能确认为精确门店。

新接口为 `GET /api/place-search?query=...`。门店/地址使用 Photon / OpenStreetMap，交通站点复用 TfL；确认候选生成十分钟令牌。通勤查询使用令牌对应的坐标或 TfL ID，回程交换起终点及各自令牌，第三方 OSM ID 不会直接传给 TfL。修改地点文字清除确认；班次变化保留地点。也支持直接输入完整 postcode、有效纬度/经度或 TfL 站点 ID；postcode 仅代表邮区，不保证是员工入口。

默认 Photon 公共 demo 仅用于合理低频测试，无可用性或门店覆盖保证；每进程限频，搜索缓存五分钟。可通过启动环境变量 `PHOTON_BASE_URL` 指向自行部署的 Photon。未接入付费服务或大模型，也不使用 Nominatim 公共服务器自动补全。没有门店结果时补充街道地址或门店公布的 postcode；中英文别名和门店入口精度仍取决于数据源。

### 地点服务逐个对比：Geoapify（实验）

在 `/place-demo` 的“选择要实际调用的接口”中选择 **Geoapify**，本次请求只发给 Geoapify Geocoding API（英国范围、最多 8 条），不会回退到 Photon 或 TfL。首次测试需自行在 Geoapify 注册并创建 API key，然后将 `GEOAPIFY_API_KEY=你的key` 写入项目根目录 `.env`，重启 `bash start-local.sh`。key 不要贴到聊天、浏览器或提交到 Git。缺 key、key 无效、额度限制和网络故障会分别显示为 Geoapify 配置/服务错误；错误内容不会回显 key。

这一步只是在独立 demo 里评估搜索候选，不会切换正式通勤地点搜索，也不会查询路线。先用相同的品牌+门店、纯门店名、postcode 等输入单独比较该服务，再决定是否测试下一家。Geoapify 官方 Geocoding API 文档说明注册 key 后有免费额度，当前额度和适用条款以其账户和官方文档为准。

下一家待测 **MapTiler**：在 `/place-demo` 选择 MapTiler 后只请求 MapTiler Geocoding API，限制英国、关闭 autocomplete、返回最多 8 条。需要自行创建 key 并在 `.env` 写 `MAPTILER_API_KEY=...` 后重启。它的官方文档描述了包含 POI 的地名搜索；免费层额度、搜索会话定义及个人/非商业限制应以[官方定价](https://www.maptiler.com/cloud/pricing/)为准，参数说明见[官方搜索文档](https://docs.maptiler.com/guides/developer/location-services/geocoding-search/parameters/)。比较时仍逐条复用 Geoapify 的三个查询词。

第三家待测 **LocationIQ**：demo 的 LocationIQ 选项对 postcode 使用专门的 postal code endpoint，其他输入走 Forward Geocoding，均限制英国并最多返回 8 条。配置 `LOCATIONIQ_API_KEY=...` 到 `.env` 后重启。官方文档当前列出 Free 层 5,000 请求/天及 2 请求/秒，免费计划的商业使用限制和 attribution 要求以[官方定价页](https://locationiq.com/pricing)为准；搜索参数和 postal code endpoint 见[官方 Search 文档](https://docs.locationiq.com/docs/search-forward-geocoding)。

**OpenCage 实测完成**：`M&S Woolwich Simply Food` 与 `Marks and Spencer Woolwich` 均无匹配；只有输入完整的 `Marks & Spencer, 4 Victory Parade, Woolwich, SE18 6FL` 才返回正确店址。`SE18 6FL` 返回邮区代表点。就本次品牌+区域找门店的任务而言不合适，详细结果见 `specs/005-location-selection/CHANGELOG.md`。OpenCage 官方标注的免费 trial 仅用于测试（2,500 次/天、1 次/秒），不作为长期免费依赖；见[官方 API 文档](https://opencagedata.com/api)与[定价页](https://opencagedata.com/pricing)。

**Positionstack 已跳过**：免费层注册要求信用卡。geolith 已完成演示测试，但品牌门店查询结果不符合要求，因此已从 demo 和后端移除。TomTom Places Search 已加入 `/place-demo` 并完成首次实测：`M&S Woolwich` 能将目标 POI 排第一，但品牌门店的完整地址数据还不够精确。完整 postcode 查询将保留用户输入的邮编。

验收步骤、环境限制与真实接口观察分别见 [005 quickstart](specs/005-location-selection/quickstart.md)、[005 CHANGELOG](specs/005-location-selection/CHANGELOG.md) 和 [浏览器观察记录](docs/location-browser-observations-2026-10-29.json)。可选低频实测脚本：`python3 scripts/check_tfl_location_types.py --live`；不加 `--live` 只显示用例，不发请求，不输出 API key。

