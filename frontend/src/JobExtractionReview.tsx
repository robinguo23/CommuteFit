import type { JobExtraction } from './api/client'
import { extractedScheduleDays, preferredScheduleEntries } from './extractionForm'

export default function JobExtractionReview({data,lang,onLocation,selected,onToggle}:{
 data:JobExtraction;lang:'zh'|'en';onLocation:(value:string)=>void;selected:number[];onToggle:(index:number)=>void
}) {
 const zh=lang==='zh',entries=preferredScheduleEntries(data)
 const exact=entries.some(e=>e.schedule.kind==='exact-shift')
 const dayNames:Record<string,string>={MONDAY:'周一',TUESDAY:'周二',WEDNESDAY:'周三',THURSDAY:'周四',FRIDAY:'周五',SATURDAY:'周六',SUNDAY:'周日'}
 return <section className="work-extraction" aria-label={zh?'岗位提取结果':'Job extraction results'}>
 <h3>{data.title||(zh?'提取结果，请确认':'Review extracted details')}</h3>
 <p>{data.status==='unavailable'?(zh?'无法读取链接，请粘贴正文或手动补充。':'Could not read the link. Paste the description or enter details manually.'):(zh?'选择要查询的岗位班次，可多选。未明确星期的班次将查询七天的往返。':'Select one or more shifts. Unspecified weekdays are checked across all seven days.')}</p>
 {data.jobStatus==='expired'&&<p role="status">{zh?'这份招聘可能已过期，请确认是否仍在招聘。':'This job may have expired. Confirm that it is still open.'}</p>}
 {(data.locations||[]).map((l,i)=><div className="work-extracted-item" key={i}><strong>{l.value}</strong>{l.sourcePath==='description.store'&&<p>{zh?'门店名称候选，查询前请确认地图位置；未推断 postcode。':'Store name candidate: confirm its map location. No postcode was inferred.'}</p>}<blockquote>{l.evidence}</blockquote><button className="work-button secondary" type="button" onClick={()=>onLocation(l.value)}>{zh?'使用这个工作地点':'Use this workplace'}</button></div>)}
 {entries.map(({schedule:s,index})=><div className="work-extracted-item" key={index}><label><input type="checkbox" checked={selected.includes(index)} onChange={()=>onToggle(index)}/>{' '}<strong>{s.cycleWeek?(zh?'第'+s.cycleWeek+'周 · ':'Week '+s.cycleWeek+' · '):''}{exact?(zh?'实际班次':'Actual shift'):(zh?'可排班窗口边界参考':'Availability window boundary')}{' · '+s.start+'–'+s.end}{s.endsNextDay?(zh?'（次日）':' (next day)'):''}</strong></label><p>{extractedScheduleDays(s).length?extractedScheduleDays(s).map(d=>zh?dayNames[d]:d).join(' · '):(zh?'星期未明确：覆盖七天查询，不代表七天都上班。':'Weekdays unspecified: check all seven, without assuming seven workdays.')}</p><blockquote>{s.evidence}</blockquote></div>)}
 {!entries.length&&<p>{zh?'没有可查询的岗位时间，可使用下方参考班次或手动填写。':'No queryable job times. Use examples below or enter a shift manually.'}</p>}
 {!exact&&entries.length>0&&<p>{zh?'岗位未给实际班次；以下按窗口开始与结束时刻查询，作为边界参考。':'Actual shifts are unknown; query window start/end as boundary examples.'}</p>}
 <p className="work-note">{zh?'已勾选班次用于下方查询；如需自定义班次，请切换到工作地点输入方式。':'Selected shifts drive the query below. Switch to Work postcode to enter a custom shift.'}</p>
 </section>
}
