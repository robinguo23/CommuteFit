# Requirements Quality Checklist: Personal Commute Fit Assessment
# 需求质量清单：个人通勤适配评估

**Purpose**: Review clarity, fare accuracy, scope and privacy in the personal commute MVP requirements.  
**用途**：审查个人通勤 MVP 需求的清晰度、票价准确性、范围和隐私。

**Created**: 2026-10-26  
**创建日期**：2026-10-26

**Feature**: [spec.md](../spec.md)  
**功能规格**：[spec.md](../spec.md)

**Review Ownership**: `[x]` means the requirements-quality criterion was reviewed and satisfied; it does not mean implementation is complete.  
**审阅责任**：`[x]` 表示该需求质量标准已审阅且符合，不代表实现已完成。

**Marker Semantics**: `[x]` records reviewer approval of requirements quality, not implementation completion. New checklist items remain unchecked until the reviewer evaluates them.  
**标记含义**：`[x]` 表示审阅者认可需求质量，不代表实现完成。新增清单项保持未勾选，等待审阅者评估。

## Requirements / 需求

- [x] CHK001 Is the user's departure postcode distinguished from a full home address?  
  是否明确出发 postcode 与完整家庭地址的区别？
- [x] CHK002 Are role location, earliest start, expected latest finish and travel date required for each assessment?  
  每次评估是否需要输入职位地点、最早开班、预计最晚下班和出行日期？
- [x] CHK003 Does weekend mode assess both Saturday and Sunday in the selected week?  
  周末模式是否评估所选周的周六和周日？
- [x] CHK004 Is the 30-minute default arrival buffer adjustable and applied to the earliest shift start?  
  默认 30 分钟到岗缓冲是否可调整，并应用于最早班次开始时间？
- [x] CHK005 Is the commute-time threshold optional, with no fit verdict when it is omitted?  
  通勤时长上限是否可选，未填时不显示适配判断？
- [x] CHK006 Is the commute-cost threshold explicitly deferred?  
  通勤费用上限是否明确延后？
- [x] CHK007 When payment method is omitted, does the result label the Journey Planner fare as adult pay-as-you-go when that is the returned basis?  
  未选择付款方式时，是否在 Journey Planner 返回该口径时，将票价明确标为成人 PAYG？
- [x] CHK008 Are route-applicable concessions listed with eligibility conditions, without assuming the user qualifies?  
  是否列出路线可能适用的优惠及资格条件，而不推定用户符合资格？
- [x] CHK009 Are conditional concession fares shown with explicit eligibility conditions, without implying the user qualifies?  
  是否以条件方案展示优惠票价并明确说明资格条件，而不暗示用户已符合资格？
- [x] CHK010 Does the spec avoid claiming an exact daily total when other trips or payment-device conditions are unknown?  
  其他行程或支付设备条件未知时，是否避免声称全天确切总额？
- [x] CHK011 Are the retained personal settings separated from role inputs and assessment history?  
  是否区分需保留的个人设置与职位输入、评估历史？
- [x] CHK012 Are postcode and workplace data transmission and retention disclosed accurately?  
  是否准确说明 postcode 和工作地点的传输与留存？
- [x] CHK013 Are ambiguous role locations resolved by user selection?  
  地点含糊时是否由用户选择正确地点？
- [x] CHK014 Are scheduled-service limits, official sources and “Unable to confirm” states required?  
  是否要求说明计划服务边界、官方来源和“无法确认”状态？
- [x] CHK015 Are Chinese and English required throughout the user-facing interface and assessment?  
  用户界面和评估内容是否要求中英双语？

## Plan alignment and edge cases / 计划对齐与边界情况

- [x] CHK016 Does the requirement clearly say whether weekend mode replaces the reference-date assessment or adds Saturday/Sunday to it? [Clarity, Spec §FR-004] — Reviewer decision: it adds Saturday and Sunday to the reference-date assessment.  
  需求是否明确说明周末模式会替代参考日期评估，还是在其基础上增加周六/周日？【清晰度，规格 §FR-004】
- [x] CHK017 Are weekend date boundaries and the week-start convention defined consistently across spec, data model, API and quickstart? [Consistency, Spec §FR-004] — Reviewer decision: use Saturday and Sunday in the selected date's week, in addition to the selected date; deduplicate if it is already a weekend day.  
  规格、数据模型、API 和快速开始文档是否一致定义周末日期范围及一周起始日？【一致性，规格 §FR-004】
- [x] CHK018 Are standard fares and conditional concession fares distinguished clearly, with each concession fare tied to its qualifying condition? [Clarity, Spec §FR-007–FR-010] — Reviewer decision: show the standard fare and the discounted fare available under a stated condition; calculate a personal concession only for a concession the user confirms holding.  
  成人票价参考、付款方式假设、所选优惠票价和仅供参考的优惠是否区分清楚，避免用户混淆？【清晰度，规格 §FR-007–FR-010】
- [x] CHK019 Do requirements define how fare matching handles mixed-mode routes, transfers, time-limited concession rules and unsupported combinations? [Coverage, Spec §FR-007–FR-009] — Owner approved on 2026-10-27: evaluate each concession using its own time basis, qualifying event/interval, fare scope, payment assumptions and user-confirmed eligibility; do not infer eligibility from route/leg duration unless the rule explicitly uses it; missing required facts stay unknown and outside the route total.  
  需求是否定义票价匹配如何处理混合交通方式、换乘、有时段限制的优惠规则和不支持的组合？【覆盖度，规格 §FR-007–FR-009】——提出者于 2026-10-27 确认：按每项优惠自己的时间基准、符合条件的事件/时间间隔、收费范围、付款假设和用户确认的资格判断；除非规则明确规定，不根据路线/leg 时长推断资格；缺少必要事实时保持未知，金额不计入路线总价。
- [x] CHK020 Are policy effective dates, source links and update responsibility defined well enough to keep fare/concession explanations current? [Dependency, Spec §FR-012] — Reviewer decision: every rule needs an official source and effective date; catalogue updates must check TfL policy.  
  是否充分定义政策生效日期、来源链接和更新责任，以维持票价/优惠说明的时效性？【依赖，待补充】
- [x] CHK021 Are database connection failure and schema migration failure outcomes defined without risking loss of the retained profile? [Recovery, Plan §Architecture] — Reviewer decision: never delete the database; migrations must be non-destructive, and failures stop startup while preserving profile data for repair/retry.  
  是否定义数据库连接失败和 schema migration 失败时的结果，并避免已保存档案丢失？【恢复，待补充】
- [x] CHK022 Are the local personal-use data boundaries clear without adding commercial privacy workflows? [Scope, Spec §FR-018] — Reviewer decision: no formal commercial privacy flow; keep a brief factual notice that route locations are sent to TfL, retain only the local profile, and do not save role inputs/results.  
  规格是否明确规定可发送给 TfL 的信息及应用日志必须排除的内容？【隐私，规格 §FR-018】
- [x] CHK023 Are destination disambiguation and provider failure requirements complete for retry, cancellation and stale candidate handling? [Exception Flow, Spec §FR-022] — Reviewer decision: require explicit candidate selection, invalidate candidates after destination edits, and allow retry after a retryable TfL failure.  
  对地点消歧和数据提供方失败，是否完整说明重试、取消及过期候选项处理？【异常流程，待补充】
- [x] CHK024 Are requirements for the React/Spring Boot/PostgreSQL local deployment boundary and supported environment assumptions explicit in the technical plan? [Dependency, Plan §Technical Context] — Owner decision recorded in 0.1.3: React + Spring Boot and a project-owned, localhost-only Docker PostgreSQL container with isolated project storage.
  技术计划是否明确 React/Spring Boot/PostgreSQL 本地部署边界及环境假设？【依赖，计划 §技术背景】
- [x] CHK025 Does the requirement distinguish Journey Planner's route total, its fare items and itinerary legs, and make FareTo queries out of MVP scope? [Clarity, Spec §FR-007] — Owner approved on 2026-10-27: show returned totals/items separately from legs; do not request FareTo in the MVP.  
  需求是否区分 Journey Planner 路线总价、票价项和行程 leg，并明确 MVP 不请求 FareTo？【清晰度，规格 §FR-007】——提出者于 2026-10-27 确认：路线总价/票价项与 leg 分开显示；MVP 不请求 FareTo。

## Notes / 备注

- CHK019 and CHK025 were resolved after the owner approved the policy-specific concession boundary and the MVP's no-FareTo scope on 2026-10-27.  
  提出者于 2026-10-27 确认了优惠按规则自身条件判断及 MVP 不请求 FareTo 的范围，CHK019 和 CHK025 已关闭。
- This checklist reviews requirements quality, not implementation behavior.  
  本清单审查需求质量，不检查实现行为。
- CHK001–CHK015 were reviewed previously. CHK016–CHK018 and CHK020–CHK024 were reviewed and clarified with the user.  
  CHK001–CHK015 为此前审阅项；CHK016–CHK018 和 CHK020–CHK024 已与用户确认并完成澄清。
