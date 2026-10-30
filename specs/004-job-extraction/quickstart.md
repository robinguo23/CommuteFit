# Local validation

项目根目录运行 bash start-local.sh；已有服务先在原终端 Ctrl+C。打开 http://localhost:5173。
粘贴正文示例：Work location: E1 6AN. Shifts: Monday to Friday 09:00-17:00。确认地点和班次建议，修改后查询。
再输入 Availability window: Monday 06:00-22:00，应显示窗口且不自动填实际班次。多地点需手选，失败 URL 显示粘贴提示。
多地点样例：Work location E1 6AN or DA18 4DE；地点字段应留空，下方出现两个地点按钮。
多班型样例：Shifts: Monday 06:00-14:00 and 14:00-22:00；时间字段应留空，两个实际班次建议可分别选择。
新岗位提取后检查：上次岗位日期和时间不会残留；个人出发地点和 availability 不变。
后端运行 ./mvnw test；前端 npm test 与 npm run build。受限环境使用已有缓存的可写副本与 Java 21，具体结果记入 CHANGELOG.md。
