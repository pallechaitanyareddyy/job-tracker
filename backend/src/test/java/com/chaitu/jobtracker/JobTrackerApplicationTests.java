package com.chaitu.jobtracker;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Starts the whole application against the jobtracker_test database.
 * Passing means Spring, the database connection and the Flyway migration all work.
 */
@SpringBootTest
@ActiveProfiles("test")
class JobTrackerApplicationTests {

    @Test
    void contextLoads() {
    }
}
