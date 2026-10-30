# 005 地点选择 — 2026-10-29

## 逐家比较地点搜索服务：Geoapify（2026-10-29）

- 按“一次只测一家”的用户选择，先在独立 `/place-demo` 增加 Geoapify，不改正式地点搜索的 Photon/TfL 流程。
- Geoapify provider 直接请求 Geocoding API，限制英国、最多 8 条；结果归一化为候选地点，路线仍使用该候选坐标，不把 Geoapify/OSM 数据源 ID 传给 TfL。
- Demo 对 Geoapify 缺 key、无结果、401/403、429、其他 HTTP 错误和网络故障分别给出 Geoapify 相关提示，避免通用异常误说成 TfL Planner 故障。
- 本机配置：项目根目录 `.env` 增加 `GEOAPIFY_API_KEY=...` 后重启 `bash start-local.sh`。不在聊天中分享或提交 key。`GEOAPIFY_BASE_URL` 可选覆盖 API host。
- 前端 `npm run build` 已通过。首次后端构建使用系统默认 Java 失败（项目源代码使用 Java 21 语法）；切换项目 Java 21 后后端打包成功，地点模块测试 13 项通过（含 Geoapify 归一化和缺 key 状态）。
- 用户在本机配置 key 并启动服务后，已通过 Chrome 的 `/place-demo` 发出真实 Geoapify 请求；key 内容未读取、展示或保存。

### 真实搜索结果

| 查询词 | Geoapify 返回 | 评估 |
|---|---|---|
| `M&S Woolwich Simply Food` | 8 条；目标 Woolwich 的 `M&S Foodhall` 在第 5 条，postcode `SE18 6FL`，坐标 `51.49158, 0.07065`。它给出的地址为 `4 Plumstead Road`，而此前 Photon 观察记录是同一 postcode 附近的 `4 Victory Parade`。 | 能召回目标 postcode/近似位置，但被其他分店排在前面，门店地址文本不一致。适用于人工候选确认，不应当作已精确识别门店入口。 |
| `M&S Woolwich` | 8 条；前四条是 Milton Keynes、Easthampstead、Thame、Ilford 的 M&S；第五条误匹配到 Blackburn 的 `Woolwich Street`。Woolwich 的目标 M&S 未出现。 | 短品牌+区域查询召回不可靠。 |
| `SE18 6FL` | 1 条 postcode 候选：`Greenwich, London, SE18 6FL, United Kingdom`，坐标 `51.49148, 0.07129`。 | postcode 匹配有效，给的是邮区级代表点，不是门店入口；契合当前“以小区域 postcode 做通勤初筛”的产品方向。 |

- Geoapify 可用于 postcode 解析/候选搜索，但现有证据不足以证明品牌+门店搜索优于 Photon。下一家服务应在这三条相同输入上逐项对比，不要因为 postcode 成功就宣称门店名解析已解决。

## 下一家供应商：MapTiler 接入

- 依据 MapTiler 官方 API 文档接入 `GET /geocoding/{query}.json`：country=gb、language=en、autocomplete=false、limit=8。文档支持 place name 与 POI 搜索，并说明 POI index 可被排除，因此实测结果决定覆盖质量。
- Demo 增加独立 MapTiler 选项；需要 `.env` 中 `MAPTILER_API_KEY`，不与 Geoapify、Photon 或 TfL fallback。结果归一化为候选，实际通勤位置继续使用坐标。
- MapTiler 代码完成时前端构建成功，地点模块 15 项测试通过（含 MapTiler POI 解析和缺 key 状态）。

## MapTiler 真实搜索记录（2026-10-29）

用户配置本机 key 并重启后，在 Chrome `/place-demo` 逐条测试：

| 查询词 | MapTiler 返回 | 评估 |
|---|---|---|
| `M&S Woolwich Simply Food` | 5 条：第 1 条 Woolwich locality；随后出现 `s`、Ford 等无关 locality；没有目标门店或 postcode。 | 未找到门店。品牌符号查询产生明显噪声。 |
| `M&S Woolwich` | 8 条：有 Woolwich locality、Croydon 的 M&S Mall、Woolwich Road 等，没有 Woolwich M&S Foodhall。 | 未找到目标门店，地址/道路候选易造成误认。 |
| `SE18 6FL` | 8 条；第 1 条为准确 `SE18 6FL` postcode，坐标 `51.49158, 0.07065`；后续含错误地理位置的 `SE18/x` 类街道项。 | postcode 主结果正确，坐标与 Geoapify/Photon 目标点一致；需只选 postcode 类型结果，忽略后续噪声。 |

- 对当前个人通勤初筛，MapTiler 的准确 postcode 首位候选有用；品牌+门店名召回失败。相较 Geoapify，postcode 都能给到相同位置，但 MapTiler 在这组输入的后续候选噪声更多。
- 注意：MapTiler 官方文档支持地点/POI 搜索，但此案例的具体搜索结果不支持“能可靠查到目标商店”的结论。下一家再接 LocationIQ，复用同一三组查询词。

## 下一家供应商：LocationIQ 接入

- 依据 LocationIQ 官方 Search 文档接入 `GET /v1/search`；英国 postcode 输入改走专用 `/v1/search/postalcode` endpoint。普通查询请求 `countrycodes=gb`、JSON、address details、最多 8 条。
- Demo 增加独立 LocationIQ provider；key 从 `.env` 的 `LOCATIONIQ_API_KEY` 加载，失效 key、额度限制和网络错误以 LocationIQ 名称提示。不会 fallback 到其他服务。
- 免费计划额度及 attribution 规则会变化，README 链接官方条款；本地个人测试目前需由用户创建 key 并重启。
- Geoapify、MapTiler 已按同一组三个查询词测试；LocationIQ 真实搜索尚未进行，等待 key 配置后复测。

## LocationIQ 真实搜索记录（2026-10-29）

用户配置本机 key 后，在 Chrome `/place-demo` 实测：

| 查询词 | LocationIQ 返回 | 评估 |
|---|---|---|
| `M&S Woolwich Simply Food` | HTTP 404。LocationIQ 官方将 forward geocoding 的 404 定义为“Unable to geocode / no address match”。 | 无候选，不是服务中断。demo 已修正为 `no-matches`，而非“服务不可用”。 |
| `M&S Woolwich` | 1 条 `Woolwich Street, Higher Audley, Blackburn... BB1 3BD`。它被识别成 street 且不可选择。 | 未找到门店；同名道路误匹配但 UI 正确禁止选择。 |
| `marks and spencer woolwich` | 1 条 `Marks & Spencer, 4, Victory Parade, Woolwich, Greenwich, London, ... SE18 6FL`，坐标 `51.49162, 0.07068`。 | 精确命中用户目标门店，postcode 与既有样例一致，地址和坐标足以用于门店候选确认。把缩写 `M&S` 展开并去掉 `Simply Food` 后缀有显著帮助。 |
| `SE18 6FL` | 专用 postcode endpoint 返回 postcode `SE18 6FL`，显示 `Greater London, England, United Kingdom`，坐标 `51.49170, 0.07040`。 | 邮区代表点命中，位置与 Geoapify/MapTiler 结果接近，非门店入口。 |

- 对照 Geoapify、MapTiler：只有 LocationIQ 在把品牌完整展开成 `Marks and Spencer` 后找到了目标店；原始缩写+职位后缀及短缩写输入都没命中。LocationIQ 的专用 postcode 查询同样返回可用于初步通勤的邮区点。
- LocationIQ 文档明确 Search API 的 404 表示无地址匹配，因此本地实现将此状态视为 `no-matches`，不会误报为网络/供应商故障。此状态映射已增加单元测试；真实浏览器中的最终文案需重启加载更新后的后端确认。

## OpenCage 接入

- Demo 新增独立 OpenCage provider，限定英国、最多 8 条结果，并请求 `no_record=1`；不会 fallback 到其他供应商。配置 `.env` 中的 `OPENCAGE_API_KEY` 并重启后即可用相同查询词验收。
- 接入时本机 `.env` 未配置 OpenCage key；后续已补 key 并完成下方真实查询记录。
- 当前 OpenCage 官方免费 trial 标注为测试用途，2,500 次/天、1 次/秒，不适合作为长期免费依赖；见 [API 文档](https://opencagedata.com/api) 和 [定价页](https://opencagedata.com/pricing)。
- 前端构建已通过。后端定向测试本轮因 Maven 用户仓库 `.m2` 锁文件无法打开而未启动，属于环境锁错误，不能计为通过或代码失败。

## OpenCage 真实搜索记录（2026-10-29）

用户配置 API key 并启动本地前后端后，在 Chrome `/place-demo` 实测 OpenCage；每次仅调用 OpenCage，不经过其他 provider。

| 查询词 | OpenCage 返回 | 评估 |
|---|---|---|
| `M&S Woolwich Simply Food` | 0 条候选。 | 原始职位/品牌简写没有召回目标。 |
| `Marks and Spencer Woolwich` | 0 条候选。 | 展开品牌全称后仍未能按品牌+区域找到门店。 |
| `Marks & Spencer, 4 Victory Parade, Woolwich, SE18 6FL` | 2 条；首条 `M&S Foodhall, 4 Victory Parade, Woolwich, SE18 6FL, United Kingdom`，postcode `SE18 6FL`，坐标 `51.49158, 0.07065`；第二条为同邮编的 London postcode 候选。 | 有完整门牌与邮编时可以 geocode 到目标店址，但这是地址解析，不代表具备品牌门店搜索召回能力。 |
| `SE18 6FL` | 1 条 `London, SE18 6FL, United Kingdom`，postcode `SE18 6FL`，坐标 `51.49166, 0.07075`。 | 邮区结果适合区域初筛，但不是商店入口。 |

- 产品判断：本轮目标是从招聘信息中的品牌/门店描述定位工作区域；OpenCage 的品牌+区域查询两种写法均未命中，完整地址查询依赖已经知道的街道和 postcode，因此不适合作为当前核心门店检索方案。其 postcode geocoding 可工作，但该能力不优于当前已测服务的邮区解析。
- 查询时序曾出现前端自动化快照滞后一轮；最终每个已记录结果均在页面显示对应查询词和候选后确认。

## geolith 实测后移除

- geolith 曾作为独立 provider 接入 demo 并使用真实 API key 试测。
- `M&S Woolwich Simply Food` 返回无关地点；`Marks and Spencer Woolwich` 未找到目标门店；`M&S Woolwich` 虽在第 7 位找到 SE18 6FL 的目标附近地点，但结果排序和噪声不适合作为用户地点选择体验。
- 用户决定不采用 geolith，已移除前后端接口、配置项、demo 选项和测试；`GEOLITH_API_KEY` 也从本机 `.env` 删除。
- Positionstack 因要求信用卡跳过；Radar 不用于同类服务竞品测试。现有地点 provider 候选已测试或排除，后续新增服务需先确认免费注册条件和商业使用条款。

## TomTom Places Search 实测

- 选用面向新集成的 Orbis Places Search API Discover，按完整文本查询返回商业地点、地址、门店品牌信息及坐标；英国国家过滤在后端传入。
- 官方文档列出 `POST /maps/orbis/places/discover`，要求 `TomTom-Api-Key`、`TomTom-Api-Version: 3` 和 `Attributes` 请求头；解析最多 8 个结果。
- 已在 MyTomTom 确认个人项目 API key 启用了 Places Search API；密钥值不记录在文档中。
- Demo 实测：`M&S Woolwich` 返回 8 条，`Marks and Spencer Woolwich` 返回 1 条；两者均将 `Marks & Spencer M&S Foodhall` 排在第一位，坐标约 `51.49156, 0.07058`。`M&S Woolwich Simply Food` 没有匹配。
- 搜 `Marks & Spencer, 4 Victory Parade, Woolwich, SE18 6FL` 仍返回同一 POI，但 TomTom 标注为 `6 Victory Parade, London, SE18`，没有返回完整邮编，门牌和邮编精度不足。
- 直接搜 `SE18 6FL` 返回正确邮编附近坐标（约 `51.49166, 0.07073`），但上游地址字段只含外邮区 `SE18`。解析逻辑已更新为完整邮编查询时保留用户输入并标记 `postcode` 精度。
- 变更后后端编译打包成功，测试跳过；当前运行进程尚未加载这次邮编解析修正，需下一次重启后才会生效。
- 资料：[Discover API](https://docs.tomtom.com/places-search-api/documentation/places-search/discover)、[官方定价](https://docs.tomtom.com/pricing)、[搜索 API 选型说明](https://docs.tomtom.com/search-api/documentation/product-information/choosing-a-search-api)。

## TomTom Search API Fuzzy Search 对照入口

- `/place-demo` 增加独立 provider `tomtom-search`，不会影响 Places Discover 或正式地点搜索流程。
- 请求旧版 Search API Fuzzy Search，限制 `countrySet=GB`，索引使用 `POI,PAD,Addr,Str,EPP`，并请求 `extendedPostalCodesFor=POI,PAD,Addr,EPP`；返回最多 8 个候选并展示 `extendedPostalCode`。
- 共用 `TOMTOM_API_KEY`。若 TomTom 控制台按产品区分权限，需另行启用 Search API；403/401 显示服务错误而非无匹配。
- 2026-10-29 浏览器实测：`M&S Woolwich Simply Food` 返回 8 条，主要是包含数字 20 的无关地址；`M&S Woolwich` 返回全国 M&S POI，Woolwich 门店未进前 8；`Marks and Spencer Woolwich` 被解释成大量 `20 Marks...` 地址；`SE18 6FL` 返回 SE18 范围内无关 POI/地址，没有命中该完整 postcode。
- 使用已知完整地址 `Marks & Spencer, 4 Victory Parade, Woolwich, SE18 6FL` 复测，仍返回 5 条无关的含数字 20 地址，没找到目标店；单独搜 `SE18 6FL` 也返回附近无关 POI，而非 postcode point。旧 Search API Fuzzy Search 不适合本项目的品牌+区域门店定位，也没有带来可用的完整 postcode 改善。
- 官方定价页 Search API 免费额度列为每月 2,500 次；以账户实时控制台为准。资料：[Fuzzy Search 参数](https://docs.tomtom.com/search-api/documentation/search-service/fuzzy-search)、[TomTom API 选型](https://docs.tomtom.com/search-api/documentation/product-information/choosing-a-search-api)、[官方定价](https://docs.tomtom.com/pricing)。
- 资料：[Fuzzy Search 参数](https://docs.tomtom.com/search-api/documentation/search-service/fuzzy-search)、[TomTom API 选型](https://docs.tomtom.com/search-api/documentation/product-information/choosing-a-search-api)、[官方定价](https://docs.tomtom.com/pricing)。

## TomTom Places Discover 邮编字段修正

- 确认 Demo 里 `SE18` 显示问题发生在 **TomTom Places Search · Discover** 的解析层：响应中的 `address.extendedPostalCode` 没有被读取，前端只收到 `address.postalCode` 的外邮区。
- 修正为优先读取并规范化 `extendedPostalCode`，缺失时才使用普通 `postalCode`；仍不猜测上游没有返回的邮编。
- 新增 Places Discover parser 单测，覆盖 `postalCode=SE18` 与 `extendedPostalCode=SE18 6FL` 同时存在的情况。
- 重新通过 `/place-demo` 以 `Marks and Spencer Woolwich foodhall` 查询，并显式请求 `address.extendedPostalCode` 后，候选仍返回 `6 Victory Parade, London, SE18`、postcode `SE18`。完整 postcode 并未出现在本次 Places Discover 候选结果里；显示层没有可展开的隐藏邮编。精确到 full postcode 仍需其他数据源或地址级 geocoding 补充。

## Postcodes.io Demo provider

- 新增免 API key 的独立 `postcodes-io` provider：完整 UK postcode 走 lookup；outward code 前缀走 postcode search；`lat,lon` 输入走 nearest postcode 查询；品牌/商店词明确说明该服务不支持 POI 搜索。
- 候选展示完整 postcode、坐标、行政区及 source。保留 Postcodes.io `quality` 语义：quality 1–4 标为 postcode 精度，其余降为 locality；坐标可用于 postcode 层级通勤初筛。
- 官方说明服务数据源包括 ONS Postcode Directory、Ordnance Survey Open Names、Scottish Postcode Directory，开放项目 MIT 许可；Docker 自托管由仓库提供。公共服务无 API key，遵守服务条款/fair use，不宣称无限制。
- 2026-10-29 直接实测 `GET /postcodes/SE18%206FL`：返回完整 `SE18 6FL`、quality `1`、坐标 `51.491661, 0.070755`、Greenwich / London。`GET /postcodes?query=SE18&limit=5` 返回 SE18 前缀下的邮编候选（例如 SE18 1AA 起），可供用户挑选；完整 postcode lookup 是目标邮区代表点，不等于商店入口。
- 本机 Demo 前端热更新后已显示 provider，但当前运行后端返回“通勤评估输入无效”（旧进程未识别 `postcodes-io`）。后端测试 24 项通过、jar 已打包；需重启 `bash start-local.sh` 后才能通过 Demo 走新端点。
- 官方文档：[项目概览](https://postcodes.io/docs/overview/)、[邮编查找](https://postcodes.io/docs/api/postcode-lookup/)、[GitHub/Docker 自托管](https://github.com/ideal-postcodes/postcodes.io)。


## ArcGIS 门店搜索直接实测（2026-10-29）

- 本机 Chrome 直接请求 World GeocodeServer/findAddressCandidates，sourceCountry=GBR、maxLocations=5、forStorage=false、f=pjson。本次未传 token；不代表生产集成无需鉴权。尚未接入 Demo。
- marks and spencer woolwich：唯一候选 Marks & Spencer，score 100，POI，Postal SE18 6FL，坐标 51.4916055, 0.070707；地址 4 Plumstead Road；返回官网门店链接 https://www.marksandspencer.com/stores/woolwich-foodhall-1990 。
- 官网核对：WOOLWICH FOODHALL，4 Victory Parade，London，SE18 6FL；电话与 ArcGIS 一致。邮编吻合，街道描述不同。
- M&S Woolwich Simply Food：前两项为 Woolwich 区域，无邮编；还有 Sheffield、Rhyl 的 Simply Food，不能当作目标店。
- Marks and Spencer Woolwich Simply Food：目标 SE18 6FL 排第三（81.82 分）；Blackheath SE3 7SX 和 Charlton/Woolwich Road SE7 7AJ 排前两项（92.12 分）。不能直接取第一条或最高分。
- 结论：简短品牌+区域查询有效；简称和附加词明显影响结果。后续需考虑品牌别名、精简查询、区域与候选类型核对。本轮仅实测和记录。


## ArcGIS 独立 Demo 接入

- 中文 /place-demo 新增 arcgis provider，后端 GET /api/place-search/provider/arcgis?query=...。调用与上述直接实测相同的公开 World Geocoding 端点，英国过滤、最多 8 条、forStorage=false；仅作接口比较，不宣称生产环境免 key。
- 原样传入 query，不改写品牌、不重排结果，区分 venue/address/postcode/street/locality；区域不能冒充精确门店。规范化完整 postcode，验证经纬度，用户选择后 token 保留返回点坐标。
- HTTP 或 ArcGIS 响应体 error 均显示服务不可用，避免当成无匹配。未修改正式合并搜索来源。
- LocationSearchServiceTest 通过；新增用例覆盖坐标顺序、完整邮编、区域不可选、无效坐标与上游排序。前端 npm run build 通过。后端 jar 已重新打包；正在运行的后端需要重启才能加载新 provider。

- 重启后本机 Chrome 页面验收通过：/place-demo 选择 ArcGIS，提交 marks and spencer woolwich，显示 1 条 Marks & Spencer 候选，SE18 6FL，坐标 51.49161, 0.07071；已确认经本地前后端实际返回。页面也已显示 M&S Woolwich Simply Food 的 4 条区域/外地候选，与上游直接实测一致。


## Foursquare Places Demo 入口（2026-10-29）

- 新增 foursquare provider，调用新版 GET https://places-api.foursquare.com/places/search，Authorization Bearer、X-Places-Api-Version=2025-06-17；原样 query，near=London, GB，limit=8，只请求 Pro 字段 fsq_place_id,name,location,latitude,longitude,date_closed。固定伦敦范围在 Demo 明示，避免默认按服务器 IP 搜索。
- 解析新版顶层 latitude/longitude，保留上游顺序，只保留 country=GB；明确已关闭候选不可选。缺失 results 或 HTTP 错误不当成无匹配。
- 实际 .env 与 .env.example 均补空白 FOURSQUARE_API_KEY；start-local.sh 加载；密钥不记录、不回传前端。
- 后端 LocationSearchServiceTest 26 项通过，前端构建通过，jar 已打包。当前未配置有效 key，尚未验证真实门店召回；等待用户配置后重启测试。
- 官方接口：https://docs.foursquare.com/fsq-developers-places/reference/place-search ；响应字段：https://docs.foursquare.com/fsq-developers-places/reference/response-fields 。官方变更说明 https://docs.foursquare.com/developer/reference/upcoming-changes 列 2026-06-01 起 500 次免费 Pro 调用，以账户账单页为准。

- 用户配置并重启后，2026-10-29 Chrome Demo 实测 Foursquare：M&S Woolwich Simply Food 和 marks and spencer woolwich 均显示“Foursquare 服务暂不可用”。不是成功返回的零候选；当前通用 RuntimeException 分支隐藏了网络/解析原因，不能据此判断该服务门店覆盖差或 key 无效。真实数据验证尚未通过。

## Foursquare 默认域名修复

- 定位到接入时字符串替换误写默认域名为 api.foursquaredata.com；官方 endpoint 为 https://places-api.foursquare.com/places/search 。修正 FoursquareClient 默认值并增加默认 endpoint 回归检查。此前 parser 测试显式传正确域名、未发真实请求，未覆盖生产默认值；之前服务不可用不能用于评估召回能力。修复后需重启正在运行的后端；真实搜索仍待重启后复测。

- 修复域名并重启后 Chrome Demo 实测已成功返回：M&S Woolwich Simply Food 与 marks and spencer woolwich 各 8 条伦敦 M&S 候选，均未出现目标 SE18 6FL；精简词前两项为 Oxford Street W1D 2JR / W1C 1AP。当前 near 固定 London, GB，Woolwich 只在 query 中；这一结果表明当前请求方式召回不准，不能证明 Foursquare 没有该门店。下一步应单独验证将区域用于 near（Woolwich, London, GB）并仅将品牌用于 query 的效果。

## 地点搜索 Demo 精简

- Demo 只保留 LocationIQ、ArcGIS、TomTom Places Discover，默认 LocationIQ；移除合并流程说明及其余页面入口。切换接口清除候选标记，查询期间禁止切换以免标签错配。本轮仅精简 Demo 展示；历史后端实现和密钥保留。

## 多源坐标聚合讨论与邮编核对

- 用户倾向每次查询 LocationIQ、ArcGIS、TomTom，允许邮编不同，以坐标是否集中（两两不超过 1 英里）为近似位置判断依据；尚未实现。
- 真实 Postcodes.io 查询 WF17 9DT：53.746135,-1.644255。附近 WF17 9AD 距 232m、WF17 9AE 279m、BD11 1AF 517m；邮区前缀不同也可相邻。WF17 8AA 与 WF17 9DT 的英国网格坐标相距约 3864m（2.40 英里），同 WF17 并不保证一英里以内。
- ONS postal geography 说明：WF17 是 district，WF17 9 是 sector，完整邮编是 unit；编码不提供距离上限。来源 https://www.ons.gov.uk/methodology/geography/ukgeographies/postalgeography 。
- 建议先核对品牌/区域，按跨来源候选的两两距离成组；不能将 24 个候选一起求均值或声称 100% 定位。1 英里适合作为可接受近似范围，不保证步行/通勤时长相同。代表点优先选聚类内到其他点距离总和最小的已有门店坐标，保留范围和来源；尚未确认自动选点规则。

## 三家区域共识 Demo 实现

- LocationIQ、ArcGIS、TomTom 三家并行查询，新增独立 consensus API。每源一票，0.6 英里=965.6064m 为组内所有点对的直线距离上限。跨来源完全图筛选，重复组合合并仍严格检查并集直径，不做单链扩张。
- 明示文本关键词与品牌别名过滤的局限，三家原始候选、失败状态、排除原因均可查看。多区域保留选择，单源只待确认；三家支持优先，两家支持仍可用。
- 代表坐标为来源等权的实际候选 medoid，令牌保留；不同 postcode 不阻止合组；不要求入口精度。仅 Demo 接入，正式通勤流程暂未迁移。
- 定向测试共 34 项通过（新增 7 项，含并发屏障、单源失败隔离、真实 HTTP 控制器契约、直径/串联/同源/多区域/文本规则）。前端 build 通过，jar 已打包；真实三源联合页面验收尚待用户重启。

## 四门店联合查询与 Google Maps 人工核对

真实测试 M&S Woolwich / M&S Birstall / Costa Woolwich / Nando's Woolwich，逐一核对 Google 名称与地址。发现关键词过严漏票，增加非递归跨源坐标补证并回放实测字段测试；37项通过、前端构建通过、jar已打包。修正后真实请求尚待重启加载；详见 multi-provider-live-review.md。

## 自动区域采用及重启后复测

用户明确不让用户选区域。Demo 自动展示首组，三家支持优先、同支持数选最大距离更小组；后端新增 selectedCandidate 并把多组的状态统一为 supported。定向38项通过、前端构建通过、jar已打包；本次新增后端选点字段仍需下次重启加载。

Chrome 实测已加载坐标补证修复的后端：Costa Woolwich 自动展示三家支持的 SE18 6EU，51.49030,0.06940，最大距离约1米；Nando's Woolwich 三家支持 SE18 6LQ，51.49144,0.06678，最大约1米，原漏筛两票现已补证、排除0条。自动显示由新版前端采用后端已有排序首组，未将此记录为 selectedCandidate 新字段实测。

## Tesco Borough Rd Express 岗位标题验证

- 用户原文 Tesco Colleague - Borough Rd Express：联合查询无共同区域，LIQ 404、TT无匹配、ArcGIS返回 Tresco 的 Borough Road街道并被过滤。原文自动定位验收不通过。
- 对照 Tesco Borough Road Express：LIQ无匹配、ArcGIS仍错误街道、TT首项正确44 Borough Road SE1 0AJ，但只有单源支持，未形成共识。
- 进一步明确城市 Tesco Borough Road London：LIQ+TT形成两源共同区域，最大距离约1米，代表点51.49916,-0.10035，SE1 0AJ；ArcGIS返回街道均不投票。城市来自Google人工核对，不能记成系统原文自动识别成功。
- Google Maps 对应门店 Tesco Express，44A Borough Rd, London SE1 0AJ；可见详情URL坐标51.4992411,-0.1003728，与Demo四舍五入代表点约相距9.2米。官方门店链接由Google展示：https://www.tesco.com/store-locator/london/tesco-express-1db87b41-6a84-4fc3-ba2b-2cb6cab0ca5b 。
- 尚需改进：搜索前分离职位词 Colleague，处理 Rd/Road 等缩写，保留门店信息并从职位上下文读取城市；没有城市不能凭本例自动加London。此轮仅验证、未修改查询预处理逻辑。

## Tesco Shefford Esso Express 岗位样例验证

- 用户提供标题 Tesco Colleague - Shefford Esso Express，Location=Shefford，Working Hours=16（周工时，不是上下班时间）。完整标题联合查询：LIQ 一条、ArcGIS/TT无匹配；LIQ候选 Tesco Express, Ivel Road, Shefford, SG17 5UB，52.03145,-0.33288，但现解析标 locality 不投票，原文不能自动形成共识。
- 人工去除职位词后输入 Tesco Shefford Esso Express：三家各一条，ArcGIS+TomTom组成2家支持区域，代表点52.03122,-0.33283，显示最大相距0米（界面四舍五入）；ArcGIS邮编SG17 5LH，TT仅SG17。LIQ仍标locality未投票，其坐标距另外两点约26米，后续需检查上游类别映射，不可声称本例三家支持。
- Google Maps人工核对 Tesco Esso Express，Ivel Rd, Shefford SG17 5LH，官网链接 https://www.tesco.com/store-locator/shefford/ivel-rd 。街道表述不同但Shefford位置/完整邮编吻合，符合附近区域用途。Google未提取精确坐标，本例未算Google点到点误差。
- 结论：清理 Colleague 后可用；当前系统尚未自动完成岗位标题清洗。测试是对照验证，不把人工清理后的成功算成原始输入通过。

## Tesco Road Leigh On Sea Express 验证

- 用户职位 tesco/TP/18292748/1019109/0，Location Leigh On Sea。原文 Tesco Colleague - Road Leigh On Sea Express：无共识；LIQ错误返回Hampton KT8 1SE，ArcGIS仅Leigh-on-Sea区域，TT无匹配，均未用于自动选点。
- 人工仅去除 Colleague 后 Tesco Road Leigh On Sea Express：LIQ8/ArcGIS5/TT5候选，自动采用LIQ+TT两家支持区域，最大相距约2米，代表51.54750,0.66543，TT postcode SS9 3JY、LIQ SS9 3NQ；还有1781 London Road等别店，不能把此例推成无歧义算法保证。
- Google Maps列表包含1017 London Rd（51.5475024,0.6654343）及1781-1799 London Rd另一家；另用Tesco官方核对 https://www.tesco.com/store-locator/leigh-on-sea/1017-london-rd ，名称明确 Road Leigh On Sea Express，1017 London Rd, Leigh-on-Sea SS9 3JY，与自动选中的点吻合。
- 原始标题自动清洗尚未实现。此轮仅测试；清理后成功不能记作原文通过。官方岗位 https://apply.tesco-careers.com/members/modules/job/detail.php?record=1019109 同时确认用户提供职位引用，职位页面availability窗口并非实际shift。

## Tralee Square 爱尔兰边界样例

- 用户职位 Tesco Customer Assistant - Tralee Square，地址 Tesco Tralee Square, Main Street, Tralee, Co. Kerry。Demo 查询 Tesco Tralee Square Main Street Tralee Co Kerry：LIQ/TT无匹配，ArcGIS返回英国 Wales/Powys 的 Kerry 区域三条，均不可选且被过滤，未形成共识。不能将此判为通勤不合适。
- Google Maps核对 Tesco Express, Maine St, Tralee，坐标52.2679812,-9.7067879，官网 https://www.tesco.ie/store-locator/tralee/main-street ，爱尔兰共和国。当前provider固定GB，TfL路线范围更小，伦敦及周边，故不支持该岗位地区。
- 当前界面仍显示通用“暂未确定参考区域”，没有明确“地区不支持”状态；后续须补范围识别并区分服务范围与未知。此轮仅边界验证，未扩展爱尔兰搜索/路线能力。

## M&S Oatlands Harrogate 样例验证

- 用户提供 Store=Oatlands Harrogate Simply Food，Location=Harrogate, North Yorkshire。查询 M&S Oatlands Harrogate Simply Food：LIQ8候选（含Pannal门店及学校/公园等）、ArcGIS4（Oatlands POI及街道区域）、TT无匹配，无共识，未自动用错位置。
- 人工对照 Marks and Spencer Oatlands Harrogate：LIQ仅市中心HG1 1QE点且标locality；ArcGIS8含目标 HG2 8DS, 3 Beech Avenue,53.97637,-1.53234，及市中心/Pannal等其他分店；TT无匹配。没有足够跨来源支持，仍未自动定位。
- Google Maps核对目标 M&S Foodhall，Retail Park, Unit 1 Leeds Rd, Oatlands, Harrogate HG2 8DS，链接官网 https://www.marksandspencer.com/stores/harrogate-oatlands-simply-food-1312 。Google名称/邮编能辨别目标与HG1市中心、HG3 Pannal分店；本轮未读取Google精确坐标，不宣称精确距离验证。
- 结论：本例当前自动定位失败，目标在ArcGIS候选里但仅单源；不能放宽区域词强行选市中心点，也不能将一般Harrogate地点当作Oatlands。后续需设计查询标准化与带上下文的召回；当前未自动实现。

## M&S Orpington Nugent Park 伦敦样例

- 用户 Store Orpington Nugent Park，Location Orpington, Greater London。搜索 M&S Orpington Nugent Park：LIQ无匹配，ArcGIS返回同名M&S venue 51.39296,0.11189（BR5 3PU）；TT返回Nugent Retail Park 51.39283,0.11249（BR5 3FF）。两点约44米，但TT为园区而非同品牌，被现品牌规则排除，无自动共识。
- 品牌全称 Marks and Spencer Orpington Nugent Park：ArcGIS3条Marks & Spencer，51.39390/51.39375/51.39370,0.11280；LIQ/TT无匹配。三条同源不能计三票，且缺Nugent关键词无锚点，不采用。
- Google Maps核对 Marks & Spencer，Nugent Shopping Park,133-135 Cray Ave.,Widmore Road,Orpington BR5 3RP，官网 https://www.marksandspencer.com/stores/orpington-nugent-park-4747 。人工核对确认目标在Nugent园区，不把园区坐标声称为具体店坐标，也不因postcode不同判错。
- 结论：当前没有自动通过，但第一写法已获得门店+同名园区的近距离证据。用户接受附近区域，后续可将有关联名称的园区候选作为辅助空间证据，需独立规则与测试，不能把任何附近商家计成相同品牌支持。本轮未改筛选。

## Orpington 精简查询成功

用户指出 Marks & Spencer Orpington 即可。Chrome联合实测：LIQ1/ArcGIS5/TT4，自动采用三家支持的Nugent区域（6候选、每源仍1票），最大点对约22米，代表51.39385,0.11277，BR5 3RP。TT为BR5 3RD、ArcGIS仅BR5 3或无完整邮编；附近坐标符合要求。其它Crofton Road、Court Road两家支持区域仍在结果中，因支持数少未采用。本例精简查询成功，并不代表可普遍丢弃岗位门店名；后续查询策略需用原门店字段对结果重排和核对。

## M&S Bluewater 简短品牌+区域验证

用户门店Bluewater、Location Greenhithe Kent。Chrome联合查询 Marks & Spencer Bluewater：LIQ2、ArcGIS3、TT3，共8候选聚成三家支持区域，最大距离约75米，代表51.43822,0.27419，LIQ postcode DA9 9SD。Google Maps核对 Marks & Spencer, Bluewater Pkwy, Dartford, Greenhithe DA9 9SD，链接 https://www.marksandspencer.com/stores/bluewater-6460 。名称/区域/完整邮编吻合；本轮不测TfL路线覆盖，不将地点定位成功等同通勤可达。

## M&S Islington 简短查询验证

Chrome查询 Marks & Spencer Islington：LIQ2、ArcGIS8、TT无匹配；自动选两家支持区域，代表LIQ51.53330,-0.10710，5 Liverpool Road N1 0RR。Google Maps同名门店显示5 Liverpool Rd, London N1 0RR，官网 https://www.marksandspencer.com/stores/islington-0725 。区域包含4个候选，最大相距约864米（其中含ArcGIS King's Cross Bridge附近的其它候选），符合0.6英里直径规则但不能宣称全组为同一门店；代表点与目标一致。这也展示966米阈值可合入相邻不同分店的局限，未自行改变用户距离标准。

## 2026-10-29 · 50 个不同品牌门店的查询规则基准

- 采集 M&S/Tesco/Sainsbury’s/Boots/Greggs/Lidl/Co-op 共 50 个不同门店招聘样本，保留官方详情或列表来源与历史职位标记。
- /place-benchmark 比较四种规则，浏览器实跑 200 条观察，原始响应已导出至 docs/location-benchmark/results-2026-10-29.json；相同查询复用，未改正式地点／通勤／票价逻辑。
- 独立参考：官方招聘／门店邮编、Postcodes.io（46 个现行＋4 个历史），机场 J36 用明确匹配店号的 Google Maps 航站楼参考替换不适用的历史邮编中心。方法和限制逐项记录于 docs/location-benchmark/README.md。
- 结果（区域符合／超出／未知）：原岗位标题 0/0/50；简化品牌分店 27/3/20；原分店＋城市 23/0/27；简化分店＋城市 27/1/22。最后一组原始候选联合覆盖 48/50，不能当作自动匹配正确率。
- 发现 Waterloo 跨城市误选及 Wandsworth Road 同路多分店误选；明确下一步是筛选和歧义保护，不能直接把成功分组当成找对地点。
- 验证：200 条唯一 sample/rule、50 样本、7 品牌、200 CSV 行完整性通过；离线评分脚本通过；frontend npm run build（TypeScript＋Vite）通过。无后端代码改动，不重复运行无关后端套件。
# 2026-10-29：基于 50 门店实测优化分店歧义处理

- 同一查询命中 0.6 英里以外的同品牌候选时，即使首选候选有三家来源支持，也将整体标记为 `ambiguous` 并清空 `selectedCandidate`。这是自动停选，不增加用户手动挑选区域的步骤。
- 完整 UK postcode 只允许相同 postcode 的候选投票，避免错误地让附近或同品牌的其他邮编组成共识。
- 查询规范化忽略英文所有格撇号，并统一 `rd/road`、`ave/avenue`，减少 King's/Kings 等文本差异造成的漏筛。
- Demo 显示“最强共识候选（暂不自动采用）”和明确的歧义说明。
- 本轮范围仅限 `/place-search/consensus` 与地点搜索 Demo；正式通勤页 `PlacePicker` 不接入三源共识。Demo 实测 Wandsworth Road 返回歧义，Bluewater 三源支持且组内最大距离 75 米。
- 新增 Demo-only 文本回退：没有多源共识时，若品牌及街道/区域词在候选名称/地址中匹配，且匹配坐标集中在一个 0.6 英里范围内，则显示 `text-match` 低置信度候选；同一街道仍命中远处分店（如 Wandsworth Road）继续显示歧义。postcode 仍严格完全一致。
- 验证：`AreaConsensusServiceTest` 18 项通过；前端 `npm run build` 通过；后端 Maven 离线 `package -DskipTests` 通过。实时界面需重启 `start-local.sh` 加载新后端。

# 2026-10-29：保留唯一强共识区域，不被单个远点否决

- 修正 `ambiguous` 判定：一个远处、没有第二来源支持的候选不再否决领先的多源共识组；仍会列入排除项，说明它是未获交叉来源支持的远点。
- 若另有至少两个来源共同支持、且距领先区域超过 0.6 英里的竞争门店组，继续返回歧义，避免在两个真实候选组之间自动猜选。
- Sainsbury’s Wandsworth Road 的三源组相距 88 米时应保留为可用区域，SW8 3SB 的远处单一候选列为组外结果。
- 回归用例覆盖“三源紧密组 + 远处单候选仍可采用”和“两个远处分组继续歧义”。
- 验证：`AreaConsensusServiceTest` 18 项通过；前端 `npm run build` 通过；最新后端 Maven 离线打包通过。重启 `bash start-local.sh` 后加载修复。

# 2026-10-29：多个三源共识区域的通勤处理决定

- 用户决定：若出现两个相距超过 0.6 英里、且各自得到 LocationIQ、ArcGIS、TomTom 三家支持的区域，应保留两个区域的代表坐标，未来分别查询通勤并区分结果，不让用户先选择区域。
- 当前地点搜索 Demo 只用于比较候选，尚未提供出发地或触发通勤查询；现有通勤页面也尚未接入三源共识。本条是后续正式整合的需求记录，尚未实现分别查路线；本次未改变当前歧义判定或两源竞争区域的处理。
- 地点 Demo 现可展示相距超过 0.6 英里、各由三家支持的多个区域，以及每个区域的地址、postcode、代表坐标和来源；后端现有 `areas` 已包含全部区域，`selectedCandidate` 仍为空，避免将多个区域误表示为一个。独立查询通勤仍待后续实现。
- 验证：双三源区域的接口回归用例通过；`AreaConsensusServiceTest` 19 项通过，前端构建通过。使用模拟数据验证，尚未发现并复测真实的双三源案例。
- 按用户提醒，10 品牌实时复测使用已定规则“品牌名 + 分店名称/区域词”，不把品牌单独搜索当作门店定位测试。结果记录见 `docs/location-benchmark/live-10-brands-2026-10-29.md`：4 个单一区域确定、1 个多源共识伴随远处分店歧义、1 个查询返回两个三源区域、4 个未形成共识。该实测确认 McDonald's Woolwich 会返回两个三源代表坐标。

