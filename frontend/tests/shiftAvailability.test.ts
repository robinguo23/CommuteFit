import { test } from 'node:test'
import { strict as assert } from 'node:assert'
import { shiftAvailability } from '../src/shiftAvailability.ts'

test('missing availability stays unknown',()=>assert.equal(shiftAvailability('2026-10-28','09:00','17:00',[],'',''),'unknown'))
test('full shift must fit available working hours',()=>{
 assert.equal(shiftAvailability('2026-10-28','09:00','17:00',['WEDNESDAY'],'08:00','18:00'),'fits')
 assert.equal(shiftAvailability('2026-10-28','07:00','15:00',['WEDNESDAY'],'08:00','18:00'),'conflict')
})
test('overnight shift requires availability on both days',()=>{
 assert.equal(shiftAvailability('2026-10-28','22:00','06:00',['WEDNESDAY'],'21:00','07:00'),'conflict')
 assert.equal(shiftAvailability('2026-10-28','22:00','06:00',['WEDNESDAY','THURSDAY'],'21:00','07:00'),'fits')
})
