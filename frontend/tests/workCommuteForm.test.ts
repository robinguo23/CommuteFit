import assert from 'node:assert/strict'
import test from 'node:test'
import { readSubmittedWorkSchedule } from '../src/workCommuteForm.ts'

test('reads selected weekdays and shift times from the submitted controls', () => {
  const formData = new FormData()
  formData.append('availabilityDays', 'MONDAY')
  formData.append('availabilityDays', 'FRIDAY')
  formData.append('availableFrom', '08:00')
  formData.append('availableUntil', '18:00')
  formData.append('workDays', 'MONDAY')
  formData.append('shiftStart', '09:00')
  formData.append('shiftEnd', '17:00')

  assert.deepEqual(readSubmittedWorkSchedule(formData), {
    availabilityDays: ['MONDAY', 'FRIDAY'],
    availableFrom: '08:00',
    availableUntil: '18:00',
    workDays: ['MONDAY'],
    shiftStart: '09:00',
    shiftEnd: '17:00',
  })
})

test('does not invent selected work days or times when the controls are empty', () => {
  assert.deepEqual(readSubmittedWorkSchedule(new FormData()), {
    availabilityDays: [],
    availableFrom: '',
    availableUntil: '',
    workDays: [],
    shiftStart: '',
    shiftEnd: '',
  })
})

test('ignores non-weekday checkbox values', () => {
  const formData = new FormData()
  formData.append('workDays', 'MONDAY')
  formData.append('workDays', 'on')
  formData.append('workDays', 'NOT_A_DAY')

  assert.deepEqual(readSubmittedWorkSchedule(formData).workDays, ['MONDAY'])
})
