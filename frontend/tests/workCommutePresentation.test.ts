import assert from 'node:assert/strict'
import test from 'node:test'
import { formatWorkdayDate } from '../src/workCommutePresentation.ts'

test('formats work dates with a weekday in English', () => {
  const label = formatWorkdayDate('2026-11-02', 'en')
  assert.match(label, /Monday/)
  assert.match(label, /2026/)
})

test('formats work dates with a weekday in Chinese', () => {
  const label = formatWorkdayDate('2026-11-02', 'zh')
  assert.match(label, /星期一/)
  assert.match(label, /2026/)
})
