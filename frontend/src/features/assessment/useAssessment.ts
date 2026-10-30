import { useCallback, useState } from 'react'
import { api, ApiFailure } from '../../api/client'
import type { AssessmentInput, AssessmentResult, Candidate } from '../../api/client'

export type CandidateField = 'origin' | 'destination'

export function useAssessment() {
  const [result, setResult] = useState<AssessmentResult | null>(null)
  const [loading, setLoading] = useState(false)
  const [candidates, setCandidates] = useState<Candidate[]>([])
  const [candidateField, setCandidateField] = useState<CandidateField | null>(null)
  const [selectedCandidates, setSelectedCandidates] = useState<{ origin: string | null; destination: string | null }>({ origin: null, destination: null })
  const [errorCode, setErrorCode] = useState<string | null>(null)

  const submit = useCallback(async (input: AssessmentInput) => {
    const requestInput: AssessmentInput = {
      ...input,
      selectedCandidateId: input.selectedCandidateId ?? selectedCandidates.destination ?? undefined,
      selectedOriginCandidateId: input.selectedOriginCandidateId ?? selectedCandidates.origin ?? undefined,
    }
    setLoading(true)
    setErrorCode(null)
    setCandidates([])
    setCandidateField(null)
    setResult(null)
    try {
      const response = await api.assess(requestInput)
      setResult(response)
      setSelectedCandidates({ origin: null, destination: null })
    } catch (error) {
      if (error instanceof ApiFailure && error.payload.code === 'location_disambiguation') {
        setCandidates(error.payload.candidates ?? [])
        setCandidateField(error.payload.candidateField ?? 'destination')
        setErrorCode(null)
      } else {
        if (error instanceof ApiFailure && error.payload.code === 'candidate_expired') {
          setSelectedCandidates({ origin: null, destination: null })
        }
        setErrorCode(error instanceof ApiFailure ? error.payload.code ?? 'app_error' : 'app_error')
      }
    } finally { setLoading(false) }
  }, [selectedCandidates])

  const chooseCandidate = useCallback(async (candidate: Candidate, input: AssessmentInput) => {
    const field = candidateField ?? 'destination'
    const updated = { ...selectedCandidates, [field]: candidate.id }
    setSelectedCandidates(updated)
    await submit({
      ...input,
      selectedCandidateId: updated.destination ?? undefined,
      selectedOriginCandidateId: updated.origin ?? undefined,
    })
  }, [candidateField, selectedCandidates, submit])

  const invalidate = useCallback(() => {
    setCandidates([])
    setCandidateField(null)
    setSelectedCandidates({ origin: null, destination: null })
    setResult(null)
    setErrorCode(null)
  }, [])

  const inputChanged = useCallback(() => { setResult(null); setErrorCode(null) }, [])
  return { result, loading, candidates, candidateField, errorCode, submit, chooseCandidate, invalidate, inputChanged, clearResults: invalidate }
}
