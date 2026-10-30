import type { ExtractedSchedule, JobExtraction, Weekday } from './api/client'

export function extractedScheduleDays(schedule: ExtractedSchedule) {
 return /(?:\d+|five|four|three)\s+days?\s+(?:from|out of|of)\s+(?:7|seven)|(?:每周|一周).*?[一二三四五六七0-7]天|[七7]天.*选|including weekends|包括周末|含周末/i.test(schedule.evidence) ? [] : schedule.days
}

export function preferredScheduleEntries(data: JobExtraction) {
 const timed=(data.schedules??[]).map((schedule,index)=>({schedule,index})).filter(({schedule:s})=>s.start&&s.end)
 const exact=timed.filter(({schedule:s})=>s.kind==='exact-shift')
 return exact.length?exact:timed.filter(({schedule:s})=>s.kind==='availability-window')
}
export function calendarDates(start:string,count:number) {
 const base=new Date(start+'T12:00:00Z')
 return Array.from({length:count},(_,i)=>new Date(base.getTime()+i*86400000).toISOString().slice(0,10))
}
export function validRotaAnchor(value:string) {
 const d=new Date(value+'T12:00:00Z')
 return /^\d{4}-\d{2}-\d{2}$/.test(value)&&!Number.isNaN(d.getTime())&&d.toISOString().slice(0,10)===value&&d.getUTCDay()===1
}
export function scheduleApplies(date:string,schedule:ExtractedSchedule,anchor:string) {
 const weekdays:Weekday[]=['SUNDAY','MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY']
 const d=new Date(date+'T12:00:00Z'),days=extractedScheduleDays(schedule)
 if(days.length&&!days.includes(weekdays[d.getUTCDay()]))return false
 if(schedule.cycleWeek){
  if(!validRotaAnchor(anchor))return false
  const offset=Math.round((d.getTime()-new Date(anchor+'T12:00:00Z').getTime())/86400000)
  if(offset<0||Math.floor(offset/7)+1!==schedule.cycleWeek)return false
 }
 return true
}

export function extractedFormFields(data: JobExtraction) {
 return {
  workplace: data.workplaceLocation ?? '',
  workDays: data.workDays ?? [],
  shiftStart: data.startTime ?? '',
  shiftEnd: data.endTime ?? '',
 }
}
