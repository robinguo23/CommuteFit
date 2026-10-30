# 功能迭代提示词模板 / Feature Iteration Prompt Template

复制下面的提示词，补充你知道的信息即可。暂时不清楚的部分可以留空。  
Copy the prompt below and fill in whatever you know. Leave unknown details blank.

```text
我想迭代伦敦通勤评估工具的一个功能。

遇到的情况：
- 我输入了：
- 系统现在显示：
- 我原本预期：
- 相关截图、路线、日期/时间或官方规则（如果有）：

请先对照当前 spec，判断这是：
1. 代码没有符合现有 spec；
2. spec 漏写了我已经确定的要求；或
3. 我想改变/增加功能。

处理方式：
- 如果现有 spec 已经写清预期，就按 spec 修复代码，并用我提供的例子核对。
- 如果需求已明确且需要改变 spec，更新中英双语 spec 和版本修改记录；若项目还没有版本记录，就先建立一份，记录日期、版本、受影响的需求、原因和验收场景。同步受影响的 plan/tasks，再运行只读 Analyze 检查一致性，然后实现并核对。
- 如果我的预期或产品规则还不明确，先问我最关键的问题，不要自行猜测。
- 不要从安装或 constitution 流程重新开始；只重跑受本次变化影响的步骤。
- 说明改了什么、版本记录了什么，以及如何核对结果。
```

## English version

```text
I want to iterate on a feature in the London Commute Assessment tool.

What happened:
- I entered:
- The system showed:
- I expected:
- Relevant screenshot, route, date/time, or official rule (if available):

First compare this with the current spec and classify it as:
1. Code not matching an existing requirement;
2. A requirement I have decided on is missing from the spec; or
3. A change or addition to the feature.

Then:
- If the spec already defines the expected behavior, fix the code against the spec and check it using my example.
- If the requirement is clear and the spec must change, update the bilingual spec and its version history. If the project has no version history yet, create one and record the date, version, affected requirements, reason, and acceptance scenario. Update affected plan/tasks, run the read-only Analyze for consistency, then implement and verify the change.
- If my expectation or the product rule is unclear, ask me the single most important question instead of guessing.
- Do not restart installation or constitution steps; rerun only the steps affected by this change.
- Tell me what changed, what was recorded in the version history, and how the result was checked.
```
