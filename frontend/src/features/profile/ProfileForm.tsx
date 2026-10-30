import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import type { Language } from '../../i18n/messages'
import { messages } from '../../i18n/messages'
import type { PaymentMethod, Profile, ProfileInput } from '../../api/client'

type Props = { profile: Profile; language: Language; saving: boolean; onSave: (profile: ProfileInput) => Promise<boolean>; onClear: () => Promise<void> }
const concessions = [
  ['jobcentre-plus', 'Jobcentre Plus'], ['railcard-offpeak', 'National Railcard (off-peak)'], ['disabled-persons-railcard', 'Disabled Persons Railcard'],
  ['student-18plus', '18+ Student Oyster'], ['bus-tram-discount', 'Bus & Tram Discount'], ['care-leaver-18-25', '18-25 Care Leaver Oyster'],
]

export default function ProfileForm({ profile, language, saving, onSave, onClear }: Props) {
  const t = messages[language]
  const [postcode, setPostcode] = useState(profile.departurePostcode)
  const [limit, setLimit] = useState(profile.maxOneWayMinutes?.toString() ?? '')
  const [buffer, setBuffer] = useState(String(profile.arrivalBufferMinutes ?? 30))
  const [selected, setSelected] = useState<string[]>(profile.selectedConcessions ?? [])
  const [payment, setPayment] = useState<PaymentMethod>(profile.preferredPaymentMethod ?? null)
  const [otherLabel, setOtherLabel] = useState(profile.otherPaymentMethodLabel ?? '')
  const [saved, setSaved] = useState(false)
  const [expanded, setExpanded] = useState(true)

  useEffect(() => {
    setPostcode(profile.departurePostcode ?? '')
    setLimit(profile.maxOneWayMinutes?.toString() ?? '')
    setBuffer(String(profile.arrivalBufferMinutes ?? 30))
    setSelected(profile.selectedConcessions ?? [])
    setPayment(profile.preferredPaymentMethod ?? null)
    setOtherLabel(profile.otherPaymentMethodLabel ?? '')
  }, [profile])

  useEffect(() => {
    if (profile.configured) setExpanded(false)
  }, [profile.configured])

  async function submit(event: FormEvent) {
    event.preventDefault()
    const didSave = await onSave({
      departurePostcode: postcode.trim().toUpperCase(), maxOneWayMinutes: limit ? Number(limit) : null,
      arrivalBufferMinutes: Number(buffer), selectedConcessions: selected, preferredPaymentMethod: payment,
      otherPaymentMethodLabel: payment === 'other' ? otherLabel.trim() : null,
    })
    if (didSave) {
      setSaved(true)
      setExpanded(false)
      window.setTimeout(() => setSaved(false), 2400)
    }
  }

  function toggle(id: string) { setSelected(current => current.includes(id) ? current.filter(value => value !== id) : [...current, id]) }

  return <details className="panel profile-panel" open={expanded} onToggle={event => setExpanded(event.currentTarget.open)}>
    <summary className="profile-summary">
      <div className="panel-heading"><div><span className="step-label">{t.profileStep}</span><h2 id="profile-title">{t.profile}</h2><p>{profile.configured ? `${t.savedPostcode}: ${profile.departurePostcode}` : t.profileHelp}</p></div><span className="panel-icon" aria-hidden="true">⌂</span></div>
      {profile.configured && <span className="profile-toggle-label">{expanded ? t.closeProfile : t.editProfile}</span>}
    </summary>
    <form className="form-stack" onSubmit={submit}>
      <label className="field">{t.postcode}
        <input value={postcode} onChange={event => setPostcode(event.target.value)} placeholder="e.g. N1 9GU" maxLength={16} autoComplete="postal-code" required />
        <small>{t.postcodeHint}</small>
      </label>
      <div className="field-grid">
        <label className="field">{t.timeLimit}
          <div className="input-suffix"><input type="number" min="1" max="1440" value={limit} onChange={event => setLimit(event.target.value)} placeholder={t.noLimit} /><span>{t.minutes}</span></div>
        </label>
        <label className="field">{t.buffer}
          <div className="input-suffix"><input type="number" min="0" max="240" value={buffer} onChange={event => setBuffer(event.target.value)} required /><span>{t.minutes}</span></div>
        </label>
      </div>
      <fieldset className="field-set"><legend>{t.payment}</legend>
        <select value={payment ?? ''} onChange={event => setPayment((event.target.value || null) as PaymentMethod)}>
          <option value="">{t.noPreference}</option><option value="apple-pay-contactless">{t.applePay}</option><option value="bank-card-contactless">{t.bankCard}</option><option value="oyster">{t.oyster}</option><option value="other">{t.other}</option>
        </select>
        {payment === 'other' && <input aria-label={t.otherLabel} value={otherLabel} onChange={event => setOtherLabel(event.target.value)} placeholder={t.otherLabel} required />}
      </fieldset>
      <fieldset className="field-set"><legend>{t.concessions}</legend>
        <div className="check-grid">{concessions.map(([id, label]) => <label className="check-option" key={id}><input type="checkbox" checked={selected.includes(id)} onChange={() => toggle(id)} /><span>{label}</span></label>)}</div>
      </fieldset>
      <div className="profile-actions"><button className="button button-dark" type="submit" disabled={saving}>{saving ? '…' : t.save}<span aria-hidden="true">↗</span></button>{saved && <span className="saved-note" role="status">✓ {t.saved}</span>}
        {profile.configured && <button className="button-link" type="button" onClick={() => void onClear()}>{t.clear}</button>}
      </div>
    </form>
  </details>
}
