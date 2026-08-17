package com.ems.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Runs the real Flyway migration (V1__init.sql, PostgreSQL dialect) against
 * H2 in PostgreSQL mode and boots Hibernate with ddl-auto=validate, proving
 * the versioned schema and the JPA entities stay in sync — the same check
 * production performs on every startup against PostgreSQL.
 */
@SpringBootTest(properties = {
        "spring.flyway.enabled=true",
        "spring.flyway.locations=classpath:db/migration",
        "spring.jpa.hibernate.ddl-auto=validate"
})
@ActiveProfiles("test")
class FlywaySchemaValidationTests {

    @Test
    void migrationScriptMatchesEntities() {
        // Context boot = Flyway applied V1 and Hibernate validated the metamodel
        // against it. Any drift fails startup with SchemaManagementException.
    }
}
