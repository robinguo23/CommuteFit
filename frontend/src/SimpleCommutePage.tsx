import { useEffect, useRef, useState } from 'react'
import type { FormEvent } from 'react'
import { ApiFailure, cancelCommuteCheck, extractJobDetails, pollCommuteCheck, startCommuteCheck } from './api/client'
import type { CommuteCheckJob, JobExtraction, ReferenceType, SimpleCheckResult, SimpleDirection, Weekday } from './api/client'
import { buildSimpleInput, createRunGuard, jobExactShifts, jobPostcodes, normalizePostcode, SimpleFormError, simpleWeekdays } from './simpleCommuteForm'
import './simpleCommute.css'

type Lang='zh'|'en'
const typeNames:Record<string,[string,string]>={day:['日间','Daytime'],early:['早班','Early'],night:['晚班','Late / overnight'],exact:['明确班次','Specified shift']}
const weekNames=['周一','周二','周三','周四','周五','周六','周日']
const clock=(value?:string)=>value?.slice(11,16)??'—'
const dayLabel=(value:string,zh:boolean)=>new Intl.DateTimeFormat(zh?'zh-CN':'en-GB',{weekday:'short',month:'short',day:'numeric',timeZone:'UTC'}).format(new Date(value+'T12:00:00Z'))
function reason(code:string,zh:boolean){
 const messages:Record<string,[string,string]>={
  'over-limit':['超过单程通勤上限','Exceeds your one-way limit'],'no-route':['这些参考时间未找到合适路线','No matching route at the checked times'],
  'missing-data':['路线资料不足，待确认','Route information is incomplete'],'service-unavailable':['路线服务暂不可用','Journey service unavailable'],
  'rate-limited':['路线服务限流，待重试','Journey service rate limited'],'location-unresolved':['无法识别该区域，请检查 postcode','Could not resolve this area; check its postcode'],
  'time-unavailable':['该当地时间无法明确查询','This local time could not be checked'],'past-time':['今天该参考时间已过，未作判断','Today’s reference time has passed; not assessed'],
  'outside-window':['不在你提供的可工作窗口内','Outside the availability window you entered'],'time-budget':['检查未能在时间内完成，请重试','Check time limit reached; retry']
 }
 return messages[code]?.[zh?0:1]??(zh?'信息不足，待确认':'Not enough information to decide')
}
function failure(error:unknown,zh:boolean){
 if(error instanceof SimpleFormError){
  const messages:Record<string,[string,string]>={origin:['请填写完整的出发 postcode。','Enter a complete starting postcode.'],work:['请填写完整的工作区域 postcode。','Enter a complete work-area postcode.'],limit:['单程上限须为1–1440之间的整数。','Use a whole-number limit from 1 to 1440 minutes.'],times:['请填写不同的上下班时间。','Enter both shift times; start and finish must differ.'],references:['请至少选择一个参考时段。','Choose at least one reference type.'],shifts:['请至少选择一条已提取班次。','Select at least one extracted shift.'],anchor:['请确认第一周周一的日期。','Confirm the Monday of rota Week 1.'],window:['请填写完整且不同的可工作时间边界。','Enter both availability boundaries; they must differ.']}
  return messages[error.code]?.[zh?0:1]??(zh?'请检查输入。':'Check your inputs.')
 }
 if(error instanceof ApiFailure){
  if(error.payload.code==='api_not_found')return zh?'后端尚未加载新版通勤检查接口，请重启本地服务。':'Restart the local backend to load the new commute-check endpoint.'
  if(error.payload.code==='check_expired')return zh?'本次检查已过期，请重新检查。':'This check expired. Start a new check.'
  if(error.payload.code==='check_busy')return zh?'已有检查正在进行，请稍后再试。':'Other checks are still running. Retry shortly.'
  if(error.payload.code==='request_timeout')return zh?'请求超时，请重试。':'The request timed out. Retry.'
  return (zh?error.payload.messageZh:error.payload.messageEn)??(zh?'无法连接检查服务，请重试。':'Could not connect to the check service. Retry.')
 }
 return zh?'检查暂未完成，请重试。':'The check could not finish. Please retry.'
}
export default function SimpleCommutePage(){
 const [lang,setLang]=useState<Lang>('zh'),zh=lang==='zh'
 const [origin,setOrigin]=useState(''),[work,setWork]=useState(''),[limit,setLimit]=useState('60'),[references,setReferences]=useState<ReferenceType[]>(['day'])
 const [manual,setManual]=useState(false),[start,setStart]=useState(''),[end,setEnd]=useState(''),[days,setDays]=useState<Weekday[]>([])
 const [windowEnabled,setWindowEnabled]=useState(false),[from,setFrom]=useState(''),[until,setUntil]=useState(''),[availableDays,setAvailableDays]=useState<Weekday[]>([])
 const [sourceType,setSourceType]=useState<'url'|'description'>('url'),[source,setSource]=useState(''),[extraction,setExtraction]=useState<JobExtraction|null>(null),[extracting,setExtracting]=useState(false),[extractError,setExtractError]=useState('')
 const [useExtracted,setUseExtracted]=useState(false),[selected,setSelected]=useState<number[]>([]),[anchor,setAnchor]=useState('')
 const [busy,setBusy]=useState(false),[job,setJob]=useState<CommuteCheckJob|null>(null),[result,setResult]=useState<SimpleCheckResult|null>(null),[error,setError]=useState('')
 const guard=useRef(createRunGuard()),jobId=useRef<string|null>(null),extractionVersion=useRef(0)
 const exactShifts=extraction?jobExactShifts(extraction):[],postcodes=extraction?jobPostcodes(extraction):[]
 const selectedShifts=useExtracted?exactShifts.filter((_,i)=>selected.includes(i)):undefined
 const hasExact=manual||useExtracted
 useEffect(()=>()=>{guard.current.next();extractionVersion.current++;if(jobId.current)void cancelCommuteCheck(jobId.current).catch(()=>{})},[])
 function stop(){guard.current.next();if(jobId.current)void cancelCommuteCheck(jobId.current).catch(()=>{});jobId.current=null;setBusy(false);setJob(null)}
 function edit(change:()=>void){stop();setResult(null);setError('');change()}
 const toggleDay=(values:Weekday[],set:(v:Weekday[])=>void,day:Weekday)=>edit(()=>set(values.includes(day)?values.filter(d=>d!==day):[...values,day]))
 const weekdayPicker=(values:Weekday[],set:(v:Weekday[])=>void)=><div className="simple-days">{simpleWeekdays.map((d,i)=><label key={d}><input type="checkbox" checked={values.includes(d)} onChange={()=>toggleDay(values,set,d)}/>{zh?weekNames[i]:['Mon','Tue','Wed','Thu','Fri','Sat','Sun'][i]}</label>)}</div>
 async function submit(e:FormEvent<HTMLFormElement>){
  e.preventDefault();stop();setResult(null);setError('')
  let input
  try{
   const data=new FormData(e.currentTarget)
   input=buildSimpleInput({origin,work,limit,references,manual,start:String(data.get('shiftStart')??start),end:String(data.get('shiftEnd')??end),days,
    extractedShifts:selectedShifts,windowEnabled,from:String(data.get('availableFrom')??from),until:String(data.get('availableUntil')??until),availableDays,anchor:String(data.get('rotaAnchor')??anchor),rotaWeeks:Math.max(1,...exactShifts.map(s=>s.cycleWeek??1))})
  }catch(e){setError(failure(e,zh));return}
  const version=guard.current.next();setBusy(true)
  let ownId:string|undefined
  try{
   let current=await startCommuteCheck(input);ownId=current.id
   if(!guard.current.isCurrent(version)){void cancelCommuteCheck(current.id).catch(()=>{});return}
   jobId.current=current.id;setJob(current)
   while(current.status==='running'){
    await new Promise(resolve=>window.setTimeout(resolve,900))
    if(!guard.current.isCurrent(version))return
    current=await pollCommuteCheck(ownId)
    if(!guard.current.isCurrent(version))return
    setJob(current)
   }
   if(current.status==='complete'&&current.result)setResult(current.result)
   else if(current.error)throw new ApiFailure(0,current.error)
   else throw new Error('Check cancelled')
  }catch(e){if(guard.current.isCurrent(version))setError(failure(e,zh))}
  finally{
   if(ownId)void cancelCommuteCheck(ownId).catch(()=>{})
   if(guard.current.isCurrent(version)){jobId.current=null;setBusy(false)}
  }
 }
 function changeSource(value:string,type=sourceType){
  extractionVersion.current++;setExtracting(false);setExtractError('');
  edit(()=>{setSource(value);setSourceType(type);setExtraction(null);setUseExtracted(false);setSelected([])})
 }
 async function extract(){
  const version=++extractionVersion.current;setExtracting(true);setExtractError('')
  edit(()=>{setExtraction(null);setUseExtracted(false);setSelected([]);setWork('');setManual(false);setStart('');setEnd('');setDays([]);setAnchor('')})
  try{
   const data=await extractJobDetails({sourceType,content:source})
   if(version!==extractionVersion.current)return
   const pc=jobPostcodes(data),shifts=jobExactShifts(data)
   edit(()=>{setExtraction(data);if(pc.length===1)setWork(pc[0]);setUseExtracted(shifts.length>0);setSelected(shifts.map((_,i)=>i))})
   if(data.status==='unavailable')setExtractError(zh?'读取失败，可粘贴正文或直接填工作区域 postcode。':'Could not read this listing. Paste its text or enter the work postcode.')
  }catch(e){if(version===extractionVersion.current)setExtractError(failure(e,zh))}
  finally{if(version===extractionVersion.current)setExtracting(false)}
 }
 const typeLabel=(type:string,label='')=>type==='exact'?label:(typeNames[type]?.[zh?0:1]??type)
 const directionLabel=(value:string)=>value==='outbound'?(zh?'去程':'Outbound'):value==='return'?(zh?'回程':'Return'):(zh?'工作时间':'Work hours')
 const issues=result?Object.values(result.issues.reduce<Record<string,{text:string;dates:string[]}>>((out,i)=>{
  const key=[i.type,i.label,i.direction,i.reason].join('|')
  out[key]??={text:typeLabel(i.type,i.label)+' · '+directionLabel(i.direction)+'：'+reason(i.reason,zh),dates:[]}
  if(!out[key].dates.includes(i.date))out[key].dates.push(i.date);return out
 },{})):[]
 const positive=result?Object.values(result.positiveScopes.reduce<Record<string,{label:string;dates:string[]}>>((out,s)=>{
  const key=s.type+'|'+s.label;out[key]??={label:typeLabel(s.type,s.label),dates:[]};out[key].dates.push(s.date);return out
 },{})):[]
 function directionDetails(d:SimpleDirection,returning:boolean){return <div className="simple-direction"><strong>{returning?(zh?'回程':'Return'):(zh?'去程':'Outbound')}</strong><span>{d.status==='within-limit'?`${d.route?.durationMinutes} ${zh?'分钟 · 符合上限':'min · within limit'}`:reason(d.reason,zh)}</span>
  {d.route&&<><p>{clock(d.route.departure)} → {clock(d.route.arrival)} · {d.route.transfers} {zh?'次换乘':'changes'}{d.targetTime&&<small> · {dayLabel(d.targetTime.slice(0,10),zh)} {clock(d.targetTime)} {returning?(zh?'后出发':'depart after'):(zh?'前到达':'arrive by')}</small>}</p>
  {d.route.legs.map((leg,i)=><p className="simple-leg" key={i}>{leg.route||leg.mode} · {leg.from} → {leg.to}</p>)}</>}
  <details className="simple-attempts"><summary>{zh?'检查过的参考时间':'Checked reference times'}</summary>{d.attempts.map((a,i)=><div key={i}>{dayLabel(a.targetTime.slice(0,10),zh)} {clock(a.targetTime)} · {a.status==='within-limit'?(zh?'符合':'Fits'):reason(a.reason,zh)}</div>)}</details>
 </div>}
 return <div className="simple-app"><header className="simple-header"><a href="/" className="simple-brand">Commute Fit</a><button type="button" className="simple-language" onClick={()=>setLang(zh?'en':'zh')}>{zh?'English':'中文'}</button></header>
 <main className="simple-main"><section className="simple-hero"><span>COMMUTE FIT</span><h1>{zh?'这份工作，通勤合适吗？':'Could this commute work for you?'}</h1><p>{zh?'输入两个 postcode，先看看是否值得尝试投递。':'Enter two postcodes for a quick check before you apply.'}</p></section>
 <form className="simple-form" onSubmit={submit} noValidate>
  <div className="simple-grid"><label>{zh?'出发 postcode':'Starting postcode'}<input value={origin} maxLength={16} autoComplete="postal-code" placeholder="DA18 4DE" onChange={e=>edit(()=>setOrigin(e.target.value))} onBlur={()=>{const p=normalizePostcode(origin);if(p)setOrigin(p)}}/></label>
  <label>{zh?'工作区域 postcode':'Work-area postcode'}<input value={work} maxLength={16} autoComplete="off" placeholder="SE18 6FL" onChange={e=>edit(()=>setWork(e.target.value))} onBlur={()=>{const p=normalizePostcode(work);if(p)setWork(p)}}/></label></div>
  <label className="simple-limit">{zh?'最长单程':'Maximum one-way commute'}<input type="number" min="1" max="1440" step="1" value={limit} onChange={e=>edit(()=>setLimit(e.target.value))}/><span>{zh?'分钟':'minutes'}</span></label>
  {!hasExact&&<fieldset className="simple-reference"><legend>{zh?'想检查哪些时段？':'Which times would you like to check?'}</legend><div>{(['day','early','night'] as ReferenceType[]).map(type=><label key={type} className={references.includes(type)?'selected':''}><input type="checkbox" checked={references.includes(type)} onChange={()=>edit(()=>setReferences(references.includes(type)?references.filter(t=>t!==type):[...references,type]))}/>{typeLabel(type)}</label>)}</div></fieldset>}
  <details className="simple-optional"><summary>{zh?'有具体时间或招聘信息？（选填）':'Have shift times or a job listing? (Optional)'}</summary>
   <section className="simple-option-section"><h2>{zh?'读取招聘信息':'Read a job listing'}</h2><div className="simple-source"><button type="button" aria-pressed={sourceType==='url'} onClick={()=>changeSource('','url')}>{zh?'招聘链接':'Job URL'}</button><button type="button" aria-pressed={sourceType==='description'} onClick={()=>changeSource('','description')}>{zh?'粘贴正文':'Paste description'}</button></div>
   <label>{sourceType==='url'?(zh?'招聘网址':'Listing URL'):(zh?'职位描述':'Job description')}{sourceType==='url'?<input type="url" value={source} placeholder="https://…" onChange={e=>changeSource(e.target.value)}/>:<textarea value={source} onChange={e=>changeSource(e.target.value)}/>}</label>
   <button type="button" className="simple-secondary" disabled={!source.trim()||extracting} onClick={extract}>{extracting?(zh?'读取中…':'Reading…'):(zh?'提取并确认':'Extract and review')}</button>
   {extractError&&<p role="alert">{extractError}</p>}
   {extraction&&<div className="simple-extraction"><p>{extraction.title}</p>{extraction.jobStatus==='expired'&&<p>{zh?'招聘可能已过期，请确认是否仍在招聘。':'This listing may have expired. Check it is still open.'}</p>}
    {postcodes.length===0?<p>{zh?'未找到完整的工作 postcode，请在上方补充工作区域。':'No complete work postcode was found. Add the work area above.'}</p>:<div className="simple-postcodes">{postcodes.map(p=><button type="button" key={p} onClick={()=>edit(()=>setWork(p))}>{p}</button>)}</div>}
    {exactShifts.length>0&&<><label className="simple-toggle"><input type="checkbox" checked={useExtracted} onChange={e=>edit(()=>{setUseExtracted(e.target.checked);if(e.target.checked)setManual(false)})}/>{zh?'使用提取出的实际班次':'Use extracted actual shifts'}</label>
    {useExtracted&&exactShifts.map((s,i)=><label className="simple-shift" key={i}><input type="checkbox" checked={selected.includes(i)} onChange={()=>edit(()=>setSelected(selected.includes(i)?selected.filter(x=>x!==i):[...selected,i]))}/><span>{s.cycleWeek?(zh?`第${s.cycleWeek}周 · `:`Week ${s.cycleWeek} · `):''}{s.start}–{s.end}{s.end<s.start?(zh?' 次日':' next day'):''}<small>{s.days.length?s.days.map(d=>zh?weekNames[simpleWeekdays.indexOf(d)]:d.slice(0,3)).join(' · '):(zh?'星期未明确，覆盖七天':'Weekdays unknown; check all seven days')}</small></span></label>)}</>}
    {!exactShifts.length&&(extraction.schedules??[]).filter(s=>s.kind==='availability-window'&&s.start&&s.end).map((s,i)=><div key={i}><p>{zh?'雇主可排班窗口，非实际班次：':'Employer window, not an actual shift: '}{s.start}–{s.end}</p>{!s.cycleWeek&&<button className="simple-secondary" type="button" onClick={()=>edit(()=>{setWindowEnabled(true);setFrom(s.start!);setUntil(s.end!);setAvailableDays(s.days??[])})}>{zh?'按此窗口做参考':'Use this reference window'}</button>}</div>)}
   </div>}
   </section>
   <section className="simple-option-section"><label className="simple-toggle"><input type="checkbox" checked={manual} onChange={e=>edit(()=>{setManual(e.target.checked);if(e.target.checked)setUseExtracted(false)})}/>{zh?'我有明确的上下班时间':'I have specific shift times'}</label>
    {manual&&<><div className="simple-grid"><label>{zh?'上班时间':'Shift starts'}<input type="time" name="shiftStart" value={start} onChange={e=>edit(()=>setStart(e.target.value))}/></label><label>{zh?'下班时间':'Shift ends'}<input type="time" name="shiftEnd" value={end} onChange={e=>edit(()=>setEnd(e.target.value))}/></label></div><p className="simple-hint">{zh?'工作日选填；不选则检查七天。结束早于开始表示次日。':'Workdays are optional; none means all seven. An earlier finish is on the next day.'}</p>{weekdayPicker(days,setDays)}</>}
    {useExtracted&&selectedShifts?.some(s=>s.cycleWeek)&&<label>{zh?'第一周的周一日期（按实际排班确认）':'Monday of rota Week 1 (confirm with the employer)'}<input name="rotaAnchor" type="date" value={anchor} onChange={e=>edit(()=>setAnchor(e.target.value))}/></label>}
   </section>
   <section className="simple-option-section"><label className="simple-toggle"><input type="checkbox" checked={windowEnabled} onChange={e=>edit(()=>setWindowEnabled(e.target.checked))}/>{zh?'限制在我的可工作时间窗口内':'Restrict to my availability window'}</label>
   {windowEnabled&&<><div className="simple-grid"><label>{zh?'窗口开始':'Available from'}<input type="time" name="availableFrom" value={from} onChange={e=>edit(()=>setFrom(e.target.value))}/></label><label>{zh?'窗口结束':'Available until'}<input type="time" name="availableUntil" value={until} onChange={e=>edit(()=>setUntil(e.target.value))}/></label></div>{weekdayPicker(availableDays,setAvailableDays)}<p className="simple-hint">{zh?'只约束时间，不把窗口当作实际班次；日期不选表示每天。':'This constrains times without inventing a shift. No selected days means every day.'}</p></>}
   </section>
  </details>
  <div className="simple-actions"><button className="simple-submit" disabled={busy||extracting} type="submit">{busy?(zh?'正在检查…':'Checking…'):(zh?'检查通勤':'Check commute')}</button>{busy&&<button type="button" className="simple-secondary" onClick={stop}>{zh?'取消':'Cancel'}</button>}</div>
  {busy&&<div role="status" className="simple-progress"><span>{job?.total?(zh?`已检查 ${job.completed} / ${job.total} 个时段`:`Checked ${job.completed} / ${job.total} references`):(zh?'准备检查…':'Preparing…')}</span><progress max={job?.total||1} value={job?.completed||0}/></div>}
  {error&&<p className="simple-error" role="alert">{error}</p>}
 </form>
 {result&&<section className={'simple-result '+result.status} aria-live="polite"><div className="simple-verdict"><span aria-hidden="true">{result.status==='suitable'?'✓':result.status==='unsuitable'?'!':'?'}</span><div><h2>{result.status==='suitable'?(zh?'可以通勤，可以尝试投递':'The commute can work — consider applying'):result.status==='unsuitable'?(zh?'不符合本次通勤条件':'Does not fit these commute conditions'):(zh?'暂时无法判断':'Not enough information yet')}</h2><p>{result.originPostcode} → {result.workPostcode}</p></div></div>
  <p>{dayLabel(result.scopeStart,zh)} — {dayLabel(result.scopeEnd,zh)} · {result.maxOneWayMinutes} {zh?'分钟单程上限':'min one-way limit'}</p>
  {positive.slice(0,4).map((p,i)=><p className="simple-positive" key={i}>{p.label}：{p.dates.length===7?(zh?'七天均有符合上限的往返':'Matching round trips on all seven days'):p.dates.map(d=>dayLabel(d,zh)).join('、')}</p>)}
  {!!issues.length&&<ul className="simple-issues">{issues.slice(0,3).map((i,n)=><li key={n}>{i.text}<small>{i.dates.map(d=>dayLabel(d,zh)).join('、')}</small></li>)}</ul>}
  {!result.days.some(d=>d.checks.length>0)&&<p>{zh?'这七天没有已确认的适用班次，请检查日期或周期设置。':'There are no confirmed applicable shifts in these seven days. Review the days or rota.'}</p>}
  <p className="simple-hint">{result.mode==='reference'?(zh?'按所选参考时段做初筛，具体班次请向雇主确认。':'An initial screen using your selected reference times. Confirm actual shifts with the employer.'):(zh?'按已给定时间检查；未明确工作日时，日期仅作通勤参考。':'Checked at the supplied times; unspecified workdays are commute examples.')} {zh?'Postcode 代表区域，精确门店的最后一段路可能不同。':'A postcode represents an area; the final walk to the workplace may differ.'}</p>
  <details className="simple-results-details"><summary>{zh?'查看日期、路线与判断依据':'View dates, routes and reasons'}</summary>
   {result.mode==='reference'&&<p className="simple-hint">{zh?'去程和回程分别检查参考窗口的交通覆盖，不组成固定班表。':'Outbound and return independently check transport coverage; they do not form a fixed shift.'}</p>}
   {result.days.map(d=><section className="simple-result-day" key={d.date}><h3>{dayLabel(d.date,zh)}</h3>{!d.checks.length?<p>{zh?'当天无已确认班次':'No confirmed shift on this day'}</p>:d.checks.map((c,i)=><details key={i}><summary>{typeLabel(c.type,c.label)} · {c.status==='suitable'?(zh?'往返符合':'Round trip fits'):c.status==='unsuitable'?(zh?'存在不符合方向或时间条件':'A direction or time condition does not fit'):(zh?'待确认':'Needs confirmation')}</summary>{c.availability==='outside'&&<p>{zh?'班次超出你提供的可工作窗口。':'The shift falls outside your availability.'}</p>}<div className="simple-directions">{directionDetails(c.outbound,false)}{directionDetails(c.returning,true)}</div></details>)}</section>)}
   <p className="simple-hint">TfL Journey Planner · Europe/London · {new Date(result.queriedAt).toLocaleString(zh?'zh-CN':'en-GB',{timeZone:'Europe/London'})}</p>
  </details>
 </section>}
 <footer className="simple-footer"><p>{zh?'计划交通信息来自 TfL，不保证实时准点。是否投递由你决定。':'Scheduled transport information from TfL. Punctuality is not guaranteed; applying is your decision.'}</p><nav><a href="/advanced">{zh?'高级工作检查':'Advanced job check'}</a><a href="/journey">{zh?'单独查路线':'Journey lookup'}</a></nav></footer>
 </main></div>
}
