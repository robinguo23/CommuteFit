import type { JobExtraction, ReferenceType, SimpleCheckInput, SimpleShift, Weekday } from './api/client.ts'

export const simpleWeekdays:Weekday[]=['MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY']
export class SimpleFormError extends Error { code:string; constructor(code:string){super(code);this.code=code} }
export function normalizePostcode(value:string){
 const compact=value.replace(/\s/g,'').toUpperCase()
 if(!/^(?:GIR0AA|[A-Z]{1,2}\d[A-Z\d]?\d[A-Z]{2})$/.test(compact))return null
 return compact.slice(0,-3)+' '+compact.slice(-3)
}
export function jobPostcodes(job:JobExtraction){
 const values=[job.workplaceLocation??'',...(job.locations??[]).map(l=>l.value)]
 const matches=values.flatMap(v=>v.toUpperCase().match(/\b(?:GIR\s*0AA|[A-Z]{1,2}\d[A-Z\d]?\s*\d[A-Z]{2})\b/g)??[])
 return [...new Set(matches.map(normalizePostcode).filter((v):v is string=>!!v))]
}
export function jobExactShifts(job:JobExtraction):SimpleShift[]{
 return (job.schedules??[]).filter(s=>s.kind==='exact-shift'&&s.start&&s.end).map((s,i)=>({label:'Shift '+(i+1),start:s.start!,end:s.end!,days:s.days??[],cycleWeek:s.cycleWeek}))
}
function validTime(value:string){return /^(?:[01]\d|2[0-3]):[0-5]\d$/.test(value)}
export interface SimpleFormState {
 origin:string;work:string;limit:string;references:ReferenceType[];
 manual:boolean;start:string;end:string;days:Weekday[];extractedShifts?:SimpleShift[];
 windowEnabled:boolean;from:string;until:string;availableDays:Weekday[];anchor:string;rotaWeeks?:number
}
export function buildSimpleInput(form:SimpleFormState):SimpleCheckInput {
 const origin=normalizePostcode(form.origin),work=normalizePostcode(form.work)
 if(!origin)throw new SimpleFormError('origin')
 if(!work)throw new SimpleFormError('work')
 const limit=Number(form.limit)
 if(!Number.isInteger(limit)||limit<1||limit>1440)throw new SimpleFormError('limit')
 const shifts:SimpleShift[]=form.manual?(()=>{
  if(!validTime(form.start)||!validTime(form.end)||form.start===form.end)throw new SimpleFormError('times')
  return [{label:'My shift',start:form.start,end:form.end,days:form.days}]
 })():form.extractedShifts??[]
 if(form.extractedShifts!==undefined&&!form.manual&&!shifts.length)throw new SimpleFormError('shifts')
 if(!shifts.length&&!form.references.length)throw new SimpleFormError('references')
 const request:SimpleCheckInput={originPostcode:origin,workPostcode:work,maxOneWayMinutes:limit,referenceTypes:[...new Set(form.references)],shifts}
 if(shifts.some(s=>s.cycleWeek)){
  const d=new Date(form.anchor+'T12:00:00Z')
  if(!/^\d{4}-\d{2}-\d{2}$/.test(form.anchor)||!Number.isFinite(d.getTime())||d.toISOString().slice(0,10)!==form.anchor||d.getUTCDay()!==1)throw new SimpleFormError('anchor')
  request.rotaStartDate=form.anchor
  request.rotaWeeks=form.rotaWeeks??Math.max(...shifts.map(s=>s.cycleWeek??1))
 }
 if(form.windowEnabled){
  if(!validTime(form.from)||!validTime(form.until)||form.from===form.until)throw new SimpleFormError('window')
  request.availability={from:form.from,until:form.until,days:form.availableDays}
 }
 return request
}
export function createRunGuard(){let generation=0;return {next:()=>++generation,isCurrent:(id:number)=>id===generation}}
