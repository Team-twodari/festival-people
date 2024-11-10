import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

import env.E2eTestEnvironment;
import io.restassured.filter.cookie.CookieFilter;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class E2eTest extends E2eTestEnvironment {

    private static CookieFilter adminCookieFilter;
    private static CookieFilter userCookieFilter;

    @BeforeEach
    public void setup() {
        truncateRedis();
        truncateMysql();

        adminCookieFilter = new CookieFilter();
        userCookieFilter = new CookieFilter();
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
                .body("data.id", notNullValue());

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

    @Test
    @DisplayName("회원은 축제와 티켓을 생성할 수 있다")
    void testFestivalAndTicketCreation() {
        given()
                .baseUri(API_SERVER_URL)
                .contentType(ContentType.JSON)
                .body("{\"name\":\"admin\", \"email\":\"admin@test.com\", \"profileImg\":\"admin.jpg\"}")
                .when()
                .post("/api/v1/member/signup")
                .then()
                .statusCode(201);

        // Step 2: Admin 로그인 후 세션 쿠키 얻기
        given()
                .baseUri(API_SERVER_URL)
                .contentType(ContentType.JSON)
                .body("{\"email\":\"admin@test.com\"}")
                .filter(adminCookieFilter)
                .when()
                .post("/api/v1/auth/login")
                .then()
                .statusCode(200);

        // Step 3: Admin 축제 생성
        Response festivalResponse = given()
                .baseUri(API_SERVER_URL)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "title" : "Summer Music Festival",
                          "description" : "A vibrant music festival",
                          "startTime" : "2024-12-10T04:39:48",
                          "endTime" : "2024-12-12T04:39:48"
                        }
                        """)
                .filter(adminCookieFilter)
                .when()
                .post("/api/v1/festivals")
                .then()
                .statusCode(201)
                .body("data.festivalId", notNullValue())
                .extract().response();

        int festivalId = festivalResponse.path("data.festivalId");

        // Step 4: Admin 티켓 생성
        given()
                .baseUri(API_SERVER_URL)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "name" : "티켓1",
                          "detail" : "티켓 설명입니다",
                          "price" : 1000,
                          "quantity" : 100,
                          "startSaleTime" : "2024-11-10T04:40:03.523081",
                          "endSaleTime" : "2024-11-12T04:40:03.523081",
                          "refundEndTime" : "2024-11-12T04:40:03.523081"
                        }
                        """)
                .filter(adminCookieFilter)
                .when()
                .post("/api/v1/festivals/" + festivalId + "/tickets")
                .then()
                .statusCode(201)
                .body("data.ticketId", notNullValue());
    }

    @Test
    @DisplayName("회원은 티켓을 구매할 수 있다")
    void testFestivalTicketPurchaseScenario() throws InterruptedException {
        given()
                .baseUri(API_SERVER_URL)
                .contentType(ContentType.JSON)
                .body("{\"name\":\"admin\", \"email\":\"admin@test.com\", \"profileImg\":\"admin.jpg\"}")
                .when()
                .post("/api/v1/member/signup")
                .then()
                .statusCode(201);

        // Step 2: Admin 로그인 후 세션 쿠키 얻기
        given()
                .baseUri(API_SERVER_URL)
                .contentType(ContentType.JSON)
                .body("{\"email\":\"admin@test.com\"}")
                .filter(adminCookieFilter)
                .when()
                .post("/api/v1/auth/login")
                .then()
                .statusCode(200);

        // Step 3: Admin 축제 생성
        Response festivalResponse = given()
                .baseUri(API_SERVER_URL)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "title" : "Summer Music Festival",
                          "description" : "A vibrant music festival",
                          "startTime" : "2024-12-10T04:39:48",
                          "endTime" : "2024-12-12T04:39:48"
                        }
                        """)
                .filter(adminCookieFilter)
                .when()
                .post("/api/v1/festivals")
                .then()
                .statusCode(201)
                .body("data.festivalId", notNullValue())
                .extract().response();

        int festivalId = festivalResponse.path("data.festivalId");

        // Step 4: Admin 티켓 생성
        int ticketPrice = 10000;
        int ticketQuantity = 100;
        Response ticketResponse = given()
                .baseUri(API_SERVER_URL)
                .contentType(ContentType.JSON)
                .body(String.format("""
                        {
                          "name" : "티켓1",
                          "detail" : "티켓 설명입니다",
                          "price" : %d,
                          "quantity" : %d,
                          "startSaleTime" : "2024-11-10T04:40:03.523081",
                          "endSaleTime" : "2024-11-12T04:40:03.523081",
                          "refundEndTime" : "2024-11-12T04:40:03.523081"
                        }
                        """, ticketPrice, ticketQuantity))
                        .filter(adminCookieFilter)
                        .when()
                        .post("/api/v1/festivals/" + festivalId + "/tickets")
                        .then()
                        .statusCode(201)
                        .body("data.ticketId", notNullValue())
                        .extract().response();

        int ticketId = ticketResponse.path("data.ticketId");

        // Step 5: User 회원가입
        given()
                .baseUri(API_SERVER_URL)
                .contentType(ContentType.JSON)
                .body("{\"name\":\"testUser\", \"email\":\"user@test.com\", \"profileImg\":\"user.jpg\"}")
                .when()
                .post("/api/v1/member/signup")
                .then()
                .statusCode(201);

        // Step 6: User 로그인 후 세션 쿠키 얻기
        given()
                .baseUri(API_SERVER_URL)
                .contentType(ContentType.JSON)
                .body("{\"email\":\"user@test.com\"}")
                .filter(userCookieFilter)
                .when()
                .post("/api/v1/auth/login")
                .then()
                .statusCode(200);

        // Step 7: User 축제 상세 조회
        given()
                .baseUri(API_SERVER_URL)
                .filter(userCookieFilter)
                .when()
                .get("/api/v1/festivals/" + festivalId)
                .then()
                .statusCode(200)
                .body("data.festivalId", equalTo(festivalId));

        // Step 8: User 티켓 구매 가능 여부 확인
        Response purchaseSessionResponse = given()
                .baseUri(API_SERVER_URL)
                .filter(userCookieFilter)
                .when()
                .get("/api/v1/festivals/" + festivalId + "/tickets/" + ticketId + "/purchase/check")
                .then()
                .statusCode(200)
                .body("data.purchasable", equalTo(true))
                .extract().response();
        String purchaseSession = purchaseSessionResponse.path("data.purchaseSession").toString();

        // Step 9: User 티켓 구매 미리보기 조회
        given()
                .baseUri(API_SERVER_URL)
                .filter(userCookieFilter)
                .when()
                .get("/api/v1/festivals/" + festivalId + "/tickets/" + ticketId + "/purchase/" + purchaseSession)
                .then()
                .statusCode(200)
                .body("data.ticketPrice", equalTo(ticketPrice))
                .body("data.ticketQuantity", equalTo(ticketQuantity));

        // Step 10: User 티켓 구매
        given()
                .contentType(ContentType.JSON)
                .baseUri(API_SERVER_URL)
                .filter(userCookieFilter)
                .when()
                .post("/api/v1/festivals/" + festivalId + "/tickets/" + ticketId + "/purchase/" + purchaseSession)
                .then()
                .statusCode(200)
                .body("data.paymentId", notNullValue());

        Thread.sleep(5000); // 결제 완료까지 대기

        // assert
        // 구매한 티켓 내역에 존재하는지 확인
        given()
                .baseUri(API_SERVER_URL)
                .contentType(ContentType.JSON)
                .filter(userCookieFilter)
                .when()
                .get("/api/v1/member/tickets/" + ticketId)
                .then()
                .statusCode(200)
                .body("data.purchaseStatus", equalTo("PURCHASED"))
                .body("data.festival.festivalId", equalTo(festivalId));

         // 남은 재고 정합성 확인
        given()
                .baseUri(API_SERVER_URL)
                .contentType(ContentType.JSON)
                .filter(userCookieFilter)
                .when()
                .get("/api/v1/festivals/"+ festivalId +"/tickets")
                .then()
                .statusCode(200)
                .body("data.tickets.remainStock[0]", equalTo(ticketQuantity - 1));
    }

    // 재고 롤백 프로세스
}
