package com.ems;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class EmsApplicationTests {

    @Test
    void contextLoads() {
        // Boots the full application (H2 in-memory, seeded demo data) and
        // verifies the JPA metamodel, Flyway-free schema, security config,
        // Thymeleaf dialects and all beans wire together cleanly.
    }
}
