import { test } from 'node:test'
import { strict as assert } from 'node:assert'
import { buildSimpleInput, jobExactShifts, jobPostcodes, createRunGuard } from '../src/simpleCommuteForm.ts'
import type { SimpleFormState } from '../src/simpleCommuteForm.ts'
import type { JobExtraction } from '../src/api/client.ts'
const form=():SimpleFormState=>({origin:'da184de',work:'se186fl',limit:'60',references:['day'],manual:false,start:'',end:'',days:[],windowEnabled:false,from:'',until:'',availableDays:[],anchor:''})
test('two postcodes need neither availability nor exact times',()=>{
 const r=buildSimpleInput(form());assert.equal(r.originPostcode,'DA18 4DE');assert.equal(r.workPostcode,'SE18 6FL');assert.deepEqual(r.shifts,[]);assert.equal(r.availability,undefined)
})
test('only user-selected references are submitted',()=>{
 assert.deepEqual(buildSimpleInput({...form(),references:['night','early','night']}).referenceTypes,['night','early'])
 assert.throws(()=>buildSimpleInput({...form(),references:[]}),/references/)
})
test('partial or equal times do not silently become default references',()=>{
 for(const [start,end] of [['07:00',''],['','17:00'],['07:00','07:00']])assert.throws(()=>buildSimpleInput({...form(),manual:true,start,end}),/times/)
})
test('overnight manual shift is retained without inventing weekdays',()=>{
 const r=buildSimpleInput({...form(),manual:true,start:'22:00',end:'06:00',references:[]});assert.equal(r.shifts[0].end,'06:00');assert.deepEqual(r.shifts[0].days,[])
})
test('employer windows are not extracted as actual shifts',()=>{
 const job={workplaceLocation:'SE18 6FL',workDays:[],startTime:null,endTime:null,notes:[],schedules:[{kind:'availability-window',start:'06:00',end:'22:00',days:[],endsNextDay:false,evidence:'',sourcePath:''}]} as JobExtraction
 assert.deepEqual(jobExactShifts(job),[])
})
test('multiple Chinese rota rows keep days and cycle week',()=>{
 const job={schedules:[{kind:'exact-shift',start:'04:00',end:'08:00',days:['WEDNESDAY'],cycleWeek:2},{kind:'exact-shift',start:'08:00',end:'12:00',days:['SUNDAY'],cycleWeek:1}]} as JobExtraction
 const shifts=jobExactShifts(job);assert.equal(shifts.length,2);assert.equal(shifts[0].cycleWeek,2)
 assert.throws(()=>buildSimpleInput({...form(),extractedShifts:shifts}),/anchor/)
 assert.equal(buildSimpleInput({...form(),extractedShifts:shifts,anchor:'2026-11-02'}).rotaStartDate,'2026-11-02')
})
test('deselecting extracted shifts cannot query a hidden stale manual time',()=>{
 assert.throws(()=>buildSimpleInput({...form(),extractedShifts:[],start:'09:00',end:'17:00'}),/shifts/)
})
test('selecting only Week 1 preserves the original two-week rota length',()=>{
 const r=buildSimpleInput({...form(),extractedShifts:[{label:'Week 1',start:'04:00',end:'08:00',days:['MONDAY'],cycleWeek:1}],anchor:'2026-11-02',rotaWeeks:2})
 assert.equal(r.rotaWeeks,2)
})
test('postcode extraction is bounded to location evidence and does not invent a store address',()=>{
 const job={workplaceLocation:'M&S Woolwich',locations:[{value:'4 Victory Parade SE18 6FL'},{value:'SE186FL'},{value:'Other DA18 4DE'}]} as JobExtraction
 assert.deepEqual(jobPostcodes(job),['SE18 6FL','DA18 4DE']);assert.deepEqual(jobPostcodes({workplaceLocation:'Woolwich Simply Food'} as JobExtraction),[])
})
test('availability remains a window and invalid limits are rejected',()=>{
 const r=buildSimpleInput({...form(),windowEnabled:true,from:'08:00',until:'18:00'});assert.deepEqual(r.shifts,[]);assert.equal(r.availability?.from,'08:00')
 for(const limit of ['0','1441','2.5','x'])assert.throws(()=>buildSimpleInput({...form(),limit}),/limit/)
})
test('old requests stay invalid after edits A to B to A, retry or cancellation',()=>{
 const g=createRunGuard();const first=g.next();g.next();const third=g.next();assert.equal(g.isCurrent(first),false);assert.equal(g.isCurrent(third),true);g.next();assert.equal(g.isCurrent(third),false)
})
