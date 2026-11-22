package com.chaitu.jobtracker.common.controller;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class HealthControllerTest {

    @Test
    void reportsUp() {
        var response = new HealthController().health();

        assertThat(response.status()).isEqualTo("UP");
        assertThat(response.service()).isEqualTo("job-tracker-api");
        assertThat(response.time()).isNotNull();
    }
}
