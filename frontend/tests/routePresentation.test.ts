import assert from 'node:assert/strict'
import test from 'node:test'
import { isWalkingOnlyRoute } from '../src/features/journey/routePresentation.ts'

test('a route with only walking legs does not need a fare', () => {
  assert.equal(isWalkingOnlyRoute(['walking']), true)
  assert.equal(isWalkingOnlyRoute([' Walk ', 'WALKING']), true)
  assert.equal(new Intl.NumberFormat('en-GB', { style: 'currency', currency: 'GBP' }).format(0), '£0.00')
})

test('transit and mixed routes still need fare presentation', () => {
  assert.equal(isWalkingOnlyRoute(['bus']), false)
  assert.equal(isWalkingOnlyRoute(['walking', 'tube']), false)
  assert.equal(isWalkingOnlyRoute([]), false)
})
