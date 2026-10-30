# Implementation Plan: 简化通勤初筛
Branch: 006-simple-commute-check（工作区没有Git，未创建分支） | Date: 2026-10-29 | Spec: spec.md

## Summary
首页只保留两处postcode、单程上限及用户选择的参考类型，显示一个带范围的通勤结论。保留独立路线页和原高级工作检查。复用JourneyLookupService；新增后台任务与按需检查引擎，解决整周查询超出现有25秒HTTP等待的问题。

## Technical Context
Language: Java21、TypeScript、React19；Spring Boot4 / RestClient / Jackson沿用项目版本。
Storage: 不新增数据库。执行期间的查询去重仅存于单个检查上下文；后台任务结果最多保留完成后2分钟，10分钟绝对过期，取消即移除；不构成查询历史。
Testing: JUnit/AssertJ/MockMvc及现有node:test，使用固定伦敦Clock和注入Planner验证调用顺序/次数/日期。
Target: 本机浏览器及127.0.0.1后端。无新外部依赖。
Performance: 后台任务立即返回标识；前端轮询进度、可取消；单进程最多2个活跃检查、最多16条短期任务；每次检查至多180秒，单次提供方请求沿用4秒连接/12秒读超时。连续3次提供方错误后停止进一步网络请求并标未知，限流立即停止；成功响应重置连续错误计数。
Scope: 七份日程，必要时含第八天回程节点；无票价，无工时限制，无自动跳过所选类型。
Default: 日间预选，可改选/多选早晚；日期在提交时由后端伦敦时区Clock确定。

## Constitution Check
通过：用户已授权006执行；区域级初筛、个人阈值、来源与未知说明保留。后台任务仅为当前请求暂存、限时删除、不落盘，符合不保存岗位历史原则。旧003/004/005保留高级能力，不要求用户填写准确门店或availability。完成后再检查边界、隐私与来源。

## Project Structure
- backend/src/main/java/uk/co/commutefit/simplecommute/SimpleCommuteService.java：模型、验证、按需搜索、日期/时点去重、汇总。
- backend/src/main/java/uk/co/commutefit/simplecommute/CommuteCheckJobs.java：可取消短期任务、进度、过期/容量限制。
- backend/src/main/java/uk/co/commutefit/simplecommute/SimpleCommuteController.java：创建/读取/取消任务。
- frontend/src/SimpleCommutePage.tsx、simpleCommute.css、simpleCommuteForm.ts、api/client.ts。
- frontend/src/App.tsx：新首页 /，原页面 /advanced，保留 /journey。
- backend/src/test/java/uk/co/commutefit/simplecommute/：核心、任务与契约回归。
- frontend/tests/simpleCommute.test.ts：输入规则及提取优先级回归。

## Phases
0. 只读研究现有服务，技术决策见research.md；未引入未知依赖。
1. 完成data-model、contracts和quickstart。
2. 建立模型和回归，实施按需引擎与后台任务；前端接入、简化页面、保留高级入口。
3. 完整检查、打包及可用环境中的页面验收；记录模拟与真实结果的区别。

## Complexity Tracking
后台任务用来保持长查询可取消、可见进度，避免单次同步请求超时。没有消息队列、账号或持久化存储。简单同步整周端点被否决：最多数十至百余提供方请求无法保证25秒内完成。
