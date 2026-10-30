# 本地运行与验收

在项目根目录运行：

```sh
bash start-local.sh
```

已有旧服务时，先在原启动终端 Ctrl+C，再运行脚本；无需删除 node_modules 或数据库。打开 Vite 实际输出的网址。脚本将运行独立 JAR 副本，并在退出时清理；后端代码变更需重启，前端开发服务热更新。

## 地点确认闭环

1. 输入出发 postcode，例如 DA18 4DE，保留单程上限 60 分钟。
2. 工作地点输入 M&S Woolwich，点击“搜索并确认位置”。也可在岗位提取结果中点击“使用这个工作地点”，触发搜索。
3. 查看名称、街道、postcode、来源、门店/交通站点分类。选候选预览地图，然后明确确认。同名分店按地址和地图区分；不默认选第一条，不用邻近车站凑门店位置。
4. 若只搜到城市或街道，确认按钮应禁用。补充街道、门牌或官方 postcode 后再搜索。数据源未收录门店不属于“通勤不符合”。
5. 确认后地点框显示候选名称。输入班次或查询七天参考组合，去程使用出发地 → 已确认地点，回程相反。更改班次保留地点；编辑地点清除确认；重新搜索可恢复过期选择。
6. 直接 postcode 可用于路线查询，也可搜索确认其邮区精度；不能将邮编中心点当作精确门店入口。ID 输入只确认格式，不假称已经核验站名。输入坐标按纬度,经度，不是 GeoJSON 顺序。

Photon 未返回精确门店、中文门店名无法匹配、服务不可用时，补充公开地址或完整 postcode。默认公共 demo 不保证覆盖或稳定性。自托管可设置启动环境变量 PHOTON_BASE_URL；未安装自托管地理库。

## 独立比较 Geoapify

此项只测试一个供应商，不改变正式地点查询的来源顺序：

1. 在 Geoapify 账户创建 API key，将 `GEOAPIFY_API_KEY=...` 写到项目根目录 `.env`（不要提交或分享该文件）。
2. 重启 `bash start-local.sh`，打开 `/place-demo`。
3. provider 下拉框选 Geoapify。依次用相同查询词测试，例如 `M&S Woolwich Simply Food`、`M&S Woolwich`、`Woolwich`、`SE18 6FL`；每次只调用当前选中的 Geoapify，不会 fallback 到别家。
4. 记录候选是否正确、门店地址与 postcode 是否吻合、目标是否在前八条、同名分店能否区分。postcode 是邮区级候选；街区或街道候选不能被当成门店入口。
5. 错误状态按缺少/无效 key、额度限制、上游错误或网络失败显示。当前本机未配置 key 时，Demo 应显示 Geoapify 缺 key，不应将其标成无结果或 TfL 故障。

Geoapify 还未通过真实搜索质量验收；完成本轮记录后再接入下一家服务。

## 下一家：MapTiler

MapTiler 已加入 provider 下拉框并完成一次真实 key 验收。复测时在 `.env` 加 `MAPTILER_API_KEY=...` 并重启；选择 MapTiler，对比 `M&S Woolwich Simply Food`、`M&S Woolwich`、`SE18 6FL`。请求限定英国、关闭 autocomplete、最多 8 条候选，且只调用 MapTiler。此案例的门店名搜索没命中目标；postcode 首位结果正确。实测细节见 CHANGELOG.md。官方免费 plan 适用限制及额度会调整，以 MapTiler 官方定价页为准。

LocationIQ 已加入 demo 并完成三条真实查询。postcode 查询会走专门的 postal code endpoint；门店/区域文字走 Forward Geocoding。官方 Free tier 当前额度为 5,000 请求/天、2 请求/秒，免费商业使用与 attribution 限制以官方定价页为准。HTTP 404 在 LocationIQ 搜索里表示无法匹配地址，应当显示“无匹配”，不是“服务不可用”。实测细节见 CHANGELOG.md。

OpenCage 已接入并完成实测。`M&S Woolwich Simply Food`、`Marks and Spencer Woolwich` 均无匹配；完整地址 `Marks & Spencer, 4 Victory Parade, Woolwich, SE18 6FL` 返回正确门店；`SE18 6FL` 返回邮区代表点。结论：完整地址 geocoding 可用，但品牌+区域门店搜索召回不足。配置 key 后仍可在 `/place-demo` 复测，请求带 `countrycode=gb`、`limit=8`、`no_record=1`。其官方免费 trial 仅供测试（2,500 次/天、1 request/sec），不是长期免费层。结果详见 CHANGELOG.md。


Positionstack 因注册要求信用卡而跳过。geolith 已实际测试，但品牌门店搜索结果不符合要求，已从项目中移除。Radar 不用于同类服务竞品测试。地点搜索 demo 保留 Photon、Geoapify、MapTiler、LocationIQ、OpenCage、TomTom Places Search 及 TfL 接口，按用户选择逐个调用。TomTom 使用官方 Orbis Places Discover API；先配置 `.env` 中的 `TOMTOM_API_KEY`，重启后在 `/place-demo` 逐项对比 M&S 门店搜索。实测 `M&S Woolwich` 能把目标 M&S Foodhall 排在第一，但完整地址下门牌和 postcode 只返回粗略的 `6 Victory Parade, SE18`；精确输入 postcode 时解析层会保留用户输入的完整邮编。官方定价列出 Places Discover 每月 5,000 次免费请求且无需信用卡。

## 班次回归

- 粘贴 backend/src/test/resources/jobextraction/marks-woolwich.txt。应保留六条实际安排及各自第一周/第二周、中文星期和时间，可多选；无实际周期日期，必须手动提供第一周的周一。不能把六条安排扩成每天都上六个班。
- 两个未写星期的班次应查七天 × 两班 × 往返，共28个方向；不能因为广告写每周五天含周末而只选周末。实际班次存在时不再查雇主可排班窗口。
- 个人 availability 只用于对比，不改变岗位班次的查询时刻。

## 自动验证

```sh
cd backend
./mvnw verify
```

```sh
cd frontend
npm test
npm run build
```

五类 TfL 端点为 postcode、WGS84、NaPTAN、ICS 和自由文本。自动化覆盖25组起终点组合以及消歧选择重查、坐标顺序、同名分店和选中令牌。真实自由文本可能返回消歧，而不是路线；不能将该响应视为格式不支持。

在项目根目录可另行低频实测：

```sh
python3 scripts/check_tfl_location_types.py
python3 scripts/check_tfl_location_types.py --live
```

第一行是 dry-run；第二行发五次请求，读取 .env 中的 app_key 但不打印密钥。输入不是同一地理点，不要求时长相等。

## 已知验收状态

用户重启后，已在新后端页面完成中文双周样例提取、地图地点确认、六个班次十二个方向的实际路线查询，以及修改地点清除确认、班次变化保留地点。结果和时长见 CHANGELOG.md。

本案例需将原提取的长名称手动细化为 M&S Woolwich；完整名称直接搜索出现其他伦敦分店，未将这些分店视为正确门店。不能因此宣称自动门店匹配已经完全解决。该页面闭环也不代表所有招聘 URL 或门店均可解析。

地点服务对照：`/place-demo` 中 **TomTom Search API · Fuzzy Search** 为旧 Search API 独立测试入口，限制 `countrySet=GB`，索引 `POI,PAD,Addr,Str,EPP`，请求扩展邮编字段，最多显示 8 个结果。配置仍使用 `TOMTOM_API_KEY`；如果控制台按 API 产品授权，需要另启 Search API。已用品牌、完整地址、postcode 实测，未找到目标。  

**Postcodes.io** 是免 key 的 postcode/geolocation 数据源，不是 POI 或商店目录。`/place-demo` 接入后可测试完整 postcode lookup、邮编前缀搜索及 `lat,lon` 最近邮编查询。邮编记录包含 `quality`，用于判断坐标代表点质量。公共服务遵守其 fair use 和服务条款；自托管选项见官方 GitHub。 


### ArcGIS 对照测试

重启 `bash start-local.sh` 后打开 http://127.0.0.1:5173/place-demo ，选择“ArcGIS 英国门店与地址搜索”。依次输入 `marks and spencer woolwich`、`M&S Woolwich Simply Food`、`Marks and Spencer Woolwich Simply Food`，核对 SE18 6FL 是否出现及排名。当前为公开 World Geocoding 端点非持久化实验，不自动改写查询或取第一条结果。公开端点拒绝请求时显示服务不可用；生产鉴权和服务条款需正式集成时另行确认。


### Foursquare Places 对照测试

在实际项目根目录 `.env` 的 `FOURSQUARE_API_KEY=` 后填入新版 Places API key，重启 `bash start-local.sh`。打开 `/place-demo` 选择“Foursquare Places 商业门店搜索”。测试固定 near=London, GB、英国候选；原样传查询词，未做品牌改写。缺 key、HTTP 拒绝、限流均明确显示服务不可用。


### 三家区域联合评估

重启 `bash start-local.sh`，打开 http://127.0.0.1:5173/place-demo ，默认“三家联合区域评估”。先输入 `marks and spencer woolwich`，观察三家响应、共同区域及最大点对距离。再输入 `M&S Woolwich Simply Food` 比较原始结果与排除原因。可继续选择 LocationIQ、ArcGIS、TomTom 独立检查。

- 三家一致：显示三家支持；一家报错时另外两家仍可组成共同区域。
- 多个有效区域：不跨区域平均，用户标记目标区域。
- 没有共同区域时再试 Demo-only 地址文字回退：品牌和街道/区域词匹配且候选集中时给出低置信度结果。已有紧密的多源共识时，单个远处候选不会否决它；只有另有至少两个来源支持的远处分店区域时才保持歧义。
- 标记区域使用已有候选代表点，仅 Demo 记录，不自动触发正式通勤查询。本轮不要求入口级精度；邮编不同不会单独导致排除。
- 名称筛选为启发式，可能漏掉缩写或资料缺失；“支持”不代表核实门店。

验证：AreaConsensusServiceTest 7 项 + LocationSearchServiceTest 27 项通过；前端 build 通过；后端已打包。三源联合的真实页面请求需重启新后端后验收，本轮不将之前单源实测记为联合验收。

### 无坐标共识时的地址文字回退（Demo 实验）

当三家没有形成可采用的坐标共识时，Demo 会查看原始门店候选：候选必须匹配品牌，查询中还必须有至少一个具体街道或区域词，并且这些地点词都要出现在候选名称或地址中。只有合格候选坐标整体处于 0.6 英里直径内，才显示 `text-match` 低置信度候选。只输入品牌名不够。已有紧密共识时，单个远处结果会列为组外候选、不否决共识；另有至少两个来源共同支持的远处分店区域时仍显示歧义。邮编查询仍要求完整 postcode 相同。

该回退只属于 `/place-demo` 的搜索实验，不接入正式地点选择或路线查询。最新定向验收：`AreaConsensusServiceTest` 18 项通过（含品牌单独查询和错误街道拒绝）；前端 `npm run build` 通过。后端新逻辑需重启 `bash start-local.sh` 才会加载。
