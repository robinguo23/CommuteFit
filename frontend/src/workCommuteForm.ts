import type { Weekday } from './api/client'

const weekdays: Weekday[] = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY']

export interface SubmittedWorkSchedule {
  availabilityDays: Weekday[]
  availableFrom: string
  availableUntil: string
  workDays: Weekday[]
  shiftStart: string
  shiftEnd: string
}

export function readSubmittedWorkSchedule(formData: Pick<FormData, 'get' | 'getAll'>): SubmittedWorkSchedule {
  const readDays = (name: string) => formData.getAll(name)
    .map(String)
    .filter((value): value is Weekday => weekdays.includes(value as Weekday))
  const readTime = (name: string) => String(formData.get(name) ?? '')

  return {
    availabilityDays: readDays('availabilityDays'),
    availableFrom: readTime('availableFrom'),
    availableUntil: readTime('availableUntil'),
    workDays: readDays('workDays'),
    shiftStart: readTime('shiftStart'),
    shiftEnd: readTime('shiftEnd'),
  }
}
