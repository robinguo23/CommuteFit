# Implementation Plan: Personal Commute Fit Assessment
# 实现计划：个人通勤适配评估

**Branch**: `001-commute-fit-assessment` | **Date**: 2026-10-26 | **Spec**: [spec.md](spec.md)  
**分支**：`001-commute-fit-assessment` | **日期**：2026-10-26 | **规格**：[spec.md](spec.md)

## Summary / 概要

Build a bilingual, single-user local web application that evaluates scheduled London journeys for early arrival, expected late return, and optionally both weekend days. Use React for the frontend, Java 21 with Spring Boot for the backend, and a project-owned PostgreSQL container in Docker. The backend queries TfL, applies source-backed fare/concession matching rules, and persists only the reusable personal profile. Role inputs and assessment results are transient.

构建中英双语、供单用户本地使用的网页应用，评估伦敦早班到岗、预计晚班返程，以及可选的周六和周日通勤。前端使用 React，后端使用 Java 21 和 Spring Boot，并通过 Docker 运行本项目专用的 PostgreSQL。后端查询 TfL 并匹配有官方来源依据的票价/优惠规则；数据库只保存可复用的个人档案，职位输入和评估结果均为临时数据。

## Technical Context / 技术背景

- **Frontend**: React + TypeScript + Vite; Node.js 24 and npm for development and production builds.
- **Backend**: Java 21, Spring Boot 4.1.x, Maven Wrapper; REST endpoints serve profile and assessment operations.
- **Database**: Run a project-owned PostgreSQL 18.6 container through Docker Desktop, bound only to localhost, with a dedicated persistent project data directory. Use Spring JDBC and Flyway for the profile schema. Keep database credentials in the ignored local `.env` file and pass them through environment variables.
- **Persistence boundary**: Store one editable personal profile only. Do not persist job location, shift time/date, resolved workplace, assessment result, or fare response.
- **Provider boundary**: Spring service calls TfL Journey Planner for date-specific scheduled journeys. A dedicated policy catalogue/matcher uses official fare and concession rules; do not infer eligibility or invent prices.
- **Testing**: The implementation includes automated unit/regression tests, a production build and executable local acceptance. Keep external TfL and database results distinct from mocked or archived-fixture test results.
- **Target platform**: User's Mac; frontend/backend and project PostgreSQL bind to localhost for personal use. Docker Desktop manages the database container.
- **Constraints**: Scheduled timetable data only; no realtime punctuality prediction or arrival guarantee. Do not log assessment bodies, location queries, provider responses, or secrets.
- **Scope**: No accounts, public hosting, job discovery/scraping, or cost-limit feature in this MVP. Optional commute-time threshold remains supported.

- **前端**：React + TypeScript + Vite；使用 Node.js 24 和 npm 进行开发及生产构建。
- **后端**：Java 21、Spring Boot 4.1.x、Maven Wrapper；通过 REST 接口处理档案和通勤评估。
- **数据库**：通过 Docker Desktop 运行项目专用的 PostgreSQL 18.6 容器，仅绑定本机地址，并使用项目专用持久化目录。使用 Spring JDBC 和 Flyway 管理档案表结构。数据库凭证保存在已忽略的本地 `.env` 文件中，并通过环境变量传递。
- **持久化边界**：只保存一份可编辑个人档案。不保存职位地点、班次时间/日期、解析后的工作地点、评估结果或票价响应。
- **数据提供方边界**：Spring 服务调用 TfL Journey Planner 查询指定日期的计划路线。独立的政策目录/匹配器使用官方票价和优惠规则；不得推断资格或编造金额。
- **测试**：实现阶段包含自动化单元测试、生产构建和可执行的本地验收；外部 TfL 查询与数据库验收应区分真实环境结果和模拟结果。
- **目标平台**：用户的 Mac；前后端和项目 PostgreSQL 均仅监听本机供个人使用；数据库容器由 Docker Desktop 管理。
- **约束**：仅使用计划班次数据；不预测实时准点或保证到达。不得记录评估请求正文、地点查询、提供方响应或密钥。
- **范围**：MVP 不含账户、公开托管、职位发现/抓取或费用上限功能；保留可选通勤时长上限。

## Constitution Check / 章程检查

| Principle | Alignment |
|---|---|
| Scheduled data and honest limits | TfL scheduled journeys are date-specific and never presented as realtime guarantees. |
| Explainable, source-backed results | Fare matcher returns its rule, effective basis and official source; unsupported values remain unknown. |
| Minimize location data | Profile stores departure postcode only; role-specific locations and results are transient. |
| User owns thresholds and decisions | Fit comparison appears only when the user sets a duration limit. |
| Focused MVP | Early/late trips, both weekend days, adult fares and applicable concessions; no scraping or cost threshold. |

| 章程原则 | 符合情况 |
|---|---|
| 计划数据与诚实边界 | 按日期查询 TfL 计划路线，不包装成实时保证。 |
| 可解释且有来源的结果 | 票价匹配器返回规则、生效依据及官方来源；无支持的数据保持未知。 |
| 最小化位置数据 | 档案只保存出发 postcode；职位地点和评估结果均为临时数据。 |
| 用户设定标准并作决定 | 只有用户设置时长上限后才显示适配比较。 |
| 聚焦 MVP | 早班/晚班、周六/周日、成人票价及路线适用优惠；不做抓取或费用上限。 |

**Gate**: PASS. Spring Boot 4.1.2 and Java 21 are selected. The project-owned PostgreSQL 18.6 container must be started and verified before database-backed acceptance.  
**关卡**：通过。采用 Spring Boot 4.1.2 和 Java 21。数据库验收前须启动并验证本项目专用的 PostgreSQL 18.6 容器。

## Architecture / 架构

```text
React + Vite (localhost:5173 in development)
        │ same-origin API via Vite proxy
        ▼
Spring Boot (localhost:8080)
  ├── Profile REST service ── Spring JDBC ── `commute_fit` DB in project-owned Docker PostgreSQL
  ├── Assessment orchestration ── TfL Journey Planner client
  └── Fare/concession policy catalogue and matcher
```

- Vite proxies `/api/**` to Spring Boot during local development. The supported local workflow runs the Vite frontend and Spring Boot API in separate terminals; production packaging/static asset serving is outside this MVP.
- `GET/PUT/DELETE /api/profile` reads, replaces, or clears the singleton profile.
- `POST /api/assessment` validates transient input, resolves the destination, calculates reference-date plus optional Saturday/Sunday date boundaries (deduplicated), queries routes, matches fares and returns a normalized explanation.
- The app uses a dedicated `commute_fit` database in its project-owned, localhost-only Docker PostgreSQL container. Persistent database files stay under ignored `.local/postgres-data/`; never mount or modify another Docker volume. Flyway owns table/schema changes. Migrations must be non-destructive: never drop/truncate/recreate existing profile data. If a migration fails, startup stops with a clear error; do not attempt an automatic database reset.
- Normalize Journey Planner's optional route-level `fare.totalCost` and `fare.fares[]` items separately from the itinerary legs. Prefer the returned route-level fare as the route quote; do not infer a full fare from each leg. The MVP does not request the FareTo endpoint or show station-pair quotes. Empty or unsupported route-fare responses remain unknown, not zero.
- Conditional concession scenarios remain separate, with eligibility/payment assumptions and sources. Evaluate each policy using its own time basis, qualifying event/interval and fare scope against available scheduled route/fare-item timestamps; do not infer eligibility from route or leg duration unless the rule explicitly uses it. If required evidence is absent or ambiguous, return unknown and do not include an amount in the route total. Keep daily/weekly caps as guidance unless complete same-card/device travel is known; do not present a planned fare as a transaction guarantee.
- For supported 50% bus/tram concessions, show the published single fare only for matching provider fare-item sequences when the user's selected Oyster concession, item amount and London-local tap time support it. Keep that per-item amount outside the route total. Show bus/tram-only PAYG caps as guidance; keep discounted 7-day pass ticket prices distinct from weekly caps.
- Journey Planner returned some `Peak`/`Off Peak` labels near published boundaries that need reconciliation. Preserve the provider's returned classification and time basis; do not add a local peak/off-peak classifier until the discrepancy is resolved.
- Do not claim exact daily spend from one commute: cap information is contextual guidance unless the relevant complete travel history and payment-device continuity are known.

- 本地开发时，Vite 通过代理将 `/api/**` 转发到 Spring Boot；受支持的本地运行流程是在两个终端分别启动 Vite 前端和 Spring Boot API。
- 生产打包和由 Spring Boot 提供前端静态文件不属于本 MVP。
- `GET/PUT/DELETE /api/profile` 读取、替换或清除唯一的个人档案。
- `POST /api/assessment` 校验临时输入、解析目的地、计算参考日期及可选的周六/周日日期（去重）、查询路线、匹配票价并返回标准化解释。
- 数据库结构由 Flyway 管理。migration 必须为非破坏性操作：不得删除、截断或重建已有档案数据。migration 失败时，应用应显示清楚错误并停止启动，保留数据库/档案供修复后重试；不得自动重置数据库。Spring JDBC 明确表达单行档案结构，避免小型应用引入不必要的 ORM 行为。
- 将 Journey Planner 可选的路线级 `fare.totalCost` 和 `fare.fares[]` 票价项与行程 legs 分开归一化。优先使用其返回的路线总价，不得为每个 leg 推算完整票价。MVP 不请求 FareTo 接口，也不展示站对报价。路线票价空缺或不受支持时保持未知，不能视为 0。
- 条件优惠方案单独展示，并注明资格/付款假设和来源。除非已知同卡/设备完整行程，否则每日/每周封顶只作说明；不得把计划报价说成实际扣款保证。
- 对有依据的公交/电车五折优惠，只有用户选择适用的 Oyster 优惠，且提供方票价项金额及伦敦当地 tap 时间均能匹配公布费率时，才按匹配的票价项序号显示折后单程价；该单项金额不计入路线总价。公交/电车 PAYG 封顶只作说明；折扣 7 日通票和每周封顶分开处理。
- 每项条件优惠都按其专属时间基准、符合条件的事件/时间间隔及收费范围，对照可用的计划路线/票价项时间戳判断；除非规则明确以路线或 leg 时长为条件，否则不得据此推断资格。必要证据缺失或含糊时返回未知，金额不得计入路线总价。除非已知同卡/设备完整行程，否则每日/每周封顶只作说明；不得把计划报价说成实际扣款保证。
- Journey Planner 在公开时段边界附近返回过需要复核的 `Peak`/`Off Peak` 标签。保留提供方返回的分类及时间依据；差异解决前不得自行增加本地高峰分类器。
- 单次通勤不得推算为全天确切支出；除非完整行程历史及付款设备连续性均已知，封顶只作为规则说明。

## Project Structure / 项目结构

```text
frontend/
├── package.json
├── vite.config.ts
└── src/
    ├── api/
    ├── components/
    ├── features/profile/
    ├── features/assessment/
    └── i18n/
backend/
├── pom.xml
├── mvnw
├── src/main/java/.../commutefit/
│   ├── profile/
│   ├── assessment/
│   ├── journey/tfl/
│   ├── fare/policy/
│   └── config/
└── src/main/resources/
    ├── application.yml
    └── db/migration/
specs/001-commute-fit-assessment/...
```

## Data and Contract Artifacts / 数据与接口文档

- [data-model.md](data-model.md) defines persisted profile fields, transient request/results, fare rules and retention boundaries.
- [contracts/internal-api.md](contracts/internal-api.md) defines the browser-to-Spring REST contract.
- [research.md](research.md) records provider and policy constraints plus technology choices.
- [quickstart.md](quickstart.md) explains local prerequisites, Docker database configuration and startup.

- [data-model.md](data-model.md) 定义持久档案字段、临时请求/结果、票价规则及留存边界。
- [contracts/internal-api.md](contracts/internal-api.md) 定义浏览器到 Spring 的 REST 契约。
- [research.md](research.md) 记录数据提供方、票价政策约束和技术选择。
- [quickstart.md](quickstart.md) 说明本地前置条件、Docker 数据库配置和启动方法。

**Post-design Constitution Gate**: PASS. No role-specific data enters persistent storage; fare claims are source-backed and explainable.  
**设计后章程关卡**：通过。职位专属数据不会进入持久存储；票价结果有来源且可解释。
