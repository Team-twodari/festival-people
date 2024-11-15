package Fixture;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.wootecam.festivals.domain.auth.dto.LoginRequest;
import com.wootecam.festivals.domain.festival.dto.FestivalCreateRequest;
import com.wootecam.festivals.domain.member.dto.MemberCreateRequest;
import com.wootecam.festivals.domain.ticket.dto.TicketCreateRequest;
import java.time.LocalDateTime;

public final class ApiRequestFixture {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper.registerModule(new JavaTimeModule());
    }

    private ApiRequestFixture() {
    }

    public static String createMemberCreateRequest(String name, String email) throws JsonProcessingException {
        return objectMapper.writeValueAsString(new MemberCreateRequest(name, email, name + ".jpg"));
    }

    public static String createMemberLoginRequest(String email) throws JsonProcessingException {
        return objectMapper.writeValueAsString(new LoginRequest(email));
    }

    public static String createFestivalCreateRequest(LocalDateTime startDateTime) throws JsonProcessingException {
        return objectMapper.writeValueAsString(
                new FestivalCreateRequest("festival "+startDateTime, "description " + startDateTime,
                        startDateTime, startDateTime.plusDays(1)));
    }

    public static String createTicketCreateRequest(int quantity, LocalDateTime startDateTime) throws JsonProcessingException {
        return objectMapper.writeValueAsString(
                new TicketCreateRequest("ticket "+ startDateTime, "detail"+ startDateTime, 1000L, quantity,
                        startDateTime, startDateTime.plusDays(1), startDateTime.plusDays(1)));
    }
}
