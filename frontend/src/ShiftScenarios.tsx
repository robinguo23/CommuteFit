import { useEffect, useMemo, useRef, useState } from 'react'
import { ApiFailure, assessScenario } from './api/client'
import type { ExtractedSchedule, WorkCommuteInput, WorkDayResult } from './api/client'
import { shiftAvailability } from './shiftAvailability'
import { calendarDates, scheduleApplies, validRotaAnchor } from './extractionForm'
import './shiftScenarios.css'

type Lang = 'zh' | 'en'
type Shift = { name: [string,string]; start: string; end: string; original?:ExtractedSchedule }
const shift = (zh: string,en: string,start: string,end: string):Shift => ({name:[zh,en],start,end})
function legLabel(mode:string, route:string, zh:boolean) {
 const key=mode.toLowerCase()
 const labels:Record<string,[string,string]>={walking:['步行','Walk'],walk:['步行','Walk'],tube:['地铁','Tube'],bus:['公交','Bus'],dlr:['轻轨','DLR'],'elizabeth-line':['伊丽莎白线','Elizabeth line'],overground:['地上铁','Overground'],'national-rail':['国家铁路','National Rail'],tram:['有轨电车','Tram'],'river-bus':['河上巴士','River bus'],'cable-car':['缆车','Cable car']}
 const name=labels[key]?.[zh?0:1]??mode
 return route && route.toLowerCase()!==name.toLowerCase() && route.toLowerCase()!==key ? name+' · '+route : name
}
const presets: Record<string,Shift[]> = {
 general:[shift('早班','Early','06:00','14:00'),shift('日班','Day','09:00','17:00'),shift('晚班','Late','14:00','22:00'),shift('通宵','Overnight','22:00','06:00')],
 supermarket:[shift('凌晨拣货','Picking','04:00','08:00'),shift('日班','Day','09:00','17:00'),shift('晚班','Late','14:00','22:00'),shift('夜间补货','Night restocking','22:00','07:00')],
 cafe:[shift('开店','Opening','05:00','13:00'),shift('日间','Day','08:00','16:00'),shift('午间支援','Lunch support','11:00','15:00'),shift('收店','Closing','13:00','21:00')],
 restaurant:[shift('早餐备料','Breakfast prep','06:00','14:00'),shift('午市','Lunch','11:00','16:00'),shift('晚市收档','Evening closing','17:30','23:30'),shift('夜间清洁','Overnight cleaning','23:00','07:00')]
}
function weekDates() {
 const parts = new Intl.DateTimeFormat('en-GB',{timeZone:'Europe/London',year:'numeric',month:'2-digit',day:'2-digit'}).formatToParts(new Date())
 const value = (part:string) => Number(parts.find(p=>p.type===part)?.value)
 return Array.from({length:7},(_,i)=>new Date(Date.UTC(value('year'),value('month')-1,value('day')+i)).toISOString().slice(0,10))
}
type State = WorkDayResult | 'loading' | 'error'
export default function ShiftScenarios({input,lang,onFailure,schedules,runVersion=0}: {input:WorkCommuteInput|null;lang:Lang;onFailure:(error:unknown)=>void;schedules?:ExtractedSchedule[];runVersion?:number}) {
 const zh=lang==='zh'
 const [preset,setPreset]=useState('general')
 const [referenceShifts,setShifts]=useState<Shift[]>(presets.general.map(s=>({...s})))
 const provided=schedules!==undefined
 const shifts=useMemo<Shift[]>(()=>schedules?schedules.map((s,i)=>({name:['班次 '+(i+1),'Shift '+(i+1)],start:s.start??'',end:s.end??'',original:s})):referenceShifts,[schedules,referenceShifts])
 const [anchor,setAnchor]=useState(''),[anchorError,setAnchorError]=useState(false)
 const cyclic=shifts.some(s=>s.original?.cycleWeek)
 const [results,setResults]=useState<Record<string,State>>({})
 const [running,setRunning]=useState(false)
 const generation=useRef(0)
 const referenceDates=useMemo(weekDates,[])
 const dates=useMemo(()=>cyclic&&validRotaAnchor(anchor)?calendarDates(anchor,14):referenceDates,[cyclic,anchor,referenceDates])
 const applies=(date:string,s:Shift)=>!s.original||scheduleApplies(date,s.original,anchor)
 const inputKey=JSON.stringify(input)
 useEffect(()=>{ generation.current++;setResults({});setRunning(false) },[inputKey,shifts,anchor])
 useEffect(()=>()=>{generation.current++},[])
 function changePreset(value:string) {
  if(JSON.stringify(shifts)!==JSON.stringify(presets[preset])&&!window.confirm(zh?'切换组合会替换已修改的时间，继续吗？':'Switching replaces your edited times. Continue?')) return
  setPreset(value);setShifts(presets[value].map(s=>({...s})))
 }
 async function run() {
  if(!input)return
  if(cyclic&&!validRotaAnchor(anchor)){setAnchorError(true);return}
  if(!shifts.length)return
  const id=++generation.current;setResults({});setRunning(true)
  const jobs=dates.flatMap(date=>shifts.map((s,i)=>({date,s,i})).filter(job=>applies(date,job.s)))
  let next=0
  async function worker() {
   while(next<jobs.length && generation.current===id) {
    const job=jobs[next++], key=job.date+'-'+job.i
    setResults(r=>({...r,[key]:'loading'}))
    try {
     const data=await assessScenario({...input!,workDays:[],shiftStart:job.s.start,shiftEnd:job.s.end},job.date)
     if(generation.current===id)setResults(r=>({...r,[key]:data}))
    } catch (error) {
     if(generation.current===id) {
      setResults(r=>({...r,[key]:'error'}))
      if(error instanceof ApiFailure && ['location_disambiguation','location_unresolved','candidate_expired','api_not_found','backend_unavailable','network_error'].includes(error.payload.code||'')) {
       generation.current++; setRunning(false); setResults(r=>Object.fromEntries(Object.entries(r).map(([k,v])=>[k,v==='loading'?'error':v]))); onFailure(error)
      }
     }
    }
   }
  }
  await Promise.all([worker(),worker()])
  if(generation.current===id)setRunning(false)
 }
 useEffect(()=>{if(runVersion>0)void run()},[runVersion])
 async function retry(date:string,i:number) {
  if(!input)return
  const id=generation.current, key=date+'-'+i
  setResults(r=>({...r,[key]:'loading'}))
  try {
   const data=await assessScenario({...input,workDays:[],shiftStart:shifts[i].start,shiftEnd:shifts[i].end},date)
   if(generation.current===id)setResults(r=>({...r,[key]:data}))
  } catch(error) {
   if(generation.current===id){setResults(r=>({...r,[key]:'error'}));onFailure(error)}
  }
 }
 const summary=(r:State|undefined)=>{
  if(!r)return zh?'尚未查询':'Not checked'
  if(r==='loading')return zh?'查询中…':'Checking…'
  if(r==='error')return zh?'查询失败，请重试':'Query failed; retry'
  if(r.outbound.status==='over-limit'||r.returning.status==='over-limit')return zh?'有方向超过通勤上限':'A journey exceeds the limit'
  if(r.outbound.status==='unknown'||r.returning.status==='unknown')return zh?'路线待确认':'Route needs confirmation'
  if(r.outbound.status==='within-limit'&&r.returning.status==='within-limit')return zh?'往返均在通勤上限内':'Both journeys within limit'
  return zh?'路线待确认':'Route needs confirmation'
 }
 function availability(date:string,s:Shift) {
  if(!input)return zh?'可工作时间未设置':'Availability not set'
  const status=shiftAvailability(date,s.start,s.end,input.availabilityDays,input.availableFrom,input.availableUntil)
  return status==='unknown'?(zh?'可工作时间未设置':'Availability not set'):status==='fits'?(zh?'符合已填可工作时间':'Fits your availability'):(zh?'与已填可工作时间有冲突，需确认':'Conflicts with your availability; confirm')
 }
 return <section className="work-card scenario-section"><h2>{provided?(zh?'已选岗位班次往返':'Selected job shift journeys'):(zh?'七天参考班次':'Seven-day shift examples')}</h2>
  <p>{provided?(zh?'按勾选的全部班次查询。未明确星期时覆盖七天，不代表岗位七天都上班。':'Check every selected shift. Missing weekdays cover all seven days without assuming seven workdays.'):(zh?'参考时间可以修改，实际岗位班次请向雇主确认。':'Edit these example times and confirm the actual shifts with the employer.')}</p>
  {provided&&cyclic&&<label className="work-field">{zh?'第一周的周一日期（按实际 rota 确认）':'Monday of Week 1 (confirm against the rota)'}<input type="date" value={anchor} onChange={e=>{setAnchor(e.target.value);setAnchorError(false)}}/><span>{zh?'原文只给周次，尚未给出实际周期起始日；请选择周一以查询对应十四天。':'The listing gives rota weeks, not their calendar dates. Choose a Monday to query the corresponding 14 days.'}</span></label>}
  {anchorError&&<p role="alert">{zh?'请选择有效的第一周周一日期。':'Choose a valid Monday for Week 1.'}</p>}
  {!provided&&<>
  <label>{zh?'参考组合':'Preset'} <select value={preset} onChange={e=>changePreset(e.target.value)}><option value="general">{zh?'通用':'General'}</option><option value="supermarket">{zh?'超市':'Supermarket'}</option><option value="cafe">{zh?'咖啡店 / 烘焙':'Café / bakery'}</option><option value="restaurant">{zh?'餐厅 / 酒吧':'Restaurant / pub'}</option></select></label>
  <div className="scenario-edit">{shifts.map((s,i)=><div key={i}><span>{s.name[zh?0:1]}</span><input aria-label={s.name[zh?0:1]+' start'} type="time" value={s.start} onChange={e=>setShifts(shifts.map((v,j)=>j===i?{...v,start:e.target.value}:v))}/><span>–</span><input aria-label={s.name[zh?0:1]+' end'} type="time" value={s.end} onChange={e=>setShifts(shifts.map((v,j)=>j===i?{...v,end:e.target.value}:v))}/>{s.end<s.start&&<small>{zh?'次日':'next day'}</small>}</div>)}</div>
  </>}
  {provided&&!shifts.length&&<p>{zh?'请至少勾选一个岗位班次。':'Select at least one job shift.'}</p>}
  <button type="button" className="work-button" disabled={!input||running||!shifts.length||shifts.some(s=>!s.start||!s.end)} onClick={run}>{running?(zh?'查询中…':'Checking…'):provided?(zh?'查询所有已选班次往返':'Check all selected shifts'):(zh?'查询七天参考班次':'Check seven days')}</button>
  {!input&&<p>{zh?'请先填写出发地点、工作地点和单程上限。':'Enter both locations and the one-way limit first.'}</p>}
  {(!cyclic||validRotaAnchor(anchor))&&<div className="scenario-scroll">{dates.map((date,dayIndex)=><article className="scenario-day" key={date}><h3>{cyclic?(zh?'第'+(Math.floor(dayIndex/7)+1)+'周 · ':'Week '+(Math.floor(dayIndex/7)+1)+' · '):''}{new Intl.DateTimeFormat(zh?'zh-CN':'en-GB',{weekday:'short',day:'numeric',month:'short',timeZone:'UTC'}).format(new Date(date+'T12:00:00Z'))}</h3>
   {!shifts.some(s=>applies(date,s))&&<p>{zh?'该日无已选班次。':'No selected shift on this day.'}</p>}
   {shifts.map((s,i)=>{if(!applies(date,s))return null;const r=results[date+'-'+i];return <details key={i}><summary><strong>{s.name[zh?0:1]} · {s.start}–{s.end}{s.end<s.start?(zh?' 次日':' next day'):''}{s.original?.kind==='availability-window'?(zh?' · 窗口边界参考':' · window boundary'):''}</strong><span>{summary(r)}</span><span>{availability(date,s)}</span></summary>
    {(r==='error'||(r&&r!=='loading'&&(r.outbound.status==='unknown'||r.returning.status==='unknown')))&&<button type="button" className="work-button secondary" onClick={()=>retry(date,i)}>{zh?'重试这个班次':'Retry this shift'}</button>}
    {r&&r!=='loading'&&r!=='error'&&[r.outbound,r.returning].map((dir,j)=><div className="scenario-route" key={j}><b>{j===0?(zh?'去程':'Outbound'):(zh?'回程':'Return')}</b> · {dir.bestDurationMinutes==null?(zh?'没有足够路线信息，待确认':'Route information unavailable'):dir.bestDurationMinutes+' min'} {dir.status==='over-limit'?(zh?'· 超出上限':'· over limit'):dir.status==='within-limit'?(zh?'· 在上限内':'· within limit'):''}
    {dir.routes.map(route=><div key={route.id}><strong>{route.departure?.slice(11,16)} → {route.arrival?.slice(11,16)} · {route.transfers} {zh?'次换乘':'changes'}</strong>{route.legs.map((leg,k)=><div key={k}>{legLabel(leg.mode,leg.route,zh)}: {leg.from} → {leg.to}</div>)}</div>)}</div>)}</details>})}
  </article>)}</div>}
 </section>
}
