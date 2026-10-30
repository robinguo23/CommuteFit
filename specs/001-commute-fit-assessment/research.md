# Research: Personal Commute Fit Assessment
# 研究：个人通勤适配评估

**Date**: 2026-10-26  
**日期**：2026-10-26

## 1. Scheduled journey source / 计划路线来源

**Decision**: Use TfL Unified API Journey Planner for dated routes. Submit postcodes or free-text locations and handle returned disambiguation candidates with an explicit user choice. Do not present scheduled routes as realtime predictions.

**决策**：使用 TfL Unified API Journey Planner 查询指定日期路线。出发地和目的地使用 postcode 或自由文本；若返回地点候选项，要求用户明确选择。不得将计划路线表述为实时预测。

The official schema accepts UK postcodes/free text and date, time and `timeIs` fields. Its fare object is optional, and journey requests do not take a selected concession card or payment method. Fare matching therefore needs its own rules and must preserve unknown states.

官方 schema 接受英国 postcode/自由文本及日期、时间、`timeIs` 字段。票价对象可缺省，路线请求不接收所选优惠卡或付款方式。因此需要独立的票价匹配规则，并保留未知状态。

**Sources**: [TfL Journey Planner Swagger](https://api.tfl.gov.uk/swagger/docs/v1); [TfL Unified API](https://tfl.gov.uk/info-for-open-data-users/unified-api); [TfL open data attribution](https://tfl.gov.uk/info-for-open-data-users/our-open-data).

## 2. Fare and concession matching / 票价与优惠匹配

**Decision**: Do not equate an itinerary leg with a fare item. When Journey Planner returns a route-level `fare.totalCost`, use that as the provider's route fare for the returned basis and display the returned `fare.fares[]` items as its breakdown. The MVP does not request FareTo or show station-pair quotes; those API results remain research evidence only. Do not independently price every leg or add a separately scoped quote to the Journey Planner total in a future version unless the scopes are proven disjoint. Missing totals and unsupported modes remain unknown; never fill them with zero or present a partial amount as a complete total. Keep conditional concession scenarios source-backed and separate, and keep daily caps as guidance unless complete same-card/device travel is known.

**决策**：路线 leg 不等于票价项。Journey Planner 返回路线级 `fare.totalCost` 时，将其作为该方案在返回票价基础下的路线票价，并把 `fare.fares[]` 返回项作为拆分明细展示。MVP 不请求 FareTo，也不展示站对报价；此前的 FareTo API 响应仅作为研究证据。不得对每个 leg 独立查完整票价；若未来版本引入其他站对报价，除非已证明收费范围互不重叠，否则不得将其加到 Journey Planner 总价。缺失总价和不受支持交通方式均保持未知；不得填为 0，也不得把部分金额说成完整总价。优惠方案须有来源并单独展示；除非已知同卡/设备完整行程，否则每日封顶只作说明。

TfL's Journey Planner schema contains an optional route-level fare (`totalCost` and `fares`). In archived run `20261026T234104Z-b18d1676`, a two-leg Tube itinerary returned one fare item; a bus-plus-Tube option returned items `175` and `310` with `totalCost=485`; and consecutive-bus options returned `175` plus zero-cost Hopper items. All 66 itineraries with both `totalCost` and `fares[]` in this batch had matching component sums. These observations show that fare items do not map one-to-one to vehicle legs; they do not prove every route or final card charge. FareTo returned adult Tube station-pair ticket rows but returned `[]` for the tested bus and National Rail station pairs. Journey Planner fares were absent from all three tested National Rail/Elizabeth line itinerary options. Missing data must remain unknown. FareTo prices aligned with Journey Planner integer values for several tested Tube cases, supporting—but not universally proving—pence as the integer unit in those cases. Daily capping spans 04:30–04:29 and depends on same-card/device usage; show caps as guidance rather than adding them to a one-journey quote.

TfL Journey Planner schema 含可选的路线级票价（`totalCost` 和 `fares`）。归档批次 `20261026T234104Z-b18d1676` 中，两段地铁行程只返回一个票价项；一条公交加地铁方案返回 `175` 和 `310` 两项、`totalCost=485`；连续公交方案返回 `175` 及 Hopper 零金额项。本批次 66 条同时有 `totalCost` 和 `fares[]` 的行程，其拆分金额合计均相符。这些观察说明票价项与乘车 leg 不一一对应，但不能证明所有路线或真实刷卡扣款都遵循相同结果。FareTo 为已测地铁站对返回成人票价行；已测公交和 National Rail 站对则返回 `[]`。所有三条已测 National Rail/Elizabeth line 方案都没有 Journey Planner 票价。缺失数据须保持未知。FareTo 金额与多个已测地铁案例的 Journey Planner 整数一致，因此可支持、但不能普遍证明这些整数单位为便士。每日封顶周期为 04:30 至次日 04:29，且依赖同卡/设备；封顶只作说明，不加到单程报价中。

**Sources**: [TfL Journey Planner Swagger](https://api.tfl.gov.uk/swagger/docs/v1); [TfL single fare finder](https://tfl.gov.uk/fares/find-fares/single-fare-finder); [TfL fare capping](https://tfl.gov.uk/fares/find-fares/capping); [TfL bus and tram fares](https://tfl.gov.uk/fares/find-fares/bus-and-tram-fares); [TfL free and discounted travel](https://tfl.gov.uk/fares/free-and-discounted-travel); [TfL National Railcard discount](https://tfl.gov.uk/fares/free-and-discounted-travel/national-railcard-discount).

**Initial published bus/tram price support**: TfL publishes adult PAYG single fares and daily/weekly PAYG caps, plus discounted singles and daily caps for eligible Oyster discount holders on its [bus and tram fares page](https://tfl.gov.uk/fares/find-fares/bus-and-tram-fares). The implementation stores separate effective periods in `fare-policies/concessions.yml`: £1.75 / £0.85 singles, £5.25 / £2.55 daily caps and £24.70 adult weekly cap until 1 November 2026 04:30 Europe/London; then £1.85 / £0.90, £5.55 / £2.70 and £26.10. Discounted 7-day pass prices are separate season-ticket products and are not treated as a discounted weekly PAYG cap. A 50%-discounted amount is shown only per fare-item sequence when the provider's bus/tram fare-item amount matches the adult single fare and its tap timestamp identifies the published period. If no item timestamp is returned, use route start time. If matched items span different published periods and a single amount cannot represent them, leave it unknown. The batch did not test the exact 04:29/04:30 boundary; this cutoff comes from TfL's published fare information, not an independent API boundary test.

**首版公交/电车公布费率支持**：TfL 在[公交/电车票价页面](https://tfl.gov.uk/fares/find-fares/bus-and-tram-fares)公布成人 PAYG 单程票价及每日/每周 PAYG 封顶，也公布符合条件的 Oyster 优惠用户折后单程价及每日封顶。实现将费率按生效时段分开保存在 `fare-policies/concessions.yml`：截至 2026 年 11 月 1 日英国时间 04:30 前，标准/折后单程票价为 £1.75 / £0.85，标准/优惠日封顶为 £5.25 / £2.55，成人每周 PAYG 封顶为 £24.70；此后为 £1.85 / £0.90、£5.55 / £2.70、£26.10。折扣 7 日通票是单独季票产品，不当作折扣每周 PAYG 封顶。只有提供方公交/电车票价项金额与公布成人单程价相符、且 tap 时间能识别生效时段时，才按票价项序号显示五折金额。若缺少票价项时间戳，则使用路线出发时间。若匹配票价项跨越不同费率时段，且单一金额无法表示，则金额保持未知。测试批次未测到 04:29/04:30 精确边界；该时点依据 TfL 公布的票价信息，而非独立 API 边界实测。

**Observed-run details**: [TfL fare API deep-test results](../../tools/tfl-fare-api-demo/deep-results-20261026T234104Z-b18d1676.md). The test also observed a bus fare value of `175` on 31 October and `185` on 1 November. TfL's fares page and the Mayor's decision state the adult fare becomes £1.85 from 1 November 2026, after 04:30; the batch did not query the exact 04:29/04:30 boundary. The test observed unexpected `Off Peak` labels for some options at 06:30/06:34 and 16:01; do not implement a local peak classifier until those cases are repeated and reconciled with the Single Fare Finder.

**实测记录**：[TfL 票价 API 深度测试结果](../../tools/tfl-fare-api-demo/deep-results-20261026T234104Z-b18d1676.md)。测试还观察到公交金额在 10 月 31 日为 `175`、11 月 1 日为 `185`。TfL 票价页面和市长决定均说明成人公交票价从 2026 年 11 月 1 日 04:30 后变为 £1.85；本批次未查询 04:29/04:30 精确边界。测试在 06:30/06:34 和 16:01 的部分方案中观察到意外的 `Off Peak` 标签；重复测试并与 Single Fare Finder 核对前，不应自行实现高峰分类规则。

Concession evaluation must follow each policy's own qualifying time basis and fare scope; route or vehicle-leg duration is not a general eligibility rule. The Journey Planner request does not accept the user's concession selection or payment method, and the archived batch did not verify discounted fare amounts. Use returned event/tap times only when the official policy defines them as relevant; if the API omits necessary times, payment facts or scope, report the concession condition but leave its amount unknown and outside the route total. — [sources: TfL Journey Planner Swagger; archived batch manifest]

优惠判断必须遵循各项规则自己的资格时间基准和收费范围；整条路线或车辆 leg 时长不是通用资格规则。Journey Planner 请求不接收用户选择的优惠或付款方式，归档批次也未验证折后金额。仅当官方规则确实以返回的乘车事件/tap 时间为依据时才使用这些时间；若 API 缺少必要时间、付款事实或收费范围，则说明优惠条件，但将金额保持未知且不计入路线总价。— [来源：TfL Journey Planner Swagger；归档批次 manifest]

## 3. Backend and database / 后端与数据库

**Decision**: Use Java 21, Spring Boot 4.1.2 with Maven Wrapper, React + TypeScript + Vite, and a project-owned PostgreSQL 18.6 Docker container. Use Spring JDBC and Flyway. Bind the database only to `127.0.0.1`, keep its persistent data under ignored `.local/postgres-data/`, and load the local-only password from ignored `.env`. Do not mount or alter another Docker volume.

**决策**：采用 Java 21、Spring Boot 4.1.2 和 Maven Wrapper，前端采用 React + TypeScript + Vite，并通过 Docker 运行本项目专用的 PostgreSQL 18.6 容器。使用 Spring JDBC 和 Flyway。数据库只绑定 `127.0.0.1`，持久数据保存在已忽略的 `.local/postgres-data/`，本地密码从已忽略的 `.env` 读取。不得挂载或改动其它 Docker 数据卷。

Java 21 is installed at `/Users/jdm/Library/Java/JavaVirtualMachines/jdk-21.0.13+9/Contents/Home`; the shell's default Java is Java 8. Node 24/npm 11 and Docker Desktop are present. The owner chose a new project-owned database container after the old volume was no longer present in Docker Desktop. Docker Desktop showed its engine running and the official `postgres:18.6-alpine3.24` image available. A read-only host check found a local `postgres` process already listening on 127.0.0.1:5432, so that process was left untouched. A first project container `commute-fit-postgres` was created but remains stopped because its port is unavailable; it has not initialized the project database. The Compose configuration now uses the project container name `commute-fit-postgres-local`, host port 127.0.0.1:5433, database/role `commute_fit`, and persistent storage `.local/postgres-data/`. The `compose.yaml` has not yet been started or verified. Keep credentials in ignored `.env`. The application accepts connection values through `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` and `SPRING_DATASOURCE_PASSWORD`.

本机已安装 Java 21（`/Users/jdm/Library/Java/JavaVirtualMachines/jdk-21.0.13+9/Contents/Home`），但 shell 默认 Java 为 Java 8。Node 24/npm 11 和 Docker Desktop 已安装。提出者决定新建项目专用数据库容器，因为 Docker Desktop 中已看不到旧数据卷。Docker Desktop 显示引擎运行，且有官方 `postgres:18.6-alpine3.24` 镜像。只读检查发现本机已有 `postgres` 进程占用 `127.0.0.1:5432`，未停止或连接该进程。首次创建的项目容器 `commute-fit-postgres` 因端口占用无法启动，仍保持停止状态，也未初始化项目数据库。Compose 配置改用容器名 `commute-fit-postgres-local`、主机端口 `127.0.0.1:5433`、数据库/角色 `commute_fit`，持久化目录 `.local/postgres-data/`；`compose.yaml` 尚未启动或验证。凭证保存在已忽略的 `.env`。应用通过 `SPRING_DATASOURCE_URL`、`SPRING_DATASOURCE_USERNAME` 和 `SPRING_DATASOURCE_PASSWORD` 接收连接配置。

## 4. Framework versioning / 框架版本

Use Spring Boot 4.1.2 with Java 21 and Maven Wrapper. Use Vite with React and TypeScript on the installed Node 24. Pin project dependency versions in build files rather than relying on unbounded latest versions.

采用 Spring Boot 4.1.2、Java 21 和 Maven Wrapper。前端使用 Vite、React 和 TypeScript，沿用本机 Node 24。项目依赖版本在构建文件中固定，不依赖无上限的 latest。

**Sources**: [Spring Boot system requirements](https://docs.spring.io/spring-boot/system-requirements.html); [Vite guide](https://vite.dev/guide/).

## 5. Data minimization and attribution / 数据最小化和署名

Persist only the editable personal profile. Keep job location, selected dates/times, resolved destination and assessment response in memory for the active request only. Do not log request bodies or provider data. Show TfL's required attribution: **“Powered by the Transport for London Journey Planner API”**. Do not imply TfL endorsement.

只持久保存可编辑的个人档案。职位地点、选定日期/时间、解析后的目的地及评估响应仅在当前请求中保留于内存。不得记录请求正文或提供方数据。显示 TfL 要求的署名：**“Powered by the Transport for London Journey Planner API”**。不得暗示获得 TfL 背书。

## Open questions / 未决问题

Before database-backed acceptance, start the project-owned PostgreSQL container and verify its local port, Flyway migration and profile endpoint. Keep the first policy catalogue deliberately bounded to common concessions and supported modes; unsupported combinations remain explicitly unknown. The current network sandbox could not complete `npm install`, so the frontend production build and browser check remain unverified.

数据库验收前需启动本项目专用 PostgreSQL 容器，并验证本机端口、Flyway migration 和档案接口。首版优惠目录限定为常见优惠及有数据支持的交通方式；不支持的组合明确显示未知。当前网络沙箱无法完成 `npm install`，因此前端生产构建和浏览器检查仍未验证。
