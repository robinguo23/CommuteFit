export type PaymentMethod = 'apple-pay-contactless' | 'bank-card-contactless' | 'oyster' | 'other' | null

export interface Profile {
  configured: boolean
  departurePostcode: string
  maxOneWayMinutes: number | null
  arrivalBufferMinutes: number
  selectedConcessions: string[]
  preferredPaymentMethod: PaymentMethod
  otherPaymentMethodLabel: string | null
  updatedAt: string | null
}

export interface ProfileInput extends Omit<Profile, 'configured' | 'updatedAt'> {}
export interface AssessmentInput {
  destinationText: string
  referenceDate: string
  earliestShiftStart: string
  expectedLatestFinish: string
  checkWeekend: boolean
  arrivalBufferMinutes: number
  maxOneWayMinutes: number | null
  paymentMethod: PaymentMethod
  concessions: string[]
  selectedCandidateId?: string
  selectedOriginCandidateId?: string
}

export interface Candidate { id: string; name: string; locality?: string }
export interface FareTap { modeType: string; timestamp: string | null; routeId: string | null; locationCode: string | null }
export interface FareItem {
  sequence: number; amount: number | null; currency: string; chargeLevel: string | null; lowZone: number | null; highZone: number | null; hopperFare: boolean | null; modes: string[]; taps: FareTap[]
}
export interface ConcessionScenario {
  policyId: string; titleEn: string; titleZh: string; eligibilityStatus: 'eligible' | 'ineligible' | 'unknown'; timeBasis: string; qualifyingEvent: string; eligibilityRule: string; fareScope: string; fareItemSequences: number[]; amountFareItemSequences: number[]; paymentAssumption: string; eligibilityAssumption: string; amount: { status: string; value: number | null; currency: string; basis: string | null }; effectiveDate: string | null; source: string; selectedByUser: boolean
}
export interface FareSummary {
  routeFare: { status: string; amount: number | null; currency: string; paymentBasis: string; paymentBasisZh: string; scope: string; scopeZh: string; travelDate: string; source: string }
  fareItems: FareItem[]; conditionalConcessionScenarios: ConcessionScenario[]; capGuidance: { status: string; adultDailyCap: number | null; adultWeeklyCap: number | null; conditionalDiscountDailyCap: number | null; currency: string; effectiveFrom: string | null; messageEn: string; messageZh: string; source: string }
}
export interface JourneyLeg { mode: string; route: string; from: string; to: string; departureDateTime: string | null; arrivalDateTime: string | null; durationMinutes: number | null; summary: string; stops?: number | null }
export interface JourneyOption { id: string; startDateTime: string | null; arrivalDateTime: string | null; durationMinutes: number | null; withinLimit: boolean | null; legs: JourneyLeg[]; fareSummary: FareSummary; source: string }
export interface Direction { targetTime: string; options: JourneyOption[]; status: string; messageEn: string | null; messageZh: string | null }
export interface DayAssessment { travelDate: string; dayLabel: string; outbound: Direction; return: Direction }
export interface AssessmentResult { status: string; assessments: DayAssessment[]; dataBasis: { provider: string; queriedAt: string; timezone: string }; limitations: string[] }
export interface ApiFailurePayload { code?: string; messageEn?: string; messageZh?: string; fields?: Record<string, string>; candidates?: Candidate[]; candidateField?: 'origin' | 'destination' }

export class ApiFailure extends Error {
  constructor(public readonly status: number, public readonly payload: ApiFailurePayload) { super(payload.messageEn ?? 'Request failed') }
}

async function request<T>(url: string, init?: RequestInit): Promise<T> {
  const controller = new AbortController()
  const timeout = window.setTimeout(() => controller.abort(), 25_000)
  try {
    const response = await fetch(url, { ...init, signal: controller.signal, headers: { 'Content-Type': 'application/json', ...init?.headers } })
    const body = await response.json().catch(() => ({}))
    if (!response.ok) {
      const payload = body as ApiFailurePayload
      if (!payload.code) payload.code = response.status === 404 ? 'api_not_found' : response.status >= 500 ? 'backend_unavailable' : 'app_error'
      throw new ApiFailure(response.status, payload)
    }
    return body as T
  }
  catch (error) {
    if (error instanceof ApiFailure) throw error
    if (error instanceof DOMException && error.name === 'AbortError') throw new ApiFailure(0, { code: 'request_timeout' })
    throw new ApiFailure(0, { code: 'network_error' })
  } finally { window.clearTimeout(timeout) }
}

export const api = {
  getProfile: () => request<Profile>('/api/profile'),
  saveProfile: (profile: ProfileInput) => request<Profile>('/api/profile', { method: 'PUT', body: JSON.stringify(profile) }),
  clearProfile: () => request<Profile>('/api/profile', { method: 'DELETE' }),
  assess: (input: AssessmentInput) => request<AssessmentResult>('/api/assessment', { method: 'POST', body: JSON.stringify(input) }),
}

export interface JourneyLookupInput {
  originPostcode: string
  destinationPostcode: string
  timeMode?: 'now' | 'leave' | 'arrive'
  travelDate?: string
  travelTime?: string
  selectedOriginCandidateId?: string
  selectedDestinationCandidateId?: string
}
export interface JourneyLookupResult {
  status: string
  originPostcode: string
  destinationPostcode: string
  queriedAt: string
  timezone: string
  timeMode: 'now' | 'leave' | 'arrive'
  requestedDateTime: string
  routes: { id: string; departure: string; arrival: string; durationMinutes: number | null; transfers: number; legs: JourneyLeg[]; totalFare: number | null; fareStatus: string; fareChargeLevels?: string[]; currency: string }[]
}
export const lookupJourney = (input: JourneyLookupInput) => request<JourneyLookupResult>('/api/journeys', { method: 'POST', body: JSON.stringify(input) })
export interface LocationSuggestion { id: string; name: string; locality?: string }
export const searchLocations = (query: string) => request<LocationSuggestion[]>(`/api/locations?query=${encodeURIComponent(query)}`)
export interface PlaceCandidate { id:string; name:string; address:string; postcode:string|null; lat:number|null; lon:number|null; precision:string; source:string; sourceId:string|null; selectable:boolean }
export interface PlaceSearchResult { status:'matches'|'no-matches'|'unavailable'; candidates:PlaceCandidate[]; message:string; attribution:string }
export const searchPlaces = (query:string) => request<PlaceSearchResult>(`/api/place-search?query=${encodeURIComponent(query)}`)
export type PlaceSearchProvider='combined'|'direct'|'photon'|'geoapify'|'maptiler'|'locationiq'|'opencage'|'tomtom'|'tomtom-search'|'postcodes-io'|'arcgis'|'foursquare'|'tfl-stops'|'tfl-places'
export const searchPlacesByProvider=(provider:Exclude<PlaceSearchProvider,'combined'>,query:string)=>request<PlaceSearchResult>(`/api/place-search/provider/${provider}?query=${encodeURIComponent(query)}`)

export type Weekday = 'MONDAY' | 'TUESDAY' | 'WEDNESDAY' | 'THURSDAY' | 'FRIDAY' | 'SATURDAY' | 'SUNDAY'
export interface JobExtractionInput { sourceType: 'url' | 'description' | 'work-postcode'; content: string }
export interface ExtractedLocation { value: string; evidence: string; sourcePath: string }
export interface ExtractedSchedule { kind: string; days: Weekday[]; start: string | null; end: string | null; endsNextDay: boolean; evidence: string; sourcePath: string; cycleWeek?: number|null }
export interface JobExtraction { workplaceLocation: string | null; workDays: Weekday[]; startTime: string | null; endTime: string | null; notes: string[]; status?: 'ready'|'partial'|'unavailable'; title?: string|null; jobStatus?: string; locations?: ExtractedLocation[]; schedules?: ExtractedSchedule[]; warnings?: string[]; missing?: string[]; source?: {method:string;url:string|null;fetchedAt:string} }
export interface WorkCommuteInput {
  originLocation: string; selectedOriginCandidateId?: string; maxOneWayMinutes: number
  availabilityDays: Weekday[]; availableFrom: string; availableUntil: string
  workplaceLocation: string; selectedWorkplaceCandidateId?: string; workDays: Weekday[]; shiftStart: string; shiftEnd: string
}
export interface WorkRoute { id: string; departure: string | null; arrival: string | null; durationMinutes: number | null; transfers: number; legs: JourneyLeg[] }
export interface WorkDirection { status: 'within-limit' | 'over-limit' | 'unknown'; targetTime: string | null; bestDurationMinutes: number | null; routes: WorkRoute[]; reason: string }
export interface WorkDayResult { date: string; day: Weekday; outbound: WorkDirection; returning: WorkDirection }
export interface WorkCommuteResult { status: 'suitable' | 'unsuitable' | 'needs-review'; scheduleStatus: 'compatible' | 'incompatible' | 'unknown'; scheduleReason: string; maxOneWayMinutes: number; days: WorkDayResult[]; reasons: string[]; timezone: string }
export const extractJobDetails = (input: JobExtractionInput) => request<JobExtraction>('/api/job-extractions', { method: 'POST', body: JSON.stringify(input) })
export const assessWorkCommute = (input: WorkCommuteInput) => request<WorkCommuteResult>('/api/work-commute/assess', { method: 'POST', body: JSON.stringify(input) })
export const assessScenario = (journey: WorkCommuteInput, date: string) => request<WorkDayResult>('/api/work-commute/scenario', { method: 'POST', body: JSON.stringify({ journey, date }) })

export type ReferenceType = 'early' | 'day' | 'night'
export interface SimpleShift { label:string; start:string; end:string; days:Weekday[]; cycleWeek?:number|null }
export interface SimpleWindow { from:string; until:string; days:Weekday[] }
export interface SimpleCheckInput { originPostcode:string; workPostcode:string; maxOneWayMinutes:number; referenceTypes:ReferenceType[]; shifts:SimpleShift[]; availability?:SimpleWindow; rotaStartDate?:string; rotaWeeks?:number }
export interface SimpleRoute { departure:string; arrival:string; durationMinutes:number; transfers:number; legs:JourneyLeg[] }
export interface SimpleDirection { status:'within-limit'|'over-limit'|'no-route'|'unknown'; reason:string; targetTime?:string; route?:SimpleRoute; attempts:{targetTime:string;status:string;reason:string}[] }
export interface SimpleCheck { type:string; label:string; status:'suitable'|'unsuitable'|'needs-review'; availability:string; outbound:SimpleDirection; returning:SimpleDirection }
export interface SimpleCheckResult {
 status:'suitable'|'unsuitable'|'needs-review'; mode:'reference'|'exact'; originPostcode:string; workPostcode:string; maxOneWayMinutes:number;
 scopeStart:string; scopeEnd:string; days:{date:string;checks:SimpleCheck[]}[];
 positiveScopes:{date:string;type:string;label:string}[]; issues:{date:string;type:string;label:string;direction:string;status:string;reason:string}[];
 queriedPoints:number; reusedPoints:number; timezone:string; queriedAt:string
}
export interface CommuteCheckJob { id:string; status:'running'|'complete'|'cancelled'|'failed'; completed:number; total:number; result?:SimpleCheckResult; error?:ApiFailurePayload }
export const startCommuteCheck=(input:SimpleCheckInput)=>request<CommuteCheckJob>('/api/commute-checks',{method:'POST',body:JSON.stringify(input)})
export const pollCommuteCheck=(id:string)=>request<CommuteCheckJob>(`/api/commute-checks/${encodeURIComponent(id)}`)
export const cancelCommuteCheck=(id:string)=>request<void>(`/api/commute-checks/${encodeURIComponent(id)}`,{method:'DELETE'})


export interface AreaConsensus {
 status:'supported'|'text-match'|'ambiguous'|'needs-confirmation'; selectedCandidate?:PlaceCandidate|null; maxDistanceMeters:number; message:string;
 providers:{provider:string;result:PlaceSearchResult}[];
 areas:{support:number;sources:string[];members:PlaceCandidate[];representative:PlaceCandidate;maxDistanceMeters:number}[];
 excluded:{candidate:PlaceCandidate;reason:string}[]; corroborated:{candidate:PlaceCandidate;reason:string}[];
}
export const searchAreaConsensus=(query:string)=>request<AreaConsensus>(`/api/place-search/consensus?query=${encodeURIComponent(query)}`)
