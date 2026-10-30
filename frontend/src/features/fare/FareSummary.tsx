import type { FareSummary as FareSummaryData } from '../../api/client'
import type { Language } from '../../i18n/messages'
import { messages } from '../../i18n/messages'

type Props = { fare: FareSummaryData; language: Language }
const money = (amount: number | null, currency = 'GBP') => amount == null ? null : new Intl.NumberFormat('en-GB', { style: 'currency', currency }).format(amount)
function localizedMode(value: string, language: Language) {
  const key = value.toLowerCase().trim().replaceAll(' ', '-').replaceAll('_', '-')
  const en: Record<string, string> = { bus: 'Bus', tram: 'Tram', tube: 'Tube', rail: 'Rail', 'national-rail': 'National Rail', dlr: 'DLR', 'london-overground': 'London Overground', overground: 'London Overground', 'elizabeth-line': 'Elizabeth line', walking: 'Walk', walk: 'Walk', coach: 'Coach', ferry: 'Ferry' }
  const zh: Record<string, string> = { bus: '公交', tram: '电车', tube: '地铁', rail: '铁路', 'national-rail': '国家铁路', dlr: 'DLR 轻轨', 'london-overground': '伦敦地上铁', overground: '伦敦地上铁', 'elizabeth-line': '伊丽莎白线', walking: '步行', walk: '步行', coach: '长途客车', ferry: '渡船' }
  return (language === 'zh' ? zh : en)[key] ?? value.replaceAll('-', ' ')
}
function localizedChargeLevel(value: string | null, language: Language) {
  if (!value) return null
  const key = value.toLowerCase()
  if (language === 'zh') return key === 'peak' ? '高峰' : key === 'off peak' || key === 'off-peak' ? '非高峰' : value
  return key === 'off peak' ? 'Off peak' : value
}
const zhPolicyText: Record<string, { event: string; rule: string; scope: string; payment: string; assumption: string }> = {
  'jobcentre-plus': { event: '符合条件的成人 PAYG 票价可享五折。', rule: '须持有效 Jobcentre Plus Travel Discount Card，并将优惠设入 Oyster。', scope: '仅适用于符合范围的公交和铁路票价项；不适用于感应支付。', payment: '付款条件：已设置优惠的 Oyster。', assumption: '未确认你是否持有有效的 Jobcentre Plus Travel Discount Card，或是否已将优惠设入 Oyster。' },
  'railcard-offpeak': { event: '符合条件的非高峰 PAYG 铁路票价可减三分之一。', rule: '须将受支持的 Railcard 优惠加入 Oyster，并确认提供方标记的非高峰票价项。', scope: '只适用于列出的铁路票价项；公交和电车不适用。', payment: '付款条件：已加入 Railcard 优惠的 Oyster。', assumption: '未确认你是否持有并已设置该 Railcard 优惠。' },
  'disabled-persons-railcard': { event: '符合条件的高峰及非高峰 PAYG 铁路票价可减三分之一。', rule: '须将 Disabled Persons Railcard 优惠加入 Oyster，并确认铁路票价项范围。', scope: '只适用于铁路票价项；公交和电车不适用。', payment: '付款条件：已加入 Railcard 优惠的 Oyster。', assumption: '未确认你是否持有并已设置该 Railcard 优惠。' },
  'student-18plus': { event: '成人 Travelcard 及 Bus & Tram Pass 季票可享七折。', rule: '此优惠适用于指定季票，不降低单程 PAYG 票价。', scope: '季票产品；不适用于本次单程票价。', payment: '付款条件：18+ Student Oyster photocard 季票。', assumption: '此路线按单程 PAYG 票价查询，不能套用季票折扣。' },
  'bus-tram-discount': { event: '符合条件的成人公交和电车 PAYG 票价可享五折。', rule: '须居住在伦敦 borough、符合年龄和福利条件，且未享有其他免费或优惠交通；还需将优惠设入 Oyster。', scope: '仅适用于公交和电车票价项。', payment: '付款条件：已设置优惠的 Oyster。', assumption: '未确认你是否符合计划资格。' },
  'care-leaver-18-25': { event: '符合条件的成人公交和电车 PAYG 票价可享五折。', rule: '须持符合条件的伦敦居民 18-25 Care Leaver Oyster photocard。', scope: '仅适用于公交和电车票价项。', payment: '付款条件：符合条件的 Oyster photocard。', assumption: '未确认你是否符合计划资格。' },
}
const zhTimeBasis: Record<string, string> = {
  'jobcentre-plus': '按提供方返回的票价项和支持的路线交通方式判断。',
  'student-18plus': '按季票产品规则判断，不按单程行程时长判断。',
  'bus-tram-discount': '按公交／电车票价项判断。',
  'care-leaver-18-25': '按公交／电车票价项判断。',
}
function localizedRailTimeBasis(value: string) {
  const levels = value.split(': ').at(-1) ?? value
  if (levels === 'Journey Planner fare-item chargeLevel not returned') return 'TfL 未返回票价项时段分类'
  return `TfL 票价项时段分类：${levels.split(', ').map(level => localizedChargeLevel(level, 'zh')).join('、')}`
}

export default function FareSummary({ fare, language }: Props) {
  const t = messages[language]
  const route = fare.routeFare
  return <details className="fare-details">
    <summary><span className="fare-summary-title">{t.standardFare}</span><strong>{route.amount == null ? t.noFare : money(route.amount, route.currency)}</strong></summary>
    <div className="fare-content">
      {route.amount == null ? <p className="muted-copy">{t.fareUnavailable}</p> : <p className="fare-basis">{language === 'zh' ? route.paymentBasisZh : route.paymentBasis} · {language === 'zh' ? route.scopeZh : route.scope}</p>}
      {fare.fareItems.length > 0 && <div className="fare-section"><h4>{t.fareItems}</h4><p className="muted-copy">{t.itemScope}</p>
        {fare.fareItems.some(item => item.hopperFare === true) && <p className="muted-copy">{t.hopperExplanation} <a href="https://tfl.gov.uk/fares/find-fares/bus-and-tram-fares" target="_blank" rel="noreferrer">{t.viewSource} ↗</a></p>}
        <ol className="fare-items">{fare.fareItems.map(item => <li key={item.sequence}>
          <span><strong>{item.modes.length ? item.modes.map(mode => localizedMode(mode, language)).join(' + ') : `${t.review} ${item.sequence}`}</strong><small>{[localizedChargeLevel(item.chargeLevel, language), item.lowZone != null ? (language === 'zh' ? `第 ${item.lowZone}${item.highZone != null && item.highZone !== item.lowZone ? `–${item.highZone}` : ''} 区` : `Zone ${item.lowZone}${item.highZone != null && item.highZone !== item.lowZone ? `–${item.highZone}` : ''}`) : null, item.hopperFare ? t.hopperLabel : null].filter(Boolean).join(' · ')}</small></span>
          <strong>{money(item.amount, item.currency) ?? t.unknown}</strong>
        </li>)}</ol>
      </div>}
      {fare.conditionalConcessionScenarios.length > 0 && <div className="fare-section"><h4>{t.possibleDiscounts}</h4>
        <div className="concession-list">{fare.conditionalConcessionScenarios.map(item => <article className="concession-card" key={item.policyId}>
          <div className="concession-heading"><strong>{language === 'zh' ? item.titleZh : item.titleEn}</strong><span className={`status-pill status-${item.eligibilityStatus}`}>{t[item.eligibilityStatus]}</span></div>
          <div className="concession-amount">{item.amount.value == null ? t.amountUnknown : money(item.amount.value, item.amount.currency)}</div>
          {item.amount.basis && <small>{language === 'zh' ? t.amountBasis : item.amount.basis}</small>}
          {item.effectiveDate && <small>{t.concessionEffective}: {item.effectiveDate}</small>}
          {language === 'zh' ? <p>{zhPolicyText[item.policyId]?.event} {zhPolicyText[item.policyId]?.rule} {zhPolicyText[item.policyId]?.scope}</p> : <p>{item.qualifyingEvent}. {item.eligibilityRule} {item.fareScope}.</p>}
          <small>{language === 'zh' ? `${item.policyId.includes('railcard') ? localizedRailTimeBasis(item.timeBasis) : zhTimeBasis[item.policyId] ?? '按该优惠的官方规则判断。'} · ${zhPolicyText[item.policyId]?.payment} ${item.selectedByUser ? '你已选择此优惠。' : zhPolicyText[item.policyId]?.assumption}` : `${item.timeBasis} · ${item.paymentAssumption} ${item.eligibilityAssumption}`}</small>
          {item.fareItemSequences.length > 0 && <small>{language === 'zh' ? '适用票价项' : 'Fare item(s)'}: {item.fareItemSequences.join(', ')}</small>}
          {item.amountFareItemSequences.length > 0 && <small>{t.amountItems}: {item.amountFareItemSequences.join(', ')}</small>}
          <a href={item.source} target="_blank" rel="noreferrer">{t.viewSource} ↗</a>
        </article>)}</div>
      </div>}
      <div className="cap-guidance"><strong>{t.cap}</strong>
        {fare.capGuidance.adultDailyCap != null && <p>{t.adultCap}: {t.dailyCap} {money(fare.capGuidance.adultDailyCap, fare.capGuidance.currency)} · {t.weeklyCap} {money(fare.capGuidance.adultWeeklyCap, fare.capGuidance.currency)}</p>}
        {fare.capGuidance.conditionalDiscountDailyCap != null && <p>{t.conditionalDiscountCap}: {t.dailyCap} {money(fare.capGuidance.conditionalDiscountDailyCap, fare.capGuidance.currency)}</p>}
        <p>{language === 'zh' ? fare.capGuidance.messageZh : fare.capGuidance.messageEn}</p>
        {fare.capGuidance.adultDailyCap != null && <small>{t.capScope}</small>}
        {fare.capGuidance.effectiveFrom && <small>{t.capEffective} {fare.capGuidance.effectiveFrom}</small>}
        <a href={fare.capGuidance.source} target="_blank" rel="noreferrer">{t.viewSource} ↗</a></div>
      <a className="fare-source" href={route.source} target="_blank" rel="noreferrer">{t.source}: TfL Single Fare Finder ↗</a>
    </div>
  </details>
}
