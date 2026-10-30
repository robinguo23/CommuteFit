export type WorkCommuteLanguage = 'zh' | 'en'

export function formatWorkdayDate(value: string, language: WorkCommuteLanguage): string {
  const locale = language === 'zh' ? 'zh-CN' : 'en-GB'
  return new Intl.DateTimeFormat(locale, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    weekday: 'long',
    timeZone: 'Europe/London',
  }).format(new Date(`${value}T12:00:00Z`))
}
