import { test } from 'node:test'
import { strict as assert } from 'node:assert'
import { extractedFormFields, extractedScheduleDays } from '../src/extractionForm.ts'
test('five days from seven is not a fixed weekend schedule',()=>{
 assert.deepEqual(extractedScheduleDays({kind:'exact-shift',days:['SATURDAY','SUNDAY'],start:'06:00',end:'14:00',endsNextDay:false,evidence:'5 days from 7 (including weekends)',sourcePath:'description'}),[])
})
test('partial extraction clears prior job fields rather than retaining them',()=>{
 assert.deepEqual(extractedFormFields({workplaceLocation:null,workDays:[],startTime:null,endTime:null,notes:[]}),{workplace:'',workDays:[],shiftStart:'',shiftEnd:''})
})
test('window evidence does not populate an actual shift',()=>{
 const fields=extractedFormFields({workplaceLocation:'E1 6AN',workDays:[],startTime:null,endTime:null,notes:[],schedules:[{kind:'availability-window',days:['MONDAY'],start:'06:00',end:'22:00',endsNextDay:false,evidence:'Mon 06-22',sourcePath:'description'}]})
 assert.equal(fields.workplace,'E1 6AN')
 assert.equal(fields.shiftStart,'')
 assert.equal(fields.shiftEnd,'')
})
test('single confirmed-format suggestion preserves its associated days',()=>{
 assert.deepEqual(extractedFormFields({workplaceLocation:'E1 6AN',workDays:['WEDNESDAY'],startTime:'22:00',endTime:'06:00',notes:[]}),{workplace:'E1 6AN',workDays:['WEDNESDAY'],shiftStart:'22:00',shiftEnd:'06:00'})
})

test('actual shifts take priority over windows',async()=>{
 const { preferredScheduleEntries }=await import('../src/extractionForm.ts')
 const s={days:[],start:'06:00',end:'14:00',endsNextDay:false,evidence:'shift',sourcePath:'description'}
 const d={workplaceLocation:null,workDays:[],startTime:null,endTime:null,notes:[],schedules:[{...s,kind:'availability-window'},{...s,kind:'exact-shift'},{...s,kind:'exact-shift',start:'14:00',end:'22:00'}]}
 assert.deepEqual(preferredScheduleEntries(d).map(e=>e.index),[1,2])
})
test('two unspecified shifts produce fourteen daily scenarios and twenty-eight directions',async()=>{
 const { calendarDates,scheduleApplies }=await import('../src/extractionForm.ts')
 const dates=calendarDates('2026-11-02',7)
 const schedules=[{kind:'exact-shift',days:[],start:'06:00',end:'14:00',endsNextDay:false,evidence:'5 days from 7 including weekends',sourcePath:'description'},{kind:'exact-shift',days:[],start:'14:00',end:'22:00',endsNextDay:false,evidence:'5 days from 7 including weekends',sourcePath:'description'}]
 const jobs=dates.flatMap(date=>schedules.filter(s=>scheduleApplies(date,s,'')))
 assert.equal(jobs.length,14);assert.equal(jobs.length*2,28)
})
test('six fortnight rota rows retain their exact dates and do not expand to daily shifts',async()=>{
 const { calendarDates,scheduleApplies,validRotaAnchor }=await import('../src/extractionForm.ts')
 const base={kind:'exact-shift',start:'04:00',end:'10:00',endsNextDay:false,evidence:'',sourcePath:'description'}
 const schedules=[{...base,days:['SUNDAY'],cycleWeek:1},{...base,days:['MONDAY'],cycleWeek:1},{...base,days:['SATURDAY'],cycleWeek:1},{...base,days:['MONDAY'],cycleWeek:2},{...base,days:['WEDNESDAY'],cycleWeek:2},{...base,days:['FRIDAY'],cycleWeek:2}] as import('../src/api/client').ExtractedSchedule[]
 const matches=calendarDates('2026-11-02',14).flatMap(date=>schedules.filter(s=>scheduleApplies(date,s,'2026-11-02')).map(()=>date))
 assert.deepEqual(matches,['2026-11-02','2026-11-07','2026-11-08','2026-11-09','2026-11-11','2026-11-13'])
 assert.equal(validRotaAnchor('2026-11-03'),false)
 assert.equal(scheduleApplies('2026-11-02',schedules[1],''),false)
})
