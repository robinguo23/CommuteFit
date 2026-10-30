# Plan: Location selection

Java 21/Spring RestClient/Jackson + React；复用 CandidateTokenStore 与 TflJourneyService。Photon 通过固定可配置 base-url、4秒连接/6秒读超时、小结果集合接入。临时缓存最多100条、五分钟；公共搜索提供方全进程限频，不批量爬取。新 /api/place-search 返回候选元数据，用户确认 token 后工作地点显示名与 routeValue 相绑定。原 /api/locations 供旧路线页继续使用。

技术分层：LocationSearchService（格式分类/候选/令牌），PhotonClient（限频缓存/固定提供方），PlaceSearchController；frontend PlacePicker（显式搜索/地图预览/确认），WorkCommutePage 集成。只读回归脚本不输出 key，真实用例不比较必须相等的时长。

Constitution: 用户已授权；不持久化地点，无模型，无票价；候选来源和精度明确。沿用已确认地点查询，不重新猜测。无冲突。

Verification: MockRestServiceServer 五类参数矩阵、GeoJSON转换与过滤、候选绑定、300 parameterValue；前端字段状态回归、构建；真实网络可用时做低频观察，失败记录不伪称通过。

本地运行：start-local.sh 在 .local/ 创建唯一运行目录，复制打包 JAR 后启动，退出时清理其副本。这样后续 Maven 构建不会覆盖活跃进程延迟读取的文件。运行条件受限时保留明确错误日志，不通过关闭验证或改变网络保护来绕过。

## US4 implementation

新增 AreaConsensusService，独立于原正式搜索。每请求 Java 21 virtual threads 并行调用三家已有客户端，客户端现有超时约束；各源异常单独转 unavailable。纯筛选器生成跨源两点及三点完全图，排除三点组已覆盖的两点组；重复组合仅在并集全体两两距离仍合规时合并，来源支持 distinct 计算。稳定排序：来源数降序、最大距离升序。medoid 取已有点，不生成虚构坐标。名称过滤统一 M&S/Marks & Spencer，忽略通用门店词，查询其余 token 必须出现在名称+地址；公开启发式及排除原因。纯 postcode 查询按格式走允许，仍需真实坐标。响应含 providers、areas、excluded、status、maxDistanceMeters。前端默认三家联合、可切单源、显示每源响应和近似代表点。


2026-10-29 实测修订：允许品牌吻合、地址缺查询区域的候选，通过另一来源原始文本合格点在 965.6064m 内的坐标补证参与；补证点不能递归作 anchor。返回 corroborated 列表解释补证；最终分组直径上限不变。四门店Google人工核对详见 multi-provider-live-review.md。

2026-10-29 Demo 回退：当多源共识缺失/因长街同品牌门店冲突而不能选定时，额外对三家原始候选执行保守的品牌+街道/区域词匹配。所有匹配坐标须落在同一 0.6 英里直径内才显示 `text-match` 候选；否则仍 ambiguous。此结果只供地点搜索 Demo 实验，不接入 `WorkCommutePage`。



2026-10-29 用户明确不提供区域选择步骤：自动采用来源数最多的组，同票按组内最大距离最小优先，完全相同时保持稳定顺序。API selectedCandidate 明确返回首组代表点；有共同区域统一 supported，无共同区域仍未知，不能强造位置。Demo 只展示自动采用组，无区域选择按钮。此修订替代前文多组要求用户选择的规则。


## US5 benchmark implementation

frontend/src/benchmark/LocationBenchmarkPage.tsx 提供 /place-benchmark，50 条 samples.json、四个显式查询规则、串行请求、1.1 秒间隔、查询复用、暂停续跑及导出。已有正式查询逻辑不变。独立地址、原始观测、邮编参考和评分在 docs/location-benchmark；scripts/analyze_location_benchmark.py 仅离线读取 JSON，按 Haversine 与 965.6064m 阈值生成 CSV/summary。机场历史邮编中心不适合时保存明确地图覆盖参考，不修改原始响应。
