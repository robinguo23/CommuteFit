package uk.co.commutefit.assessment;

public final class AssessmentTimeBoundaryException extends RuntimeException {
    private final boolean ambiguous;

    public AssessmentTimeBoundaryException(boolean ambiguous) {
        this.ambiguous = ambiguous;
    }

    public boolean ambiguous() { return ambiguous; }
}
