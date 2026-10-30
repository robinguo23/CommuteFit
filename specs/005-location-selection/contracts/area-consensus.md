# 工作区域共识 API

`GET /api/place-search/consensus?query=...`，3–120 字符。用户点击才调用三家，不逐字请求。

响应：status=supported（有多源共同区域）/text-match（无共识但只有一个位置文本匹配簇，低置信度）/ambiguous（至少两个来源支持的门店区域彼此相距较远）/needs-confirmation（没有合格匹配）；maxDistanceMeters=965.6064；providers=[{provider,result}] 含每源 status/message/candidates/attribution；areas=[{support,sources,members,representative,maxDistanceMeters}]；excluded=[{candidate,reason}]；message。ambiguous 时可附最强共识组，但 selectedCandidate 必须为空。text-match 的 selectedCandidate 只供 Demo 核对，不代表多源共识，也不接入正式路线评估。

每源最多 8 条，只有坐标有效、可选且 postcode/address/venue 候选可投票。文本比较：M&S 和 Marks & Spencer 统一；英式撇号忽略、rd/road 与 ave/avenue 统一；忽略 simply/food/foodhall/store 等通用词，其余查询词必须出现在名称或地址。完整 postcode 查询只接受候选 postcode 完全一致（忽略空格和大小写）。保守启发式会漏匹配；原始候选完整可见。此模块不保证语义正确，不能因无共识判通勤失败。

枚举不同来源两点/三点完全图，两两距离 <= 965.6064m（Haversine）；三点组合覆盖的两点组合不再单列。重复组合仅在并集全体点对符合上限时合并；相同来源多点始终只一票。重叠但不能合并的区域分别展示。三家优先、同支持数按最大距离升序。代表点为已有候选，每源平均距离后等权求和最小；不生成中心点。原 token 保持十分钟有效。

单家超时/限流/异常隔离为 unavailable，另外两家可组成 supported；两个失败或只有单一来源则 needs-confirmation。该状态仅表示位置相互支持，不直接决定通勤。


2026-10-29 实测修订：允许品牌吻合、地址缺查询区域的候选，通过另一来源原始文本合格点在 965.6064m 内的坐标补证参与；补证点不能递归作 anchor。返回 corroborated 列表解释补证；最终分组直径上限不变。四门店Google人工核对详见 multi-provider-live-review.md。


2026-10-29 用户明确不提供区域选择步骤：自动采用来源数最多的组，同票按组内最大距离最小优先，完全相同时保持稳定顺序。API selectedCandidate 明确返回首组代表点；有共同区域统一 supported，无共同区域仍未知，不能强造位置。Demo 只展示自动采用组，无区域选择按钮。此修订替代前文多组要求用户选择的规则。

2026-10-29 基准规则（随后被下方修订取代）：曾把任何远处合格候选都视为分店歧义。

2026-10-29 修订：单个远点不否决已有的紧密共识组；将远点列入 excluded。只有另有至少两个来源组成的远处区域组，才阻止自动选择并返回 ambiguous。这样保留多源共识，同时避免把一个低支持点误当成竞争门店共识。完整 postcode 必须严格匹配候选 postcode；规范化修复 King's/Kings 与 Rd/Road 写法。

2026-10-29 Demo 文本回退：没有共识时，仅使用有有效坐标、可选择的 venue/address/postcode 候选；品牌及非通用的街道/区域词必须出现在名称或地址中。忽略通用店型和宽泛城市词。只有这些文本匹配候选整体直径不超过 0.6 英里时返回 `text-match` 并展示单个代表候选；它明确标为低置信度，不等同多源确认。若同一街道词匹配到多个远处门店则仍为 ambiguous（例如 Sainsbury’s Wandsworth Road），postcode 查询仍必须完全相同。该回退目前只用于 Demo 检验，不用于正式通勤地点选择。
