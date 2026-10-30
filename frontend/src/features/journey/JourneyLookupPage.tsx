import { useEffect, useRef, useState } from 'react'
import type { FormEvent } from 'react'
import { ApiFailure, lookupJourney, searchLocations } from '../../api/client'
import type { Candidate, JourneyLookupResult } from '../../api/client'
import { scheduledSearchArguments, swapJourneySelection } from './scheduledSearch'
import { isWalkingOnlyRoute } from './routePresentation'
import '../../journey.css'

type Language = 'zh' | 'en'
const names: Record<string, string> = { bus: '公交', 'public-bus': '公交', tube: '地铁', walking: '步行', walk: '步行', tram: '电车', dlr: 'DLR 轻轨', 'national-rail': '国家铁路', rail: '铁路', train: '铁路', overground: '伦敦地上铁', 'london-overground': '伦敦地上铁', 'elizabeth-line': '伊丽莎白线', 'river-bus': '河上巴士', 'cable-car': '缆车' }
const englishNames: Record<string, string> = { bus: 'Bus', 'public-bus': 'Bus', tube: 'Tube', walking: 'Walk', walk: 'Walk', tram: 'Tram', dlr: 'DLR', 'national-rail': 'National Rail', rail: 'Rail', train: 'Rail', overground: 'London Overground', 'london-overground': 'London Overground', 'elizabeth-line': 'Elizabeth line', 'river-bus': 'River Bus', 'cable-car': 'Cable Car' }
const money = (value: number) => new Intl.NumberFormat('en-GB', { style: 'currency', currency: 'GBP' }).format(value)
function clock(value: string | null) { return value ? value.slice(11, 16) : '—' }
function candidateType(value: string | undefined, language: Language) {
  const type = value?.replaceAll(/([a-z])([A-Z])/g, '$1 $2').toLowerCase().replaceAll(' ', '') ?? ''
  const en: Record<string, string> = { stoppoint: 'Station or stop', address: 'Address', postcode: 'Postcode area', postcodelocation: 'Postcode area', street: 'Street', road: 'Road', place: 'Place', locality: 'Area' }
  const zhTypes: Record<string, string> = { stoppoint: '车站或站点', address: '地址', postcode: '邮编区域', postcodelocation: '邮编区域', street: '街道', road: '道路', place: '地点', locality: '地区' }
  return (language === 'zh' ? zhTypes : en)[type] ?? (value || (language === 'zh' ? '匹配地点' : 'Matching place'))
}
function isPostcodeLike(value: string) { return /^(?:GIR|[A-Z]{1,2}\d)/i.test(value.trim()) }
function londonDateNow() { return new Intl.DateTimeFormat('en-CA', { timeZone: 'Europe/London', year: 'numeric', month: '2-digit', day: '2-digit' }).format(new Date()) }
function requestedTime(value: string, language: Language) {
  const [date, time] = value.split('T')
  if (!date || !time) return value
  const [year, month, day] = date.split('-').map(Number)
  const [hour, minute] = time.slice(0, 5).split(':').map(Number)
  return new Intl.DateTimeFormat(language === 'zh' ? 'zh-CN' : 'en-GB', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(year, month - 1, day, hour, minute))
}
function weekdayDate(value: string, language: Language) {
  const [year, month, day] = value.split('-').map(Number)
  const date = new Date(year, month - 1, day)
  const locale = language === 'zh' ? 'zh-CN' : 'en-GB'
  const monthDay = new Intl.DateTimeFormat(locale, { month: 'long', day: 'numeric' }).format(date)
  return `${monthDay} · ${new Intl.DateTimeFormat(locale, { weekday: 'long' }).format(date)}`
}
function chargeLevelLabel(level: string, language: Language) {
  const normalized = level.trim().toLowerCase().replaceAll('-', ' ')
  if (normalized === 'peak') return language === 'zh' ? '高峰' : 'Peak'
  if (normalized === 'off peak') return language === 'zh' ? '非高峰' : 'Off peak'
  return level
}
function routeLabel(mode: string, route: string, language: Language) {
  const normalizedMode = mode.toLowerCase().replaceAll(/[^a-z0-9]/g, '')
  const normalizedRoute = route.toLowerCase().replaceAll(/[^a-z0-9]/g, '')
  const modeLabel = (language === 'zh' ? names : englishNames)[mode.toLowerCase().replaceAll(' ', '-')] ?? mode
  const normalizedModeLabel = modeLabel.toLowerCase().replaceAll(/[^a-z0-9]/g, '')
  const routeSuffix = route && normalizedRoute !== normalizedMode && normalizedRoute !== normalizedModeLabel ? ` · ${route}` : ''
  return `${modeLabel}${routeSuffix}`
}
function monthDate(value: string) { const [year, month] = value.split('-').map(Number); return new Date(year, month - 1, 1) }
function dateKey(value: Date) { return `${value.getFullYear()}-${String(value.getMonth() + 1).padStart(2, '0')}-${String(value.getDate()).padStart(2, '0')}` }

export default function App() {
  const [language, setLanguage] = useState<Language>('zh')
  const zh = language === 'zh'
  const [origin, setOrigin] = useState('')
  const [destination, setDestination] = useState('')
  const [timeMode, setTimeMode] = useState<'now' | 'leave' | 'arrive'>('now')
  const [travelDate, setTravelDate] = useState(londonDateNow)
  const [travelTime, setTravelTime] = useState('09:00')
  const [pickerOpen, setPickerOpen] = useState(false)
  const [timeMenuOpen, setTimeMenuOpen] = useState(false)
  const [pickerMonth, setPickerMonth] = useState(() => monthDate(londonDateNow()))
  const [result, setResult] = useState<JourneyLookupResult | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<{ code?: string; fields?: Record<string, string> } | null>(null)
  const [candidates, setCandidates] = useState<Candidate[]>([])
  const [candidateField, setCandidateField] = useState<'origin' | 'destination'>('destination')
  const [suggestions, setSuggestions] = useState<Candidate[]>([])
  const [activeSuggestionField, setActiveSuggestionField] = useState<'origin' | 'destination' | null>(null)
  const [suggestionsLoading, setSuggestionsLoading] = useState(false)
  const [suggestionsLoaded, setSuggestionsLoaded] = useState(false)
  const [suggestionsError, setSuggestionsError] = useState(false)
  const [tokens, setTokens] = useState<{ origin?: string; destination?: string }>({})
  const requestVersion = useRef(0)
  const suggestionVersion = useRef(0)
  const autoSearchTimer = useRef<number | null>(null)
  const dateTimeRowRef = useRef<HTMLDivElement>(null)
  const timeControlRef = useRef<HTMLDivElement>(null)
  const timeMenuRef = useRef<HTMLDivElement>(null)
  const selectedTimeRef = useRef<HTMLButtonElement>(null)
  const calendarStart = new Date(pickerMonth.getFullYear(), pickerMonth.getMonth(), 1 - ((pickerMonth.getDay() + 6) % 7))
  const calendarDays = Array.from({ length: 42 }, (_, index) => new Date(calendarStart.getFullYear(), calendarStart.getMonth(), calendarStart.getDate() + index))
  const monthLabel = new Intl.DateTimeFormat(zh ? 'zh-CN' : 'en-GB', { month: 'long', year: 'numeric' }).format(pickerMonth)
  const weekdays = Array.from({ length: 7 }, (_, index) => new Intl.DateTimeFormat(zh ? 'zh-CN' : 'en-GB', { weekday: 'short' }).format(new Date(2024, 0, 1 + index)))
  const timeChoices = Array.from({ length: 96 }, (_, index) => `${String(Math.floor(index * 15 / 60)).padStart(2, '0')}:${String(index * 15 % 60).padStart(2, '0')}`)

  useEffect(() => {
    if (!pickerOpen && !timeMenuOpen && !activeSuggestionField) return
    function handlePointerDown(event: PointerEvent) {
      const target = event.target as Node
      if (!dateTimeRowRef.current?.contains(target)) setPickerOpen(false)
      if (!timeControlRef.current?.contains(target)) setTimeMenuOpen(false)
      if (!(target instanceof Element) || !target.closest('.journey-location-field')) setActiveSuggestionField(null)
    }
    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === 'Escape') { setPickerOpen(false); setTimeMenuOpen(false); setActiveSuggestionField(null) }
    }
    document.addEventListener('pointerdown', handlePointerDown)
    document.addEventListener('keydown', handleKeyDown)
    return () => {
      document.removeEventListener('pointerdown', handlePointerDown)
      document.removeEventListener('keydown', handleKeyDown)
    }
  }, [pickerOpen, timeMenuOpen, activeSuggestionField])

  useEffect(() => {
    if (!activeSuggestionField) return
    const raw = activeSuggestionField === 'origin' ? origin : destination
    const query = raw.trim()
    // Postcodes are resolved by Journey Planner; place autocomplete is for names and stops.
    if (query.length < 3 || isPostcodeLike(query)) {
      setSuggestions([]); setSuggestionsLoading(false); setSuggestionsLoaded(false); setSuggestionsError(false)
      return
    }
    const version = ++suggestionVersion.current
    setSuggestions([]); setSuggestionsLoading(true); setSuggestionsLoaded(false); setSuggestionsError(false)
    const timer = window.setTimeout(async () => {
      try {
        const found = await searchLocations(query)
        if (version !== suggestionVersion.current) return
        setSuggestions(found); setSuggestionsLoaded(true)
      } catch {
        if (version !== suggestionVersion.current) return
        setSuggestions([]); setSuggestionsLoaded(true); setSuggestionsError(true)
      } finally {
        if (version === suggestionVersion.current) setSuggestionsLoading(false)
      }
    }, 500)
    return () => { window.clearTimeout(timer); suggestionVersion.current++ }
  }, [activeSuggestionField, origin, destination])

  useEffect(() => {
    if (timeMenuOpen && timeMenuRef.current && selectedTimeRef.current) {
      timeMenuRef.current.scrollTop = selectedTimeRef.current.offsetTop - timeMenuRef.current.offsetTop
    }
  }, [timeMenuOpen])

  function change(field: 'origin' | 'destination', value: string) {
    if (autoSearchTimer.current !== null) { window.clearTimeout(autoSearchTimer.current); autoSearchTimer.current = null }
    requestVersion.current++
    if (field === 'origin') setOrigin(value); else setDestination(value)
    setTokens(current => { const next = { ...current }; delete next[field]; return next }); setCandidates([]); setSuggestions([]); setSuggestionsLoaded(false); setSuggestionsError(false); setActiveSuggestionField(null); setError(null); setLoading(false)
  }
  function changeTimeMode(value: 'now' | 'leave' | 'arrive') {
    requestVersion.current++; setTimeMode(value); setCandidates([]); setError(null); setLoading(false)
    setPickerOpen(false)
    scheduleTimeSearch(value, travelDate, travelTime)
  }
  function changeRequestedTime(field: 'date' | 'time', value: string) {
    requestVersion.current++
    const date = field === 'date' ? value : travelDate
    const time = field === 'time' ? value : travelTime
    if (field === 'date') setTravelDate(value); else setTravelTime(value)
    setCandidates([]); setError(null); setLoading(false)
    scheduleTimeSearch(timeMode, date, time)
  }
  function scheduleTimeSearch(mode: 'now' | 'leave' | 'arrive', date: string, time: string) {
    if (autoSearchTimer.current !== null) window.clearTimeout(autoSearchTimer.current)
    autoSearchTimer.current = window.setTimeout(() => {
      autoSearchTimer.current = null
      if (origin.trim() && destination.trim()) {
        const [selectedCandidates, scheduled] = scheduledSearchArguments(tokens, mode, date, time)
        void search(selectedCandidates, scheduled)
      }
    }, 250)
  }
  function shiftDate(days: number) {
    const [year, month, day] = travelDate.split('-').map(Number)
    const next = new Date(year, month - 1, day + days)
    const value = dateKey(next)
    setPickerMonth(monthDate(value))
    changeRequestedTime('date', value)
  }
  function shiftTime(minutes: number) {
    const [year, month, day] = travelDate.split('-').map(Number)
    const [hour, minute] = travelTime.split(':').map(Number)
    const next = new Date(year, month - 1, day, hour, minute + minutes)
    const date = dateKey(next)
    const time = `${String(next.getHours()).padStart(2, '0')}:${String(next.getMinutes()).padStart(2, '0')}`
    setTravelDate(date); setTravelTime(time); setPickerMonth(monthDate(date))
    requestVersion.current++; setCandidates([]); setError(null); setLoading(false)
    scheduleTimeSearch(timeMode, date, time)
    setTimeMenuOpen(false)
  }
  async function search(selected = tokens, scheduled?: { mode: 'now' | 'leave' | 'arrive'; date: string; time: string }, locations?: { origin: string; destination: string }) {
    if (autoSearchTimer.current !== null) { window.clearTimeout(autoSearchTimer.current); autoSearchTimer.current = null }
    const version = ++requestVersion.current
    setLoading(true); setError(null); setCandidates([]); setSuggestions([]); setSuggestionsLoading(false); setSuggestionsError(false); setActiveSuggestionField(null)
    try {
      const mode = scheduled?.mode ?? timeMode
      const date = scheduled?.date ?? travelDate
      const time = scheduled?.time ?? travelTime
      const input = { originPostcode: locations?.origin ?? origin.trim(), destinationPostcode: locations?.destination ?? destination.trim(), timeMode: mode, travelDate: mode === 'now' ? undefined : date, travelTime: mode === 'now' ? undefined : time, selectedOriginCandidateId: selected.origin, selectedDestinationCandidateId: selected.destination }
      let response: JourneyLookupResult | null = null
      for (let attempt = 0; attempt < 2; attempt++) {
        if (version !== requestVersion.current) return
        try {
          response = await lookupJourney(input)
          break
        } catch (failure) {
          const retryable = failure instanceof ApiFailure && ['provider_unavailable', 'network_error', 'request_timeout'].includes(failure.payload.code ?? '')
          if (!retryable || attempt === 1 || version !== requestVersion.current) throw failure
          await new Promise(resolve => window.setTimeout(resolve, 350))
        }
      }
      if (version !== requestVersion.current || !response) return
      setResult(response)
    } catch (failure) {
      if (version !== requestVersion.current) return
      if (failure instanceof ApiFailure && failure.payload.code === 'location_disambiguation') {
        setCandidates(failure.payload.candidates ?? []); setCandidateField(failure.payload.candidateField ?? 'destination')
      } else {
        setError(failure instanceof ApiFailure ? failure.payload : { code: 'network_error' })
        if (failure instanceof ApiFailure && failure.payload.code === 'candidate_expired') setTokens({})
      }
    } finally { if (version === requestVersion.current) setLoading(false) }
  }
  function submit(event: FormEvent) { event.preventDefault(); void search() }
  function swapLocations() {
    if (autoSearchTimer.current !== null) { window.clearTimeout(autoSearchTimer.current); autoSearchTimer.current = null }
    requestVersion.current++
    const swapped = swapJourneySelection(origin, destination, tokens)
    setOrigin(swapped.origin); setDestination(swapped.destination); setTokens(swapped.tokens)
    setCandidates([]); setSuggestions([]); setSuggestionsLoaded(false); setSuggestionsError(false); setActiveSuggestionField(null); setError(null); setLoading(false)
  }
  function selectSuggestion(field: 'origin' | 'destination', suggestion: Candidate) {
    requestVersion.current++
    const nextOrigin = field === 'origin' ? suggestion.name : origin
    const nextDestination = field === 'destination' ? suggestion.name : destination
    const selected = { ...tokens, [field]: suggestion.id }
    if (field === 'origin') setOrigin(suggestion.name); else setDestination(suggestion.name)
    setTokens(selected)
    setSuggestions([]); setSuggestionsLoaded(false); setSuggestionsLoading(false); setSuggestionsError(false); setActiveSuggestionField(null)
    setCandidates([]); setError(null); setLoading(false)
    if (nextOrigin.trim() && nextDestination.trim()) void search(selected, undefined, { origin: nextOrigin.trim(), destination: nextDestination.trim() })
  }
  function renderLocationInput(field: 'origin' | 'destination') {
    const value = field === 'origin' ? origin : destination
    const isOrigin = field === 'origin'
    const open = activeSuggestionField === field && value.trim().length >= 3 && !isPostcodeLike(value)
    const listId = `${field}-location-suggestions`
    const inputId = `${field}-location-input`
    return <div className="journey-location-field"><label htmlFor={inputId}>{zh ? (isOrigin ? '出发地点' : '目的地') : (isOrigin ? 'From' : 'To')}</label><div className="journey-input-wrap">
      <input id={inputId} name={isOrigin ? 'originPostcode' : 'destinationPostcode'} autoComplete="off" value={value} onFocus={() => setActiveSuggestionField(field)} onChange={event => { event.currentTarget.setCustomValidity(''); change(field, event.target.value); setActiveSuggestionField(field) }} onKeyDown={event => { if (event.key === 'ArrowDown' && suggestions.length > 0) { event.preventDefault(); document.getElementById(listId)?.querySelector('button')?.focus() } }} onInvalid={event => event.currentTarget.setCustomValidity(zh ? (isOrigin ? '请输入出发 postcode 或地点。' : '请输入目的 postcode 或地点。') : (isOrigin ? 'Enter an origin postcode or place.' : 'Enter a destination postcode or place.'))} placeholder={zh ? 'postcode、车站或地址' : 'Postcode, station, or address'} required maxLength={120} aria-autocomplete="list" aria-expanded={open && (suggestions.length > 0 || suggestionsLoading || suggestionsLoaded)} aria-controls={open ? listId : undefined} aria-invalid={!!error?.fields?.[isOrigin ? 'originPostcode' : 'destinationPostcode']} />
      </div>
      {open && (suggestionsLoading || suggestions.length > 0 || suggestionsLoaded) && <div className="journey-suggestion-menu" id={listId} role="listbox" aria-label={zh ? '匹配地点' : 'Matching locations'}>
        {suggestionsLoading && <p role="status">{zh ? '正在查找地点…' : 'Searching TfL locations…'}</p>}
        {!suggestionsLoading && suggestions.length === 0 && suggestionsLoaded && <p role="status">{suggestionsError ? (zh ? '暂时无法加载匹配地点，可以继续输入或直接查询。' : 'Could not load matches. Keep typing or search this location directly.') : (zh ? 'TfL 没有找到匹配地点，可以继续输入或直接查询。' : 'No TfL matches. Keep typing or search this location directly.')}</p>}
        {suggestions.map(suggestion => <button key={suggestion.id} type="button" role="option" aria-selected="false" onClick={() => selectSuggestion(field, suggestion)}><span><strong>{suggestion.name}</strong><small>{candidateType(suggestion.locality, language)}</small></span><span aria-hidden="true">→</span></button>)}
      </div>}
    </div>
  }
  const errorText = error?.code === 'backend_unavailable' || error?.code === 'network_error'
    ? (zh ? '无法连接本地查询服务，请启动服务后重试。' : 'Cannot connect to the local journey service. Start the service and try again.')
    : error?.code === 'request_timeout'
      ? (zh ? '查询超过 25 秒仍未完成。请稍后重试；按钮已恢复。' : 'The search did not finish within 25 seconds. Try again; the button is available again.')
    : error?.code === 'api_not_found'
      ? (zh ? '本地服务尚未加载新版路线查询，请重启服务后重试。' : 'The local service has not loaded the new journey search. Restart it and try again.')
    : error?.code === 'invalid_request' ? (zh ? '请检查出发地和目的地，输入 postcode 或更具体的地点。' : 'Check both locations. Enter a postcode or a more specific place.')
    : error?.code === 'rate_limited' ? (zh ? 'TfL 暂时限制了请求，请稍后重试。' : 'TfL has limited requests. Try again later.')
    : error?.code === 'provider_unavailable' ? (zh ? 'TfL 路线服务暂时没有返回结果，请稍后重试。' : 'TfL did not return a journey result. Please try again shortly.')
    : error?.code === 'location_unresolved' ? (zh ? 'TfL 无法识别该地点，请尝试输入完整地址、街道或车站名称。' : 'TfL could not find this place. Try a full address, street, or station name.')
    : error?.code === 'candidate_expired' ? (zh ? '地点选择已过期，请重新查询。' : 'The location choice expired. Search again.')
    : (zh ? '暂时无法查询路线，请重试。' : 'Unable to load journeys. Please try again.')

  return <div className="journey-app" lang={zh ? 'zh-CN' : 'en-GB'}>
    <header className="journey-header"><a href="#" className="journey-brand">↗ Commute Fit</a><label><span className="sr-only">Language / 语言</span><select value={language} onChange={event => { const value = event.target.value as Language; setLanguage(value); document.documentElement.lang = value === 'zh' ? 'zh-CN' : 'en-GB' }}><option value="zh">中文</option><option value="en">English</option></select></label></header>
    <main className="journey-main">
      <section className="journey-intro"><span className="journey-eyebrow">LONDON · TfL</span><h1>{zh ? '从这里，到那里。' : 'From here to there.'}</h1><p>{zh ? '输入 postcode、车站或地址，查看公共交通路线和全程票价。' : 'Enter postcodes, stations, or addresses for public transport journeys and whole-route fares.'}</p></section>
      <section className="journey-search" aria-label={zh ? '路线查询' : 'Journey search'}>
        <form onSubmit={submit}>
          <div className="journey-fields">
            {renderLocationInput('origin')}
            <button className="journey-swap" type="button" aria-label={zh ? '交换起终点' : 'Swap origin and destination'} onClick={swapLocations}>⇄</button>
            {renderLocationInput('destination')}
            <button className="journey-submit" disabled={loading} type="submit">{loading ? (zh ? '查询中…' : 'Searching…') : (zh ? '查询路线 →' : 'Find journeys →')}</button>
          </div>
          <fieldset className="journey-time" aria-label={zh ? '出行时间' : 'Journey time'}>
            <legend>{zh ? '出行时间' : 'Journey time'}</legend>
            <div className="journey-time-choice" role="radiogroup" aria-label={zh ? '选择时间方式' : 'Choose time mode'}>
              {(['now', 'leave', 'arrive'] as const).map(mode => <button key={mode} type="button" role="radio" aria-checked={timeMode === mode} className={timeMode === mode ? 'selected' : ''} onClick={() => changeTimeMode(mode)}>{mode === 'now' ? (zh ? '现在' : 'Now') : mode === 'leave' ? (zh ? '出发' : 'Leave') : (zh ? '到达' : 'Arrive')}</button>)}
            </div>
            {timeMode !== 'now' && <div className="journey-datetime-row" ref={dateTimeRowRef}>
              <div className="journey-time-control" ref={timeControlRef}>
                <button className="journey-time-value" type="button" aria-label={zh ? '选择时间' : 'Choose time'} aria-haspopup="listbox" aria-expanded={timeMenuOpen} onClick={() => { setTimeMenuOpen(!timeMenuOpen); setPickerOpen(false) }}>{travelTime}</button>
                <div className="journey-time-step-group">
                  <button type="button" aria-label={zh ? '更早 15 分钟' : '15 minutes earlier'} onClick={() => shiftTime(-15)}>‹</button>
                  <button type="button" aria-label={zh ? '更晚 15 分钟' : '15 minutes later'} onClick={() => shiftTime(15)}>›</button>
                </div>
                {timeMenuOpen && <div className="journey-time-menu" ref={timeMenuRef} role="listbox" aria-label={zh ? '选择时间，每 15 分钟一档' : 'Choose a time in 15-minute intervals'}>
                  {timeChoices.map(value => <button key={value} ref={value === travelTime ? selectedTimeRef : undefined} type="button" role="option" aria-selected={value === travelTime} onClick={() => { changeRequestedTime('time', value); setTimeMenuOpen(false) }}>{value}</button>)}
                </div>}
              </div>
              <div className="journey-date-group">
                <button className="journey-date-control" type="button" aria-label={zh ? `日期：${weekdayDate(travelDate, language)}` : `Date: ${weekdayDate(travelDate, language)}`} aria-expanded={pickerOpen} onClick={() => { setPickerMonth(monthDate(travelDate)); setPickerOpen(!pickerOpen) }}>
                  <span>{weekdayDate(travelDate, language)}</span>
                </button>
                <button className="journey-date-step" type="button" aria-label={zh ? '前一天' : 'Previous day'} onClick={() => shiftDate(-1)}>‹</button>
                <button className="journey-date-step" type="button" aria-label={zh ? '后一天' : 'Next day'} onClick={() => shiftDate(1)}>›</button>
                {pickerOpen && <div className="journey-date-popover" aria-label={zh ? '选择日期' : 'Choose date'}>
                  <div className="journey-calendar-heading"><button type="button" aria-label={zh ? '上个月' : 'Previous month'} onClick={() => setPickerMonth(new Date(pickerMonth.getFullYear(), pickerMonth.getMonth() - 1, 1))}>‹</button><strong>{monthLabel}</strong><button type="button" aria-label={zh ? '下个月' : 'Next month'} onClick={() => setPickerMonth(new Date(pickerMonth.getFullYear(), pickerMonth.getMonth() + 1, 1))}>›</button></div>
                  <div className="journey-calendar-grid" role="grid">
                    {weekdays.map((day, index) => <span className="journey-weekday" key={`weekday-${index}`}>{day}</span>)}
                    {calendarDays.map(day => {
                      const key = dateKey(day)
                      return <button key={key} type="button" role="gridcell" aria-label={new Intl.DateTimeFormat(zh ? 'zh-CN' : 'en-GB', { dateStyle: 'full' }).format(day)} aria-pressed={key === travelDate} className={`${day.getMonth() === pickerMonth.getMonth() ? '' : 'outside-month'} ${key === travelDate ? 'selected' : ''}`} onClick={() => { changeRequestedTime('date', key); setPickerOpen(false) }}>{day.getDate()}</button>
                    })}
                  </div>
                </div>}
              </div>
            </div>}
          </fieldset>
          <p className="journey-disclosure">{zh ? '使用伦敦当地时间。出发地和目的地文本将发送给 TfL，本应用不保存查询。' : 'Uses London local time. Your origin and destination are sent to TfL. This app does not save searches.'}</p>
        </form>
        {error && <p className="journey-error" role="alert">{errorText}</p>}
        {candidates.length > 0 && <div className="journey-candidates" aria-live="polite"><h2>{zh ? `请选择${candidateField === 'origin' ? '出发' : '目的'}地点` : `Choose the ${candidateField} location`}</h2><p>{zh ? 'TfL 找到了几个相近地点。请选择与你要去的位置相符的一项。' : `TfL found a few matches. Choose the ${candidateField === 'origin' ? 'starting' : 'destination'} place that matches your journey.`}</p>{candidates.map(candidate => <button type="button" key={candidate.id} disabled={loading} onClick={() => { const selected = { ...tokens, [candidateField]: candidate.id }; setTokens(selected); void search(selected) }}><span><strong>{candidate.name}</strong><small>{candidateType(candidate.locality, language)}</small></span><span aria-hidden="true">→</span></button>)}</div>}
      </section>
      <div aria-live="polite" aria-busy={loading}>{loading && <p className="journey-loading">{zh ? '正在向 TfL 查询路线及票价…' : 'Finding journeys and fares with TfL…'}</p>}</div>
      {result && <section className="journey-results" aria-labelledby="journey-results-title">
        <div className="journey-results-heading"><h2 id="journey-results-title">{result.originPostcode} → {result.destinationPostcode}</h2><p>{result.timeMode === 'now' ? (zh ? '现在出发' : 'Leaving now') : `${result.timeMode === 'leave' ? (zh ? '出发' : 'Leave') : (zh ? '到达' : 'Arrive')}: ${requestedTime(result.requestedDateTime, language)}`} · Europe/London</p></div>
        {result.routes.length === 0 && <p className="journey-empty">{zh ? 'TfL 没有返回该查询的路线，可核对 postcode 或稍后重试。这不代表一定没有服务。' : 'TfL returned no journeys for this query. Check the postcodes or try again later; this does not prove no service exists.'}</p>}
        {result.routes.map((route, index) => { const walkingOnly = isWalkingOnlyRoute(route.legs.map(leg => leg.mode)); return <article className="journey-route" key={route.id}>
          <div className="journey-route-top"><div><span className="journey-eyebrow">{zh ? '路线' : 'OPTION'} {index + 1}</span><h3>{clock(route.departure)} <span>→</span> {clock(route.arrival)}</h3><p>{route.durationMinutes == null ? (zh ? '耗时未知' : 'Duration unavailable') : `${route.durationMinutes} ${zh ? '分钟' : 'min'}`} · {route.transfers === 0 ? (zh ? '无需换乘' : 'No changes') : `${route.transfers} ${zh ? '次换乘' : 'change(s)'}`}</p></div><div className="journey-price"><span>{zh ? '全程总价' : 'Whole-route fare'}</span><div className="journey-price-line"><strong>{walkingOnly ? money(0) : route.totalFare == null ? (zh ? '票价暂不可用' : 'Fare unavailable') : money(route.totalFare)}</strong>{!walkingOnly && (route.fareChargeLevels?.length ?? 0) > 0 && <small className="journey-charge-level">{route.fareChargeLevels?.map(level => chargeLevelLabel(level, language)).join(' / ')}</small>}</div>{!walkingOnly && <small>{zh ? '成人 PAYG · TfL 报价' : 'Adult PAYG · TfL quote'}</small>}</div></div>
          <p className="journey-endpoints">{route.legs[0]?.from} → {route.legs.at(-1)?.to}</p>
          <ol className="journey-legs">{route.legs.map((leg, legIndex) => { const isWalk = ['walking', 'walk'].includes(leg.mode.toLowerCase()); return <li key={legIndex}><span className="journey-mode">{routeLabel(leg.mode, leg.route, language)}</span><div>{leg.from} → {leg.to}{!isWalk && leg.stops != null && <span className="journey-stops"> ({leg.stops} {zh ? '站' : 'stops'})</span>}<small>{clock(leg.departureDateTime)} — {clock(leg.arrivalDateTime)}{leg.durationMinutes != null && ` (${leg.durationMinutes} ${zh ? '分钟' : 'min'})`}</small></div></li>})}</ol>
          {!walkingOnly && route.totalFare == null && <p className="journey-fare-note">{zh ? 'TfL 未提供这条路线的完整总价；不将分段金额拼成总价。' : 'TfL did not provide a complete fare for this route. Leg prices are not added to invent a total.'}</p>}
        </article>})}
        <p className="journey-disclosure">{zh ? '计划班次不保证实时准点或到达。报价不是最终扣款，未应用个人优惠或全天封顶。Postcode 对应区域，请核对实际步行起终点。' : 'Scheduled journeys do not guarantee punctuality or arrival. Quotes are not final charges and do not apply personal discounts or daily caps. Postcodes represent areas; check the actual walking endpoints.'}</p>
      </section>}
      <footer className="journey-footer"><p>Powered by the Transport for London Journey Planner API</p><a href="https://tfl.gov.uk/plan-a-journey/" target="_blank" rel="noreferrer">{zh ? '在 TfL 核对路线 ↗' : 'Check journeys on TfL ↗'}</a><p>{zh ? '非 TfL 官方应用' : 'An independent app; not endorsed by TfL.'}</p></footer>
    </main>
  </div>
}
