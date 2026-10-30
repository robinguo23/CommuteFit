# Concept / 概念方案：伦敦求职通勤评估工具

- **Slug**: london-commute-check
- **Created / 日期**: 2026-10-26
- **Recommended option / 推荐选项**: Option B — Single-role commute-fit assessment / 单职位通勤适配评估

## Options / 选项

### Option A — Keep current approach / 维持现状

- **Sketch / 概述**: The idea owner continues using existing journey planners and personally translate route information into a decision about a role. / 求职者继续使用现有路线工具，自行把路线信息转换成对职位通勤的判断。
- **Appetite / 投入量**: small (days) / 小（数天）— no new product; only retain the current approach.
- **Trade-offs / 取舍**: Avoids building a duplicate route planner; leaves the proposed preference-based judgment and its value untested. / 避免重复开发路线规划器；但不验证偏好适配判断是否有价值。
- **Rabbit holes / 主要风险**: Assuming current alternatives are adequate without knowing how target users actually assess jobs. / 未了解目标用户如何评估职位，就假设现有替代方案已足够。

### Option B — Single-role commute-fit assessment / 单职位通勤适配评估

- **Sketch / 概述**: The idea owner checks one role at a time while repeating the check across listings. Using a postcode, workplace, shift availability and user-set commute-time limit, it checks arrival before the earliest workable shift, return after the latest shift, relevant Sunday service and provider-supported route fares. It lists common route-relevant concessions and shows a conditional discounted amount only when the official rule defines the time basis and fare scope and the available journey data can evaluate them. Otherwise it shows the eligibility conditions and marks the amount “Unable to confirm.” It distinguishes route totals from fare items and vehicle legs, explains fare assumptions and cap guidance, and does not claim a final card charge. It uses scheduled public-transport data and does not predict real-time punctuality or guarantee arrival. It does not source or rank job listings. The MVP is for personal use; open-source reuse may be considered later. / 提出者逐个岗位筛查，并在多个职位间重复使用同一套检查。输入 postcode、工作地点、可工作班次及用户自设的通勤时间上限后，检查最早到岗、最晚返程、相关周日服务及有提供方依据的路线票价。产品列出路线可能适用的常见优惠；只有当官方规则明确规定时间基准和收费范围，且现有路线数据足以判断时才显示折后金额，否则说明资格条件并将金额标为“无法确认”。路线总价、票价项和车辆 leg 分开呈现，说明票价假设及封顶参考，不声称最终刷卡扣款。仅依据计划公共交通数据，不预测实时准点或保证到达；不负责抓取或排序职位。MVP 供提出者本人使用，未来可考虑开源复用。

- **Appetite / 投入量**: medium (weeks) / 中（数周） as a rough budget only; data and scoring uncertainty may change it. / 仅作粗略投入档位；数据与判断标准的不确定性可能改变投入。
- **Trade-offs / 取舍**: Stays close to the stated idea and avoids job-board integration; depends on a usable origin, defensible personal-fit criteria and an authoritative route-data source, none of which is validated yet. / 紧扣原始想法且避免职位网站集成；依赖可用的出发地、可信的个人适配标准和权威路线数据，这些目前都未验证。
- **Rabbit holes / 主要风险**: Turning diverse preferences into a misleading universal score; irregular schedules, disrupted service, accessibility needs, privacy around origin locations, and unsupported data coverage. / 将多样偏好压成误导性的统一分数；不规则排班、服务中断、无障碍需求、出发地隐私和数据覆盖不足。

### Option C — Compare roles or integrate with job search / 职位比较或求职集成

- **Sketch / 概述**: Job seekers compare commute fit across several roles, potentially alongside job listings. / 求职者比较多个职位的通勤适配度，未来可与职位列表并列展示。
- **Appetite / 投入量**: large (months) / 大（数月） as a rough budget only.
- **Trade-offs / 取舍**: Could support a broader job-search decision; expands scope and depends on demand, listing access and consistent data across many roles. / 可能支持更完整的求职决策；但扩大范围，并依赖需求、职位数据接入及多职位数据一致性。
- **Rabbit holes / 主要风险**: Job-board permissions and integrations, stale or ambiguous listing locations/schedules, and comparing scores users may not trust. / 职位平台授权与集成、职位地点或时间信息过期或含糊、用户不信任的分数比较。

## Recommendation / 推荐意见

Prefer Option B. Its value is the repeated pre-application decision across shift boundaries and affordability, with the limits of fare and concession evidence made explicit—not another generic route planner or fare display. TfL already provides journey-time routing and estimated adult PAYG fare information, while Citymapper publicly advertises fare comparison and the England bus cap. The first concept should bring earliest arrival, latest return/Sunday service, provider-supported route fare details and carefully qualified concession information together for one role at a time. The MVP is for personal use, so broad-market demand is not a launch gate. Open-source reuse may be considered later.

建议选项 B。其价值在于投递前反复判断班次边界与负担能力，并清楚说明票价和优惠依据的边界，而不是再造通用路线规划器或票价显示器。TfL 已提供按时间规划路线和成人 PAYG 估价，Citymapper 也公开宣传路线票价比较与英格兰公交封顶。初始概念应逐个职位整合最早到岗、最晚返程/周日服务、提供方支持的路线票价细节及有条件的优惠信息。MVP 面向个人自用，因此大众需求不是启动门槛。未来可考虑开源复用。

## Out of Scope (for the recommended option) / 推荐方案的范围之外

- Job discovery, scraping chain-brand career sites, job-board integration or ranking roles by overall career value.
- Replacing Google Maps, TfL Go or Citymapper for turn-by-turn navigation.
- Guaranteeing a personally discounted fare without confirmed eligibility and payment assumptions.
- Predicting real-time punctuality or guaranteeing arrival; results assess scheduled service only.
- Treating fares or scheduled service as timeless: fares have effective dates and planned/live service changes.
- 职位发现、抓取连锁品牌招聘网站、职位平台集成，或按职业整体价值排序。
- 取代 Google Maps、TfL Go 或 Citymapper 的逐步导航。
- 未确认优惠资格和支付方式前，保证个人折扣票价。
- 预测实时准点或保证到达；结果只评估计划班次。
- 把票价或计划班次视为永远不变：票价有生效日期，服务会有计划和实时变化。

## Assumptions to Validate / 待验证假设

- Official TfL journey/fare data can produce useful date-specific estimates for the selected modes and times; unsupported cases can be identified clearly. / TfL 官方路线/票价数据可对选定方式及时段提供有用的日期化估算，并能明确识别不支持的情形。
- The first release can show provider-supported route totals and fare items, and evaluate only those conditional concessions whose rule-specific time basis and fare scope are supported by available journey data; other amounts remain unknown. / 首版能显示提供方支持的路线总价和票价项，并只计算路线数据足以判断其专属时间基准和收费范围的条件优惠；其他金额保持未知。
- A single-role result can save meaningful time over repeated manual searches, even without job-board integrations. / 即使不接入招聘网站，单职位结果也能显著减少反复手动搜索的时间。
