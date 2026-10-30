---
description: "Task list for Personal Commute Fit Assessment"
---

# Tasks: Personal Commute Fit Assessment

**Input**: Design documents in `specs/001-commute-fit-assessment/`  
**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `contracts/internal-api.md`, `quickstart.md`  
**Tests**: Automated service/unit tests cover fare normalization, policy matching, date/DST rules and token expiry. Manual product acceptance remains tracked in T035 and T041.  
**Organization**: Tasks are grouped by user story for incremental delivery.

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Create the React/Spring Boot application skeleton and local configuration.

- [X] T001 Create `backend/pom.xml`, Maven Wrapper files, and Spring Boot Java 21 application entry point in `backend/src/main/java/uk/co/commutefit/CommuteFitApplication.java` with Web, Validation, JDBC, Flyway, and PostgreSQL driver dependencies.
- [X] T002 [P] Create React + TypeScript + Vite project files in `frontend/package.json`, `frontend/tsconfig.json`, `frontend/vite.config.ts`, and `frontend/index.html`.
- [X] T003 [P] Create bilingual React app shell and styles in `frontend/src/App.tsx`, `frontend/src/main.tsx`, and `frontend/src/styles.css`.
- [X] T004 Create root `.gitignore`, `backend/src/main/resources/application.yml`, and `frontend/.env.example` for local secrets/configuration; configure Vite proxy for `/api/**` to `http://127.0.0.1:8080`.

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Establish safe configuration, profile persistence, API error conventions, and provider boundary before story work.

- [X] T005 Add environment-based PostgreSQL settings and optional `TFL_APP_KEY` configuration in `backend/src/main/resources/application.yml`; bind Spring Boot to `127.0.0.1` and implement safe connection/migration failure startup handling in `backend/src/main/java/uk/co/commutefit/config/DatabaseStartupConfig.java`.
- [X] T006 Create singleton profile schema migration in `backend/src/main/resources/db/migration/V1__create_commute_profile.sql` with only departure postcode, optional one-way limit, arrival buffer, selected concessions, preferred payment method, other-method label, and update timestamp; ensure migrations never drop/truncate/recreate existing profile data.
- [X] T007 Implement shared bilingual API error response types and exception mapping in `backend/src/main/java/uk/co/commutefit/common/ApiError.java` and `backend/src/main/java/uk/co/commutefit/common/ApiExceptionHandler.java`.
- [X] T008 Implement TfL Journey Planner HTTP client configuration (timeouts, optional server-side key, upstream error mapping) in `backend/src/main/java/uk/co/commutefit/journey/tfl/TflClient.java` and `backend/src/main/java/uk/co/commutefit/config/HttpClientConfig.java`.
- [X] T009 Implement common validation and Europe/London date-time utilities in `backend/src/main/java/uk/co/commutefit/assessment/AssessmentTimePolicy.java`; include overnight shift rollover, arrival-buffer subtraction and week-date derivation.
- [X] T010 Configure log redaction and request logging exclusions in `backend/src/main/resources/application.yml` and `backend/src/main/java/uk/co/commutefit/config/RequestLoggingConfig.java`; do not log request bodies, destination text, provider payloads, or secrets.

## Phase 3: User Story 1 — Check a role's commute before applying (Priority: P1) 🎯 MVP

**Goal**: Assess scheduled outbound and return options for a manually found role, with optional duration comparison and Saturday/Sunday assessment.

**Independent Test**: With profile and provider data available, enter one role and confirm the early-arrival target, late-return boundary, dated route options, optional personal time-limit comparison, and—when enabled—separate Saturday and Sunday results. Confirm role inputs/results are not persisted.

### Implementation

- [X] T011 [P] [US1] Implement profile request/response DTOs and validation in `backend/src/main/java/uk/co/commutefit/profile/ProfileDto.java` and `backend/src/main/java/uk/co/commutefit/profile/ProfileRequest.java`, enforcing UK postcode, positive optional minutes, non-negative arrival buffer, and the payment-method enum from `data-model.md`.
- [X] T012 [US1] Implement singleton profile JDBC repository and service in `backend/src/main/java/uk/co/commutefit/profile/ProfileRepository.java` and `backend/src/main/java/uk/co/commutefit/profile/ProfileService.java`; profile fields follow `data-model.md` and no role data is accepted.
- [X] T013 [US1] Implement `GET`, `PUT`, and `DELETE /api/profile` in `backend/src/main/java/uk/co/commutefit/profile/ProfileController.java`, including default profile behavior and clear semantics.
- [X] T014 [P] [US1] Implement transient assessment DTOs and field validation in `backend/src/main/java/uk/co/commutefit/assessment/AssessmentRequest.java` and `backend/src/main/java/uk/co/commutefit/assessment/AssessmentResponse.java`; require destination, ISO reference date, `HH:mm` shift times, and boolean weekend toggle.
- [X] T015 [US1] Implement TfL Journey Planner request/response mapping and location disambiguation in `backend/src/main/java/uk/co/commutefit/journey/tfl/TflJourneyService.java`; define the transient `JourneyOption` response record in `backend/src/main/java/uk/co/commutefit/assessment/AssessmentResponse.java`.
- [X] T016 [US1] Implement route assessment orchestration in `backend/src/main/java/uk/co/commutefit/assessment/AssessmentService.java` and `backend/src/main/java/uk/co/commutefit/assessment/AssessmentController.java`; `checkWeekend=false` assesses `referenceDate`, while `true` adds its week's Saturday and Sunday and deduplicates dates; apply the same shift boundaries to all assessed dates.
- [X] T017 [P] [US1] Implement bilingual profile and role-assessment forms in `frontend/src/features/profile/ProfileForm.tsx` and `frontend/src/features/assessment/AssessmentForm.tsx`; persist profile only through the API and keep job fields in component memory.
- [X] T018 [US1] Implement typed API client and profile/assessment submission flow in `frontend/src/api/client.ts` and `frontend/src/features/assessment/useAssessment.ts`; support destination disambiguation follow-up, invalidate candidates when destination text changes, and allow retryable provider failures without retaining candidate IDs or assessment data.
- [X] T019 [US1] Render outbound/return route cards, actual travel dates, scheduled durations, legs/transfers, optional time-limit explanation, weekend day labels, and loading/empty/error states in `frontend/src/features/assessment/AssessmentResults.tsx` and `frontend/src/styles.css`; do not present a universal job-quality score or application recommendation, and leave the application decision to the user.
- [X] T020 [US1] Add editable/clearable profile interactions and ensure reload restores only profile fields in `frontend/src/features/profile/ProfileForm.tsx`; do not use localStorage/sessionStorage for role fields or results.

## Phase 4: User Story 2 — Understand fare and available concessions (Priority: P2)

**Goal**: Explain the provider-returned route fare and fare-item breakdown, route-applicable concessions, payment assumptions and cap guidance with official sources.

**Independent Test**: Compare multi-leg Tube routes, Hopper routes, mixed bus/Tube routes, missing-fare routes and time-limited concession cases with archived Journey Planner responses and the relevant official rules. Confirm fare items remain distinct from legs, the route total is not double-counted, each concession uses its own time basis and fare scope, and missing/partial concession data stays unknown. The MVP does not request FareTo.

### Implementation

- [X] T021 [P] [US2] Create versioned policy catalogue data in `backend/src/main/resources/fare-policies/concessions.yml` with policy ID, supported modes, payment basis, eligibility time basis, qualifying event or interval, applicable fare scope, duration rules only when explicitly defined by the policy, eligibility text, fare treatment, effective date, and official source URL.
- [X] T022 [US2] Implement a fare-item model and source-backed concession matcher in `backend/src/main/java/uk/co/commutefit/fare/policy/FarePolicy.java` and `backend/src/main/java/uk/co/commutefit/fare/policy/FarePolicyMatcher.java`; evaluate each policy against its own required route events/timestamps, fare scope, payment basis and user-confirmed eligibility, return eligible/ineligible/unknown status, never treat each itinerary leg as a separately priced fare, and do not infer eligibility from route or leg duration unless the policy explicitly uses it.
- [X] T023 [US2] Normalize Journey Planner `fare.totalCost` and `fare.fares[]` into the route quote and fare-item breakdown in `backend/src/main/java/uk/co/commutefit/fare/FareAssessmentService.java`; do not call FareTo; keep missing totals/components unknown rather than zero or partial complete totals.
- [X] T024 [US2] Add route-level fare, returned fare items, separate conditional concession scenarios with eligibility status, rule-specific time basis, evaluated fare scope and amount status, fare/payment basis, effective date/source, and cap guidance to `backend/src/main/java/uk/co/commutefit/fare/FareSummary.java` and `backend/src/main/java/uk/co/commutefit/assessment/AssessmentResponse.java`.
- [X] T025 [US2] Render standard route fare, fare-item scope, payment assumptions, concession eligible/ineligible/unknown status, the rule's time basis and fare scope, eligibility/source links, “Unable to confirm” explanations, and cap guidance in `frontend/src/features/fare/FareSummary.tsx` and `frontend/src/styles.css`.
- [X] T026 [US2] When no payment method is selected, return the adult pay-as-you-go route fare from Journey Planner when available and label its basis; list conditional concessions separately. Display a discounted amount only when the policy-specific time basis, fare scope, payment conditions and user-confirmed eligibility are evaluable, otherwise show conditions/source and keep the amount out of the route total in `backend/src/main/java/uk/co/commutefit/fare/FareAssessmentService.java` and `frontend/src/features/fare/FareSummary.tsx`.

## Phase 5: User Story 3 — Understand uncertainty, privacy and sources (Priority: P3)

**Goal**: Make scheduled-data limits, upstream uncertainty, location transmission, retention, and user decision ownership explicit.

**Independent Test**: Review an assessment with missing fare data, no scheduled option, ambiguous destination, or provider failure; affected information is clearly distinguished, source-linked where possible, and no realtime or arrival guarantee is implied. Privacy disclosure explains what is sent and what is retained.

### Implementation

- [X] T027 [P] [US3] Add bilingual localization strings for form labels, validation, no-route, ambiguity, provider/database errors, privacy disclosures, fare uncertainty and limitations in `frontend/src/i18n/messages.ts`.
- [X] T028 [US3] Implement explicit no-scheduled-option, unable-to-confirm, partial-result, ambiguous-location and provider-unavailable response states in `backend/src/main/java/uk/co/commutefit/assessment/AssessmentService.java` and `backend/src/main/java/uk/co/commutefit/common/ApiExceptionHandler.java`.
- [X] T029 [US3] Add pre-submit disclosure that departure postcode and role location are sent to TfL, and scheduled-data/no-guarantee explanation to `frontend/src/features/assessment/AssessmentForm.tsx` and `frontend/src/features/assessment/AssessmentResults.tsx`.
- [X] T030 [US3] Display the required attribution “Powered by the Transport for London Journey Planner API” and official fare/source links in `frontend/src/App.tsx` and `frontend/src/features/fare/FareSummary.tsx` without TfL branding or implied endorsement.
- [X] T031 [US3] Ensure backend error and audit paths do not include postcode, role destination, shift data, provider bodies or credentials in `backend/src/main/resources/application.yml` and relevant service logging.

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Complete local operation guidance and final scope alignment.

- [X] T032 [P] Update root `README.md` with Java 21, Node 24, Maven Wrapper, Vite startup, one-time startup of the project-owned `commute_fit` PostgreSQL container, environment variables, optional TfL key, attribution and data-retention boundary.
- [X] T033 Align `specs/001-commute-fit-assessment/quickstart.md` with actual app commands and profile/assessment field behavior after implementation.
- [X] T034 Review `spec.md`, `plan.md`, `data-model.md`, `contracts/internal-api.md`, `quickstart.md`, and `tasks.md` for scope/contract consistency, including the adult pay-as-you-go default and no-FareTo MVP boundary, before the final manual acceptance step.
- [ ] T035 [US2] After route and fare implementation, manually verify SC-002 against 10 representative weekday, Saturday and Sunday assessments. Compare scheduled durations and available route totals/items with archived TfL responses for the same date/time; include multi-leg Tube, mixed bus/Tube, Hopper and missing-fare cases; confirm fare items are not treated as legs, totals are not double-counted, and missing/partial amounts stay unable to confirm. Record pass/fail, source URLs or archive IDs, and unresolved cases in `specs/001-commute-fit-assessment/acceptance/SC-002.md`.
  实现路线和票价后，按 10 个有代表性的工作日、周六和周日评估手动核对 SC-002。针对相同日期/时间，将计划时长及可用路线总价/票价项与 TfL 归档响应对照；覆盖多 leg 地铁、公交/地铁混合、Hopper 和缺失票价案例；确认票价项不等于 leg、不重复计算总价，缺失/部分金额保持无法确认。将通过/未通过、来源链接或归档 ID 及未解决案例记录在 `specs/001-commute-fit-assessment/acceptance/SC-002.md`。

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies.
- **Foundational (Phase 2)**: Depends on Setup; blocks user stories.
- **US1 (Phase 3)**: Depends on Foundation; provides route results consumed by US2.
- **US2 (Phase 4)**: Depends on Foundation and US1 route/leg shape.
- **US3 (Phase 5)**: Depends on response/error states established by US1 and US2.
- **Polish (Phase 6)**: Follows desired user stories; T034 follows the implementation work, and T035 follows T034 and completion of US1/US2.

### Parallel Opportunities

- After T001 defines the backend root, T002–T004 can progress in separate frontend/config files.
- T007–T009 are separate foundational concerns after project skeleton/configuration.
- In US1, T011 and T014 can proceed in parallel; frontend form work T017 can proceed independently of provider mapping T015 until API integration T018.
- In US2, T021 policy data and T024 response DTO shape can be drafted independently; matcher T022 precedes calculation integration T023.
- In US3, T027 localization and T030 attribution UI can proceed in separate frontend files.

## Parallel Example: User Story 1

```text
Task A: T011 profile DTO and validation
Task B: T014 assessment DTO and validation
Task C: T017 profile and assessment forms
```

## Parallel Example: User Story 2

```text
Task A: T021 official policy catalogue data
Task B: T024 fare response model in AssessmentResponse.java and FareSummary.java
```

## Parallel Example: User Story 3

```text
Task A: T027 bilingual UI messages
Task B: T030 TfL attribution and official source links
```

## Implementation Strategy

1. Complete Setup and Foundation, confirming Java 21 and starting the project-owned PostgreSQL 18.6 container with isolated storage.
2. Deliver US1 as the first usable increment: profile, scheduled routes, early arrival, latest return and weekend assessment.
3. Add US2 fare/concession policies with strict source-backed calculation boundaries.
4. Complete US3 transparency and privacy disclosures.
5. Finish Polish, review the artifacts and confirmed scope in T034, add automated regression checks for core rules, then complete the manual SC-002 acceptance record in T035.

## Phase 7: Convergence — Remaining Spec Gaps

**Purpose**: Close implementation and acceptance gaps found by comparing the current code with the approved specification and constitution. These tasks do not change the agreed product scope.

- [X] T036 [US3] Bound and purge expired workplace-disambiguation tokens in `backend/src/main/java/uk/co/commutefit/journey/tfl/CandidateTokenStore.java`; remove abandoned expired entries without waiting for token reuse and ensure memory use stays bounded, preserving the short-lived in-memory-only flow required by FR-018 and Constitution Data Retention. (CRITICAL — partial)
  `backend/src/main/java/uk/co/commutefit/journey/tfl/CandidateTokenStore.java` 中须限制并清理过期的工作地点消歧令牌；不得等到令牌再次使用才移除已被放弃的过期条目，并确保内存使用有界，同时保持 FR-018 和章程“数据留存”原则要求的短期、仅内存流程。（CRITICAL — 部分实现）
- [X] T037 [US1] Handle `fromLocationDisambiguation` in `backend/src/main/java/uk/co/commutefit/journey/tfl/TflJourneyService.java` and the assessment flow; let the user resolve multiple origin candidates or report an unresolved origin, and never silently select one. (HIGH — partial)
  在 `backend/src/main/java/uk/co/commutefit/journey/tfl/TflJourneyService.java` 和评估流程中处理 `fromLocationDisambiguation`；允许用户从多个出发地候选中选择，或明确报告出发地未解决，不得静默选择。（HIGH — 部分实现）
- [X] T038 [US3] Apply `Europe/London` zone rules to shift and target-time conversion in `backend/src/main/java/uk/co/commutefit/assessment/AssessmentTimePolicy.java`; explicitly handle nonexistent spring-transition times and ambiguous autumn-transition times before querying routes, and keep the displayed/query date-time basis consistent with FR-019. (HIGH — partial)
  在 `backend/src/main/java/uk/co/commutefit/assessment/AssessmentTimePolicy.java` 的班次和目标时间换算中应用 `Europe/London` 时区规则；查询路线前明确处理春季切换时不存在的当地时间及秋季切换时有歧义的当地时间，并确保展示与查询使用一致的日期/时间依据，符合 FR-019。（HIGH — 部分实现）
- [X] T039 [US2] Carry the effective fare-period date used to calculate each conditional discounted amount into `FareSummary.ConcessionScenario` and display it in both languages when the source data provides it; when unavailable, do not invent or imply an effective date. (MEDIUM — partial)
  将计算每项条件优惠金额所用的票价时段生效日期传递到 `FareSummary.ConcessionScenario`；在来源数据提供时以中英文展示，数据缺失时不得编造或暗示生效日期。（MEDIUM — 部分实现）
- [X] T040 [US3] Localize user-visible assessment-result values and labels, including route-fare payment basis/scope, transport mode labels and direction/result headings, so the Chinese and English interfaces and results satisfy FR-017; retain official proper names and source text as published. (MEDIUM — partial)
  本地化用户可见的评估结果内容与标签，包括路线票价付款口径/范围、交通方式标签及方向/结果标题，使中英文界面和结果符合 FR-017；官方专名和来源原文按发布内容保留。（MEDIUM — 部分实现）
- [ ] T041 [US1] [US2] [US3] Complete remaining manual acceptance for SC-001, SC-003 and SC-004, including one query with the exact employer-listed branch postcode/address. Record completion times, source/limitation comprehension, fare/concession interpretation and whether the resolved endpoint matches the job listing in `specs/001-commute-fit-assessment/acceptance/SC-001-003-004.md`. (MEDIUM — partially validated)
  使用有代表性的职位和路线案例手动评估并记录 SC-001、SC-003 和 SC-004；记录完成时长、每份结果是否清楚说明计划数据/来源/限制依据，以及用户能否正确理解优惠状态、时间基准、收费范围和所选优惠金额。将证据及未解决发现记录在 `specs/001-commute-fit-assessment/acceptance/SC-001-003-004.md`。（MEDIUM — 尚未验证）
- [X] T042 [Setup] Closed as superseded by the owner's explicit decision to create a project-owned PostgreSQL container (CHANGELOG 0.1.3). Do not execute the former existing-container restoration instructions; startup and database verification are tracked as new work. (scope superseded)
  按提出者明确决定新建项目专用 PostgreSQL 容器，本任务已关闭并由 CHANGELOG 0.1.3 记录的方案取代。不得执行原先“恢复已有容器”的指令；启动与数据库验证另行跟踪。（范围已变更）
- [X] T043 [US3] Installed the already-declared frontend dependencies (`npm install`; 22 packages added) and completed the TypeScript/Vite production build. Fixed the build error by separating the profile's `concessions` label from the results area's `possibleDiscounts` label in both languages; dependency declarations were unchanged. `npm run build` passes. npm reported one high-severity audit finding; no automatic dependency upgrades were applied. (MEDIUM — build validated; browser acceptance remains in T041)
  已安装前端已声明的依赖（`npm install`；新增 22 个包）并完成 TypeScript/Vite 生产构建。修复了构建错误：中英文均将档案表单的 `concessions` 标签与结果区的 `possibleDiscounts` 标签拆分；依赖声明未变。`npm run build` 通过。npm 报告一个高危 audit 项；未自动升级依赖。（MEDIUM — 构建已验证；浏览器验收仍由 T041 跟踪）

## Phase 8: Convergence — Project Database Setup

**Purpose**: Verify the owner-approved project-owned database decision before database-backed acceptance.

- [X] T044 [Setup] Started the project-owned `commute-fit-postgres-local` PostgreSQL 18.6 container on host port 5433 with data isolated under `.local/postgres-data/`, bound only to `127.0.0.1`, using the ignored `.env`. Runtime evidence: PostgreSQL 18.6 connection succeeded; Flyway validated V1 and reported schema version 1 up to date; `GET /api/profile` returned HTTP 200 with the unconfigured default profile (`arrivalBufferMinutes: 30`, no saved postcode or concessions). No other Docker volume was mounted or modified. The stopped first-attempt `commute-fit-postgres` container remains untouched.
  已在主机 5433 端口启动项目专用 `commute-fit-postgres-local` PostgreSQL 18.6 容器，数据独立保存在 `.local/postgres-data/`，端口仅绑定 `127.0.0.1`，凭证来自已忽略的 `.env`。运行证据：成功连接 PostgreSQL 18.6；Flyway 验证 V1 并确认 schema 版本 1 已是最新；`GET /api/profile` 返回 HTTP 200 和未配置的默认档案（`arrivalBufferMinutes: 30`，没有已保存的 postcode 或优惠）。没有挂载或修改其他 Docker 数据卷。首次尝试创建的停止状态 `commute-fit-postgres` 容器保持原样。

## Phase 9: Convergence — TfL Journey Time Parameter

- [X] T045 [US1] Format TfL Journey Planner `time` query parameters as `HHmm` (for example, `0230`) rather than `HH:mm`; verified the old format receives HTTP 400 from TfL and the corrected request returns itinerary data for the reported commute. Add a client regression test that asserts the outgoing request URI. Keep the existing provider-error handling unchanged. (HIGH — fixed; restart backend to load the correction)
  将 TfL Journey Planner 的 `time` 查询参数格式改为 `HHmm`（例如 `0230`），而不是 `HH:mm`；已确认旧格式会收到 TfL HTTP 400，修正后用户报告的通勤查询返回路线数据。新增客户端回归测试，断言实际请求 URI。保持现有提供方错误处理不变。（HIGH — 已修复；需重启后端加载修正）

## Phase 10: Convergence — Frontend Usability and Result Accuracy

- [X] T046 [US1] Apply the bilingual product-review improvements: collapse saved profile settings, recommend precise workplace address/postcode input, prominently show resolved itinerary endpoints and data query time, distinguish provider failure from no returned itinerary, and reduce hero height. Chrome confirmed the saved profile is collapsed/editable, language selection displays the selected language, weekday/Saturday/Sunday tabs work, overnight travel dates are labelled, route cards show endpoints/duration/fare, and the submitted form values remain intact. `npm run build` passes. The tested brand-plus-street query resolved to a Croydon branch; this is recorded as an unconfirmed destination match in SC-002, not as an Oxford Street commute. (HIGH — implemented and browser-checked)
  [X] T046 [US1] 完成双语界面的产品审查改进：已保存档案可收起；建议填写具体工作地址/postcode；醒目显示实际路线端点和数据查询时间；区分提供方失败与未返回路线；缩短头图区。Chrome 已确认档案默认收起且可编辑、语言下拉框与页面语言一致、工作日/周六/周日 tab 可用、跨夜实际出行日期有标注、路线卡显示端点/时长/票价，且提交内容保持不变。`npm run build` 通过。测试中的品牌加街道查询解析为 Croydon 门店；SC-002 将其记录为目的地尚未确认，而非 Oxford Street 通勤结果。（HIGH — 已实现并通过浏览器检查）
