package uk.co.commutefit.common;

import java.util.List;
import java.util.Map;

public class LocationDisambiguationException extends RuntimeException {
    private final String candidateField;
    private final List<Map<String, String>> candidates;
    public LocationDisambiguationException(String candidateField, List<Map<String, String>> candidates) {
        this.candidateField = candidateField;
        this.candidates = candidates;
    }
    public String candidateField() { return candidateField; }
    public List<Map<String, String>> candidates() { return candidates; }
}
