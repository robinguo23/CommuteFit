import { useEffect, useState } from 'react'
import type { AssessmentResult, Direction, JourneyOption } from '../../api/client'
import type { Language } from '../../i18n/messages'
import { messages } from '../../i18n/messages'
import FareSummary from '../fare/FareSummary'

type Props = { result: AssessmentResult; language: Language }
function formatClock(value: string | null) {
  if (!value) return '—'
  return value.slice(11, 16)
}
function formatDate(value: string, language: Language) {
  return new Intl.DateTimeFormat(language === 'zh' ? 'zh-GB' : 'en-GB', { timeZone: 'Europe/London', weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' }).format(new Date(`${value}T12:00:00Z`))
}
function formatQueriedAt(value: string, language: Language) {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return new Intl.DateTimeFormat(language === 'zh' ? 'zh-GB' : 'en-GB', { timeZone: 'Europe/London', dateStyle: 'medium', timeStyle: 'short' }).format(date)
}
function localizedMode(value: string, language: Language) {
  const key = value.toLowerCase().trim().replaceAll(' ', '-').replaceAll('_', '-')
  const en: Record<string, string> = { bus: 'Bus', tram: 'Tram', tube: 'Tube', rail: 'Rail', 'national-rail': 'National Rail', dlr: 'DLR', 'london-overground': 'London Overground', overground: 'London Overground', 'elizabeth-line': 'Elizabeth line', walking: 'Walk', walk: 'Walk', coach: 'Coach', ferry: 'Ferry' }
  const zh: Record<string, string> = { bus: '公交', tram: '电车', tube: '地铁', rail: '铁路', 'national-rail': '国家铁路', dlr: 'DLR 轻轨', 'london-overground': '伦敦地上铁', overground: '伦敦地上铁', 'elizabeth-line': '伊丽莎白线', walking: '步行', walk: '步行', coach: '长途客车', ferry: '渡船' }
  return (language === 'zh' ? zh : en)[key] ?? value.replaceAll('-', ' ')
}

function RouteCard({ option, language, shiftDate }: { option: JourneyOption; language: Language; shiftDate: string }) {
  const t = messages[language]
  const transitLegs = option.legs.filter(leg => !['walking', 'walk'].includes(leg.mode.toLowerCase()))
  const transfers = Math.max(0, transitLegs.length - 1)
  const actualJourneyDate = option.startDateTime?.slice(0, 10) ?? option.arrivalDateTime?.slice(0, 10)
  const firstLeg = option.legs[0]
  const lastLeg = option.legs[option.legs.length - 1]
  return <article className="route-card">
    <div className="route-topline"><div className="route-time"><strong>{formatClock(option.startDateTime)}</strong><span className="route-dash" /><strong>{formatClock(option.arrivalDateTime)}</strong></div>
      <div className="route-meta">{option.durationMinutes != null ? <span className="duration-chip">◷ {option.durationMinutes} {t.duration}</span> : <span className="duration-chip">{t.unknown}</span>}{option.withinLimit != null && <span className={`fit-chip ${option.withinLimit ? 'fit-good' : 'fit-over'}`}>{option.withinLimit ? t.within : t.over}</span>}</div>
    </div>
    {actualJourneyDate && actualJourneyDate !== shiftDate && <div className="route-date-note">{t.travelDate}: {formatDate(actualJourneyDate, language)}</div>}
    {firstLeg && lastLeg && <div className="resolved-endpoints"><strong>{t.resolvedRoute}</strong><span>{firstLeg.from} → {lastLeg.to || lastLeg.from}</span><small>{t.verifyWorkplace}</small></div>}
    <div className="route-steps">{option.legs.map((leg, index) => <div className="route-step" key={`${option.id}-${index}`}>
      <span className={`mode-marker mode-${leg.mode.toLowerCase().replaceAll(' ', '-')}`} aria-hidden="true">{leg.mode.toLowerCase().includes('bus') ? 'B' : leg.mode.toLowerCase().includes('walk') ? '↗' : 'M'}</span>
      <div><strong>{localizedMode(leg.mode, language)}{leg.route ? ` ${leg.route}` : ''}</strong><small>{leg.from}{leg.to ? ` → ${leg.to}` : ''}</small></div>
      <small className="leg-duration">{leg.durationMinutes != null ? `${leg.durationMinutes}m` : ''}</small>
    </div>)}</div>
    <div className="route-footnote"><span>{transfers > 1 ? `${transfers} ${t.transfers}` : `${transfers} ${t.transfer}`}</span>{option.source && <a href={option.source} target="_blank" rel="noreferrer">TfL {t.source} ↗</a>}</div>
    <FareSummary fare={option.fareSummary} language={language} />
  </article>
}

function DirectionPanel({ direction, kind, language, shiftDate }: { direction: Direction; kind: 'outbound' | 'return'; language: Language; shiftDate: string }) {
  const t = messages[language]
  const title = kind === 'outbound' ? t.outbound : t.returning
  const timeLabel = kind === 'outbound' ? t.target : t.departAfter
  return <section className={`direction-panel direction-${kind}`}>
    <div className="direction-heading"><div><span className="direction-kicker">{kind === 'outbound' ? t.outboundLabel : t.returnLabel}</span><h3>{title}</h3></div><span className="target-time"><small>{timeLabel}</small><strong>{formatClock(direction.targetTime)}</strong></span></div>
    {direction.options.length === 0 ? <div className="no-route"><span aria-hidden="true">◷</span><p>{direction.messageZh && language === 'zh' ? direction.messageZh : direction.messageEn ?? t.noRoutes}</p></div> : <div className="route-list">{direction.options.map(option => <RouteCard key={option.id} option={option} language={language} shiftDate={shiftDate} />)}</div>}
  </section>
}

export default function AssessmentResults({ result, language }: Props) {
  const t = messages[language]
  const [activeDate, setActiveDate] = useState(result.assessments[0]?.travelDate ?? '')
  useEffect(() => setActiveDate(result.assessments[0]?.travelDate ?? ''), [result])
  return <section className="results-section" aria-labelledby="results-title">
    <div className="results-heading"><div><span className="step-label">{t.resultsStep}</span><h2 id="results-title">{t.results}</h2><p>{t.providerName} · {t.dataChecked}: {formatQueriedAt(result.dataBasis.queriedAt, language)} ({result.dataBasis.timezone})</p></div><span className="results-status">{t.scheduled}</span></div>
    <div className="day-tabs" role="tablist" aria-label={t.selectTravelDate}>
      {result.assessments.map((day, index) => <button key={day.travelDate} id={`day-tab-${day.travelDate}`} type="button" role="tab" aria-selected={activeDate === day.travelDate} aria-controls={`day-panel-${day.travelDate}`} onClick={() => setActiveDate(day.travelDate)}>
        <small>{t.dayOption} {index + 1}</small><strong>{formatDate(day.travelDate, language)}</strong>
      </button>)}
    </div>
    {result.assessments.filter(day => day.travelDate === activeDate).map(day => <article className="day-results" key={day.travelDate} id={`day-panel-${day.travelDate}`} role="tabpanel" aria-labelledby={`day-tab-${day.travelDate}`}>
      <header className="day-heading"><div><span>{t.travelDate.toUpperCase()}</span><h3>{formatDate(day.travelDate, language)}</h3></div><span className="day-date">{day.travelDate}</span></header>
      <div className="directions-grid"><DirectionPanel direction={day.outbound} kind="outbound" language={language} shiftDate={day.travelDate} /><DirectionPanel direction={day.return} kind="return" language={language} shiftDate={day.travelDate} /></div>
    </article>)}
    <p className="user-decision">{t.decision}</p>
  </section>
}
