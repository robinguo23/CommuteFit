# Tasks: Job extraction

## Setup
- [x] T001 完成 specs/004-job-extraction/spec.md、plan.md、research.md、data-model.md、contracts/extraction.md、quickstart.md。
## US1 地点和契约
- [x] T002 [US1] 在 backend/src/main/java/uk/co/commutefit/jobextraction/ 建立提取 DTO、服务与独立 API。
- [x] T003 [US1] 实现 PublicPageFetcher.java 的受控公网抓取与边界验证。
- [x] T004 [US1] 实现 JobDocumentParser.java 的 JSON-LD 与地点证据提取。
## US2 时间语义
- [x] T005 [US2] 实现 JobDocumentParser.java 的分日时间、多班型和窗口识别。
- [x] T006 [US2] 更新 backend/src/main/java/uk/co/commutefit/workcommute/JobListingService.java 委托和兼容投影。
- [x] T007 [US2] 更新 frontend/src/WorkCommutePage.tsx 与 frontend/src/api/client.ts 展示候选和证据，清理旧岗位字段。
## US3 验证和交付
- [x] T008 [US3] 在 backend/src/test/java/uk/co/commutefit/jobextraction/ 添加解析、抓取策略与 HTTP 契约测试。
- [x] T009 [US3] 运行后端/前端检查，补齐 README.md、specs/004-job-extraction/CHANGELOG.md。

Dependencies: T001 → T002/T003 → T004 → T005 → T006 → T007 → T008 → T009。并行机会：抓取策略测试与纯解析测试可以独立准备。按功能完成验证后再勾选。

## 用户验收修订

- [x] T010 [US2] 更新 JobDocumentParser.java 和 Schedule DTO 支持中文、周次与门店名称。
- [x] T011 [US2] 在 frontend/src/extractionForm.ts 集中优先级、多选、七日覆盖与周期日期展开规则。
- [x] T012 [US2] 更新 JobExtractionReview.tsx、WorkCommutePage.tsx 和 ShiftScenarios.tsx 多班次查询入口及分周结果。
- [x] T013 [US3] 添加用户 M&S 样例与多班次日期矩阵测试，完成构建和验收记录。
