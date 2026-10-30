# Tasks: 简化通勤初筛

Input: 本目录spec、plan、research、data-model、contracts及quickstart。按用户授权连续完成全部故事。

## Phase 1: Setup

- [x] T001 检查既有服务与研究建议，完成 specs/006-simple-commute-check/plan.md、research.md、data-model.md、contracts/check.md、quickstart.md。
- [x] T002 检查 .gitignore、规格质量及扩展钩子，完成实现前检查，记录在 specs/006-simple-commute-check/CHANGELOG.md。

## Phase 2: Foundation

- [x] T003 在 backend/src/main/java/uk/co/commutefit/simplecommute/SimpleCommuteService.java 建立契约模型和验证：两个完整postcode；单程上限整数1–1440；类型仅day/early/night；最多32项明确班次；cycleWeek为1–8，周期锚点必须周一；所有时间HH:mm。
- [x] T004 在 backend/src/test/java/uk/co/commutefit/simplecommute/SimpleCommuteServiceTest.java 先建立固定Clock与假Planner，并写顺序、日期、汇总及无工时限制的回归。

## Phase 3: US1 简单输入与单一结论

Independent test: 无availability即可用两个postcode获得一个带范围结果；明确班次优先；错误保持未知。

- [x] T005 [US1] 在 backend/src/main/java/uk/co/commutefit/simplecommute/SimpleCommuteService.java 完成参考/明确班次/窗口处理、七份日程及后端汇总，不投影票价。
- [x] T006 [US1] 在 backend/src/main/java/uk/co/commutefit/simplecommute/CommuteCheckJobs.java 和 SimpleCommuteController.java 实现后台任务、进度、取消、容量及过期限制；同目录测试覆盖生命周期与HTTP契约。
- [x] T007 [US1] 在 frontend/src/api/client.ts、simpleCommuteForm.ts 和 frontend/tests/simpleCommute.test.ts 加入输入、明确班次优先、postcode提取和结果契约回归。
- [x] T008 [US1] 在 frontend/src/SimpleCommutePage.tsx、simpleCommute.css、App.tsx 实现简化首页、类型多选、可取消进度、一个主结论、折叠详情和高级入口。
- [x] T009 [US1] 在 frontend/src/SimpleCommutePage.tsx 接入折叠的现有URL/文本提取与可选明确时间/窗口，保留中文、多班次及周期确认；无postcode时明确要求补工作区域。

## Phase 4: US2 逐点停止

Independent test: 第三个点命中仅调用前三个；最快有效路线不是首项；未选类型零查询。

- [x] T010 [US2] 在 backend/src/main/java/uk/co/commutefit/simplecommute/SimpleCommuteService.java 实现候选顺序、首个合格即停、时间过滤后取最快、单次节点缓存。
- [x] T011 [US2] 在 backend/src/test/java/uk/co/commutefit/simplecommute/SimpleCommuteServiceTest.java 覆盖06:30已查询/未查询、缺时长、晚到/早走、未知后命中、只选日间和全部确定失败。

## Phase 5: US3 日期边界与复用

Independent test: 首日早班当天，次日早班复用前晚，第七晚仅跨第八节点，结果七份日程。

- [x] T012 [US3] 在 backend/src/main/java/uk/co/commutefit/simplecommute/SimpleCommuteService.java 完成上午候选组按实际日期缓存、跨午夜和明确班次周期/个人窗口处理。
- [x] T013 [US3] 在 backend/src/test/java/uk/co/commutefit/simplecommute/SimpleCommuteServiceTest.java 验证首尾日期、共享结果零重复、无第八日程、限流/超时/取消及不施加工时限制。

## Phase 6: Polish

- [x] T014 验证所有影响判断的输入使旧任务失效，失败和取消可恢复；更新 frontend/tests/simpleCommute.test.ts 与必要状态逻辑。
- [x] T015 完成后端verify、前端npm test及build；确认旧 /journey、/advanced 入口与后端兼容。
- [x] T016 按 specs/006-simple-commute-check/quickstart.md 完成可用的首页布局、双语提示及版本检查；记录运行中旧后端导致的完整页面阻塞，更新 README.md、CHANGELOG.md 及任务状态。

## Dependencies and execution

T001→T002→T003→T004；引擎实现T005/T010/T012按同文件顺序完成，再T006接入；前端T007→T008→T009→T014；最后T015/T016。
US2/US3是同一引擎的可独立验证行为，测试可独立执行。并行例：后端测试与前端输入测试可分别运行；修改同一文件仍顺序执行。研究代理只读，不修改工作区代码。
完整交付先使最小两个postcode闭环成立，再补已选类型、明确时间和异常恢复；不以只完成US1代替用户要求的全部范围。

验收边界：任务复选框包含实现、自动化、可执行界面检查与明确的阻塞记录；不代表重启后的真实TfL整周页面检查已经通过。该最后运行验收步骤见CHANGELOG和quickstart。
