# Decision / 决策：伦敦求职通勤评估工具

- **Slug**: london-commute-check
- **Decided / 日期**: 2026-10-27
- **Verdict / 结论**: go

## Scorecard / 评分卡

| Criterion / 标准 | Rating / 评级 | Justification / 理由 |
|---|---|---|
| Problem validity / 问题有效性 | adequate / 充分 | The idea owner gives a concrete, repeated workflow tied to applying for urgent shift-based work; this is adequate for a first user-led version. / 提出者给出与紧急轮班求职直接相关、反复出现的具体流程，足以支持先做面向首位用户的版本。 |
| Evidence strength / 证据强度 | adequate / 充分 | The owner's repeated workflow is firsthand evidence. An archived 30-request TfL batch supports route-level fare handling for tested routes, while missing fares and untested concession cases remain explicit limits. This still does not establish broader market demand. / 用户反复进行的流程有一手依据。归档的 30 个 TfL 请求支持在已测路线中采用路线级票价处理；缺失票价和未测试的优惠案例仍是明确限制。这仍不能证明更广泛的市场需求。 |
| Value vs. inaction / 相对不作为的价值 | adequate / 充分 | The current process repeats manual route searches and fare research before each application; the user's stated fatigue and decision importance are direct evidence. / 当前每次申请前都要重复查询路线和票价；用户明确表示疲惫且通勤决定是否申请，这是直接证据。 |
| Feasibility / appetite fit / 可行性与投入匹配 | adequate / 充分 | A bounded single-role workflow can use official time-specific routes and returned route fares. Conditional discount amounts must be restricted to policies whose eligibility time basis and fare scope can be evaluated; unsupported cases stay unknown. / 有范围限制的单职位流程可利用官方按时间规划的路线和路线级票价。条件优惠金额仅限于其资格时间基准及收费范围可被判断的规则；不支持的案例保持未知。 |
| Strategic fit / 战略契合度 | adequate / 充分 | The ratified constitution now prioritizes explainable source-backed results and a personal-use repeated commute assessment. / 已通过的项目章程明确要求结果有来源、可解释，并聚焦个人反复进行的通勤评估。 |
| Risk posture / 风险状态 | adequate / 充分 | Risks are bounded by keeping provider totals and fare items separate from legs, applying concessions only under each rule's stated time and scope conditions, and marking unsupported amounts unknown. Broad adoption is not claimed. / 通过区分提供方总价、票价项和 leg，按每项规则的时间及收费范围条件判断优惠，并将不支持的金额标为未知，可限制主要风险；不声称已验证大众市场。 |

## Verdict & Rationale / 结论与理由

**Go — continue with a bounded personal-use MVP.** The idea owner supplied direct evidence of a repeated, consequential task: before applying to shift-based jobs, they check earliest arrival, latest return, Sunday service and affordability. The archived TfL API batch confirms useful route-level fare data for tested cases and also exposes missing-fare and fare-scope limits. TfL Go and Citymapper already cover individual route/fare lookup; the concept is the repeated job pre-screen, not another general planner. The MVP uses scheduled data, never guarantees arrival or final card charges, and leaves the application decision to the user. It shows provider-returned route totals and fare items separately from legs. It lists relevant concessions, but calculates a conditional amount only when the official rule's time basis, fare scope, payment assumptions and user-confirmed eligibility can be evaluated from available journey data. Otherwise it shows the condition and source and marks the amount unable to confirm. Broader market demand remains unproven; open-source reuse may be considered later.

**Go：继续推进范围受限的个人自用 MVP。** 提出者本人提供了反复且影响投递决定的任务证据：申请轮班岗位前需检查最早到岗、最晚返程、周日服务和通勤负担。归档的 TfL API 批次确认已测案例中存在有用的路线级票价数据，也揭示了票价缺失和收费范围限制。TfL Go 与 Citymapper 已覆盖单次路线/票价查询；本产品的定位是反复进行的职位通勤预筛，而非另一个通用路线规划器。MVP 使用计划数据，不保证到达或最终刷卡扣款，是否投递由用户决定。提供方路线总价和票价项与 leg 分开显示。产品列出相关优惠，但只有当现有路线数据足以依据官方规则的时间基准、收费范围、付款假设和用户确认的资格进行判断时，才计算条件优惠金额；否则显示条件与来源，并将金额标为无法确认。更广泛市场需求仍未验证；未来可考虑开源复用。

## If go — Handoff to `$speckit-specify` / Go 后交给 `$speckit-specify`

- **Problem / 问题**: Repeatedly deciding whether each shift-based part-time job is reachable and affordable from home takes several separate route searches and manual fare checks. / 逐个判断轮班兼职从家出发是否可达且负担得起，需要反复查路线和手动核算票价。
- **Chosen approach / 推荐方案**: Option B — one-role-at-a-time pre-screen for personal use, reusable across job listings; check earliest arrival, latest return and relevant Sundays, with a reasoned time/cost summary. / 选项 B：逐个职位预筛，可在多个招聘列表间重复使用；检查最早到岗、最晚返程和相关周日，并给出有依据的时间/费用总结。
- **In scope / Out of scope**: Use a postcode, role location, shift availability and user-set time limit; show scheduled routes, Journey Planner route totals and fare items, and list route-relevant concession conditions. The MVP does not request FareTo. Calculate a conditional concession amount only when its official time basis, fare scope, payment assumptions and user-confirmed eligibility are evaluable from the available route data; otherwise show its conditions/source and mark the amount unable to confirm. Do not sum full fares per leg or claim a final card charge. When no payment method is selected, label the Journey Planner adult pay-as-you-go fare basis when available. No job discovery, listing scraping, ranking or job-board integration. / 范围内：使用 postcode、岗位地点、班次和用户自设时长上限；呈现计划路线、Journey Planner 路线总价和票价项，并列出路线相关优惠条件。MVP 不请求 FareTo。只有当现有路线数据足以判断官方时间基准、收费范围、付款假设及用户确认的资格时，才计算条件优惠金额；否则显示条件/来源并将金额标为无法确认。不得按 leg 重复计价，也不得声称是最终刷卡扣款。未选择付款方式时，如有 Journey Planner 成人 PAYG 票价，应标明其付款口径。范围外：职位发现、招聘抓取、职位排序和平台集成。
- **Success metrics / 成功指标**: Reduce time spent screening one role versus the user's current workflow; match route/fare details to TfL for representative early, late and Sunday cases; user can explain why a role passes/fails their own threshold. Baselines/targets remain to be set in specification. / 成功指标：比用户当前流程减少单职位筛查时间；代表性的早班、晚班和周日案例与 TfL 路线/费用信息一致；用户能说明职位为何符合/不符合自己的标准。基线和目标值在规格阶段确定。
- **Carried-forward questions / 后续问题**: Which common concession policies should be catalogued first, and what event/time basis and fare scope does each policy use? Which API fields provide those required facts for each route type? Keep unsupported or ambiguous concession amounts unknown. Baselines for saved time and factual accuracy remain to be measured. / 后续问题：首批应收录哪些常见优惠？每项规则以什么事件/时间基准和收费范围判断？不同路线类型的 API 是否提供所需事实？不支持或含糊的优惠金额保持未知。节省时间和事实准确度的基线仍需测量。

