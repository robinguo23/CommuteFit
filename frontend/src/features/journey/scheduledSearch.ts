export type CandidateTokens = { origin?: string; destination?: string }
export type ScheduledJourneyTime = { mode: 'now' | 'leave' | 'arrive'; date: string; time: string }
export type JourneySelection = { origin: string; destination: string; tokens: CandidateTokens }

export function scheduledSearchArguments(
  tokens: CandidateTokens,
  mode: ScheduledJourneyTime['mode'],
  date: string,
  time: string,
): [CandidateTokens, ScheduledJourneyTime] {
  return [tokens, { mode, date, time }]
}

export function swapJourneySelection(
  origin: string,
  destination: string,
  tokens: CandidateTokens,
): JourneySelection {
  return {
    origin: destination,
    destination: origin,
    tokens: { origin: tokens.destination, destination: tokens.origin },
  }
}
