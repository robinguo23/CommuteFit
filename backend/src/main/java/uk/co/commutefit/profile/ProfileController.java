package uk.co.commutefit.profile;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profile")
@org.springframework.context.annotation.Profile("legacy")
public class ProfileController {
    private final ProfileService service;
    public ProfileController(ProfileService service) { this.service = service; }
    @GetMapping public ProfileDto get() { return service.get(); }
    @PutMapping public ProfileDto save(@Valid @RequestBody ProfileRequest request) { return service.save(request); }
    @DeleteMapping public ProfileDto clear() { service.clear(); return ProfileDto.defaults(); }
}
