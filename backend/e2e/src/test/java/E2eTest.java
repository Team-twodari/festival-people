import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

import env.E2eTestEnvironment;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class E2eTest extends E2eTestEnvironment {

    @BeforeEach
    public void setup() {
        truncateRedis();
        truncateMysql();
    }

    @Test
    @DisplayName("사용자는 회원가입 후 로그인할 수 있다")
    void testUserSignUpAndLogin() {
        // 회원가입
        String signupPayload = """
                    {
                      "name" : "test",
                      "email" : "test@test.com",
                      "profileImg" : "test"
                    }
                """;

        given()
                .baseUri(API_SERVER_URL)
                .contentType("application/json")
                .body(signupPayload)
                .when()
                .post("/api/v1/member/signup")
                .then()
                .statusCode(201)
                .body("data.id", equalTo(1));

        // 로그인
        String loginPayload = """
                    {
                      "email" : "test@test.com"
                    }
                """;

        Response loginResponse = given()
                .baseUri(API_SERVER_URL)
                .contentType("application/json")
                .body(loginPayload)
                .when()
                .post("/api/v1/auth/login")
                .then()
                .statusCode(200)
                .extract().response();

        assert loginResponse.header("Set-Cookie").contains("SESSION");
    }
}
