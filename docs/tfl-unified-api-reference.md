# TfL Unified API Reference (v1) / TfL Unified API 参考

> 这份文档是项目内的使用索引和实现笔记，不是 TfL API 的替代规范。所有 endpoint、参数、枚举和响应 schema 以 TfL 在线 Swagger 为准；调用前应重新查阅在线定义。
>
> 本文依据的 Swagger 规范：[`https://api.tfl.gov.uk/swagger/docs/v1`](https://api.tfl.gov.uk/swagger/docs/v1)（Swagger 2.0，API version `v1`）。[TfL 官方 Swagger UI](https://api.tfl.gov.uk/swagger/ui/index.html)；用户提供的 [Petstore Swagger 查看器](https://petstore.swagger.io/?url=https://api.tfl.gov.uk/swagger/docs/v1#/AccidentStats) 是同一规范的浏览界面。

## 快速入口

- API 根地址：`https://api.tfl.gov.uk`
- 默认响应：JSON；规范也声明支持 XML 等响应格式，具体以 endpoint 为准。
- 在线规范是完整、最新的 endpoint / 参数 / schema 索引；项目不复制一份容易过期的 Swagger JSON。
- TfL 当前 API Portal 说明：匿名访问限制为每分钟 50 次；需要提高限额时注册并订阅产品，在请求中附加 `app_key`。旧材料可能还提到 `app_id`，实施认证时以 [TfL API Portal 当前说明](https://api-portal.tfl.gov.uk/) 为准。[产品限额说明](https://api-portal.tfl.gov.uk/products)
- API 使用需遵守 TfL 的 [开放数据条款与条件](https://tfl.gov.uk/info-for/open-data-users/)。

## API 分组和用途

下表覆盖 Swagger v1 的 API tag 分组。各组中的 route pattern 是阅读入口；某些组还有 meta、分页、过滤或旧版兼容操作，使用前从该组的在线 Swagger 展开确认。

| 分组 | 主要用途 | 常用 endpoint pattern |
|---|---|---|
| `AccidentStats` | 道路交通事故历史统计 | `/AccidentStats/{year}` |
| `AirQuality` | 空气质量数据 feed | `/AirQuality` |
| `BikePoint` | Santander Cycles 停车点和可用车辆/桩位 | `/BikePoint`、`/BikePoint/{id}`、`/BikePoint/Search?query=…` |
| `Cabwise` | 附近出租车、私人租车联系方式 | `/Cabwise/search?lat=…&lon=…` |
| `Journey` | 规划路线、查询可用交通方式 | `/Journey/Meta/Modes`、`/Journey/JourneyResults/{from}/to/{to}` |
| `Line` | 线路、线路状态、服务中断、到站预测、站点、路线序列和时刻表 | `/Line/{ids}`、`/Line/{ids}/Status`、`/Line/{ids}/Disruption`、`/Line/{ids}/Arrivals/{stopPointId}`、`/Line/{id}/StopPoints`、`/Line/{id}/Timetable/{fromStopPointId}`、`/Line/{id}/Route/Sequence/{direction}` 及 `Meta/*` |
| `Mode` | 交通方式服务类型、该方式全网到站预测 | `/Mode/ActiveServiceTypes`、`/Mode/{mode}/Arrivals` |
| `Occupancy` | 汽车停车场、电动车充电位、BikePoint 占用情况 | `/Occupancy/CarPark`、`/Occupancy/CarPark/{id}`、`/Occupancy/ChargeConnector`、`/Occupancy/ChargeConnector/{ids}`、`/Occupancy/BikePoints/{ids}` |
| `Place` | 地理地点、地点类型/属性、postcode 街道和地点搜索 | `/Place`、`/Place/{id}`、`/Place/Type/{types}`、`/Place/Search`、`/Place/Address/Streets/{Postcode}`、`/Place/Meta/*` |
| `Road` | TfL 管理道路、路况、封路和 disruption | `/Road`、`/Road/{ids}`、`/Road/{ids}/Status`、`/Road/{ids}/Disruption`、`/Road/all/Disruption/{disruptionIds}`、`/Road/Meta/*` |
| `Search` | TfL 网站内容及公交时刻表搜索 | `/Search?query=…`、`/Search/BusSchedules?query=…`、`/Search/Meta/*` |
| `StopPoint` | 车站/站点检索、地理搜索、到站预测、线路可达性、站点 disruption、周边设施 | `/StopPoint/{ids}`、`/StopPoint/Search/{query}` 或 `/StopPoint/Search?query=…`、`/StopPoint/Mode/{modes}`、`/StopPoint/{id}/Arrivals`、`/StopPoint/{id}/ArrivalDepartures`、`/StopPoint/{id}/CanReachOnLine/{lineId}`、`/StopPoint/{id}/Route`、`/StopPoint/{ids}/Disruption`、`/StopPoint/Meta/*` |
| `TravelTime` | 通勤时间地图图层及对比图层 | `/TravelTimes/overlay/{z}/mapcenter/{mapCenterLat}/{mapCenterLon}/pinlocation/{pinLat}/{pinLon}/dimensions/{width}/{height}`、`/TravelTimes/compareOverlay/{z}/…` |
| `Vehicle` | 指定车辆的实时预测 | `/Vehicle/{ids}/Arrivals` |

此 API 集合包含实时信息和计划/静态信息。响应是否实时、更新频率、可用交通方式和数据覆盖范围因 endpoint 而异；不能仅因 endpoint 属于 TfL API 就假定它表示实时或全网完整数据。TfL 对 Unified API 的总体介绍见[官方文档](https://tfl.gov.uk/info-for/open-data-users/unified-api)。

## Journey Planner：本项目当前使用

### 请求

`GET /Journey/JourneyResults/{from}/to/{to}`

`from`、`to` 可用英国 postcode、坐标、NaPTAN/ICS stop ID 或文本地点。文本可能需要地点消歧。重要 query 参数：

| 参数 | 用途 |
|---|---|
| `date` | `yyyyMMdd` 出行日期 |
| `time` | `HHmm` 24 小时时间 |
| `timeIs` | `Departing` 或 `Arriving` |
| `mode` | 交通方式筛选；Swagger 将它定义为数组，并说明可传逗号分隔的 mode 集合 |
| `journeyPreference` | `LeastInterchange`、`LeastTime`、`LeastWalking` |
| `accessibilityPreference` | 无楼梯、无电梯、step-free 等偏好；值以 Swagger 枚举为准 |
| `via` | 指定途经地点 |
| `maxWalkingMinutes` / `maxTransferMinutes` | 限制总体步行或换乘步行时间 |
| `walkingSpeed` | `Slow`、`Average`、`Fast` |
| `includeAlternativeRoutes` | 请求替代路线 |
| `useRealTimeLiveArrivals` | 对支持的情况请求实时到站数据；此开关本身不保证整个路线实时 |

示例（请 URL encode postcode 的空格，并按实际要筛选的交通方式调整 `mode`）：

```http
GET https://api.tfl.gov.uk/Journey/JourneyResults/DA18%204DE/to/Abbey%20Wood?date=20261027&time=1645&timeIs=Departing
```

### 响应结构和字段映射

- `journeys[]`：路线方案；路线总耗时是 journey 级 `duration`，起终时间是 `startDateTime`、`arrivalDateTime`。
- `journeys[].legs[]`：每段步行/交通；`mode.name` 是交通方式，`routeOptions[].name` 是线路/路线名称。二者可能表达相同名称，UI 应去重（例如 `elizabeth-line` + `Elizabeth line`）。
- `legs[].departurePoint`、`arrivalPoint`：该段的起点和终点。具体返回字段以 schema 为准。
- `legs[].departureTime`、`arrivalTime`、`duration`：该段时刻和分钟数。
- `legs[].path.stopPoints[]`：Swagger 定义为 Identifier 数组；行程响应中通常包含途经/到达 stop point。显示停靠数量时使用此数组项数，只有数组存在且非空才显示，不从时间、距离或文本推算。示例归档响应也实际包含该字段。
- `legs[].instruction.summary` / `detailed`：面向旅客的分段说明；不要把摘要解析成唯一的数据源。
- `journeys[].fare.totalCost`：提供方返回的路线级票价字段。`fare.fares[]` 的 `cost`、`chargeLevel` 等是票价项数据，不能当成 leg 单价相加。Swagger 把 `totalCost` 定义为 integer，但字段 schema 本身不承诺货币单位；单位和完整性要结合返回样本及 TfL 票价资料验证。
- 可选字段、disambiguation、无路线及错误响应均需按实际 JSON 结构处理；不可把缺失票价解释成免费，也不能把某个日期时间的 `chargeLevel` 自己推导出来。

### 地点检索：`Place` 与 Journey Planner 消歧的区别

Swagger 的 `Place` tag 中有两个与输入地点相关但用途不同的能力：

| 操作 | 用途与输入 | 响应/路线衔接 |
|---|---|---|
| `GET /Place/Search?name={text}&types={type,...}` | 按地点名称搜索 TfL Place 目录；`name` 必填，`types` 可选，为逗号分隔地点类型。 | 返回 `Place[]`；可用字段包括 `id`、`commonName`、`placeType`、`lat`、`lon`、`additionalProperties` 等。适合站点或 TfL 已登记地点的候选检索。 |
| `GET /StopPoint/Search/{query}?maxResults={n}` | 按站点 common name 或五位 Countdown bus stop code 搜索；匹配忽略大小写，前后自动套用 wildcard。`maxResults` 默认/最大 50，stop 层级子项可能使实际扁平结果超过 50。 | 返回 `SearchResponse.matches[]`。一般 `id` 是 NaPTAN 站点标识；枢纽结果也可能返回 `HUBKGX` 这类 HUB `id`，同时给出 `icsId`（例如 `1000129`）。Journey Planner 不接受该 HUB key，应传其 `icsId`；不能把每个搜索 `id` 不加判断地当成路线站点标识。 |
| `GET /Place/Address/Streets/{Postcode}` | 查询某 postcode 关联的街道集合。 | 不等同于把任意文字地址转成门牌号/坐标的地理编码服务。 |
| `GET /Journey/JourneyResults/{from}/to/{to}` | `from`/`to` 支持坐标、英国 postcode、NaPTAN StopPoint ID、ICS StopId 或自由文本；Swagger 说明自由文本通常会产生地点消歧（精确匹配兴趣点名称时例外）。 | 直接返回路线查询结果以及 Journey Planner 自己的起点/终点消歧字段。本项目现在读取这组候选并让用户选择。 |

重要边界：`Place/Search` 是 TfL 地点目录的名称搜索，不应视作通用英国地址/邮编地理编码。Place 响应有坐标，但 Place 的任意 `id` 是否可直接作为 Journey Planner 的 `from`/`to`，Swagger 的 Journey 参数并未承诺；需要时优先使用受支持的坐标、postcode、NaPTAN/ICS ID，或将原始文本交给 Journey Planner 消歧。不要只因 Place 返回了候选就假定它一定能规划出路线。

建议用途：输入停顿后由后端先请求 StopPoint Search；结果不足时用 Place Search 补充 TfL 已登记地点候选。postcode 不需要逐字搜索，直接交给 Journey Planner。候选选中后仍以 Journey Planner 是否能计算出路线作为最终验证。普通地址在 TfL 目录中未必存在，Journey Planner 的自由文本消歧作为补充路径。

### stops 的显示约定

目标呈现：

```text
Elizabeth line
Abbey Wood → Tottenham Court Road (N stops)
16:45 — 17:31 (46 min)
```

当前实现读取 `leg.path.stopPoints.length`，映射成 `JourneyLeg.stops`，并在交通段端点后显示。Walking 不显示 stops。TfL 的 `stopPoints` 是单独返回的站点数组；如果某个 journey response 没有提供 `path.stopPoints`，该段就没有可用的 stops 数字，界面不得伪造。先检查 Network 面板中 `/api/journeys` 原始响应是否有 `legs[].stops`；如果没有，需确认前端所连的后端已从该 Swagger path 映射并重启。

## 其他值得区分的 TfL 数据

- Journey Planner 的路线/行程票价：来自 `Journey/JourneyResults` 响应的 `journeys[].fare`。
- 线路状态和 disruptions：看 `Line`、`StopPoint` 相关 endpoints；它们描述服务状况，不等同于重新规划后的旅程方案。
- 到站预测：看 `StopPoint/{id}/Arrivals`、`Line/.../Arrivals`、`Mode/{mode}/Arrivals`；这些是站点/车辆层的预测，不是 Journey Planner 路线时刻的替代。
- timetable：`Line/{id}/Timetable/...` 提供线路/站点的时刻表数据，不一定能表达具体 door-to-door 的行程规划。
- **历史研究，不由应用调用：** `GET /StopPoint/{from}/FareTo/{to}` 不在当前提供的 TfL Swagger v1 `paths` 中；TfL 的独立 API 示例 PDF 将其标为 **Tube only**。用户于 2026-10-27 对该接口做了以下 live 查询：

| from → to | HTTP / body | 实测说明 |
|---|---|---|
| `HUBABW` → `HUBKGX` | 200 / `[]` | Hub → Hub 未返回票价记录。 |
| `910GABWD` → `HUBKGX` | 200 / `[]` | 起点具体站点 → 目的地 Hub 仍未返回票价记录。 |
| `910GABWD` → `940GZZLUKSX` | 200 / 有成人 PAYG 票价 | Off Peak £3.60、Peak £4.80；另一个替代票价为 Off Peak £5.90、Peak £7.30。 |
| `910GABWD` → `910GSTPX` | 200 / 非空响应 | 用户确认返回票价；本记录未提供金额。 |

空数组代表**接口没有返回可用票价记录**，不是 £0，也不能据此断定该站对没有票价。`/StopPoint/HUBABW` 的 children 映射给出 `910GABWD` = Abbey Wood (London) Rail Station、`910GABWDXR` = Abbey Wood（Elizabeth line）；King’s Cross 与 St Pancras 可分别使用 `940GZZLUKSX`（Tube）和 `910GSTPX`（National Rail）。用户确认的 `910GABWD → 940GZZLUKSX` 默认路线成人 PAYG 票价为 Off Peak £3.60、Peak £4.80；替代路线为 Off Peak £5.90、Peak £7.30。FareTo 是站点对报价，不是 leg 单价。**当前产品不调用 FareTo**：全程票价仅来自 Journey Planner 的路线级 `journeys[].fare.totalCost`；缺价时保留无可用票价状态，不拆分或相加 legs。

2026-10-27 历史后端日志曾确认 Walk + N1 + N73 选项请求 `/StopPoint/490G000957/FareTo/940GZZLUKSX`，TfL 返回 HTTP 200 和原始正文 `[]`；此调用逻辑已于 2026-10-28 从应用移除。完整历史记录见 [acceptance](../specs/002-postcode-journey/acceptance.md)。
- 当前 Swagger v1 的 schema 直接确认：`Journey.fare`（`JourneyFare`）在 itinerary / journey 级；`JourneyPlanner.Leg` 没有 fare 属性。`JourneyFare` 有 `totalCost`、`fares[]`、`caveats[]`；其中 `Fare` 有 `cost`、`chargeLevel`、zone/profile 字段及 `taps[]`。`FareTap` 只包含 `atcoCode` 和 tap 的 mode / validation / host device / 时间等详情，不含金额，也没有定义与 `legs[]` 的关联。故这些 fare items/taps 不能解释为逐 leg 价格。若 `journeys[].fare` 缺失，不能从 leg 自行拆价或把 Tube 与 National Rail 分段报价相加为总价。

### 混合 Tube / National Rail 票价与外部数据源（2026-10-27）

- National Rail Online Journey Planner 的 RTJP SOAP feed 可提供任意两站间或 multi-leg 行程规划，源引擎也用于计算票价和票可用性；但 RTJP 当前受正式许可约束、按成本回收收费，National Rail 说明旧申请表已停止受理，计划于 2026 年 11 月迁移到 Rail Data Marketplace。接入前需确认可申请状态、费用、许可用途及对商用/售票的限制。
- Rail Data Marketplace 上 BR Fares Easy Fares API 可查询任意两个地点的单程/往返铁路票价，涵盖 National Rail fares database 中 walk-up、PAYG 等类别；属于收费订阅（页面列出的中量方案 £500/月 + 每 5,000 次 £50，未含 VAT），并非免费 API。其说明也指出它不连接实时订座服务，不能核实受配额控制的票种当前是否可售。
- 免费替代路径（非实时查询 API）：TfL 的 [Single fare finder 网页](https://tfl.gov.uk/fares/find-fares/single-fare-finder) 可查询任意两站之间的单次 rail PAYG 票价，覆盖 Tube、DLR、London Overground、Elizabeth line 和接受 PAYG 的 National Rail。TfL 说明票价可能随线路、方向、日期及时段变化，且此工具只显示单次 rail journey；当前公开 Unified API Swagger/Portal 没有对应的公开查询操作。因此可供用户手工核价或做小规模人工验证，不应直接假设能作为稳定、受支持的后端 API 接入。
- 实测参考：2026-10-27 在 TfL Single Fare Finder 选择 Abbey Wood (Elizabeth line) → King's Cross St. Pancras Underground Station、Adult 后，主 Oyster/Contactless PAYG 票价为 Peak £4.80（工作日早高峰），Off Peak £3.60（其余时段及公众假期）。需注意晚高峰例外：工作日 16:00–18:59 在 Zone 1 外 touch in、最终在 Zone 1 touch out 的行程按 Off Peak 收费。因此本次用户给出的 18:36 Abbey Wood → King's Cross 路线对应 **£3.60 Off Peak**。另列出经特定 National Rail/London Underground 换乘站的 alternative fares。详情与查询上下文记在 `specs/002-postcode-journey/acceptance.md`。这是按站点对查出的整程票价，不能据此拆成 legs 的价格。
- 另一个低成本研究选择是 RDG/National Rail Data Portal 上供 proof-of-concept / development 使用的静态 fares feed；官方说明其每年发布三次，数据为技术性文件并需按相关条款使用。它不是实时 HTTP 查询接口；如要用于产品，需要解析 fares/routeing 数据并核对授权、更新和产品用途，成本是工程复杂度和数据时效，而非 Easy Fares 的逐次 API 查询费用。
- 这些 National Rail 产品面向铁路站间票价/旅程，不自动等于某条 TfL 混合 journey 中的实际 leg 应付金额。跨运营方换乘、PAYG/Contactless、through fare 与票价有效条件都可能使“逐段查价后相加”失真；只有数据源明确返回与完整行程相符的总价时才显示全程总价。
- 项目中的 Abbey Wood → King's Cross 示例尤其不能简单拆价：TfL 给出的 Abbey Wood → Barbican 独立查询和 Barbican → King's Cross 独立查询是各自的全程报价；TfL 站点信息还提到 Barbican 西行站台与 Farringdon Elizabeth line 之间有站内无障碍连接，路线中的 `Walk` 本身不足以证明乘客在 Barbican 出站/重新进站。必须知道实际刷卡/站内换乘方式，才能判断分段总和是否对应实际费用。
- 参考： [National Rail OJP feeds](https://www.nationalrail.co.uk/developers/online-journey-planner-data-feeds/)、[RDG fares data access](https://rdg.webx.host/our-services/essential-services/customer-ticket-sales.html)、[BR Fares Easy Fares API](https://raildata.org.uk/dataProduct/P-e4dc44fd-f4d8-4ace-b859-ff46366a5d00/overview)、[TfL Single Fare Finder](https://tfl.gov.uk/fares/find-fares/single-fare-finder)、[TfL API 示例](https://content.tfl.gov.uk/example-api-requests.pdf)。

### TfL API Portal 的 Journey Planner 操作详情（2026-10-27）

用户提供的 [API Portal Journey operation](https://api-portal.tfl.gov.uk/api-details#api=Journey&operation=Journey_JourneyResultsByPathFromPathToQueryViaQueryNationalSearchQueryDateQu) 对应 `GET /Journey/JourneyResults/{from}/to/{to}`，属于路线规划查询，不是针对 leg 的票价 endpoint。页面展开的请求参数包括地点、via、`nationalSearch`、date/time/timeIs、journeyPreference、mode、accessibilityPreference，以及步行、骑行、多模式等选项；没有单独票价或逐段票价查询参数。

该 operation 的 `200` 响应是 `ItineraryResult`，包含 `journeys[]`。Portal 定义中每个 `Journey` 有 `legs[]` 和 `fare`；`fare` 引用 `JourneyFare`（`totalCost`、`fares[]`、`caveats[]`）。单个 `Leg` 的 schema 包含时长、说明、起终点、path、routeOptions、mode 等，但没有 `fare` 属性。因此 API Portal 与 Swagger v1 一致：有路线级票价结构，没有 leg 级金额字段。此 Portal operation 详情不能据此推断 fare 一定会返回；实时响应仍可能把 fare 缺省或置空。

### TfL 官网“Find fares”工具与可计算范围（2026-10-27）

官网把以下项目列成不同票价工具；各页面返回价格/规则，不代表都提供可集成的公开 API：

| TfL 工具 | 能解决的问题 | 与单条 Journey Planner 路线总价的关系 |
|---|---|---|
| [Single Fare Finder](https://tfl.gov.uk/fares/find-fares/single-fare-finder) | 接受 PAYG 的两站之间单次 rail 行程；票价可能受线路、方向、时间、星期影响。当前官网实测 Abbey Wood (Elizabeth line) → King's Cross St. Pancras Underground Station 成人 Oyster/Contactless：Peak £4.80（工作日早高峰），Off Peak £3.60（其他时段及公众假期）。工作日 16:00–18:59 从 Zone 1 外出发并在 Zone 1 结束也按 Off Peak 收费；另有经指定 National Rail/Underground 换乘点的 alternative fare。 | 唯一明确按两站查单程价的网页工具；结果是这对站点的一次完整单程价，可作为同一 PAYG 站点对的权威核价参考。该网页查询不等于 Journey Planner `legs[]` 逐段报价；当前公开 Swagger/Portal 未列出其独立 API。 |
| [Caps and Travelcard prices](https://tfl.gov.uk/fares/find-fares/caps-and-travelcard-prices) / [Tube and Rail fares](https://tfl.gov.uk/fares/find-fares/tube-and-rail-fares) | 按 zone 组合、乘客票种展示 PAYG cap 与 Travelcard 价格；官网明确具体站点对请用 Single Fare Finder。 | 是区域上限/通票比较，不是任意多 leg itinerary 的计算器。 |
| [Fare capping](https://tfl.gov.uk/fares/find-fares/capping) | 按实际刷卡记录、经过 zones 和触碰时间，计算每日（04:30–次日 04:29）或周一至周日 PAYG 封顶；需同一 Oyster/contactless 卡或设备。 | 依赖用户整日/整周的真实行程和付款介质，无法从一条计划路线单独算出最终封顶扣费。 |
| [Bus and tram fares](https://tfl.gov.uk/fares/find-fares/bus-and-tram-fares) | 巴士/电车单次 PAYG、Hopper、Bus & Tram Pass 与 cap。 | 适用于该类服务的票价规则，不是 rail itinerary 的通用总价接口。 |
| [London Cable Car fares](https://tfl.gov.uk/fares/find-fares/ifs-cloud-cable-car-fares) | 缆车单程/往返、在线/现场/体验票价；PAYG 乘坐不计入 TfL caps。 | 独立服务/票种价格。 |
| [River fares](https://tfl.gov.uk/fares/find-fares/river-fares) | River 服务自身的票种和 fare zones；部分服务 Travelcard 可享折扣。 | 独立服务价格，不是地铁/铁路行程计价接口。 |
| [Fares beyond Zone 9](https://tfl.gov.uk/fares/find-fares/fares-beyond-zone-9) | 适用站点的 National Rail PAYG fares、caps、Travelcards；单次站点对仍导向 Single Fare Finder，Contactless-only cap 需看 National Rail。 | Zone 9 外的铁路票价/封顶说明，不是 Journey Planner leg fare API。 |

结论边界：从公开产品行为看，Single Fare Finder 与 Journey Planner 是分开的用户查询能力；Journey Planner operation 的公开响应 schema 有可空/缺失的 itinerary-level fare，而 Fare Finder 对明确的站点对能给票价。公开文档**没有披露**二者内部是否连接不同 API、共享同一 fare engine 或通过其他服务组合，因此不能仅凭页面行为断言底层实现不同。可确认的是：Fare Finder 没有出现在当前 Unified API Swagger/Portal 的公开操作列表中，不应当作已支持的集成 API。

### Abbey Wood → King's Cross：Elizabeth line + Liverpool Street/Moorgate 换乘票价分析（2026-10-27）

用户提供的候选路线是 Abbey Wood → Liverpool Street（Elizabeth line），步行换乘到 Moorgate，再乘 Northern line 到 King's Cross St. Pancras。对该例做可复核的初步推导：

- 2026-10-27 是周二，用户给出的 18:36 touch in 在一般工作日晚高峰时段内；但本行程从 Zone 1 外（Abbey Wood，Zone 4）进入并在 Zone 1（King's Cross）结束，TfL PAYG 规则对这种晚高峰进 Zone 1 行程规定收 Off Peak。关键输入是起点 touch-in 时间和最终 touch-out 区域，不是列车到达 King's Cross 的 19:07。
- Abbey Wood 在 Zone 4，Liverpool Street、Moorgate、King's Cross 均在 Zone 1；本路线触及 Zones 4、3、2、1，共覆盖含 Zone 1 的四个区。2026 成人 TfL rail PAYG 4-zone off-peak 档为 **£3.60**。£4.80 是相同区域跨度的 peak 价，不适用于这个“晚高峰从 Zone 1 外进 Zone 1”的例子。
- 该 £4.80 应理解为整段 PAYG rail journey 的票价，不是 Elizabeth line 票价加 Northern line 票价。路线文字里的 `Walk` 只描述换乘，不会自动产生一笔独立票价；实际扣费取决于乘客是否在换乘中触碰出站/进站、是否适用相应的站内或站外换乘规则，以及是否始终使用同一张卡/设备。若实际出站、重进站或换了支付卡/设备，实际计费可能不同。
- 可由公开资料确认的是票价结果、高峰时间和各站所属 zone；TfL 没公开 Single Fare Finder 的内部计算公式，也没公开说明它如何把该具体路线映射到票价档。因此“按 Zones 1–4 Peak 档解释 £4.80”是与官方 Fare Finder 结果及票价表相符的推断，并非对其内部算法的完整复刻。Fare Finder 自身也说明价格可能因线路、方向、时段和星期不同。
- 该票价不含用户当天其他乘车记录对 daily cap 的影响；封顶后的实际新增扣费可能更低。

参考：[TfL Single Fare Finder](https://tfl.gov.uk/fares/find-fares/single-fare-finder)、[TfL PAYG 支付说明](https://tfl.gov.uk/fares/ways-to-pay/pay-as-you-go)、[TfL Out-of-station interchanges](https://tfl.gov.uk/corporate/publications-and-reports/out-of-station-interchanges)、[2026 TfL fare changes and peak periods](https://www.london.gov.uk/media/111829/download)。

### TfL PAYG 实际计费机制（依据 TfL FOI 披露，2026-10-27）

本节记录收费系统的公开逻辑，避免把 Fare Finder 页面误当成计价公式：

- TfL 说明，PAYG 系统通过 touch in / touch out 识别起点与终点；每个站点对有默认路线，通常按默认路线收费。
- 如卡/设备在行程中间被网络通过相关 OSI 或粉色 route validator 识别，系统可据此确认乘客走了另一条受支持路线，并改用相应票价；没有识别到中间验证时，应用默认路线。无关的 OSI/pink-reader 验证不改变票价，粉色读卡器不应增加票价。
- TfL 仅概括说多数票价取决于所用交通方式和经过的 zones，部分站点对是单独定价。TfL 没有公开每个站点对默认路线的完整表和精确的计费规则，称进一步细节涉及收入保护。
- 因而 Abbey Wood → King's Cross 这一例的独立计算步骤应是：确认起终站 touch pair；确认 EL+Underground 属适用的 TfL 票价服务组合；根据被系统识别的默认/替代路线确定计价带和 zones；根据 touch-in 时间判断峰/非峰；查 2026 对应的 PAYG 票价表；最后才应用该付款 token 当天已有 journey 对 cap 的影响。它不是“每个 leg 查价后相加”。
- 对用户给出的 18:36 weekday 路线，touch in 时刻处于一般晚高峰窗口，但该行程在 Zone 1 外开始、Zone 1 内结束，适用晚高峰入 Zone 1 的 Off Peak 规则；公开成人 TfL rail PAYG Zone 1–4 off-peak 档是 £3.60。无需知道或调用 Fare Finder 的 UI 才能从公开规则和 fare table 推导该金额。Fare Finder 可用于核对站点对结果，但不是公式。
- Liverpool Street Elizabeth line 与 Moorgate Underground 之间存在地下通道连接；但这项物理连通证据本身不能证明这条具体路径是否在付费区内、是否经过闸机，或乘客是否需要验证票卡。Journey Planner 的 `Walk` 只表示步行 leg。应该用 Stop Structure topology 检查具体 area/footpath 连线；在缺少 paid-area / gateline 字段时，仍不能从 topology 单独推断刷卡/计费行为。

参考：[TfL FOI：Rules regarding PAYG charge for a single rail journey](https://tfl.gov.uk/corporate/transparency/freedom-of-information/foi-request-detail?referenceId=FOI-1357-2425)、[TfL Staff Guide：Peak/Off-Peak PAYG 与晚高峰进 Zone 1 规则](https://foi.tfl.gov.uk/FOI-3904-2425/Book%202%20Ways%20to%20Pay%20-%20March%202025_Redacted.pdf)、[TfL Moorgate/Elizabeth line 站体图](https://foi.tfl.gov.uk/FOI-2058-2425/Moorgate%20LU%20Elizabeth%20Line%20Axo%20Drawings.pdf)、[National Rail Moorgate 站点信息](https://railmap.nationalrail.co.uk/backend/public/station/MOG)、[TfL 如何正确支付票价](https://tfl.gov.uk/fares/pay-the-right-fare)、[TfL/City Hall 2026 PAYG 票价表](https://www.london.gov.uk/md3464-march-2026-transport-london-fare-changes)。

### Stop Structure：判断站内步行连接的资料源（2026-10-27）

- TfL 官方 Open Data 页面将 Stop Structure 定义为站点拓扑 API，包含 entrance、concourse、ticket hall、platform 等 area，以及 area 之间可能的 interchange/footpath 连接。它比 Journey Planner 单独的 `Walk` leg 更适合判定两站/两平台之间是否有站内路径。
- API endpoint：`GET https://api.tfl.gov.uk/jp_public/api10/XML_STOPSTRUCTURE_REQUEST`。请求需要 `app_key` 与站点 `sSStopNr`（ATCO/NaPTAN ID 或 Journey Planner `icsCode`）；`sSInclSP=1` 可带出 stop points，`sSInclSL=1` 可带出 serving lines，`sSESM=0` 可包含周边站点和连接到周边站点的 footpaths。响应以 `stopArea` 描述区域，以 `footpathInfo` 描述连接的 duration/distance，以 `footpathPartInfo.type` 描述 walkway、stairs、escalator、elevator、ramp 等路径类型。
- TfL `StopPoint/Search/Moorgate?maxResults=20` 返回枢纽结果 `id=HUBZMG`, `name=Moorgate`, `icsId=1000149`, `modes=[tube,bus,national-rail]`；这是用于 Stop Structure 重试的 Journey Planner Stop ID。不要把 `HUBZMG` 枢纽 ID 当作结构查询 ID。TfL 站点页另外确认 Moorgate Underground 的 NaPTAN/ATCO ID 为 `940GZZLUMGT`；Stop Structure 文档允许 NaPTAN 或 Journey Planner Stop ID 两种格式。
- 后续仅用 Moorgate `icsId=1000149` 发送最简 Stop Structure 请求；当前执行环境在 DNS 阶段无法解析 `api.tfl.gov.uk`，请求尚未到达 TfL。项目已有的旧 ATCO 响应文件 `stop-structure-940gzzlumgt.xml` 虽 HTTP 200，但 `stopAreas`、`footpaths` 均为空。因此 Moorgate 自身结构尚未取到。
- 用户本机随后以 `sSStopNr=1000149` 成功取得 Moorgate 响应：28 个站区、119 条 footpath、无 XML error。119 条中 110 条连接在 Moorgate StopPoint 内、6 条连接到相邻 `London Wall / Moorgate Station` bus stop（`1002153`）、3 条连接到 Liverpool Street (`1000138`)。Liverpool Street 三条分别是：Moorgate Bus area 1 → Liverpool Street BookH area 1，5 分钟 `STAIRS`；Moorgate area 24 `MMEL`（level -6）→ Liverpool Street `EL EB` area 19 和 `EL WB` area 23（都 level -6），各 1 分钟 `LEVEL`。这份 Moorgate 响应没有 National Rail `RLY` area 的 footpath 端点；它只列出 Liverpool Street 的 BookH 与 Elizabeth line areas，因此当前数据不能声称存在 Moorgate ↔ Liverpool Street National Rail 的直接 footpath。逐条列表见 [`stop-structure-moorgate-footpaths.md`](stop-structure-moorgate-footpaths.md)。
- 对“9 条跨 StopPoint footpath 是否需要出站”的解释：6 条的另一端是 `London Wall / Moorgate Station` 的街面 bus stop，物理上需要到站外公交站；另 1 条从 Moorgate bus stop 连到 Liverpool Street BookH（ticket hall），也不是单纯两个地下站区之间的换乘。剩余 2 条是 Moorgate `MMEL` 到 Liverpool Street Elizabeth line `EL EB/EL WB` 的地下 `LEVEL` 连接。Stop Structure 不含 paid/unpaid area、gateline 或 touch-out 字段，故不能据此判断是否需要经过闸机/刷卡出站；这里只能依据端点和路径类型判断是否涉及街面公交站。
- TfL `StopPoint/Search/Liverpool Street?maxResults=20` 返回主结果 `id=HUBLST`, `name=Liverpool Street`, `icsId=1000138`, `modes=[tube,bus,elizabeth-line,national-rail,overground]`。`HUBLST` 是 Hub 聚合 ID，不是 Stop Structure 文档列出的 ATCO 或 Journey Planner Stop ID；该查询应使用 `icsId=1000138`。搜索共返回 7 项，其余是方向不同的 Liverpool Street 周边 bus stop，不应误选作铁路站。
- Liverpool Street 的 Stop Structure 请求以 `sSStopNr=1000138` 成功返回 29 个站区、109 条 footpath、无 XML error。筛选同时连接 `stopID=1000138` 与 `1000149` 的 footpath，得到 3 条跨站连接：Liverpool Street 主站区 area 1 → Moorgate area 1，5 分钟 `STAIRS`；Liverpool Street Elizabeth line 东/西行站区 area 19/23（均 level -6）→ Moorgate area 24（level -6），各有 1 分钟 `LEVEL` 连接。位置字段的 `gid` 分别为 Liverpool Street `940GZZLULVT` 与 Moorgate `910GMRGT`。这证明 Stop Structure 数据明确编码了 Liverpool Street Elizabeth line 与 Moorgate 之间的地下步行连通路径；不证明该路径是否完全在付费区内、是否免出闸或如何计费。
- 后续按 Liverpool Street National Rail 查询检查最简 XML：National Rail area（`stopID=1000138`, area `15`, `areaName=RLY`, `areaGid=9100LIVST`）在该响应的 footpath 图中没有邻接边；另一个 `RLY` area 24 的 `areaGid=9100LIVSTLL` 也没有邻接边。该文件未设置 `sSOnlyDF=0`，所以不能据此证明不存在需经中间 area 的 footpath。用户本机随后运行完整参数请求，虽 HTTP 200，响应体却是 `BROKER -100 unknown error`，没有站区/footpath，且覆盖了此前成功的最简 XML。现已修复脚本：XML 应用错误另存为 `*-error.xml`，不再覆盖成功文件；新增 `--all-footpaths` 诊断模式，只额外设置 `sSOnlyDF=0`，但用户本机重试仍得到 `BROKER -100 unknown error`。无论以完整参数还是只设 `sSOnlyDF=0`，该站查询均未提供全量连接图；目前只能报告最简响应未列出 National Rail 的直接连接，不能断言不存在间接路径。
- 获取 key：在 [TfL API Portal](https://api-portal.tfl.gov.uk/) 注册并验证邮箱，登录后到 Products 订阅 `500 requests per minute` product，再到 Profile 复制 subscription key。Portal 当前要求将 key 作为 `app_key` 查询参数；不再需要 `app_id`。项目本地凭据变量名是 `TFL_APP_KEY`，key 不应提交到代码仓库或发在聊天中。
- 用户已完成 TfL Portal 订阅并配置本地 key；本轮有认证请求尝试，但执行环境 shell 无法解析 TfL 主机名，浏览器也拒绝读取本地 `.env` 路径。未获取 live payload；待在可直连网络的本机终端执行请求后再分析，不能绕过浏览器安全策略。
- 用户在本机运行请求时，Python 3.10 报 `SSLCertVerificationError: unable to get local issuer certificate`，请求尚未到达 TfL。该解释器已有 `certifi` CA bundle；重试时可用 `ssl.create_default_context(cafile=certifi.where())` 并传给 `urlopen(context=...)`，保留 TLS 证书校验，不能通过关闭验证来规避。
- 用户复制长 heredoc 时终端粘贴内容发生混杂，造成 SyntaxError；已整理成 `scripts/fetch_tfl_stop_structure.py`，运行 `python3 scripts/fetch_tfl_stop_structure.py` 即从本地 `.env` 读取 key 并保存原始 Moorgate XML 到 `docs/stop-structure-moorgate.xml`，不输出 key。
- 本地脚本首次成功建立 TLS 后，TfL 返回 HTTP 403 Forbidden。脚本已调整为在认证错误时打印 TfL 响应正文（不打印请求 URL/key），用于判断订阅 key 与 JPlive Public API 的授权问题；尚未取得 payload。
- 后续本机重试返回 HTTP 403，正文为 `error code: 1010`。这是 Cloudflare 的客户端/浏览器签名拦截错误，表示上游安全层按请求客户端特征拒绝访问；它本身不能证明 subscription key 错误，也没有返回 Stop Structure 数据。脚本现会识别该错误并提示走 TfL API Portal 支持渠道。不要通过伪造浏览器身份或关闭 TLS 校验来规避拦截；需要 TfL/API 服务方确认并处理访问限制后才能继续获取 payload。
- 依据后续排查建议，抓取脚本现使用明确的 `User-Agent: CommuteFit/1.0`，并在 HTTP 错误中显示 `cf-ray` 响应头（若存在）；请求仍将 `app_key` 作为查询参数传递，不会打印 key。当前受限执行环境重跑时 DNS 无法解析 `api.tfl.gov.uk`，所以尚不能验证显式 User-Agent 是否能解除用户本机的 1010，也不能提供新的 cf-ray。可在原本能访问 TfL 的本机终端重跑脚本；若仍 1010，将请求时间、完整错误和 cf-ray（若出现）交给 TfL API 支持。
- 用户本机随后得到 HTTP 200，并保存 802-byte XML；检查后发现这不是有效拓扑，而是响应体内 `<itdMessage type="error" module="BROKER" code="-1">unknown error</itdMessage>`，且 `stopAreaLinesSeq`、`stopPoints`、`footpathInfos` 都为空。因此只能确认 HTTP 层请求成功，不能说 Moorgate 数据已成功取回，也不能据此判断 Moorgate/Liverpool Street 的内部连接。脚本现会检查 XML 的应用级错误并明确报出；可选传入站点 ID，以 TfL 文档的 St James’s Park 示例 `940GZZLUSJP` 作对照：`python3 scripts/fetch_tfl_stop_structure.py 940GZZLUSJP`。对照结果可帮助区分 Moorgate 查询特定问题与 Stop Structure 服务端问题，但本身不改变目标站点的证据状态。
- St James’s Park 对照请求成功返回实际拓扑（HTTP 200、13 个 `stopArea`、83 个 `footpathInfo`，无 XML error），说明 key、endpoint、XML 数据路径以及完整参数组合能够工作。Moorgate 的 `940GZZLUMGT` 已由 TfL 站点页面确认是其 NaPTAN ID，且是 Stop Structure 文档支持的输入类型；该 ID 请求响应体仍是 Broker unknown error。随后 StopPoint Search 找到了另一种官方返回的 Moorgate 标识：`icsId=1000149`；下一步应以它重试最简请求：`python3 scripts/fetch_tfl_stop_structure.py 1000149 --minimal`。目前仍无 Moorgate topology 证据。
- 公开 schema 没有 documented 的 paid/unpaid-area、ticket barrier/gateline 或“是否需要出闸”字段。即使 topology 证明两个 area 有 footpath，也只能确认物理连接，不能据此确定该 path 是否在付费区或计费上算同一验证过程。
- 因此 Journey Planner 中两段公共交通之间的 `Walk` 不能一概称作站内换乘。先用 Stop Structure 检查 area/footpath 图；对 Liverpool Street Elizabeth line → Moorgate Northern line，另有 TfL 站体图证明两站体之间有地下连接，但仍需单独核实该具体 path 的付费区/闸机边界，不能由连通性推断。

参考：[TfL Stop Structure API 文档与示例](https://techforum.tfl.gov.uk/t/stop-structure-station-and-stop-topology-api/2796)、[TfL Open Data：Station and stop topology](https://tfl.gov.uk/info-for/open-data-users/our-open-data?intcmp=3671)、[TfL Moorgate/Elizabeth line 站体图](https://foi.tfl.gov.uk/FOI-2058-2425/Moorgate%20LU%20Elizabeth%20Line%20Axo%20Drawings.pdf)。

## 开发约定

1. 先查本文件和 [官方 Swagger JSON](https://api.tfl.gov.uk/swagger/docs/v1)，以在线 schema 核对 path、HTTP verb、参数大小写、数组编码、字段可空性和响应形状。
2. 项目调用放在后端；API key（若订阅后使用）放后端环境变量，不能进入前端、源码示例、日志或提交记录。
3. 对 JSON 用树/DTO 按字段做防御式解析。Swagger 里的 nullable/optional 字段不保证每次响应都出现；新字段可能被 TfL 增加。
4. 对可能变化的数据标出查询时间/数据来源；Scheduled journey 和 live arrival predictions 是不同数据产品。
5. 变更某个 endpoint 的使用或字段映射时，在此文件“项目当前映射”部分更新日期，并同步项目功能 spec / changelog。
6. TfL 标识/署名应符合当前 TfL open data terms；不要暗示应用由 TfL 背书。

## 本项目当前映射

| 功能 | TfL 操作 | 实现位置 |
|---|---|---|
| postcode 解析/地点消歧 | `Journey/JourneyResults/{from}/to/{to}` 的地点输入与 disambiguation 响应 | `backend/src/main/java/uk/co/commutefit/journey/tfl/TflJourneyService.java` |
| 输入联想站点/地点 | `StopPoint/Search/{query}`；当站点候选不足时用 `Place/Search?name=...` 补充；Place 坐标与 StopPoint ID 保存为短期 token | `backend/src/main/java/uk/co/commutefit/journey/tfl/LocationSuggestionService.java`、`LocationSuggestionController.java` |
| 前端联想下拉 | 本地 `GET /api/locations?query=...`，500ms 防抖，最多 8 项；postcode-like 查询跳过 | `frontend/src/api/client.ts`、`frontend/src/App.tsx` |
| 查路线和全程总价 | `Journey/JourneyResults/{from}/to/{to}` | `backend/src/main/java/uk/co/commutefit/journey/lookup/JourneyLookupService.java` |
| 路线票价项、charge level | Journey `fare.totalCost`、`fare.fares[]` | `backend/src/main/java/uk/co/commutefit/journey/lookup/JourneyLookupService.java` |
| 前端结果 | 本地标准化 Journey API 响应 | `frontend/src/App.tsx`、`frontend/src/api/client.ts` |

## Changelog

- 2026-10-27: 首次建立。记录用户提供的 Petstore Viewer 链接、TfL 官方在线 Swagger 规范、Unified API 分组、Journey Planner 请求/响应要点和当前项目字段映射。
- 2026-10-27: 查阅 Swagger `Place` tag。补充 `/Place/Search`、postcode 街道查询、Place 响应字段，以及它们与 Journey Planner 自由文本消歧的适用边界；记录 Place API 尚未接入当前代码。
- 2026-10-27: 加入 TfL 输入联想。补记 `StopPoint/Search/{query}` 的 common-name 搜索、`SearchResponse.matches[]` 字段及 maxResults 规则；实现 `/api/locations` 代理、Place Search 补充、候选 token 到 StopPoint ID/Place 坐标的路由映射。实测发现 StopPoint Search 的枢纽 `HUB` id 不可直接用于规划，需改用同项的 `icsId`。
- 2026-10-27: 调查混合 Tube / National Rail 行程缺价。归档样例中 TfL 返回了 rail legs 但 journey-level `fare` 为 null，legs 无独立 fare 字段；记录 TfL FareTo 仅 Tube 的边界，以及 National Rail RTJP / BR Fares API 的许可和费用，明确不可将独立 leg 票价简单相加。
- 2026-10-27: 按 TfL Swagger v1 schema 复核 fares。确认 `Journey.fare` 是路线级，`Leg` 无 fare 字段，FareTap 仅描述刷卡详情、不关联 leg；Swagger 当前 `paths` 没有 FareTo 操作，FareTo 仅见于 TfL 示例 PDF 且标为 Tube only，需验证其当前可用性。
- 2026-10-27: 调查 National Rail 付费票价 API 的免费替代。记录 TfL Single Fare Finder 对 PAYG Elizabeth line / National Rail 的人工查价覆盖，以及 RDG 每年三次更新的开发用静态 fares feed；两者均不能直接提供混合 TfL itinerary 的逐 leg 实际分摊价。
- 2026-10-27: 梳理官网 Find fares 页面各子工具的范围。区分按站点对查询单程 fare、按 zone 查询 cap/Travelcard、依赖账户实际刷卡轨迹的 fare capping，以及 bus/tram、River、Cable Car、Zone 9 外票价规则；说明官网页面展示票价不证明存在公开 API，并补充目前没有公开材料确认 Journey Planner 与 Fare Finder 的内部 API/引擎关系。
- 2026-10-27: 初次分析 Abbey Wood–Liverpool Street–Moorgate–King's Cross 时误将一般 PM peak 时段直接套用，得出 £4.80；后续依据 TfL Staff Guide 更正：Zone 1 外 touch in、Zone 1 内 touch out 的工作日晚高峰行程按 off-peak 计费，本例应为 £3.60。
- 2026-10-27: 根据 TfL FOI 答复记录 PAYG 实际计费逻辑：起终点触碰、站点对默认路线、中途 OSI/pink reader 验证触发可识别的替代路线；记录 TfL 不公开精确内部规则。结合晚高峰进 Zone 1 特例和 2026 Zone 1–4 off-peak 表，推导 Abbey Wood–King's Cross 18:36 这条路线为 £3.60；Fare Finder 仅作为结果核验来源。
- 2026-10-27: 补充 TfL Stop Structure topology API，用于判断站区之间是否存在 footpath；记录公开 schema 未说明付费区/闸机边界或是否出闸，避免仅凭地下连接或 Journey Planner `Walk` 推断刷卡和票价行为。
- 2026-10-27: 记录本机请求的 HTTP 403 `error code: 1010`。按 [Cloudflare Error 1010 文档](https://developers.cloudflare.com/support/troubleshooting/http-status-codes/cloudflare-1xxx-errors/error-1010/) 解释为基于客户端/浏览器签名的上游拦截，而非单凭此错误判定 key 无效；更新抓取脚本打印明确诊断。当前仍未取得 XML，需 TfL/API 服务方确认访问限制。
- 2026-10-27: 根据排查建议为 Stop Structure 请求增加 `CommuteFit/1.0` User-Agent 和 `cf-ray` 输出（若响应含该头）；确认 `app_key` 仍由本地 `.env` 读取并随请求发送、不打印。执行环境重试因 DNS 无法解析 TfL 域名而无法复测 1010，因此尚无 XML 或新的 cf-ray 可记录。
- 2026-10-27: 用户本机 Stop Structure HTTP 请求返回 200，但检查 802-byte XML 后发现 TfL 应用层返回 `BROKER` code `-1` / `unknown error`，未含任何 stop areas 或 footpaths。更新脚本检测 XML 内嵌错误，并支持传站点 ID；记录以官方文档示例 St James’s Park (`940GZZLUSJP`) 作对照的命令。Moorgate 与 Liverpool Street 拓扑仍未取得。
- 2026-10-27: St James’s Park 对照数据成功（13 areas、83 footpaths、无 XML error），证明 Stop Structure 请求端到端可用；Moorgate 自身 ID 请求仍返回 Broker unknown error。
- 2026-10-27: 复核 Moorgate ID：TfL 站点页确认 `940GZZLUMGT` 是 Moorgate Underground Station 的 NaPTAN ID，Stop Structure 文档允许用 NaPTAN；更正前述“候选/未确认”的措辞。Moorgate 仍返回内部 Broker error，故现阶段缺的是成功的拓扑响应，不是站点 ID。
- 2026-10-27: 直接读取 TfL `StopPoint/Search/Moorgate?maxResults=20` 响应，确认主枢纽 `HUBZMG` 的 Journey Planner `icsId=1000149`。记录该 ICS ID 用于下一次 Stop Structure 最简请求；`HUBZMG` 是 Hub ID，不应传入结构 endpoint。
- 2026-10-27: 按用户最新要求只查 Moorgate，以 `1000149 --minimal` 请求；当前代理运行环境 DNS 无法解析 TfL 域名，未能发出请求。记录本机命令 `python3 scripts/fetch_tfl_stop_structure.py 1000149 --minimal`；旧 NaPTAN 响应是空结构，不能替代该 ICS ID 查询。
- 2026-10-27: 用户本机成功查询 Moorgate `icsId=1000149`：28 areas、119 footpaths、无 XML error。提取 110 条 Moorgate 站内、6 条到相邻 bus stop、3 条到 Liverpool Street 的连接；Liverpool Street 连接为 BookH↔Moorgate bus 的 5 分钟 STAIRS，以及两条 Moorgate `MMEL` 到 Liverpool Street Elizabeth line EB/WB 的 1 分钟 LEVEL。未见 National Rail `RLY` endpoint。将全部 footpath 导出到 `docs/stop-structure-moorgate-footpaths.md`；付费区/闸机仍无法由 topology 判断。
- 2026-10-27: 补充解释 9 条跨 StopPoint 连接：6 条通往街面 bus stop，物理上涉及离开站内到公交站；1 条 Moorgate bus stop ↔ Liverpool Street BookH；2 条 EL platform 与 Moorgate MMEL 的地下 LEVEL 通道。严格区分“走到站外公交站”与“刷卡/过闸机出站”，后者无 topology 字段可确认。
- 2026-10-27: 直接读取 TfL `StopPoint/Search/Liverpool Street?maxResults=20` 响应：主匹配 `id=HUBLST` 为 Hub、`icsId=1000138` 覆盖 Tube / Elizabeth line / National Rail 等模式；记录 `1000138` 作为可用于 Stop Structure 的 Journey Planner Stop ID，避免误用 Hub ID 或周边 bus stop。
- 2026-10-27: 用户本机成功取得 Liverpool Street topology：29 areas、109 footpaths、无 XML error。解析到两条 Elizabeth line platform areas（1000138 areas 19/23）与 Moorgate area 24 的 direct LEVEL footpath，各 1 分钟；另有两个主站区间 5 分钟 STAIRS footpath。记录这能证明拓扑连通，但不能判定通道付费区/闸机与 PAYG 计费。
- 2026-10-27: 检查 National Rail 区域（Liverpool Street area 15 / `9100LIVST`）在最简 XML 中没有列出 direct footpath 边；该请求没有 `sSOnlyDF=0`，不能据此断言无经由中间 area 的连接。代理环境 DNS 阻止补取 full query，记录本机运行无 `--minimal` 命令以完成核查。
- 2026-10-27: 用户本机完整 Stop Structure 查询返回 HTTP 200，但 XML 内部为 `BROKER -100 unknown error`，未返回 topology，并覆盖了原最简响应。修复脚本使应用错误写入独立 `*-error.xml` 而不覆盖有效数据；新增 `--all-footpaths`（仅额外请求 `sSOnlyDF=0`）用于隔离完整参数中导致 Broker 错误的因素。Liverpool Street National Rail 到 Moorgate 是否有可达 footpath 仍待成功的 all-footpaths 响应。
- 2026-10-27: 用户重试 `--all-footpaths` 后仍是 HTTP 200 + XML `BROKER -100 unknown error`；这验证了全量路径开关本身对当前 Liverpool Street 查询也触发上游错误。TfL `StopPoint/Search/Liverpool Street?modes=national-rail` 仍仅返回聚合枢纽 `HUBLST` / `icsId=1000138`，未发现独立的 National Rail 子 ID 可用来绕开。结论保持未定：当前最简 topology 未展示 National Rail 直连 Moorgate，但全量/间接 footpath 未能取得。
- 2026-10-27（历史研究）：记录用户提供的 FareTo 实测和研究探针结果。此 API 不在公开 Unified API Swagger 中，应用现已明确不调用 FareTo；仅保留这些结果作为历史调查记录。
