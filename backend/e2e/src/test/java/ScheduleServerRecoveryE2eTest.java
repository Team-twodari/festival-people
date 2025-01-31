import static Fixture.ApiRequestFixture.createFestivalCreateRequest;
import static Fixture.ApiRequestFixture.createMemberCreateRequest;
import static Fixture.ApiRequestFixture.createMemberLoginRequest;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import com.fasterxml.jackson.core.JsonProcessingException;
import env.E2eTestEnvironment;
import io.restassured.filter.cookie.CookieFilter;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;

class ScheduleServerRecoveryE2eTest extends E2eTestEnvironment {

    private static CookieFilter adminCookieFilter;
    private static CookieFilter userCookieFilter;
    private static GenericContainer<?> recoveryScheduleServer;

    @BeforeEach
    public void setup() {
        truncateRedis();
        truncateMysql();

        adminCookieFilter = new CookieFilter();
        userCookieFilter = new CookieFilter();

        startScheduleServer();
        stopRecoveryScheduleServer(recoveryScheduleServer);
    }

    @Test
    @DisplayName("스케줄 서버 다운 시 다른 스케줄 서버가 해당 스케줄링을 실행한다.")
    void testRecoveryScheduleServer() throws InterruptedException, JsonProcessingException {
        given()
                .baseUri(API_SERVER_URL)
                .contentType(ContentType.JSON)
                .body(createMemberCreateRequest("admin", "admin@email.com"))
                .when()
                .post("/api/v1/member/signup")
                .then()
                .statusCode(201);

        // Step 2: Admin 로그인 후 세션 쿠키 얻기
        given()
                .baseUri(API_SERVER_URL)
                .contentType(ContentType.JSON)
                .body(createMemberLoginRequest("admin@email.com"))
                .filter(adminCookieFilter)
                .when()
                .post("/api/v1/auth/login")
                .then()
                .statusCode(200);

        // Step 3: Admin 축제 생성
        Response festivalResponse = given()
                .baseUri(API_SERVER_URL)
                .contentType(ContentType.JSON)
                .body(createFestivalCreateRequest(LocalDateTime.now().plusMinutes(1)))
                .filter(adminCookieFilter)
                .when()
                .post("/api/v1/festivals")
                .then()
                .statusCode(201)
                .body("data.festivalId", notNullValue())
                .extract().response();

        int festivalId = festivalResponse.path("data.festivalId");

        stopScheduleServer();

        recoveryScheduleServer = startRecoveryScheduleServer();

        Thread.sleep(90000);

        given()
                .baseUri(API_SERVER_URL)
                .filter(userCookieFilter)
                .when()
                .get("/api/v1/festivals/" + festivalId)
                .then()
                .statusCode(200)
                .body("data.festivalId", equalTo(festivalId))
                .body("data.festivalProgressStatus", equalTo("ONGOING"));
    }
}
