import assert from 'node:assert/strict'
import test from 'node:test'
import { scheduledSearchArguments, swapJourneySelection } from '../src/features/journey/scheduledSearch.ts'

const selectedCandidates = {
  origin: 'abbey-wood-selection-token',
  destination: 'kings-cross-selection-token',
}

test('switching from Now to Leave keeps both selected TfL locations', () => {
  assert.deepEqual(
    scheduledSearchArguments(selectedCandidates, 'leave', '2026-10-28', '09:00'),
    [selectedCandidates, { mode: 'leave', date: '2026-10-28', time: '09:00' }],
  )
})

test('changing the date keeps both selected TfL locations', () => {
  assert.deepEqual(
    scheduledSearchArguments(selectedCandidates, 'leave', '2026-10-29', '09:00'),
    [selectedCandidates, { mode: 'leave', date: '2026-10-29', time: '09:00' }],
  )
})

test('changing the time keeps both selected TfL locations', () => {
  assert.deepEqual(
    scheduledSearchArguments(selectedCandidates, 'leave', '2026-10-28', '09:15'),
    [selectedCandidates, { mode: 'leave', date: '2026-10-28', time: '09:15' }],
  )
})

test('switching to Arrive keeps both selected TfL locations and the requested time', () => {
  assert.deepEqual(
    scheduledSearchArguments(selectedCandidates, 'arrive', '2026-10-28', '09:15'),
    [selectedCandidates, { mode: 'arrive', date: '2026-10-28', time: '09:15' }],
  )
})

test('switching back to Now keeps selected locations for the immediate re-query', () => {
  assert.deepEqual(
    scheduledSearchArguments(selectedCandidates, 'now', '2026-10-28', '09:15'),
    [selectedCandidates, { mode: 'now', date: '2026-10-28', time: '09:15' }],
  )
})

test('changing date and time together preserves selections and both requested values', () => {
  assert.deepEqual(
    scheduledSearchArguments(selectedCandidates, 'leave', '2026-10-29', '17:45'),
    [selectedCandidates, { mode: 'leave', date: '2026-10-29', time: '17:45' }],
  )
})

test('swapping an A-to-B journey moves each selected token with its location', () => {
  const swapped = swapJourneySelection('Abbey Wood', "King's Cross & St Pancras International", selectedCandidates)
  assert.deepEqual(swapped, {
    origin: "King's Cross & St Pancras International",
    destination: 'Abbey Wood',
    tokens: { origin: selectedCandidates.destination, destination: selectedCandidates.origin },
  })
})

test('swapping twice restores the original journey and candidate tokens', () => {
  const once = swapJourneySelection('Abbey Wood', "King's Cross & St Pancras International", selectedCandidates)
  assert.deepEqual(
    swapJourneySelection(once.origin, once.destination, once.tokens),
    { origin: 'Abbey Wood', destination: "King's Cross & St Pancras International", tokens: selectedCandidates },
  )
})

test('swapping a partly selected journey keeps the token attached to the selected location', () => {
  const swapped = swapJourneySelection('Abbey Wood', 'DA18 4DE', { origin: selectedCandidates.origin })
  assert.deepEqual(swapped, {
    origin: 'DA18 4DE',
    destination: 'Abbey Wood',
    tokens: { origin: undefined, destination: selectedCandidates.origin },
  })
})
