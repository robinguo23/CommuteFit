# 四门店真实联合搜索 + Google Maps 核对（2026-10-29）

通过本机 Chrome 的 /place-demo 联合查询，再通过 Google Maps 搜索相同品牌+区域。以下为修正前运行后端真实响应；Google 仅核对显示的名称/地址/区域，未获取 Google 精确坐标，不能宣称做了 Google 点到点距离测量。

| 查询 | 原后端结果 | 原始候选关键数据 | Google Maps 显示 |
|---|---|---|---|
| marks and spencer woolwich | 2 家支持，最大相距约2米 | LIQ 51.49162,0.07068 SE18 6FL；ArcGIS 51.49161,0.07071 SE18 6FL；TT 51.49156,0.07058 SE18，被缺 Woolwich 关键词排除 | M&S Foodhall，Beresford Street 4 Victory Parade, High Street 61, London SE18 6FL |
| marks and spencer birstall | 待确认，未形成区域 | LIQ 404无匹配；ArcGIS WF17 9AD 53.74474,-1.64436 与 WF17 9DT 53.74610,-1.64321；TT 53.74480,-1.64443 WF17 9DT，因地址仅 Batley 排除 | M&S Simply Food，7, Holden Ing Way Unit 12 Birstall Shopping Park, Drumgold Street, Batley WF17 9AE |
| Costa Woolwich | 2 个区域：3 家/2 家支持，各最大距离约1米 | 核心区域 LIQ SE18 6AB 51.49031,0.06939；ArcGIS SE18 6EU 51.49030,0.06940；TT SE18 6EX 51.49030,0.06940。另一组是 395 Woolwich Road Charlton SE7 7AJ，距核心约2.9km | Costa Coffee - Woolwich DLR，1 Woolwich New Rd, London SE18 6EU |
| Nando's Woolwich | 待确认，旧规则排除两票 | LIQ 51.49143,0.06679；ArcGIS、TT 51.49144,0.06678，均 SE18 6LQ；仅 ArcGIS 地址含 Woolwich | Nando's Woolwich，50 Powis St, London SE18 6LQ |

## 发现及修正

严格要求候选地址出现全部查询词会漏掉实际位于同一区域、但上游地址缺少区域名的门店。修正：保留直接通过文本核对的原始 anchors；若另一个来源候选品牌词与查询一致，坐标距某个 anchor <=965.6064m，则允许以坐标补证参与共识，并在 UI 单独解释。被补证点不能继续作为 anchor 吸纳其他点，最终组内仍全体两两检查，防止链式扩散。错误品牌、距离过远或没有任何文本 anchor 都不能获得补证。

Google Maps 是人工核对参考，不接入正式搜索、不抓取门店库。Google 自己的邮编也可能不同；本产品接受近似工作区域，不能只以邮编不同判错。Costa 的真实多区域提醒继续保留，不能把含 Woolwich Road 的 Charlton 分店当作已确认的 Woolwich 镇中心店。

修正后：Nando's 实测字段回放得到3家支持；Birstall 实测字段回放得到2家支持并聚合到一个区域，最大距离小于200米。另测错误品牌/远点不能补证。定向共37项通过（AreaConsensus 10 + LocationSearch 27），前端构建通过、jar打包。修正后的真实联合请求尚需加载新后端后复测，不能将回放测试当成新后端实测。

Google核对链接：
- https://www.google.com/maps/search/Marks+and+Spencer+Woolwich/
- https://www.google.com/maps/search/Marks+and+Spencer+Birstall/
- https://www.google.com/maps/search/Costa+Woolwich/
- https://www.google.com/maps/search/Nandos+Woolwich/
