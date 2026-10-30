> 历史方案：当前 MVP 已按用户指示收窄，见 [002-postcode-journey](../002-postcode-journey/spec.md)。本文件不再代表当前交付范围。

# Feature Specification: Personal Commute Fit Assessment
# 功能规格：个人通勤适配评估

**Feature Branch**: `001-commute-fit-assessment`  
**功能分支**：`001-commute-fit-assessment`

**Created**: 2026-10-26  
**创建日期**：2026-10-26

**Status**: Draft  
**状态**：草稿

**Version**: 0.2.1 | **Last Updated**: 2026-10-27  
**版本**：0.2.1 | **最后更新**：2026-10-27

**Input**: The idea owner's request and clarifications gathered during the Spec Kit workflow; these clarify and supersede any conflicting details in the initial assessment handoff.  
**输入**：Spec Kit 流程中提出者的构想及澄清；若与最初评估交接文档有冲突，以本规格中的澄清为准。

## Clarifications / 澄清

### Session 2026-10-26 / 2026-10-26 澄清记录

- Interface and assessment results are bilingual in Chinese and English. / 界面和评估结果使用中英双语。
- The outbound arrival buffer defaults to 30 minutes before the earliest shift start and is adjustable. / 去程默认在最早班次开始前 30 分钟到达，且缓冲时间可调整。
- The origin is a user-selected departure postcode; it is not limited to the user's home address. / 出发地是用户选择的 postcode，不限定为居住地址。
- A reference date selects the ordinary assessment day. If weekend assessment is enabled, the app also assesses Saturday and Sunday of that reference date's week. / 参考日期指定普通评估日；开启周末评估后，系统还会评估该参考日期所在周的周六和周日。
- The one-way commute-time limit and payment method are optional inputs. A cost-limit input is deferred. / 单程通勤时长上限和付款方式均为可选输入；费用上限输入延后。
- With no payment method selected, show the adult pay-as-you-go route fare returned by Journey Planner when available, labelled with that payment basis; keep route-relevant concessions separate and conditional. / 未选择付款方式时，如 Journey Planner 返回成人 PAYG 路线票价，就按该付款口径标注显示；路线相关优惠另行展示并明确标注条件。
- A route leg is not automatically a separately charged fare. Use TfL's returned route-level fare and fare items when available; do not calculate a full fare per leg. / 路线 leg 不自动等于一次独立收费。优先使用 TfL 返回的路线总价及票价项；不得按每个 leg 都计算一笔完整票价。
- Retain personal commute settings for reuse; do not retain role-specific details or assessment history. / 保留个人通勤设置供重复使用；不保存职位资料或评估历史。

### Session 2026-10-27 / 2026-10-27 澄清记录

- Show a concession amount only when the official policy's own eligibility time basis and fare scope can be evaluated from the available scheduled journey, payment method and user-confirmed eligibility. Do not infer eligibility or ineligibility from total route or leg duration unless the policy explicitly uses that duration; otherwise show the conditions and source and mark the amount unable to confirm. / 只有当现有计划路线、付款方式和用户确认的资格资料足以判断官方规则自身的时间基准及收费范围时，才显示优惠金额。除非规则明确以整条路线或 leg 时长为条件，不得仅凭总时长推断适用或不适用；否则显示条件和来源，并将金额标为无法确认。

## User Scenarios & Testing / 用户场景与验收

### User Story 1 - Check a role's commute before applying (Priority: P1)
### 用户故事 1——投递前检查职位通勤（优先级：P1）

The user finds a role on a brand's career site, then enters the role's location, earliest available shift start, expected latest finish, and a reference date. The location may be a postcode, a specific address/location shown in the listing, or a named brand branch. The user can optionally set a one-way time limit. The assessment shows scheduled outbound and return options, using an adjustable arrival buffer that defaults to 30 minutes. If weekend checking is selected, it assesses the reference date plus Saturday and Sunday in the reference date's week.

用户先在品牌招聘网站找到职位，再输入职位地点、最早可上班时间、预计最晚下班时间及参考日期。地点可以是 postcode、招聘信息中的具体地点或品牌门店。用户可以选填单程通勤时长上限。评估显示计划去程和返程路线，到岗缓冲默认 30 分钟且可调整。若勾选周末检查，系统会评估参考日期，并额外评估该日期所在周的周六和周日。

**Why this priority**: Repeated manual route searches are the main burden this tool is intended to reduce. / **优先级理由**：本工具主要用于减少反复手动搜索路线的负担。

**Independent Test**: Enter a representative role with an early start and late finish; confirm the outbound arrival target, return boundary, route duration and optional personal-limit comparison. / **独立验收**：输入一个早班、晚班代表性职位，确认去程到达目标、返程边界、路线时长及可选的个人上限比较。

**Acceptance Scenarios / 验收场景**:

1. **Given** a user has a departure postcode, **When** they enter a supported role location and shift boundaries, **Then** results show scheduled outbound and return options, their dates and durations, and compare each duration with the personal limit only when one is set.  
   **假设**用户已设置出发 postcode，**当**输入可识别的职位地点和班次边界时，**则**结果显示计划去程/返程路线、对应日期和时长；仅在用户设置了时长上限时才进行比较。
2. **Given** the default arrival buffer is unchanged, **When** the shift starts at 03:00, **Then** the outbound route targets arrival by 02:30.  
   **假设**用户未修改默认到岗缓冲，**当**班次在 03:00 开始时，**则**去程以 02:30 前到达为目标。
3. **Given** weekend checking is selected for a target week, **When** the assessment runs, **Then** it checks the corresponding Saturday and Sunday independently and labels each date.  
   **假设**用户选择检查目标周末，**当**系统执行评估时，**则**分别检查对应周六和周日，并标明各自日期。
4. **Given** a role location name matches multiple places, **When** the user requests an assessment, **Then** the user can select the intended location before route results are produced.  
   **假设**职位地点名称匹配到多个地点，**当**用户发起评估时，**则**用户可以先选择正确地点，再生成路线结果。
5. **Given** no scheduled option is found for a route boundary, **When** results are shown, **Then** the tool distinguishes “no supported scheduled option found” from “unable to confirm.”  
   **假设**某个路线边界没有查到计划方案，**当**显示结果时，**则**明确区分“未找到受支持的计划路线”和“无法确认”。
6. **Given** a destination query returns multiple candidates, **When** the user changes the destination text, **Then** the previous candidates are invalidated and cannot be submitted for route planning.  
   **假设**目的地查询返回多个候选地点，**当**用户修改地点文字时，**则**之前的候选失效，不得继续用于路线查询。
7. **Given** a retryable TfL request failure occurs, **When** the failure is shown, **Then** the user can retry the same assessment without the app saving role inputs or results.
   **假设**TfL 请求发生可重试失败，**当**显示错误时，**则**用户可以重试本次评估，且应用不保存职位输入或结果。

### User Story 2 - Understand fare and available concessions (Priority: P2)
### 用户故事 2——了解票价和可用优惠（优先级：P2）

The user sees TfL Journey Planner's returned route fare and fare-item breakdown separately from vehicle legs. The MVP does not query FareTo. A vehicle leg is not assumed to be a new charge. Use a route-level Journey Planner total when present. Missing or partial route fares remain unknown, not zero or a complete total. The user also sees route-relevant concessions and separate conditional scenarios. A discounted amount is shown only when the official rule defines its eligibility time basis and fare scope and the available scheduled journey, payment method and user-confirmed eligibility can evaluate them. The initial published fare support may show a discounted adult bus/tram PAYG single fare per matching fare-item sequence when the user selects an applicable Oyster concession and the provider fare-item amount and tap time match the source-backed schedule; this amount is not a route total. Other concession amounts remain unknown unless their full rule and pricing inputs are supported. Do not infer eligibility or ineligibility from total journey or leg duration unless that rule explicitly uses it. If required facts or scope are unavailable, show the condition and source and mark the amount unable to confirm. The user may select concessions they hold so matching offers can be highlighted, but eligibility is never inferred. A payment method such as Apple Pay/contactless device, bank card contactless or Oyster is optional. When none is selected, label the adult pay-as-you-go route fare as such when the provider returns it; do not imply it represents every payment basis. Bus/tram caps are guidance, not an exact commute or full-day/week total; a discounted 7-day pass price is not a weekly PAYG cap. A commute-cost limit is deferred until after the core commute assessment is implemented.

用户查看 TfL Journey Planner 返回的路线票价及票价项拆分；MVP 不请求 FareTo。票价项与车辆 leg 分开显示，不假定每条车辆 leg 都产生一笔新收费。若 Journey Planner 返回路线总价，则使用该总价。缺失或部分路线票价保持未知，不得当成 0 或完整总价。用户还会看到路线相关优惠及单独的条件方案。只有当官方规则明确规定资格时间基准和收费范围，且现有计划路线、付款方式和用户确认的资格足以判断时，才显示折后金额。首个公布费率支持下，用户选择了适用的 Oyster 优惠，且提供方成人公交/电车 PAYG 票价项的金额和 tap 时间与有来源的费率表相符时，可按匹配的票价项序号显示折后单程价；此金额不是路线总价。其他优惠金额仍须有完整规则和价格依据支持，否则保持未知。除非规则明确以整条路线或 leg 时长为条件，不得根据总时长推断适用或不适用。若缺少必要事实或收费范围，则显示条件与来源并将金额标为无法确认。用户可选择自己持有的优惠以突出显示，但系统不得推断其资格。付款方式可选，包括 Apple Pay/设备感应支付、银行卡感应支付或 Oyster。未选择付款方式时，如提供方返回成人 PAYG 路线票价，应明确标注为 PAYG，不暗示其涵盖所有付款口径。公交/电车封顶仅作说明，不代表精确通勤或全天/全周总额；折扣 7 日通票价格不等于每周 PAYG 封顶。通勤费用上限输入延后至核心通勤评估实现之后。

**Why this priority**: Fare and concession information affects whether repeated part-time commutes are affordable, but the exact daily charge depends on factors beyond one route. / **优先级理由**：票价和优惠会影响兼职通勤是否负担得起，但全天确切收费还取决于单条路线以外的因素。

**Independent Test**: Compare multi-leg Tube transfers, Hopper journeys, mixed bus/Tube routes, missing-fare routes and time-limited concession cases with provider responses and official policies. Confirm totals are not reconstructed by pricing legs, concession eligibility follows its policy-specific time basis and fare scope, and unsupported amounts stay unknown. / **独立验收**：将多 leg 地铁换乘、Hopper、公交/地铁混合、缺失票价及有时间限制的优惠案例，与提供方响应和官方规则对照。确认不按 leg 重建总价、优惠资格按各自规则的时间基准和收费范围判断，且不支持的金额保持未知。

**Acceptance Scenarios / 验收场景**:

1. **Given** Journey Planner returns a route-level fare and fare items, **When** the result is shown, **Then** it shows the route total and returned fare items separately from itinerary legs; it does not reconstruct fares by pricing every leg independently.  
   **假设**Journey Planner 返回路线总价和票价项，**当**显示结果时，**则**将路线总价及返回的票价项与行程 leg 分开显示；不得通过逐个 leg 独立查价重建总价。
2. **Given** the user selects a concession they hold and the route contains the policy-defined qualifying event/time and fare scope, **When** authoritative rules and available data confirm eligibility for the travel date and payment method, **Then** the result shows the conditional amount for that stated scope and explains its basis.  
   **假设**用户选择了自己持有的优惠，且路线包含规则要求的事件/时间和收费范围，**当**权威规则与现有数据确认该优惠适用于出行日期和付款方式时，**则**结果显示对应范围的条件金额并说明依据。
3. **Given** a concession has a time or duration restriction, **When** the journey's total duration or a leg duration crosses a threshold, **Then** eligibility is decided only by the official rule's stated time basis and fare scope; the system does not assume that the whole route or each leg qualifies or fails solely from its duration.  
   **假设**优惠有时间或时长限制，**当**整段路线或某个 leg 的时长跨过阈值时，**则**系统仅按官方规则规定的时间基准和收费范围判断，不得仅凭路线或 leg 时长假定整条路线或每段均符合/不符合。
4. **Given** required route event times, payment facts, eligibility or fare scope are missing or ambiguous, **When** the concession is displayed, **Then** its conditions and source remain visible, its amount is unable to confirm, and the amount is not included in the route total.  
   **假设**必要的路线事件时间、付款信息、资格或收费范围缺失或含糊，**当**显示该优惠时，**则**仍显示条件和来源，金额标为无法确认，且不计入路线总价。
5. **Given** a daily cap depends on other travel or using the same card/device, **When** those details are unavailable, **Then** the tool explains the cap rules but does not claim an exact full-day charge.  
   **假设**每日封顶取决于其他行程或使用同一张卡/设备，**当**系统无法获知这些资料时，**则**说明封顶规则，但不声称全天确切费用。
6. **Given** the route fare is missing or a response contains only a partial fare breakdown, **When** results are displayed, **Then** the missing/partial amount is not treated as zero or as a complete route total.  
   **假设**路线票价缺失或响应只包含部分票价拆分，**当**显示结果时，**则**不得把缺失/部分金额当成 0 或完整路线总价。

### User Story 3 - Understand uncertainty, privacy and sources (Priority: P3)
### 用户故事 3——了解不确定性、隐私和信息来源（优先级：P3）

The user can see the date/time basis and source for each material route and fare claim, understand what the assessment could not establish, and know which personal settings are retained. The tool uses scheduled public-transport information and leaves the final application decision to the user.

用户可以查看重要路线和票价信息对应的日期/时间和来源，了解评估无法确认的内容，并清楚哪些个人设置会被保留。工具依据计划公共交通信息进行评估，是否申请由用户决定。

**Why this priority**: The result informs an important personal decision, so uncertainty and data use must remain clear. / **优先级理由**：评估结果会影响个人重要决定，因此必须清楚说明不确定性和数据用途。

**Independent Test**: Use an ambiguous location, missing fare, or unsupported discount case and confirm the affected result is labelled, linked to an official source where available, and not presented as certain. / **独立验收**：使用地点含糊、票价缺失或优惠无法支持的案例，确认相关结果被清楚标记、在可用时提供官方来源，且不会被说成确定结论。

**Acceptance Scenarios / 验收场景**:

1. **Given** route or fare information is missing, incomplete or uncertain, **When** results are displayed, **Then** only the affected item is marked “Unable to confirm” / “无法确认” and an official source is provided where available.  
   **假设**路线或票价资料缺失、不完整或不确定，**当**显示结果时，**则**只将受影响项目标记为“Unable to confirm” / “无法确认”，并在可用时提供官方来源。
2. **Given** an assessment result is shown, **When** the user reviews it, **Then** it states that it uses scheduled data, does not predict real-time punctuality and does not guarantee arrival.  
   **假设**评估结果已显示，**当**用户查看结果时，**则**结果说明其依据为计划数据、不预测实时准点情况，也不保证到达。
3. **Given** an assessment is submitted, **When** location data is sent for route planning, **Then** the user is told that the departure postcode and role location are sent to TfL; personal settings are retained for reuse, while role-specific inputs, dates, times and results are not retained.  
   **假设**用户提交评估，**当**地点资料用于路线规划时，**则**用户会获知出发 postcode 和职位地点会发送给 TfL；个人设置会保留供重复使用，职位输入、日期、时间和结果不会保留。
4. **Given** the user has set a commute-time limit, **When** they change it and reassess, **Then** the explanation uses the new threshold; if no limit is set, the result shows duration without a fit classification.  
   **假设**用户设置了通勤时长上限，**当**修改上限并重新评估时，**则**说明采用新标准；若未设置上限，结果仅显示时长，不给出适配分类。
5. **Given** a role shift crosses midnight or the journey occurs on a different calendar day, **When** the assessment runs, **Then** it uses and labels the actual journey date in London local time, including daylight-saving changes.  
   **假设**班次跨越午夜或路线发生在另一个日历日，**当**系统评估时，**则**采用并标明实际路线日期，按伦敦当地时间处理夏令时变化。

### Edge Cases / 边界情况

- A departure postcode is invalid, approximate or maps to multiple origin points; explain the limitation and allow the user to correct or choose an appropriate result without requiring a full home address.  
  出发 postcode 无效、仅代表近似区域或匹配到多个出发点时，应说明限制，并允许用户修正或选择合适地点，不要求完整家庭地址。
- A workplace postcode, address or branch name is ambiguous; present candidates for the user to choose rather than silently selecting one.  
  工作地点 postcode、地址或门店名称含糊时，应展示候选地点供用户选择，不得静默选定一个地点。
- No scheduled outbound or return service is found for one of the selected dates; report the missing supported option without claiming that all possible travel is impossible.  
  所选日期之一没有查到计划去程或返程服务时，应报告未找到受支持的方案，不得断言所有出行方式都不可能。
- A route includes modes for which concession eligibility or fare treatment differs; list only relevant options, state conditions and mark unsupported amounts unable to confirm.  
  路线包含优惠资格或票价规则不同的交通方式时，只列出相关优惠，说明条件，并将无法支持的金额标记为“无法确认”。
- A journey crosses a concession's time window, maximum duration or qualifying-event boundary; evaluate it according to that policy's documented time basis and fare scope. If the route response lacks the required events or times, do not apply or reject a discount by guessing; mark the amount unable to confirm.  
  行程跨越优惠的时段、最长时长或符合条件事件边界时，按该优惠有文档依据的时间基准和收费范围判断。若路线响应缺少必要事件或时间，不得猜测并应用或排除优惠；将金额标为无法确认。
- A fare change takes effect between the search date and travel date; show the fare effective date used when known.  
  查询日与出行日之间有票价变更时，在已知情况下显示所采用票价的生效日期。
- A planned work or live disruption changes service after the scheduled lookup; explain that real-time changes are outside the assessment and link to an official service source.  
  计划查询后因工程或实时中断导致服务改变时，应说明实时变化不属于本评估范围，并链接官方服务信息。

## Requirements / 需求

### Functional Requirements / 功能需求

- **FR-001**: The system MUST let the user provide and edit a departure postcode without requiring a full home address.  
  **FR-001**：系统 MUST 允许用户提供和修改出发地 postcode，不要求完整家庭地址。
- **FR-002**: The system MUST let the user optionally set and change a maximum acceptable one-way commute duration; when absent, it MUST show duration without a fit classification.  
  **FR-002**：系统 MUST 允许用户选填并修改单程最长通勤时长；未填写时 MUST 仅显示时长，不给出适配分类。
- **FR-003**: The system MUST accept a role location as a postcode, a specific address/location, or a named brand branch, plus earliest shift start, expected latest finish and a travel date.  
  **FR-003**：系统 MUST 接受 postcode、具体地址/地点或品牌门店作为职位地点，并接受最早开班时间、预计最晚下班时间和出行日期。
- **FR-004**: The system MUST support an optional weekend check that adds separate Saturday and Sunday assessments from the Monday-to-Sunday week containing the reference date to the ordinary reference-date assessment, using the entered shift boundaries for all assessed dates. Avoid duplicate assessment dates if the reference date is Saturday or Sunday.  
  **FR-004**：系统 MUST 支持可选周末检查；开启后，在参考日期评估之外，按该日期所在的周一至周日增加周六和周日评估，所有评估日期均使用相同班次边界。若参考日期本身为周六或周日，不得重复评估该日期。
- **FR-005**: The system MUST plan outbound arrival by the earliest shift start minus a configurable buffer, defaulting to 30 minutes, and plan a return after the expected latest finish.  
  **FR-005**：系统 MUST 按最早班次开始时间减去可配置缓冲规划去程，默认缓冲为 30 分钟；并规划预计最晚下班后的返程。
- **FR-006**: For each supported option, the system MUST show scheduled duration and explain comparison with the user's time limit when one is provided.  
  **FR-006**：对于每条受支持路线，系统 MUST 显示计划耗时；仅当用户提供时长上限时，才说明与该上限的比较结果。
- **FR-007**: When available, the system MUST display the Journey Planner route-level fare total and its returned fare items separately from itinerary legs, including the fare/payment basis, travel date, source and relevant item details. The MVP MUST NOT request FareTo station-pair quotes. It MUST NOT infer a new full fare from each leg. If a complete route total is missing or cannot be established, it MUST mark the total unable to confirm; missing fare items MUST NOT be treated as zero, and partial item amounts MUST NOT be labelled as a complete total. Conditional concession fares MUST remain separate, state eligibility/payment assumptions and cite their source. Daily/weekly caps MUST be guidance unless complete travel and same-card/device context is known.  
  **FR-007**：有数据时，系统 MUST 将 Journey Planner 返回的路线总价及票价项与行程 leg 分开显示，并注明票价/付款基础、出行日期、来源和相关票价项明细。MVP MUST NOT 请求 FareTo 站对报价；也不得根据每个 leg 推算出一笔新的完整票价。缺少完整路线总价或无法确认时，MUST 将总价标记为无法确认；缺失票价项不得视为 0，部分金额不得标记为完整总价。条件优惠票价 MUST 单独展示并说明资格/付款条件及来源。若缺少完整行程和同卡/设备信息，每日/每周封顶 MUST 仅作为说明。
- **FR-008**: The system MUST list common concessions that may apply to the route's modes, with eligibility conditions and official sources where available.  
  **FR-008**：系统 MUST 列出可能适用于路线交通方式的常见优惠，并在可用时提供资格条件和官方来源。
- **FR-009**: For each route-relevant concession, the system MUST identify the official eligibility time basis, qualifying event or interval, applicable fare scope, payment conditions and effective date when available. It MUST show a discounted amount only if the available scheduled journey data and user-confirmed eligibility support evaluating all required conditions for that fare scope. It MUST NOT infer eligibility or ineligibility from whole-route or leg duration unless the rule explicitly uses that duration. If required times, events, scope or eligibility are missing or ambiguous, it MUST show the conditions and source, mark the amount unable to confirm, and exclude that amount from the route total. The user MAY select concessions they hold to highlight matching scenarios; the system MUST NOT imply they qualify for an unselected concession.  
  **FR-009**：对于每项路线相关优惠，系统 MUST 在可用时明确官方规则规定的资格时间基准、符合条件的事件或时间间隔、适用收费范围、付款条件和生效日期。只有当现有计划路线数据及用户确认的资格足以判断该收费范围的所有必要条件时，MUST 显示折后金额。除非规则明确以整条路线或 leg 时长为条件，MUST NOT 据此推断适用或不适用。若必要时间、事件、收费范围或资格缺失或含糊，MUST 显示条件和来源、将金额标为无法确认，并从路线总价中排除该金额。用户 MAY 选择自己持有的优惠以突出显示；系统 MUST NOT 暗示用户符合未选择优惠的资格。
- **FR-010**: The system MUST accept an optional payment method including Apple Pay/contactless device, contactless bank card, Oyster, or another method. When no method is selected, it MUST show the adult pay-as-you-go route fare returned by Journey Planner when available, label that basis, and keep concession scenarios separate and explicitly conditional.  
  **FR-010**：系统 MUST 接受可选付款方式，包括 Apple Pay/设备感应支付、银行卡感应支付、Oyster 或其他方式。未选择付款方式时，如 Journey Planner 返回成人 PAYG 路线票价，系统 MUST 显示并标明该付款口径；优惠方案须单独展示并明确标注条件。
- **FR-011**: The system MUST NOT claim an exact daily charge when other journeys or same-card/device conditions cannot be established; it MUST present cap rules as guidance.  
  **FR-011**：当其他行程或同卡/设备条件无法确认时，系统 MUST NOT 声称全天确切费用；MUST 仅将封顶规则作为参考说明。
- **FR-012**: The system MUST identify the date/time basis and source for material journey and fare information.  
  **FR-012**：系统 MUST 标明重要路线和票价信息对应的日期/时间依据及来源。
- **FR-013**: When route, fare or concession information is missing, incomplete or unreliable, the system MUST mark the affected item “Unable to confirm” / “无法确认” and link to an official source where available.  
  **FR-013**：路线、票价或优惠信息缺失、不完整或不可靠时，系统 MUST 将受影响项目标记为“Unable to confirm” / “无法确认”，并在可用时链接官方来源。
- **FR-014**: The system MUST state that it uses scheduled public-transport information, does not predict real-time punctuality and does not guarantee arrival.  
  **FR-014**：系统 MUST 说明其依据为计划公共交通信息、不预测实时准点情况，也不保证到达。
- **FR-015**: The system MUST leave the application decision to the user and MUST NOT provide a universal job-quality ranking.  
  **FR-015**：是否申请 MUST 由用户决定；系统 MUST NOT 提供普遍适用的职位质量排名。
- **FR-016**: The MVP MUST NOT discover jobs, scrape listings or integrate with job-board/career-site accounts.  
  **FR-016**：MVP MUST NOT 搜索职位、抓取招聘信息或接入招聘平台/雇主招聘网站账户。
- **FR-017**: The interface and assessment results MUST be available in both Chinese and English.  
  **FR-017**：界面和评估结果 MUST 支持中英双语。
- **FR-018**: Before requesting an assessment, the system MUST disclose that the departure postcode and role location are sent to TfL for route planning. It MUST retain personal commute settings for reuse but MUST NOT retain role-specific inputs, dates, times or assessment results.  
  **FR-018**：发起评估前，系统 MUST 告知用户出发 postcode 和职位地点会发送给 TfL 用于路线规划。系统 MUST 保留个人通勤设置供重复使用，但 MUST NOT 保存职位输入、日期、时间或评估结果。
- **FR-019**: The system MUST interpret dates and times as London local time, account for daylight-saving changes and label actual journey dates when they differ from the selected shift date.  
  **FR-019**：系统 MUST 将日期和时间解释为伦敦当地时间，处理夏令时变化，并在实际路线日期与所选班次日期不同时标明实际日期。
- **FR-020**: The system MUST retain only the departure postcode, optional time limit, arrival buffer, selected concessions and preferred payment method as reusable personal settings; the user MUST be able to edit or clear them.  
  **FR-020**：系统 MUST 仅将出发 postcode、可选时长上限、到岗缓冲、已选优惠和首选付款方式作为可重复使用的个人设置保留；用户 MUST 可以修改或清除这些设置。
- **FR-021**: A user-defined maximum commute-cost input is outside the initial MVP and MUST be considered only after the core commute assessment is complete.  
  **FR-021**：用户自设的通勤费用上限输入不属于首版 MVP，MUST 在核心通勤评估完成后再考虑。
- **FR-022**: The system MUST invalidate destination candidates when the destination query changes. Retryable journey-provider failures MUST be explained and allow the user to retry the current assessment without persisting its inputs or results.  
  **FR-022**：目的地查询内容改变时，系统 MUST 使旧候选地点失效。路线数据提供方发生可重试错误时，MUST 说明错误并允许用户重试本次评估，且不得持久保存输入或结果。
- **FR-023**: For every returned itinerary, the system MUST prominently show its resolved origin and destination using the first and last itinerary-leg endpoints. It MUST advise users to verify the destination against the exact employer-listed branch and prefer a postcode or specific address when a brand/location query could match ambiguously.  
  **FR-023**：对于每条返回路线，系统 MUST 使用首尾 itinerary leg 的端点醒目显示实际解析出的起点和终点。当品牌/地点文本可能含糊匹配时，MUST 提醒用户对照雇主列出的具体门店核实，并优先填写 postcode 或具体地址。
- **FR-024**: The system MUST distinguish a successful query with no scheduled journey from a failed/unavailable provider query; neither state may be presented as proof that no service exists. Results MUST show when the provider data was queried in London local time.  
  **FR-024**：系统 MUST 区分“查询成功但没有返回计划路线”和“路线提供方查询失败/不可用”；两种状态都不得被表述为“已证明没有服务”。结果 MUST 显示提供方数据的查询时间（伦敦当地时间）。
- **FR-025**: After a commute profile is configured, its detailed settings MUST be collapsible while remaining editable; the job check and results MUST remain distinct, clearly headed modules on the same page.  
  **FR-025**：通勤档案配置完成后，详细设置 MUST 可以收起但仍可编辑；岗位评估和结果 MUST 在同一页面中作为标题清楚、彼此区分的模块呈现。

### Key Entities / 关键实体

- **Commute Profile**: Departure postcode, optional one-way time limit, arrival buffer, selected concessions and preferred payment method.  
  **通勤档案**：出发 postcode、可选单程时长上限、到岗缓冲、已选优惠和首选付款方式。
- **Role Commute Request**: Role location, earliest shift start, expected latest finish, reference date, optional weekend check and optional payment-method override. It is transient and is not retained.  
  **职位通勤请求**：职位地点、最早开班时间、预计最晚下班时间、参考日期、可选周末检查及可选付款方式覆盖设置。该请求仅临时处理，不予保存。
- **Scheduled Journey Option**: Dated outbound or return route, legs/transfers, scheduled duration, source and date/time basis.  
  **计划路线方案**：指定日期的去程或返程路线、路段/换乘、计划耗时、来源及日期/时间依据。
- **Fare and Concession Information**: Provider-returned route total and fare-item breakdown (distinct from itinerary legs); conditional concession scenarios with rule-specific time basis, evaluated fare scope, eligibility status, payment assumptions, amount status, effective basis and sources; cap guidance.  
  **票价与优惠信息**：提供方返回的路线总价及票价项拆分（与行程 leg 区分）；注明各项优惠规则时间基准、评估收费范围、资格状态、付款假设、金额状态、生效依据和来源的条件优惠方案；封顶说明。
- **Commute Assessment**: Explanation of route availability, durations, optional personal-limit comparison, fare information and uncertainty.  
  **通勤评估**：路线可用性、时长、可选个人上限比较、票价信息和不确定性说明。

## Success Criteria / 成功标准

### Measurable Outcomes / 可衡量结果

- **SC-001**: For at least 4 of 5 representative roles, the user can complete the commute pre-screen in under 2 minutes without separate manual route searches.  
  **SC-001**：对 5 个代表性职位中的至少 4 个，用户可在 2 分钟内完成通勤预筛，无需分别手动搜索路线。
- **SC-002**: In 10 representative weekday, Saturday and Sunday checks, displayed durations and provider-supported route fares agree with cited sources for the same journey date/time; fare items are not confused with legs, totals are not double-counted, and incomplete fare data is labelled unable to confirm.  
  **SC-002**：在 10 个有代表性的工作日、周六和周日案例中，计划耗时和有提供方依据的路线票价均与相同日期/时间的引用来源一致；不混淆票价项与 leg、不重复计算总价，并将不完整票价资料标记为无法确认。
- **SC-003**: Every result identifies its scheduled-data and fare basis, states the limits of the assessment, and avoids implying real-time punctuality or guaranteed arrival.  
  **SC-003**：每份结果均标明计划数据及票价依据，说明评估边界，且不暗示实时准点或保证到达。
- **SC-004**: In at least 4 of 5 representative route assessments, the user can identify whether a listed concession is eligible, ineligible or unable to confirm for the stated route/time, understand its policy-specific time basis and fare scope, and identify any amount that reflects a concession they selected.  
  **SC-004**：在 5 个代表性路线评估中的至少 4 个，用户能够判断所列优惠对该路线/时间是适用、不适用还是无法确认，理解其专属时间基准和收费范围，并识别采用所选优惠的金额。

## Assumptions / 假设

- The MVP is for the idea owner's personal use; potential open-source reuse does not expand the initial scope.  
  MVP 供提出者本人使用；未来可能开源，但不因此扩大首版范围。
- The user manually finds job listings and enters the location; job discovery and scraping remain out of scope.  
  用户自行寻找职位并输入地点；职位发现和抓取不在范围内。
- A reference date identifies the ordinary assessment day and target week. With weekend mode on, assess the reference date plus Saturday and Sunday in that week, deduplicating dates when the reference date itself is a weekend day. The user chooses a new reference date for each assessment.  
  参考日期指定普通评估日并确定目标周。开启周末模式后，评估参考日期以及该周周六和周日；若参考日期本身是周末，则去重。用户每次评估重新选择参考日期。
- Where a location name is ambiguous, the user chooses from matching location candidates.  
  地点名称有歧义时，由用户从匹配候选项中选择。
- If no payment method is selected, show the adult pay-as-you-go route fare returned by Journey Planner when available and label its basis; show concession scenarios separately with their payment and eligibility assumptions.  
  未选择付款方式时，如 Journey Planner 返回成人 PAYG 路线票价，则显示并标明该付款口径；优惠方案另行展示，并注明付款方式和资格假设。
- Fare and scheduled-service information may vary by date, payment method, mode and concession eligibility. The tool does not claim an exact full-day total when other journeys or payment-device details are unknown.  
  票价和计划服务可能因日期、付款方式、交通方式和优惠资格而异。其他行程或支付设备信息未知时，工具不声称全天确切总费用。
- Concession rules may use different time bases and fare scopes. Total route duration or a vehicle leg's duration is not a general eligibility test; when required rule inputs are unavailable, show conditions and source but do not calculate or include a discounted amount.  
  不同优惠可能使用不同的时间基准和收费范围。整条路线或某个车辆 leg 的时长不能作为通用资格判断；若缺少规则所需输入，只显示条件和来源，不计算或计入折后金额。
- The user controls their retained personal settings and may clear them. Role-specific request fields and results are not retained.  
  用户控制并可清除被保留的个人设置；职位请求字段和评估结果不予保存。
- Success thresholds are initial validation targets and may be revised after use.  
  成功门槛是初始验证目标，实际使用后可以调整。

## Future Product Direction (Not an MVP Requirement) / 后续产品方向（非 MVP 需求）

The product owner wants future assessments to explain source-backed ways a traveller may reduce fares for a route portion or interchange, such as an eligible out-of-station interchange, bus/tram Hopper, or a pink-reader validation for a recognised alternative route. A suggestion must identify the exact condition and action, payment assumptions, official source and uncertainty. Fare guidance must follow TfL fare journeys and charging rules; it must not infer a separate charge or saving from the number of journey legs. Do not claim a saving amount unless the applicable fare comparison is supported by authoritative data. This direction is recorded for later refinement and is not added to the current MVP acceptance criteria.

Reference material: [TfL pay the right fare](https://tfl.gov.uk/fares/pay-the-right-fare), [out-of-station interchanges](https://tfl.gov.uk/corporate/publications-and-reports/out-of-station-interchanges), [bus and tram fares](https://tfl.gov.uk/fares/find-fares/bus-and-tram-fares), and [Single Fare Finder](https://tfl.gov.uk/fares/find-fares/single-fare-finder).

提出者希望后续版本能在路线的某一段或换乘点，提示有官方依据的省钱方式，例如符合条件的站外换乘（OSI）、公交/电车 Hopper，或通过粉色读卡器验证可识别的替代路线。提示必须说明具体条件和操作、付款假设、官方来源及不确定性。票价说明应遵循 TfL 对收费旅程及收费规则的定义，不能根据路线 leg 数量推断收费次数或节省金额。只有权威资料支持时才可声称具体节省金额。此方向先记录供后续细化，不加入当前 MVP 验收标准。

参考资料：[TfL 正确支付车费说明](https://tfl.gov.uk/fares/pay-the-right-fare)、[站外换乘](https://tfl.gov.uk/corporate/publications-and-reports/out-of-station-interchanges)、[公交与电车票价](https://tfl.gov.uk/fares/find-fares/bus-and-tram-fares)及[单程票价查询](https://tfl.gov.uk/fares/find-fares/single-fare-finder)。
