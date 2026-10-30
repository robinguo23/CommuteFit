import type { Weekday } from './api/client'
const weekdays:Weekday[]=['SUNDAY','MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY']
export function shiftAvailability(date:string,start:string,end:string,days:Weekday[],from:string,until:string):'unknown'|'fits'|'conflict' {
 if(!days.length||!from||!until)return 'unknown'
 const minutes=(v:string)=>Number(v.slice(0,2))*60+Number(v.slice(3,5))
 const day=new Date(date+'T12:00:00Z').getUTCDay()
 const s=minutes(start),e=minutes(end),a=minutes(from),b=minutes(until)
 const fits=days.includes(weekdays[day]) && (e<s ? b<=a && s>=a && e<=b && days.includes(weekdays[(day+1)%7]) : (b>a && s>=a && e<=b))
 return fits?'fits':'conflict'
}
