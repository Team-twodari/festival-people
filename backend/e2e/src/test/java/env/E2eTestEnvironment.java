package env;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import java.io.FileInputStream;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.utility.DockerImageName;

public abstract class E2eTestEnvironment {

    private static final Properties config = new Properties();
    private static String mysqlHost;
    private static Integer mysqlPort;
    private static String database;
    private static String user;
    private static String password;
    private static String redisHost;
    private static Integer redisPort;

    static {
        try (FileInputStream input = new FileInputStream("config.properties")) {
            config.load(input);

            mysqlHost = config.getProperty("mysql.host");
            mysqlPort = Integer.parseInt(config.getProperty("mysql.port", "3306")); // 기본값 3306 설정
            database = config.getProperty("mysql.database");
            user = config.getProperty("mysql.user");
            password = config.getProperty("mysql.password");
            redisHost = config.getProperty("redis.host");
            redisPort = Integer.parseInt(config.getProperty("redis.port"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 네트워크 생성 (컨테이너들 간 통신용)
    private static final Network network = Network.newNetwork();

    // MySQL 컨테이너 설정
    private static MySQLContainer<?> mysqlContainer = new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
            .withDatabaseName(database)
            .withUsername(user)
            .withPassword(password)
            .withNetwork(network)
            .withInitScript("init.sql")
            .withNetworkAliases(mysqlHost)
            .withExposedPorts(mysqlPort)
            .waitingFor(Wait.forListeningPort());

    // Redis 컨테이너 설정
    private static GenericContainer<?> redisContainer = new GenericContainer<>(DockerImageName.parse("redis:latest"))
            .withExposedPorts(redisPort)
            .withNetwork(network)
            .withNetworkAliases(redisHost)
            .waitingFor(Wait.forListeningPort());

    // API 서버 컨테이너 설정
    private static final Logger logger = LoggerFactory.getLogger(E2eTestEnvironment.class);

    private static GenericContainer<?> apiServerContainer = new GenericContainer<>(
            new ImageFromDockerfile().withDockerfile(Paths.get("../api-server/Dockerfile")))
            .withExposedPorts(8080)
            .withEnv("SPRING_PROFILES_ACTIVE", "docker")
            .withNetwork(network)
            .withLogConsumer(new Slf4jLogConsumer(logger))
            .waitingFor(Wait.forHttp("/health").forStatusCode(200));

    // Queue 서버 컨테이너 설정
    private static GenericContainer<?> queueServerContainer = new GenericContainer<>(
            new ImageFromDockerfile().withDockerfile(Paths.get("../queue-server/Dockerfile")))
            .withExposedPorts(8080)
            .withEnv("SPRING_PROFILES_ACTIVE", "docker")
            .withNetwork(network)
            .withLogConsumer(new Slf4jLogConsumer(logger))
            .waitingFor(Wait.forHttp("/health").forStatusCode(200));

    // Schedule 서버 컨테이너 설정
    private static GenericContainer<?> scheduleServerContainer = new GenericContainer<>(
            new ImageFromDockerfile().withDockerfile(Paths.get("../schedule-server/Dockerfile")))
            .withExposedPorts(8080)
            .withEnv("SPRING_PROFILES_ACTIVE", "docker")
            .withNetwork(network)
            .withLogConsumer(new Slf4jLogConsumer(logger));

    protected static String API_SERVER_URL;
    protected static String QUEUE_SERVER_URL;
    protected static String mysqlUrl;
    protected static String redisUrl;

    @BeforeAll
    public static void setUp() {
        mysqlContainer.start();
        redisContainer.start();

        apiServerContainer.start();
        queueServerContainer.start();
        scheduleServerContainer.start();

        mysqlUrl =
                "jdbc:mysql://" + mysqlContainer.getHost() + ":" + mysqlContainer.getMappedPort(3306) + "/" + database;
        redisUrl = "redis://" + redisContainer.getHost() + ":" + redisContainer.getMappedPort(6379);

        API_SERVER_URL = "http://localhost:" + apiServerContainer.getMappedPort(8080);
        QUEUE_SERVER_URL = "http://localhost:" + queueServerContainer.getMappedPort(8080);
    }

    @AfterAll
    public static void tearDown() {
        apiServerContainer.stop();
        queueServerContainer.stop();
        scheduleServerContainer.stop();

        redisContainer.stop();
        mysqlContainer.stop();
    }

    /*
     * Redis 데이터베이스의 모든 데이터를 삭제합니다.
     */
    protected void truncateRedis() {
        RedisClient redisClient = RedisClient.create(redisUrl);
        StatefulRedisConnection<String, String> connection = redisClient.connect();

        try {
            // Stream 데이터만 삭제 (Consumer Group 유지)
            trimStream(connection, "festival-schedule-stream");
            trimStream(connection, "ticket-schedule-stream");

            // Redis의 다른 모든 키 삭제
            connection.sync().keys("*").stream()
                    .filter(key -> !key.equals("festival-schedule-stream") && !key.equals("ticket-schedule-stream"))
                    .forEach(connection.sync()::del);
        } finally {
            connection.close();
            redisClient.shutdown();
        }
    }

    /**
     * MySQL 데이터베이스의 모든 테이블을 초기화합니다.
     */
    protected void truncateMysql() {
        try (Connection conn = DriverManager.getConnection(mysqlUrl, user, password);
             Statement stmt = conn.createStatement();
             Statement truncateStmt = conn.createStatement()) {

            stmt.execute("SET FOREIGN_KEY_CHECKS = 0");

            String query = "SELECT table_name FROM information_schema.tables WHERE table_schema = '" + database + "'";
            ResultSet rs = stmt.executeQuery(query);

            while (rs.next()) {
                String table = rs.getString("table_name");
                truncateStmt.execute("TRUNCATE TABLE " + table);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    protected GenericContainer<?> startRecoveryScheduleServer() {
        GenericContainer<?> recoveryScheduleServerContainer = new GenericContainer<>(
                new ImageFromDockerfile().withDockerfile(Paths.get("../schedule-server/Dockerfile")))
                .withExposedPorts(8080)
                .withEnv("SPRING_PROFILES_ACTIVE", "docker")
                .withNetwork(network)
                .withLogConsumer(new Slf4jLogConsumer(logger));

        recoveryScheduleServerContainer.start();

        return recoveryScheduleServerContainer;
    }

    protected void stopRecoveryScheduleServer(GenericContainer<?> recoveryScheduleServerContainer) {
        if (recoveryScheduleServerContainer != null && recoveryScheduleServerContainer.isRunning()) {
            recoveryScheduleServerContainer.stop();
        }
    }

    protected void startScheduleServer() {
        if (!scheduleServerContainer.isRunning()) {
            scheduleServerContainer.start();
        }
    }

    protected void stopScheduleServer() {
        if (scheduleServerContainer.isRunning()) {
            scheduleServerContainer.stop();
        }
    }

    private void trimStream(StatefulRedisConnection<String, String> connection, String streamKey) {
        try {
            // XTRIM 명령어로 Stream 데이터를 삭제
            connection.sync().xtrim(streamKey, 0); // 모든 데이터를 삭제
        } catch (Exception e) {
            return;
        }
    }
}
