# Implementation Plan: Job extraction

Date: 2026-10-29 | Spec: spec.md

## Technical Context
Java 21 / Spring Boot 4 / Jackson，Apache HttpClient 5；React/TypeScript。无持久存储。JUnit、MockMvc、现有前端测试与构建。

## Constitution Check
用户已授权扩大 004 提取范围，优先于旧章程的 002 范围。保留数据最小化、来源证据、未知不伪造、用户确认原则。无模型调用或票价逻辑。

## Structure / Implementation
backend/.../jobextraction/{PublicPageFetcher,JobDocumentParser,JobExtractionService,JobExtractionController}.java；原 JobListingService 委托。前端增加证据候选界面，原字段保留手动编辑。请求有界；禁私网、受控跳转、DOM 主体优先、先 JSON-LD 后时间文本与表格解析。

## Verification
测试结构化字段、噪声地址、多地点、多个岗位、两种时间制、分日表格、窗口/实际班次、超时与失败、HTTP 契约和兼容投影。真实网络受环境限制时不得伪称已实测。

## 验收修订实现

Schedule 增加 nullable cycleWeek。解析器保留中文星期与中英周次标题，地点增加店铺/Store 与地点/Location 标签组合提取。前端集中实现优先级、查询日展开和日期基准校验；多个勾选班次通过已有 /scenario 逐个查询。普通班次展示七日；双周排班在用户明确第一周周一日期后展示十四日。无明确星期展开全七天，准确星期仅查对应日。后台原始证据保留供审查，面向用户的查询入口隐藏低优先级窗口。
