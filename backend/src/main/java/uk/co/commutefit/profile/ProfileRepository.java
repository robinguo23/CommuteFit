package uk.co.commutefit.profile;

import java.sql.Array;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@org.springframework.context.annotation.Profile("legacy")
public class ProfileRepository {
    private final JdbcTemplate jdbc;
    public ProfileRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public Optional<ProfileDto> get() {
        List<ProfileDto> rows = jdbc.query("SELECT departure_postcode,max_one_way_minutes,arrival_buffer_minutes,selected_concessions,preferred_payment_method,other_payment_method_label,updated_at FROM commute_profile WHERE id=1", (rs, rowNum) -> {
            Array values = rs.getArray("selected_concessions");
            List<String> concessions = values == null ? List.of() : Arrays.asList((String[]) values.getArray());
            return new ProfileDto(true, rs.getString("departure_postcode"), (Integer) rs.getObject("max_one_way_minutes"), rs.getInt("arrival_buffer_minutes"), concessions, rs.getString("preferred_payment_method"), rs.getString("other_payment_method_label"), rs.getTimestamp("updated_at").toInstant());
        });
        return rows.stream().findFirst();
    }

    public ProfileDto save(ProfileRequest request) {
        jdbc.update(connection -> {
            var statement = connection.prepareStatement("INSERT INTO commute_profile (id,departure_postcode,max_one_way_minutes,arrival_buffer_minutes,selected_concessions,preferred_payment_method,other_payment_method_label,updated_at) VALUES (1,?,?,?,?,?,?,now()) ON CONFLICT (id) DO UPDATE SET departure_postcode=EXCLUDED.departure_postcode,max_one_way_minutes=EXCLUDED.max_one_way_minutes,arrival_buffer_minutes=EXCLUDED.arrival_buffer_minutes,selected_concessions=EXCLUDED.selected_concessions,preferred_payment_method=EXCLUDED.preferred_payment_method,other_payment_method_label=EXCLUDED.other_payment_method_label,updated_at=now()");
            statement.setString(1, request.departurePostcode().trim().toUpperCase());
            statement.setObject(2, request.maxOneWayMinutes());
            statement.setInt(3, request.arrivalBufferMinutes() == null ? 30 : request.arrivalBufferMinutes());
            Array array = connection.createArrayOf("text", request.selectedConcessions() == null ? new String[0] : request.selectedConcessions().toArray(String[]::new));
            statement.setArray(4, array);
            statement.setString(5, request.preferredPaymentMethod());
            statement.setString(6, "other".equals(request.preferredPaymentMethod()) ? request.otherPaymentMethodLabel() : null);
            return statement;
        });
        return get().orElseThrow();
    }

    public void clear() { jdbc.update("DELETE FROM commute_profile WHERE id=1"); }
}
