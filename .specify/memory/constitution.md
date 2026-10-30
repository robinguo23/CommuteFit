# Current scope amendment — 2026-10-29

**Version: 4.1.0**

用户已授权 003 工作通勤检查和 004 职位链接提取模块。当前增量以各自 spec 为准，允许按用户提交的单个公开 URL 或粘贴文本提取岗位地点和排班证据，不建设职位发现索引、不引入付费模型。保留来源、未知不伪造、用户确认和不持久保存岗位资料的原则。旧章程中排除抓取的首版范围不限制此已授权增量。理由：减少用户为通勤检查手动录入岗位资料的负担。

# Previous MVP scope amendment / 旧版 MVP 范围修订

**Version: 4.0.0 — 2026-10-27**

用户明确将首版收窄为两个 postcode 查询现在出发的路线与全程报价。当前以 `specs/002-postcode-journey/spec.md` 为准。
替代下方历史版本中要求档案持久化、班次、周末、优惠及付款方式的首版必需范围。
当前不需要数据库、不保存查询；已有档案数据不删除。保留来源署名、数据最小化、未知金额不伪造原则。
理由：先完成可独立验证的路线与全程价格闭环；其他功能延后。相关当前规格、计划、任务与 README 同步更新。

---

## Historical constitution (3.0.0; superseded where conflicting)

<!--
Sync Impact Report
- Version change: 2.1.0 → 3.0.0 (replace mandatory per-segment fare sums with provider-scoped route fares and policy-specific concession evaluation)
- Modified principles: II. Explainable, Source-Backed Results; V. Keep the MVP Focused on Repeated Commute Checks
- Modified sections: Product Boundaries
- Added sections: none
- Removed sections: none
- Rationale: Archived TfL responses show that vehicle legs, fare items and route totals are distinct; summing a full fare per leg can overstate a route. Conditional discounts also require each policy's own eligibility time basis and fare scope.
- Follow-up TODOs: none; the problem/concept/decision handoff and feature artifacts were aligned on 2026-10-27
-->
# London Commute Fit Constitution
# 伦敦通勤适配工具项目章程

## Core Principles
## 核心原则

### I. Scheduled Data and Honest Limits
### I. 依据计划数据并诚实说明边界

The product MUST assess scheduled public-transport information for the stated date or day type. It MUST NOT predict real-time punctuality or guarantee arrival. Results MUST distinguish a scheduled option that was not found from information that could not be confirmed.

产品 MUST 根据所注明日期或日期类型的计划公共交通信息进行评估。产品 MUST NOT 预测实时准点情况或保证用户能准时到达。结果 MUST 区分“未找到计划路线”和“信息无法确认”。

### II. Explainable, Source-Backed Results
### II. 结果可解释且有来源依据

Every material route and fare claim MUST identify its source and date/time basis. The product MUST explain scheduled duration, transfers, standard fares, potentially applicable concessions, payment assumptions, and daily cap conditions. A conditional fare amount MUST be shown only when the authoritative rule's eligibility basis and fare scope can be evaluated from available journey, payment and user-confirmed eligibility data. Unsupported or indeterminate information MUST be labelled as unknown and linked to an authoritative source where available.

每项重要路线和票价结论 MUST 标明来源及对应日期/时间。产品 MUST 解释计划耗时、换乘、标准票价、可能适用的优惠、付款方式假设和每日封顶条件。只有当现有路线、付款方式及用户确认的资格资料足以判断权威规则的资格依据和收费范围时，MUST 显示条件优惠金额。缺乏依据或无法确定的信息 MUST 标记为未知；如有权威来源，应提供链接。

### III. Minimize and Limit Location Data
### III. 最小化并限制位置资料

The user MUST be able to provide a departure postcode without providing a full home address. The product MUST retain only the user's personal commute settings needed for reuse. It MUST NOT retain role locations, shift dates or times, or assessment history. Departure location data MUST only be used to support the user's requested commute assessments.

用户 MUST 可以提供出发地 postcode，而无需提供完整家庭地址。产品 MUST 仅保留重复使用所需的个人通勤设置。产品 MUST NOT 保存职位地点、班次日期或时间，也不得保存评估历史。出发地资料 MUST 仅用于支持用户请求的通勤评估。

### IV. The User Owns Thresholds and Decisions
### IV. 标准由用户设定，决定由用户作出

The user MUST be able to set an optional acceptable one-way commute-time limit. When provided, the product MUST explain fit against that personal limit. The product MUST leave the decision to apply to the user and MUST NOT present a universal job-quality or suitability score.

用户 MUST 可以自行设置可选的单程通勤时间上限。用户设置上限后，产品 MUST 按该个人标准解释通勤是否适配。是否投递 MUST 由用户自行决定；产品 MUST NOT 给出普遍适用的职位质量或适配分数。

### V. Keep the MVP Focused on Repeated Commute Checks
### V. 聚焦反复通勤评估的 MVP

The first release MUST focus on the idea owner's repeated pre-application commute checks: arrival for the earliest available shift, return after the expected latest finish, Saturday and Sunday service, provider-returned route fares and fare-item details, route-applicable conditional concession information, and payment-method-aware fare guidance. The product MUST list relevant concession options and their eligibility conditions without assuming the user qualifies. It MUST show a conditional concession amount only when an official rule defines the eligibility time basis and fare scope and the available journey data can evaluate them. The user may select concessions they hold to highlight matching offers. A user-defined maximum commute-cost threshold is deferred until after the core commute assessment works. Job discovery, listing scraping, and job-board integrations remain outside the MVP.

首版 MUST 聚焦提出者反复进行的投递前通勤检查：最早可上班时段的到岗路线、预计最晚下班后的返程、周六和周日服务、提供方返回的路线票价及票价项明细、路线可能适用的条件优惠信息，以及结合付款方式的票价参考。产品 MUST 列出相关优惠及资格条件，但不得推定用户符合资格。只有官方规则明确规定资格时间依据和收费范围，且现有路线数据足以判断时，MUST 显示条件优惠金额。用户可选择自己持有的优惠以突出匹配方案。用户自设的通勤费用上限延后至核心通勤评估完成后再做。职位发现、招聘信息抓取及招聘平台集成不属于 MVP。

## Product Boundaries
## 产品边界

The product MUST show the provider-returned route-level fare and returned fare items as distinct from itinerary legs. It MUST NOT calculate a new full fare for every leg, sum such leg fares, or add a FareTo quote to the route total unless authoritative evidence establishes that the charging scopes are disjoint. Conditional concession amounts MUST be shown separately from the standard route fare and only when an official rule identifies its eligibility time basis, qualifying event or interval, applicable fare scope, and payment/eligibility conditions, and the available scheduled journey data supports that evaluation. Do not infer eligibility or ineligibility from total journey or leg duration unless the rule explicitly uses that duration. If required facts or fare scope are missing or ambiguous, show the concession conditions and source but mark its amount “Unable to confirm” / “无法确认”; do not include it in the route total. Missing provider fares and partial item amounts MUST remain unknown and MUST NOT be treated as zero or as a complete total. Daily/weekly caps remain guidance unless complete travel and same-card/device context is known.

产品 MUST 将提供方返回的路线级票价及票价项与行程 leg 分开显示。不得为每个 leg 重新计算一笔完整票价、将这些 leg 票价相加，或将 FareTo 报价加到路线总价上；除非权威依据确认收费范围互不重叠。条件优惠金额 MUST 与标准路线票价分开显示，并且仅在官方规则明确规定资格时间依据、符合条件的事件或时间间隔、适用收费范围及付款/资格条件，且计划路线数据足以判断时显示。除非规则明确以整段路线或 leg 时长为条件，不得根据总行程或 leg 时长推断适用或不适用。若必要事实或收费范围缺失或含糊，应显示优惠条件和来源，并将金额标记为“Unable to confirm” / “无法确认”；不得将其计入路线总价。提供方票价缺失或票价项不完整时必须保持未知，不得当作 0 或完整总价。除非已知完整行程和同卡/设备情况，每日/每周封顶仅作为说明。

## Data Retention
## 数据留存

The product MUST retain the departure postcode, optional commute-time limit, arrival buffer, selected concessions, and preferred payment method for reuse. It MUST NOT persist role-specific request data or assessment results. Retained personal settings MUST remain under the user's local control.

产品 MUST 为重复使用而保留出发地 postcode、可选通勤时长上限、到岗缓冲时间、已选优惠和首选付款方式。产品 MUST NOT 持久保存职位请求资料或评估结果。保留的个人设置 MUST 由用户在本机控制。

## Delivery Workflow
## 交付流程

Each feature MUST be grounded in an approved specification before planning implementation. Planning and task breakdown MUST preserve the scope and constraints in that specification. Changes to scope or product boundaries MUST be reflected in the relevant project artifacts before implementation proceeds.

每项功能在制定实现计划前 MUST 有经确认的规格。规划和任务拆分 MUST 保留规格中的范围与约束。若范围或产品边界发生变化，MUST 先更新相关项目文档，再继续实现。

## Governance
## 治理

This constitution governs product specifications and implementation decisions for this project. Amendments require a documented rationale, an update to the version and amendment date, and review of affected specifications. Versioning follows semantic versioning: MAJOR for incompatible principle changes, MINOR for new or materially expanded principles, PATCH for clarifications that do not change intent. Feature artifacts MUST be reviewed for alignment with these principles before implementation is considered complete.

本章程约束本项目的产品规格和实现决策。修订 MUST 记录理由、更新版本及修订日期，并检查受影响的规格。版本遵循语义化版本规则：不兼容的原则变更提升 MAJOR；新增或实质扩展的原则提升 MINOR；不改变意图的澄清提升 PATCH。实现完成前 MUST 检查功能产物是否符合本章程。

**Version**: 3.0.0 | **Ratified**: 2026-10-26 | **Last Amended**: 2026-10-27
