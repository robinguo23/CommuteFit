# Commute check HTTP contract

POST /api/commute-checks
JSON为data-model.md的CheckRequest。通过完整验证后返回202及Job snapshot。错误输入400；容量暂满429（check_busy）。该端点创建的任务不持久化。
GET /api/commute-checks/{id}
返回200及当前快照，complete时包含完整CheckResult。找不到或过期404（check_expired）；浏览器可重新发起，不能继续展示为当前输入的结果。
DELETE /api/commute-checks/{id}
取消并删除该任务；幂等204。终止后续提供方请求，已在途请求受原网络超时限制。
客户端每约1秒轮询一次；输入修改、重试或离开页面使旧代次失效并尽力取消，不允许旧成功/失败覆盖新输入。

Route source: TfL Journey Planner；所有日期/时刻基于Europe/London。结果无FareTo、票价或资格判断。
参考类型标签：early早班、day日间、night晚班。只查询选中集合。
明确shift优先于references；个人窗口不转成shift。提供方未给完整资料时unknown，不能通过异常给负向结论。
