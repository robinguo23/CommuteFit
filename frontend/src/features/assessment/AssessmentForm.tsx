import { useState } from 'react'
import type { FormEvent } from 'react'
import type { AssessmentInput, Candidate, PaymentMethod, Profile } from '../../api/client'
import type { CandidateField } from './useAssessment'
import type { Language } from '../../i18n/messages'
import { messages } from '../../i18n/messages'

type Props = {
  profile: Profile; language: Language; loading: boolean; candidates: Candidate[]; candidateField: CandidateField | null; error: string | null;
  onSubmit: (input: AssessmentInput) => Promise<void>; onChoose: (candidate: Candidate, input: AssessmentInput) => Promise<void>; onDestinationChange: () => void; onInputChange: () => void; onRetry: (input: AssessmentInput) => Promise<void>
}
function localizedPlaceType(value: string | undefined, language: Language) {
  if (!value) return ''
  if (language === 'en') return value
  const known: Record<string, string> = { stoppoint: '交通站点', address: '地址', postcode: '邮编区域', postcodelocation: '邮编区域', place: '地点', street: '街道', road: '道路', locality: '地区' }
  return known[value.toLowerCase().replaceAll(/[^a-z]/g, '')] ?? value
}

export default function AssessmentForm({ profile, language, loading, candidates, candidateField, error, onSubmit, onChoose, onDestinationChange, onInputChange, onRetry }: Props) {
  const t = messages[language]
  const today = new Intl.DateTimeFormat('en-CA', { timeZone: 'Europe/London' }).format(new Date())
  const [destination, setDestination] = useState('')
  const [date, setDate] = useState(today)
  const [earliest, setEarliest] = useState('')
  const [latest, setLatest] = useState('')
  const [weekend, setWeekend] = useState(false)
  const [buffer, setBuffer] = useState(profile.arrivalBufferMinutes ?? 30)
  const [paymentChoice, setPaymentChoice] = useState<'profile' | 'none' | PaymentMethod>('profile')

  function changeDestination(value: string) { setDestination(value); onDestinationChange() }
  function buildInput(): AssessmentInput {
    return {
      destinationText: destination.trim(), referenceDate: date, earliestShiftStart: earliest, expectedLatestFinish: latest,
      checkWeekend: weekend, arrivalBufferMinutes: buffer, maxOneWayMinutes: profile.maxOneWayMinutes,
      paymentMethod: paymentChoice === 'profile' ? profile.preferredPaymentMethod : paymentChoice === 'none' ? null : paymentChoice,
      concessions: profile.selectedConcessions,
    }
  }
  async function submit(event: FormEvent) {
    event.preventDefault()
    await onSubmit(buildInput())
  }

  return <section className="panel assessment-panel" aria-labelledby="job-title">
    <div className="panel-heading"><div><span className="step-label">{t.jobStep}</span><h2 id="job-title">{t.job}</h2><p>{t.jobHelp}</p></div><span className="panel-icon panel-icon-orange" aria-hidden="true">↗</span></div>
    {!profile.configured && <div className="inline-note">{t.noProfile}</div>}
    <form className="form-stack" onSubmit={submit}>
      <label className="field">{t.destination}
        <input value={destination} onChange={event => changeDestination(event.target.value)} placeholder={t.destinationPlaceholder} required maxLength={180} />
        <small>{t.locationHint}</small>
      </label>
      <div className="field-grid date-grid">
        <label className="field">{t.date}<input type="date" value={date} onChange={event => { setDate(event.target.value); onInputChange() }} required /></label>
        <label className="field">{t.buffer}<div className="input-suffix"><input type="number" min="0" max="240" value={buffer} onChange={event => { setBuffer(Number(event.target.value)); onInputChange() }} required /><span>{t.minutes}</span></div></label>
      </div>
      <div className="field-grid time-grid">
        <label className="field">{t.earliest}<input type="time" value={earliest} onChange={event => { setEarliest(event.target.value); onInputChange() }} required /></label>
        <label className="field">{t.latest}<input type="time" value={latest} onChange={event => { setLatest(event.target.value); onInputChange() }} required /></label>
      </div>
      <div className="field-grid last-grid">
        <label className="field">{t.payment}
          <select value={paymentChoice ?? ''} onChange={event => { setPaymentChoice((event.target.value || 'none') as 'profile' | 'none' | PaymentMethod); onInputChange() }}>
            <option value="profile">{t.useProfile}</option><option value="none">{t.noPreference}</option><option value="apple-pay-contactless">{t.applePay}</option><option value="bank-card-contactless">{t.bankCard}</option><option value="oyster">{t.oyster}</option><option value="other">{t.other}</option>
          </select>
        </label>
        <label className="check-option weekend-option"><input type="checkbox" checked={weekend} onChange={event => { setWeekend(event.target.checked); onInputChange() }} /><span>{t.weekend}</span></label>
      </div>
      <div className="privacy-note"><span aria-hidden="true">ⓘ</span><p>{t.privacy}</p></div>
      <button className="button button-orange button-full" type="submit" disabled={loading || !profile.configured}>{loading ? <><span className="spinner" />{t.sending}</> : <>{t.run}<span aria-hidden="true">→</span></>}</button>
      {error && <div className="error-box" role="alert"><span>{error}</span><button className="button button-small" type="button" onClick={() => void onRetry(buildInput())} disabled={loading}>{t.retry}</button></div>}
    </form>
    {candidates.length > 0 && <div className="candidate-box"><h3>{candidateField === 'origin' ? t.chooseOrigin : t.choose}</h3>{candidates.map(candidate => <button type="button" key={candidate.id} className="candidate-choice" onClick={() => void onChoose(candidate, buildInput())}><span><strong>{candidate.name}</strong><small>{localizedPlaceType(candidate.locality, language)}</small></span><span>{t.useChoice} →</span></button>)}</div>}
  </section>
}
