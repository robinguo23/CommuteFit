# Local App Contract: Browser to Assessment Server
# 本地应用契约：浏览器到评估服务端

These same-origin REST endpoints are served by Spring Boot for the single-user local app, not a public API. The backend listens only on `127.0.0.1`; profile values are stored in the project-owned PostgreSQL container, while assessment requests and results are transient. In development, Vite proxies `/api/**` to Spring Boot.  
以下同源 REST 接口由 Spring Boot 提供，仅供单用户本地应用使用，不是公开 API。后端只监听 `127.0.0.1`；档案数据保存在项目专用 PostgreSQL 容器中，评估请求和结果仅作临时处理。开发期间由 Vite 将 `/api/**` 代理到 Spring Boot。

## `GET /api/profile`

Returns the one saved commute profile, or defaults when no profile exists.  
返回已保存的通勤档案；若尚无档案则返回默认值。

## `PUT /api/profile`

Validates and replaces the personal profile. This is the only write that persists application data. It must not accept role location, shift date/time or assessment-result fields.  
校验并替换个人档案。这是唯一会持久化应用数据的写操作；不得接受职位地点、班次日期/时间或评估结果字段。

### Request / 请求

```json
{
  "departurePostcode": "SW1A 1AA",
  "maxOneWayMinutes": null,
  "arrivalBufferMinutes": 30,
  "selectedConcessions": [],
  "preferredPaymentMethod": null,
  "otherPaymentMethodLabel": null
}
```

The profile contains only the fields defined in `data-model.md`; invalid concession identifiers or payment-method values return `400 invalid_profile`.  
档案仅包含 `data-model.md` 定义的字段；优惠标识或付款方式无效时返回 `400 invalid_profile`。

## `DELETE /api/profile`

Clears the saved profile and restores defaults.  
清除已保存档案并恢复默认值。

## `POST /api/assessment`

Accepts one transient commute assessment request. Validate, query TfL and return a normalized response; do not insert the request, response, role data or fare results into PostgreSQL or logs.  
接收一项临时通勤评估请求。完成校验、查询 TfL 并返回标准化响应；不得将请求、响应、职位资料或票价结果写入 PostgreSQL 或日志。

### Request / 请求

```json
{
  "destinationText": "Covent Garden, London",
  "referenceDate": "2026-11-02",
  "earliestShiftStart": "03:00",
  "expectedLatestFinish": "23:00",
  "checkWeekend": true,
  "arrivalBufferMinutes": null,
  "maxOneWayMinutes": null,
  "paymentMethod": null,
  "concessions": []
}
```

- `destinationText`, `referenceDate`, `earliestShiftStart` and `expectedLatestFinish` are required.
- `checkWeekend` defaults to `false`; when false, assess `referenceDate`; when true, assess `referenceDate` plus the Saturday and Sunday in its Monday-to-Sunday week, deduplicating dates when the reference date is itself Saturday or Sunday.
- Date is `YYYY-MM-DD`; times use 24-hour `HH:mm` in `Europe/London`.
- For `arrivalBufferMinutes` and `maxOneWayMinutes`, omitted means use the saved profile value; explicit `null` means reset the buffer to 30 minutes or disable the time-limit classification, respectively.
- For `paymentMethod`, omitted means use the saved preferred method; explicit `null` means do not select a payment method. When none is selected, return the adult pay-as-you-go route fare from Journey Planner when supported, label its payment basis, and list route-relevant concession scenarios separately without applying them. The MVP does not call FareTo.
- `concessions` identifies concessions the user says they hold; when omitted, use saved selections. These selections highlight matching conditional fare scenarios. Route-applicable conditional scenarios are still shown independently, subject to supported pricing data; never infer eligibility.
- `selectedCandidateId` and `selectedOriginCandidateId` are transient follow-up tokens for a selected workplace or departure-postcode candidate. A `location_disambiguation` response includes `candidateField` (`destination` or `origin`) and candidate choices. Bind each token to its original query, expire it after ten minutes, clear it after a successful assessment, and never persist it. If TfL returns an ambiguous source or destination, ask the user to choose; if no usable choices are returned, respond with `location_unresolved` rather than silently using one.
- An expired follow-up token returns `candidate_expired`; clear it in the client and allow a fresh submission to retrieve current choices.
- Shift start, outbound target and return boundary times that are nonexistent or ambiguous under the `Europe/London` daylight-saving rules return a bilingual `nonexistent_london_time` or `ambiguous_london_time` error before any journey lookup.
- For retryable TfL errors, return a clear provider error that lets the client retry the current assessment. Do not persist the request while waiting or retrying.

- `destinationText`、`referenceDate`、`earliestShiftStart` 和 `expectedLatestFinish` 必填。
- `checkWeekend` 默认为 `false`；为 false 时评估 `referenceDate`；为 true 时评估 `referenceDate`，并增加它所在周的周六和周日；若参考日期本身是周末则去重。
- 日期格式为 `YYYY-MM-DD`；时间为 `Europe/London` 的 24 小时制 `HH:mm`。
- `arrivalBufferMinutes` 和 `maxOneWayMinutes` 缺省时使用已保存档案值；显式传入 `null` 时，缓冲恢复为 30 分钟、时长分类关闭。若未设置时长上限，则不进行适配分类。
- `paymentMethod` 缺省时使用档案首选方式；显式设为 `null` 时不选择付款方式。未选择付款方式时，如 Journey Planner 有支持的数据，则返回成人 PAYG 路线票价并标明付款口径，路线相关优惠另行展示且不直接应用。MVP 不请求 FareTo。
- `concessions` 标识用户表示自己持有的优惠；缺省时使用档案设置。该选择用于突出匹配的条件票价方案。路线适用的其他条件方案仍应根据可用数据展示；不得推断用户资格。
- `selectedCandidateId` 和 `selectedOriginCandidateId` 是用户选择工作地点或出发 postcode 候选项时使用的临时后续请求令牌。`location_disambiguation` 响应包括 `candidateField`（`destination` 或 `origin`）及候选项。令牌须绑定原查询、十分钟后过期、评估成功后清理，且不得持久化。TfL 对起点或终点返回歧义时必须请用户选择；若没有可用候选项，则返回 `location_unresolved`，不得静默选用某个地点。
- 后续请求令牌过期时返回 `candidate_expired`；客户端清除此令牌后允许重新提交并获取最新候选项。
- 若班次开始、去程目标或返程边界时间在 `Europe/London` 夏令时规则下不存在或含义不唯一，须在查询路线前返回中英双语的 `nonexistent_london_time` 或 `ambiguous_london_time` 错误。
- 对可重试的 TfL 错误，返回清晰的提供方错误，使前端可以重试当前评估。等待或重试时不得持久保存请求。

### Success response / 成功响应

```json
{
  "status": "available",
  "assessments": [
    {
      "travelDate": "2026-11-02",
      "dayLabel": "Monday",
      "outbound": {
        "targetArrival": "2026-11-02T02:30:00",
        "options": []
      },
      "return": {
        "departAfter": "2026-11-02T23:00:00",
        "options": []
      }
    }
  ],
  "dataBasis": {
    "provider": "Transport for London Journey Planner",
    "queriedAt": "2026-10-26T12:00:00Z",
    "timezone": "Europe/London"
  },
  "limitations": ["Scheduled information only; no realtime punctuality prediction or arrival guarantee."]
}
```

When weekend mode is enabled, `assessments` contains the reference date and separate entries for Saturday and Sunday in the same week, with duplicate dates removed. Each route option contains provider-supported itinerary legs and its own `fareSummary` with that option's route-level fare and returned fare items; never share a fare summary across different dates or options. The MVP does not call FareTo or include station-pair quote references. Fare items are not itinerary legs. Do not price each leg independently. If the provider's complete route fare is missing, return `unable-to-confirm`; do not treat absent items as zero or label a partial amount as the total.  
启用周末模式时，`assessments` 包含参考日期以及同一周的周六和周日，并对相同日期去重。每条路线方案均包含有依据的行程 leg 和自己的 `fareSummary`，其中票价只对应该条路线的路线总价及票价项；不同日期或路线方案之间不得共用票价摘要。MVP 不请求 FareTo，也不包含站对报价参考。票价项不是行程 leg，不得按 leg 独立定价。提供方缺少完整路线票价时，应返回 `unable-to-confirm`；不得把缺失票价项当成 0，也不得将部分金额标为总价。

Each `conditionalConcessionScenarios` entry MUST identify the concession, `eligibilityStatus` (`eligible`, `ineligible` or `unknown`), the official rule's time basis and qualifying event/interval, the fare scope (`route` or identified fare-item sequence(s)), payment and user-confirmed eligibility assumptions, effective date and source. Its amount MUST carry an explicit status; provide a numeric amount only when all required facts and the rule-defined scope are supported by the assessment. If timing, scope, payment or eligibility is missing or ambiguous, return `unknown` with `amount.status: "unable-to-confirm"` and no numeric value. Do not infer eligibility from whole-route or leg duration unless the policy explicitly defines that duration, and do not include an unknown or partial-scope amount in the route total.

`routeFare` includes English and Chinese payment-basis and scope text (`paymentBasis`, `paymentBasisZh`, `scope`, `scopeZh`) so the client can switch language without changing the assessment. In a conditional scenario, `effectiveDate` identifies the published fare period used for a supported amount; otherwise it contains the policy effective date when available or `null`.  
`routeFare` 提供中英文付款口径和收费范围（`paymentBasis`、`paymentBasisZh`、`scope`、`scopeZh`），客户端可直接切换语言，无需重新评估。条件优惠方案中的 `effectiveDate` 标明有依据的优惠金额所采用的公布费率生效时间；否则返回可用的政策生效日期，若无则为 `null`。

When the published bus/tram schedule supports a discounted single fare, the amount is per fare item and `amountFareItemSequences` identifies exactly which items it prices; it is never a route total or a sum across items. Match by London-local bus/tram tap timestamp (or route start time only when the item timestamp is absent) and the provider's returned adult single-fare amount. If multiple matched items fall into different published price periods and a single amount cannot represent them, leave the amount unable to confirm.

`capGuidance` may include adult bus/tram daily and Monday-to-Sunday weekly PAYG caps, an explicitly conditional discounted daily cap, currency, effective date-time, source and a full-day-context caveat. It MUST NOT treat a discounted 7-day pass price as a weekly cap, apply a bus/tram-only cap as the total cap for a mixed rail itinerary, or claim the user's exact commute/day/week spend.

每项 `conditionalConcessionScenarios` MUST 标明优惠标识、资格状态（`eligible`、`ineligible` 或 `unknown`）、官方规则规定的时间基准和符合条件的事件/时间间隔、收费范围（`route` 或明确的票价项序号）、付款和用户确认资格假设、生效日期及来源。金额 MUST 带有明确状态；只有所有必要事实和规则定义的收费范围均有依据时才提供数值。若时间、范围、付款或资格缺失或含糊，应返回 `unknown` 和 `amount.status: "unable-to-confirm"`，不得提供数值。除非优惠政策明确以整段路线或 leg 时长为条件，不得据此推断资格；未知金额或部分范围金额不得计入路线总价。

若已公布的公交/电车费率足以支持折后单程价，金额按票价项计算，并由 `amountFareItemSequences` 精确列出对应票价项；不得标为路线总价或跨项求和。使用伦敦当地公交/电车 tap 时间匹配费率（仅在票价项时间戳缺失时才使用路线出发时间），且提供方返回的成人单程价须与费率表一致。若多个匹配项落在不同费率时段而一个数值无法代表，金额保持无法确认。

`capGuidance` 可包含成人公交/电车 PAYG 每日和周一至周日每周封顶、明确标为条件性的优惠日封顶、货币、生效日期时间、来源和全天行程限制说明。不得将折扣 7 日通票价格当作每周封顶，不得将仅适用于公交/电车的封顶说成混合铁路行程的总封顶，也不得声称这是用户通勤、全天或全周的确切费用。

An itinerary can contain time-separated legs and fare items with different eligibility outcomes. Apply a concession only to the route or fare-item scope defined by its official rule; do not apply it to all fare items because one qualifying event is present. If the provider does not return the events/timestamps needed for a supported policy, show its conditions and source while leaving the amount unable to confirm.

同一行程中的不同 leg/票价项可能发生在不同时间，因此优惠资格结果可能不同。优惠只能应用于官方规则规定的路线或票价项范围；不得因为出现一个符合条件的事件就将优惠应用于全部票价项。若提供方未返回判断受支持政策所需的事件/时间戳，则显示条件和来源，并将金额保持为无法确认。

### Errors / 错误

- `400 invalid_request`: missing/invalid input; include bilingual field-level messages.
- `400 invalid_profile`: invalid personal preference data.
- `422 location_disambiguation`: return candidate locations and ask the user to select one.
- `502 provider_unavailable`: TfL request fails or returns unusable data; do not fabricate a route or fare.
- `503 database_unavailable`: profile cannot be loaded/saved; explain that the local database is unavailable.
- `429 rate_limited`: upstream quota reached; suggest retrying later and link to TfL.

- `400 invalid_request`：输入缺失/无效；提供双语字段级提示。
- `400 invalid_profile`：个人偏好数据无效。
- `422 location_disambiguation`：返回候选地点，请用户选择。
- `502 provider_unavailable`：TfL 请求失败或数据不可用；不得编造路线或票价。
- `503 database_unavailable`：无法读取/保存档案；说明本地数据库当前不可用。
- `429 rate_limited`：达到上游限额；提示稍后重试并链接 TfL。

## Upstream / 上游服务

Use TfL Unified API Journey Planner with date/time and arriving/departing semantics. Send the departure postcode and workplace query only for the active assessment. Keep optional `TFL_APP_KEY` server-side. Follow TfL attribution requirements; never use TfL branding or imply endorsement. Spring JDBC and Flyway manage the single-profile database schema; assessment payloads/results must not enter PostgreSQL or logs.  
使用 TfL Unified API Journey Planner，并传递日期/时间和到达/出发语义。仅在当前评估期间发送出发 postcode 和工作地点查询。可选 `TFL_APP_KEY` 仅保留在服务端。遵循 TfL 署名要求；不得使用 TfL 品牌标识或暗示获得背书。单档案数据库结构由 Spring JDBC 和 Flyway 管理；评估请求/结果不得写入 PostgreSQL 或日志。
