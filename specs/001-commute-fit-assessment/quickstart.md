# Quickstart: Personal Commute Fit Assessment
# 快速开始：个人通勤适配评估

## Prerequisites / 前置条件

- Java 21. The current shell defaults to Java 8, so select the installed JDK 21 before running Spring Boot.
- Node.js 24 and npm for the React/Vite frontend.
- Docker Desktop. This project uses a dedicated local PostgreSQL 18.6 container on host port 5433 and an ignored `.local/postgres-data/` directory; it does not reuse the old `myproject_postgres_data` volume. Port 5432 is occupied by another local PostgreSQL process on this machine.
- Internet access for TfL Journey Planner. A TfL API key is optional and must remain server-side.

- Java 21。本机 shell 默认使用 Java 8，启动 Spring Boot 前需切换到已安装的 JDK 21。
- Node.js 24 和 npm，用于 React/Vite 前端。
- Docker Desktop。本项目使用独立的本地 PostgreSQL 18.6 容器，主机端口为 5433，数据保存在已忽略的 `.local/postgres-data/` 目录，不复用旧 `myproject_postgres_data` 数据卷。本机的另一个 PostgreSQL 进程已占用 5432。
- 可访问 TfL Journey Planner 的网络。TfL API key 可选，且只能放在服务端。

## Create the project PostgreSQL / 创建项目 PostgreSQL

Create a local password file and persistent data directory once:

```sh
cp -n .env.example .env
mkdir -p .local/postgres-data
```

Replace the password placeholder in `.env` with a local-only password. Then start the project database:

```sh
docker compose up --detach
```

Then set the application connection values locally. Keep `.env` out of source control.

首次运行时创建本地密码文件和持久化数据目录：

```sh
cp -n .env.example .env
mkdir -p .local/postgres-data
```

将 `.env` 中的密码占位符替换为仅供本地使用的密码，然后启动项目数据库：

```sh
docker compose up --detach
```

然后在本地设置应用连接信息。不要将 `.env` 提交到代码仓库。

```sh
export SPRING_DATASOURCE_URL='jdbc:postgresql://127.0.0.1:5433/commute_fit'
export SPRING_DATASOURCE_USERNAME='commute_fit'
set -a; source .env; set +a
export SPRING_DATASOURCE_PASSWORD="$POSTGRES_PASSWORD"
export TFL_APP_KEY='' # optional
```

Flyway applies the application schema on backend startup using non-destructive migrations only. It must never drop, truncate or recreate an existing profile table. If connection or migration startup fails, the backend stops with an error and leaves existing data untouched; fix the configuration/migration and retry. Confirm the intended database before first launch.

后端启动时由 Flyway 通过非破坏性 migration 应用项目 schema；不得删除、截断或重建已有档案表。若数据库连接或 migration 失败，后端停止并报错，原有数据保持不动；修复配置/脚本后再重试。首次启动前请确认连接的是预期数据库。

## Run locally / 本地启动

1. In one terminal, select Java 21 and start Spring Boot:

   在一个终端中切换至 Java 21 并启动 Spring Boot：

   ```sh
   export JAVA_HOME='/Users/jdm/Library/Java/JavaVirtualMachines/jdk-21.0.13+9/Contents/Home'
   export PATH="$JAVA_HOME/bin:$PATH"
   cd backend
   ./mvnw spring-boot:run
   ```

2. In another terminal, start the React development server:

   在另一个终端中启动 React 开发服务器：

   ```sh
   cd frontend
   npm install
   npm run dev
   ```

3. Open the local Vite URL printed in the terminal (normally `http://localhost:5173`). Vite forwards `/api/**` requests to Spring Boot on port 8080.

   打开终端显示的 Vite 本地地址（通常为 `http://localhost:5173`）。Vite 会将 `/api/**` 请求转发到 8080 端口的 Spring Boot。

## First-use walkthrough / 首次使用流程

1. Enter your departure postcode, optional one-way time limit, arrival buffer (default 30 minutes), concessions you hold and preferred payment method. Save the reusable profile. The buffer can also be adjusted for one assessment without changing the saved profile.
2. For a role, enter its postcode/address/branch, a reference date, earliest shift start and expected latest finish. These role-specific inputs are used only for this assessment.
   If TfL returns multiple possible matches for either your departure postcode or the workplace, choose the matching location before the assessment continues. Times that are missing or repeated during London clock changes are rejected with an explanation so the route lookup is not sent an ambiguous local time.
3. Optionally enable weekend assessment. The app evaluates the reference date plus Saturday and Sunday in its week; if the reference date is Saturday or Sunday, that date appears only once.
4. Review the scheduled outbound journey targeting arrival before shift start, the return journey from expected finish, durations, route legs and optional personal time-limit comparison.
5. Review TfL Journey Planner's route-level fare and returned fare items separately from itinerary legs. The MVP does not request FareTo. A leg is not automatically a separately charged fare. Missing/partial fares show “Unable to confirm” / “无法确认”, never £0 or a complete total. Each conditional concession scenario states its policy-specific time basis, fare scope, eligibility/payment conditions and source. For selected Oyster concessions that support 50% bus/tram PAYG fares, a discounted amount appears only when the provider bus/tram fare-item amount and London-local tap time match the published schedule; it is labelled per fare item and is not added to the route total. Other concession amounts remain unknown unless supported by complete rules and price data.
6. When no payment method is selected, show the adult pay-as-you-go route fare returned by Journey Planner when available and label its basis. A selected payment method labels the assumption; the Journey Planner route quote remains its returned adult PAYG amount, and concessions are not applied to the route total. Bus/tram adult daily and weekly PAYG caps and, when supported, a conditional reduced daily cap are guidance only. They do not represent a mixed-mode journey total or the user's full-day/week total. A discounted 7-day Bus & Tram Pass is a separate ticket product, not a weekly PAYG cap.
7. Reload the app: only personal profile settings should return. Role inputs and results should not be retained.

1. 输入出发 postcode、可选单程时长上限、到岗缓冲（默认 30 分钟）、自己持有的优惠和首选付款方式，并保存可复用档案。也可以只修改本次评估的到岗缓冲，不改变已保存档案。
2. 对每个职位输入 postcode/地址/门店、参考日期、最早开班和预计最晚下班时间。职位专属输入仅用于本次评估。
   若 TfL 为居住地 postcode 或工作地点返回多个候选位置，须先选择对应地点再继续评估。伦敦时钟切换时不存在或重复的班次时间会显示说明并被拒绝，避免路线查询使用含糊的当地时间。
3. 可选开启周末评估；应用会评估参考日期，并增加该周周六和周日；若参考日期本身是周六或周日，该日期只评估一次。
4. 查看以班次开始前到达为目标的计划去程、预计下班后的返程、耗时、路线分段和可选的个人时长上限比较。
5. 将 TfL Journey Planner 返回的路线总价及票价项与行程 leg 分开查看。MVP 不请求 FareTo。一个 leg 不自动等于一次单独收费。缺失/部分票价显示“Unable to confirm” / “无法确认”，不得显示 £0 或完整总价。每项条件优惠说明自己的时间基准、收费范围、资格/付款条件和来源。对于支持公交/电车 PAYG 五折的已选 Oyster 优惠，只有提供方公交/电车票价项金额和伦敦当地 tap 时间与公布费率表相符时，才按单个票价项显示折后金额；该金额不会计入路线总价。其他优惠若缺少完整规则或价格依据，则金额保持未知。
6. 未选择付款方式时，如 Journey Planner 返回成人 PAYG 路线票价，则显示并标明该付款口径。选定付款方式时只标注付款假设；优惠不直接应用到路线总价。成人公交/电车每日和每周 PAYG 封顶，以及有依据时的条件性折扣日封顶，仅作说明；不代表混合交通路线总价或全天/全周实际费用。折扣 7 日 Bus & Tram Pass 是单独票种，不是每周 PAYG 封顶。
7. 重新加载应用：只应恢复个人档案；职位输入和评估结果不得留存。

## Verify changes / 验证变更

Run the backend tests and package verification with Java 21:

```sh
export JAVA_HOME='/Users/jdm/Library/Java/JavaVirtualMachines/jdk-21.0.13+9/Contents/Home'
export PATH="$JAVA_HOME/bin:$PATH"
cd backend
./mvnw --batch-mode --no-transfer-progress verify
```

After installing the pinned frontend dependencies, verify the TypeScript and production build:

```sh
cd frontend
npm install
npm run build
```

使用 Java 21 运行后端测试和打包验证：

```sh
export JAVA_HOME='/Users/jdm/Library/Java/JavaVirtualMachines/jdk-21.0.13+9/Contents/Home'
export PATH="$JAVA_HOME/bin:$PATH"
cd backend
./mvnw --batch-mode --no-transfer-progress verify
```

安装固定版本的前端依赖后，验证 TypeScript 和生产构建：

```sh
cd frontend
npm install
npm run build
```

## Attribution / 数据署名

Display **“Powered by the Transport for London Journey Planner API”**. Do not use TfL branding or imply official endorsement.

显示 **“Powered by the Transport for London Journey Planner API”**。不得使用 TfL 品牌标识或暗示官方背书。
