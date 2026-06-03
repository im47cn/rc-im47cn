package com.rc.notification;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Testcontainers integration test base class.
 * <p>
 * Provides real Redis (redis:7-alpine) and MySQL (mysql:8.0) containers,
 * with dynamic property binding via {@code @DynamicPropertySource}.
 * Subclasses inherit fully bootstrapped Spring context with real infrastructure.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
public abstract class IntegrationTestBase {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
            .withDatabaseName("notification_test")
            .withUsername("test")
            .withPassword("test")
            .withInitScript("schema-mysql.sql");

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // MySQL datasource
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");

        // Disable H2 schema init (Testcontainers uses initScript instead)
        registry.add("spring.sql.init.mode", () -> "never");

        // Redis / Redisson - single server mode
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));

        // Test credentials
        registry.add("credential.master-key", () -> "test-master-key-for-integration-test");
        registry.add("admin.username", () -> "admin");
        registry.add("admin.password", () -> "admin");
    }
}
