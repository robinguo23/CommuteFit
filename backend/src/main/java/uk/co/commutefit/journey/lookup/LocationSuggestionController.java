package uk.co.commutefit.journey.lookup;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.co.commutefit.journey.tfl.LocationSuggestionService;

@RestController
@RequestMapping("/api/locations")
public class LocationSuggestionController {
    private final LocationSuggestionService suggestions;

    public LocationSuggestionController(LocationSuggestionService suggestions) {
        this.suggestions = suggestions;
    }

    @GetMapping
    public List<LocationSuggestionService.Suggestion> search(
        @RequestParam String query
    ) {
        String value = query == null ? "" : query.trim();
        if (value.length() < 3 || value.length() > 120) {
            throw new IllegalArgumentException("Enter at least 3 characters and no more than 120 characters.");
        }
        return suggestions.search(value);
    }
}
