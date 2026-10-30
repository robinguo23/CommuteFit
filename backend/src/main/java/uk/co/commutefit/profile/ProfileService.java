package uk.co.commutefit.profile;

import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import uk.co.commutefit.common.ProfileValidationException;
import uk.co.commutefit.fare.policy.FarePolicyCatalogue;

@Service
@org.springframework.context.annotation.Profile("legacy")
public class ProfileService {
    private final ProfileRepository repository;
    private final Set<String> concessionIds;
    public ProfileService(ProfileRepository repository, FarePolicyCatalogue catalogue) {
        this.repository = repository;
        this.concessionIds = catalogue.policies().stream().map(policy -> policy.id()).collect(Collectors.toUnmodifiableSet());
    }
    public ProfileDto get() { return repository.get().orElseGet(ProfileDto::defaults); }
    public boolean supportsConcession(String id) { return concessionIds.contains(id); }
    public ProfileDto save(ProfileRequest request) {
        if (request.selectedConcessions() != null && request.selectedConcessions().stream().anyMatch(id -> !supportsConcession(id)))
            throw new ProfileValidationException("Unknown concession option. / 优惠选项无效。");
        if ("other".equals(request.preferredPaymentMethod()) && (request.otherPaymentMethodLabel() == null || request.otherPaymentMethodLabel().isBlank()))
            throw new ProfileValidationException("Name the other payment method. / 请填写其他付款方式名称。");
        return repository.save(request);
    }
    public void clear() { repository.clear(); }
}
