# Data model

CheckRequest:
- originPostcode/workPostcode：完整英国postcode，规范化空格与大写，不接受仅城市作为完整区域。
- maxOneWayMinutes：整数1–1440，默认60。
- referenceTypes：day/early/night的非空去重集合，仅参考模式必需，默认day。
- shifts：可空数组，最多32项；每项label最多100字、start/end为HH:mm且不同，days可空（空表示七天范围逐日查）；cycleWeek可空或1–8。
- rotaStartDate：存在cycleWeek时必需，必须周一；仅检查七份日程中适用安排。
- rotaWeeks：可空或1–8，不能小于所选cycleWeek；前端保留原始提取的完整周期长度，取消第二周选项不能把双周周期变成单周。未提供时按提交班次的最大周次处理。
- availability：可空；from/until必须同时存在且不同，days可空（未限制星期）。只过滤参考时点或比较明确安排，不把窗口自动变成实际班次。

CheckResult:
status=suitable/unsuitable/needs-review；mode=reference/exact；伦敦scopeStart/scopeEnd固定七个日期；days恰好七项，未安排日期checks为空。
每个check：type、label、status、availability、outbound/returning。
明确班次但days为空时，各日期是机会参考，不要求七天全部上班；每个已选班型需至少有一个可行日期。days已明确时覆盖其全部必需安排。
Direction：status=within-limit/over-limit/no-route/unknown；reason；命中targetTime、最多一条不带票价的route、实际尝试的节点列表。
Route：departure/arrival、durationMinutes、transfers、legs，不含票价。
positiveScopes与issues分别列出适用日期类型与方向限制；后端唯一拥有主结论。可附queriedPoints/reusedPoints用于测试与折叠详情。

Job snapshot:
id为随机不可猜标识；status=running/complete/cancelled/failed；completed/total；result可空；error为双语简短错误。
状态：创建→running→complete/failed；取消删除；完成后2分钟或创建后10分钟自动清理。最多2个运行、16条暂存，不落盘不保存历史。
