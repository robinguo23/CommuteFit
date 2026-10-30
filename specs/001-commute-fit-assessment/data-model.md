# Data Model: Personal Commute Fit Assessment
# 数据模型：个人通勤适配评估

## Commute Profile / 通勤档案

This is the only persisted feature data: one editable profile for the local user. The user may clear it.  
这是唯一持久保存的功能数据：本机用户的一份可编辑档案。用户可以清除档案。

| Field | Type | Rules | 中文说明 |
|---|---|---|---|
| `id` | integer | Fixed singleton key; only one profile is supported | `id`：固定单档案主键；仅支持一份档案 |
| `departurePostcode` | string | Required UK postcode; normalized for lookup; no full address | `departurePostcode`：必填英国 postcode；查询前规范化；不收集完整地址 |
| `maxOneWayMinutes` | integer or null | Optional; positive when provided; user-editable | `maxOneWayMinutes`：可选；填写时为正整数；用户可修改 |
| `arrivalBufferMinutes` | integer | Defaults to 30; user-editable; non-negative | `arrivalBufferMinutes`：默认 30 分钟；用户可修改；不得为负数 |
| `selectedConcessions` | list of catalogue identifiers | Only concessions the user says they hold; may be empty | `selectedConcessions`：用户表示自己持有的优惠目录标识；可为空 |
| `preferredPaymentMethod` | enum or null | `apple-pay-contactless`, `bank-card-contactless`, `oyster`, `other`; optional | `preferredPaymentMethod`：可选，`apple-pay-contactless`、`bank-card-contactless`、`oyster` 或 `other` |
| `otherPaymentMethodLabel` | string or null | Used only when method is `other`; user-editable | `otherPaymentMethodLabel`：仅付款方式为 `other` 时使用；用户可修改 |
| `updatedAt` | timestamp | Updated when profile changes | `updatedAt`：档案修改时间 |

The profile is stored in a dedicated `commute_fit` database inside the project-owned PostgreSQL 18.6 Docker container. Its persistent data is isolated under the ignored `.local/postgres-data/` directory, and the host port is bound to `127.0.0.1`. The browser loads the profile from the local application and does not keep a second persistent copy in browser storage.  
档案保存在项目专用 PostgreSQL 18.6 Docker 容器内的 `commute_fit` 数据库中，数据独立保存在已忽略的 `.local/postgres-data/`，并且只绑定本机地址。浏览器从本地应用读取档案，不在浏览器存储中另存一份持久副本。

## Role Commute Request / 职位通勤请求

Transient for one assessment only. Do not insert it into the database, request logs or browser storage.  
仅用于单次评估的临时数据。不得写入数据库、请求日志或浏览器存储。

| Field | Type | Rules | 中文说明 |
|---|---|---|---|
| `destinationText` | string | Required; postcode, listed location/address or brand branch | `destinationText`：必填；postcode、招聘信息地点/地址或品牌门店 |
| `referenceDate` | ISO date | Required date for a one-day check or to identify the target week | `referenceDate`：必填日期，用于单日评估或确定目标周 |
| `earliestShiftStart` | local time | Required; earliest possible shift start | `earliestShiftStart`：必填；最早可能开班时间 |
| `expectedLatestFinish` | local time | Required; when earlier than start, interpreted as following day | `expectedLatestFinish`：必填；若早于开班时间则视为次日 |
| `checkWeekend` | boolean | Defaults to false; when true, add Saturday and Sunday in the week containing `referenceDate` to the ordinary reference-date assessment; deduplicate dates | `checkWeekend`：默认为 false；为 true 时，在 `referenceDate` 评估之外增加该周周六和周日评估；日期去重 |
| `arrivalBufferMinutes` | integer or null | Omitted uses profile; explicit null resets to product default (30) | `arrivalBufferMinutes`：缺省时使用档案值；显式 null 恢复产品默认值（30） |
| `maxOneWayMinutes` | integer or null | Omitted uses profile; explicit null disables fit classification | `maxOneWayMinutes`：缺省时使用档案值；显式 null 关闭适配分类 |
| `paymentMethod` | enum or null | Omitted uses profile preference; explicit null means no selected method | `paymentMethod`：缺省时使用档案首选值；显式 null 表示未选择付款方式 |
| `concessions` | list of catalogue identifiers | Omitted uses profile selections; empty list applies no concession | `concessions`：缺省时使用档案已选优惠；空列表表示不应用优惠 |

## Location Resolution / 地点识别

The origin is the profile postcode. The destination is the user's free-text workplace input. When TfL returns multiple candidates or a disambiguation result, present the candidates and require an explicit selection before route assessment. Do not retain resolved workplace identifiers after the request completes.  
出发地使用档案 postcode，目的地使用用户输入的工作地点文字。TfL 返回多个候选或消歧结果时，展示候选并要求用户明确选择后再评估路线。请求结束后不保留解析出的工作地点标识。

## Assessment Boundaries / 评估边界

- Outbound query targets `earliestShiftStart - arrivalBufferMinutes`, using the actual preceding calendar date if subtraction crosses midnight.  
  去程查询目标为 `earliestShiftStart - arrivalBufferMinutes`；若计算跨越午夜，使用实际的前一日日期。
- Return query departs at or after `expectedLatestFinish`, using the next calendar date when the shift ends after midnight.  
  返程查询时间为 `expectedLatestFinish` 或之后；班次跨午夜时使用次日日期。
- When `checkWeekend` is false, assess `referenceDate`. When true, assess `referenceDate` plus the Saturday and Sunday dates in the Monday-to-Sunday week containing it, deduplicating dates when the reference date is itself a weekend day. The selected shift boundaries apply to all assessed dates.  
  `checkWeekend` 为 false 时评估 `referenceDate`；为 true 时评估 `referenceDate` 以及该日期所在周的周六和周日，若参考日期本身是周末则去重。所有评估日期均使用用户输入的班次边界。
- Interpret user date/time values in `Europe/London`, including daylight-saving transitions.  
  用户日期/时间按 `Europe/London` 解释，并处理夏令时切换。

## Scheduled Journey Option / 计划路线方案

Transient response data only. Each assessment may contain one or more options for each date/direction.  
仅为临时响应数据。每次评估中，每个日期/方向可能包含一条或多条路线方案。

| Field | Type | 中文说明 |
|---|---|---|
| `travelDate` | ISO date | 实际路线日期 |
| `direction` | `outbound` or `return` | 去程或返程 |
| `queryDateTime` | local date/time | 查询目标日期和时间 |
| `startDateTime` / `arrivalDateTime` | provider date-time or unknown | 提供方返回的出发/到达时间；可为未知 |
| `durationMinutes` | integer or unknown | 计划耗时；没有依据时为未知 |
| `legs` | ordered list | 行程路段、交通方式、站点、换乘及路段时长 |
| `withinLimit` | boolean or null | 是否符合个人时长上限；未设上限或数据未知时为 null |
| `source` | URL/text | TfL 来源及查询依据 |

## Fare and Concession Information / 票价与优惠信息

Fare breakdowns/results are transient; do not persist computed fare results. The versioned policy catalogue may be shipped as application source/configuration and updated against official sources.  
票价拆分/结果均为临时数据；不得持久保存计算后的票价。带版本的政策目录可作为应用源码/配置发布，并根据官方来源更新。

| Field | Meaning |
|---|---|
| `routeFare` | Provider's route-level quote when returned, including amount, currency, payment/fare basis, travel date, effective date where available, source and status. A missing route fare is unknown. |
| `fareItems` | Returned Journey Planner fare-item breakdown, including provider fields such as cost, zones, charge level, Hopper flag and taps when present. These items are not itinerary legs and must not be reconstructed by pricing every leg independently. |
| `conditionalConcessionScenarios` | Separate scenarios with eligibility status (`eligible`, `ineligible`, `unknown`), policy-specific time basis and qualifying event/interval, evaluated fare scope (route or identified provider fare item(s)), payment/eligibility assumptions, amount status/value/basis, `fareItemSequences` for the policy scope and `amountFareItemSequences` for items with supported prices. A per-item amount is never presented as a complete route total. |
| `capGuidance` | Published adult bus/tram daily and Monday-to-Sunday weekly caps, optionally a conditional discounted daily cap, effective date-time, explanation and source. These values are guidance; do not claim an exact commute or full-day spend without complete travel/device context. A discounted 7-day pass price is not a weekly PAYG cap. |

| 字段 | 含义 |
|---|---|
| `routeFare` | 提供方返回的路线级报价，包括金额、货币、付款/票价基础、出行日期、可用时的生效日期、来源和状态。缺少路线总价时保持未知。 |
| `fareItems` | Journey Planner 返回的票价项拆分；保留可用的金额、区域、高峰等级、Hopper 标记和 taps 等字段。这些票价项不是行程 leg，不得通过逐个 leg 独立查价重建。 |
| `conditionalConcessionScenarios` | 单独的优惠方案，注明资格状态（`eligible`、`ineligible`、`unknown`）、该规则专属的时间基准及符合条件的事件/时间间隔、评估收费范围（整条路线或明确的提供方票价项）、付款/资格假设、金额状态/数值/依据、规则涉及的 `fareItemSequences`，以及金额有依据的 `amountFareItemSequences`。单项金额不得表示为完整路线总价。 |
| `capGuidance` | TfL 公布的成人公交/电车 PAYG 每日封顶和周一至周日每周封顶，可选的条件性优惠日封顶、生效日期时间、解释和来源。这些金额仅作说明；缺少全天完整行程/设备信息时不得声称精确通勤或全天费用。折扣 7 日通票价格不等于每周 PAYG 封顶。 |

Use the provider's route total and fare-item breakdown when available. The MVP does not request FareTo station-pair quotes. Keep route legs and provider fare items distinct. Do not calculate one full fare per leg or add overlapping sources. If a complete route total is missing or cannot be supported, mark it unable to confirm; do not substitute zero or present partial item values as a complete route fare. A planned route quote is not a transaction record, and caps remain guidance unless the necessary card/device history is known.  
有数据时使用提供方返回的路线总价及票价项拆分。MVP 不请求 FareTo 站对报价。路线 leg 与提供方票价项是不同概念。不得按每个 leg 计算一笔完整票价，也不得叠加收费范围重叠的来源。缺少完整且有依据的路线总价时，标记为无法确认；不得以 0 代替，也不得把部分票价项说成完整总价。计划路线报价不是交易记录；在缺少必要的卡片/设备行程信息时，封顶仅作说明。

Evaluate each concession using only its official eligibility time basis and fare scope. A route or vehicle-leg duration is not a generic eligibility test. The initial published bus/tram schedule is effective in `Europe/London` from 2026-10-27 00:00 through (but not including) 2026-11-01 04:30, then from 2026-11-01 04:30. Prefer the relevant bus/tram fare-item tap time; if absent, use the route start time. A discounted single fare is shown per fare-item sequence only when the user selected the applicable Oyster concession and the returned adult bus/tram fare item matches the published standard fare for that time. Do not sum these amounts into a route total. If required event timestamps, payment basis, user-confirmed eligibility or fare-item scope are unavailable or ambiguous, return `unknown`, show the conditions/source, and leave the discounted amount unavailable and outside the route total. Do not apply a concession to all fare items because one event qualifies, unless the rule explicitly defines that broader scope.

首个公交/电车费率表按 Europe/London 当地时间执行：旧费率持续至 2026-11-01 04:30，新公布费率从该时刻起生效。优先使用每个公交/电车票价项的 tap 时间；缺失时才使用路线出发时间。只有用户选择了适用的 Oyster 优惠，且提供方成人单程票价项与该时刻公布的成人标准价相符，才按对应票价项序号显示折后成人单程价；金额不计入路线总价。

每项优惠都必须按其官方资格时间基准和收费范围判断。整条路线或车辆 leg 的时长不能作为通用资格标准。若必要事件时间戳、付款依据、用户确认资格或票价项范围缺失或含糊，应返回 `unknown`，显示条件/来源，将折后金额标为无法确认且不计入路线总价。除非规则明确规定更大范围，不得因一个事件符合条件就将优惠应用到所有票价项。
- `capGuidance`: published daily/weekly cap conditions and applicability status. Do not represent it as an exact total for the user's full day or week.  
  `capGuidance`：已公布的每日/每周封顶条件及适用状态；不得将其表示为用户全天/全周确切总费用。

## Commute Assessment State / 通勤评估状态

| State | Meaning | 中文含义 |
|---|---|---|
| `available` | Scheduled option found and requested route fields are supported | 找到计划路线且请求字段有依据 |
| `partial` | Route found but one or more fare/concession details are unknown | 找到路线，但部分票价/优惠细节未知 |
| `no-scheduled-option` | Planner returned no scheduled route for a date/boundary | 规划器在该日期/边界未返回计划路线 |
| `ambiguous-location` | User selection is required to resolve the workplace | 需用户选择以消歧工作地点 |
| `unable-to-confirm` | Provider error or unsupported data prevents a reliable conclusion | 提供方错误或资料不受支持，无法可靠判断 |
