# Specification Change Log / 规格变更记录

| Version | Date | Requirements / artifacts | Change |
|---|---|---|---|
| 0.1.0 | 2026-10-27 | Initial reviewed MVP baseline | Recorded the agreed commute, fare, concession, privacy and local deployment scope before implementation. |
| 0.1.1 | 2026-10-27 | `contracts/internal-api.md` | Clarified that every route option owns its fare summary. Route fare data must not be shared across dates or itinerary options. This is a contract-shape clarification; product scope is unchanged. |
| 0.1.2 | 2026-10-27 | `spec.md`, `data-model.md`, `contracts/internal-api.md`, `research.md` | Clarified the supported per-item bus/tram discount amount and its London-local effective time, and separated bus/tram PAYG caps from discounted 7-day ticket prices. This makes fare scope explicit; it does not add fare-per-leg calculations. |
| 0.1.3 | 2026-10-27 | `plan.md`, `quickstart.md`, root startup configuration | Per owner direction, changed local development from reusing a pre-existing PostgreSQL server to a project-owned, localhost-only PostgreSQL 18.6 container with isolated persistent storage. No existing Docker volume is mounted. |
| 0.1.4 | 2026-10-27 | `README.md`, `plan.md`, `research.md`, `data-model.md`, `contracts/internal-api.md`, `quickstart.md`, checklists, tasks, acceptance evidence, frontend demo | Aligned the database decision across artifacts, added regression evidence from 10 archived TfL route responses and automated backend tests, and added three opt-in visual themes while keeping language selection independent. |
| 0.1.5 | 2026-10-27 | `compose.yaml`, `README.md`, `quickstart.md`, `research.md`, `tasks.md`, backend datasource defaults | A host PostgreSQL process occupies port 5432. Leave it untouched; move the project container to loopback port 5433 and add a Compose setup. The first stopped container remains untouched. |
| 0.1.6 | 2026-10-27 | `backend/pom.xml`, backend runtime verification, `tasks.md` | Added the Spring Boot 4 REST client starter required by the existing TfL client configuration, and a context smoke test. Verified local PostgreSQL 18.6, Flyway V1 and the default profile GET endpoint. No MVP behavior changed. |
| 0.1.7 | 2026-10-27 | `frontend/src/i18n/messages.ts`, `frontend/src/features/fare/FareSummary.tsx`, `tasks.md` | Fixed the TypeScript build failure caused by reusing the `concessions` translation key for both profile input and result headings. Added a separate bilingual `possibleDiscounts` key and verified the production build. No dependency declarations or MVP behavior changed. |
| 0.1.8 | 2026-10-27 | `frontend/src/App.tsx`, `frontend/src/i18n/messages.ts`, `frontend/src/styles.css` | Replaced the language toggle with an explicit Chinese/English dropdown whose selected value names the active language. Kept the English interface labels and Chinese translations conditional on the selected language. |
| 0.1.9 | 2026-10-27 | `backend/src/main/java/uk/co/commutefit/journey/tfl/TflClient.java`, `backend/src/test/java/uk/co/commutefit/journey/tfl/TflClientTest.java`, `tasks.md`, acceptance evidence | Fixed TfL journey queries to send time as `HHmm`, as required by the provider, and added a URI regression test. The old `HH:mm` format caused TfL HTTP 400 and the UI's generic provider error. |
| 0.2.0 | 2026-10-27 | `spec.md` FR-023–FR-025, `frontend/src/features/profile/ProfileForm.tsx`, `frontend/src/features/assessment/AssessmentForm.tsx`, `frontend/src/features/assessment/AssessmentResults.tsx`, bilingual messages and styles | Product usability/accuracy pass: collapse saved profile details to keep repeat checks focused; recommend exact branch address/postcode; show actual resolved journey endpoints and query time; distinguish provider failure from no returned itinerary and clarify that neither proves no service exists. Added bilingual copy and lower hero height. |
| 0.2.1 | 2026-10-27 | `acceptance/SC-002.md`, `tasks.md`, `frontend/src/i18n/messages.ts`, `frontend/src/styles.css` | Completed the post-restart browser scenario: weekday/weekend route tabs, overnight travel dates, fare display and concession assumptions were checked. The brand-plus-street query resolved to Croydon instead of the intended Oxford Street branch; documented this as an unresolved destination match, strengthened the bilingual warning styling/copy, and added exact employer-listed address verification to remaining manual acceptance. |

| 版本 | 日期 | 需求 / 文档 | 变更 |
|---|---|---|---|
| 0.1.0 | 2026-10-27 | 首个经审阅的 MVP 基线 | 记录通勤、票价、优惠、隐私和本地部署范围，供实现前使用。 |
| 0.1.1 | 2026-10-27 | `contracts/internal-api.md` | 明确每条路线方案各自包含票价摘要；不同日期或路线的票价不得共用。此变更澄清接口结构，不改变产品范围。 |
| 0.1.2 | 2026-10-27 | `spec.md`、`data-model.md`、`contracts/internal-api.md`、`research.md` | 明确公交/电车折扣单价按匹配票价项显示及其伦敦当地生效时间，并将 PAYG 封顶与折扣 7 日票价分开。此变更明确收费范围，不增加按 leg 算票价的逻辑。 |
| 0.1.3 | 2026-10-27 | `plan.md`、`quickstart.md`、根目录启动配置 | 按提出者决定，将本地开发方式从复用已有 PostgreSQL 服务调整为项目专用、仅监听本机的 PostgreSQL 18.6 容器，并使用隔离的持久化目录。不挂载任何已有 Docker 数据卷。 |
| 0.1.4 | 2026-10-27 | `README.md`、`plan.md`、`research.md`、`data-model.md`、`contracts/internal-api.md`、`quickstart.md`、检查清单、任务清单、验收证据、前端演示 | 统一各文档中的数据库方案；基于 10 条 TfL 归档路线响应增加回归证据和后端自动化测试；增加三种可选视觉风格演示，语言仍可独立选择。 |
| 0.1.5 | 2026-10-27 | `compose.yaml`、`README.md`、`quickstart.md`、`research.md`、`tasks.md`、后端数据库默认连接 | 发现本机 PostgreSQL 已占用 5432。保留该进程不动，将项目容器改用仅本机的 5433 端口并新增 Compose 配置；首次创建但已停止的容器保持原样。 |
| 0.1.6 | 2026-10-27 | `backend/pom.xml`、后端运行验证、`tasks.md` | 补上现有 TfL 客户端配置所需的 Spring Boot 4 REST client starter，并新增应用上下文冒烟测试。已验证本机 PostgreSQL 18.6、Flyway V1 和默认档案 GET 接口。未改变 MVP 行为。 |
| 0.1.7 | 2026-10-27 | `frontend/src/i18n/messages.ts`、`frontend/src/features/fare/FareSummary.tsx`、`tasks.md` | 修复 TypeScript 构建失败：档案输入和结果标题此前复用了 `concessions` 翻译键。新增独立的中英文 `possibleDiscounts` 键，并验证生产构建通过。未改变依赖声明或 MVP 行为。 |
| 0.1.8 | 2026-10-27 | `frontend/src/App.tsx`、`frontend/src/i18n/messages.ts`、`frontend/src/styles.css` | 将语言切换按钮改为明确的中文/English 下拉框，当前选中值显示正在使用的语言；中文文案仍仅在选择中文时显示。 |
| 0.1.9 | 2026-10-27 | `backend/src/main/java/uk/co/commutefit/journey/tfl/TflClient.java`、`backend/src/test/java/uk/co/commutefit/journey/tfl/TflClientTest.java`、`tasks.md`、验收记录 | 按 TfL 要求将 Journey Planner 时间参数改为 `HHmm`，并新增 URI 回归测试。此前的 `HH:mm` 格式会导致 TfL HTTP 400，界面显示通用的提供方错误。 |
| 0.2.0 | 2026-10-27 | `spec.md` FR-023–FR-025、档案/评估/结果前端、中英双语文案与样式 | 产品体验与准确性改进：已保存档案默认收起，让重复查询聚焦岗位；建议输入具体门店地址/postcode；醒目显示实际解析路线端点与查询时间；区分提供方查询失败和未返回路线，并说明两者都不能证明没有服务。同步更新中英文文案并压缩首页头图区高度。 |
| 0.2.1 | 2026-10-27 | `acceptance/SC-002.md`、`tasks.md`、`frontend/src/i18n/messages.ts`、`frontend/src/styles.css` | 完成后端重启后的浏览器场景核验：检查工作日/周末路线 tab、跨夜出行日期、票价显示和优惠假设。品牌加街道查询被解析到 Croydon，而非预期的 Oxford Street 门店；将其记录为目的地尚未匹配确认，并加强中英文警示文案和样式，在剩余人工验收中加入招聘信息所列具体地址核验。 |

## Versioning policy / 版本规则

- `PATCH`: clarify wording or contract details without changing intended user-visible behavior.
- `MINOR`: add or materially change behavior within the approved MVP.
- `MAJOR`: change an incompatible product boundary or approved principle.

- `PATCH`：澄清措辞或接口细节，不改变预期的用户可见行为。
- `MINOR`：在已批准的 MVP 范围内新增或实质改变功能行为。
- `MAJOR`：改变不兼容的产品边界或已批准的原则。
