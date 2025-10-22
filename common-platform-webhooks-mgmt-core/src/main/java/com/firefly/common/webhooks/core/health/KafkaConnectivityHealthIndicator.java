/*
 * Copyright 2025 Firefly Software Solutions Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.firefly.common.webhooks.core.health;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Health indicator for Kafka connectivity.
 * <p>
 * Performs actual connectivity test by describing the Kafka cluster.
 * This is more reliable than just checking if the admin client exists.
 */
@Component("kafkaConnectivity")
@RequiredArgsConstructor
@Slf4j
public class KafkaConnectivityHealthIndicator implements HealthIndicator {

    private final KafkaAdmin kafkaAdmin;
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    @Override
    public Health health() {
        try {
            // Create admin client from KafkaAdmin
            try (AdminClient adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
                
                // Describe cluster to test connectivity
                DescribeClusterResult clusterResult = adminClient.describeCluster();
                
                // Wait for result with timeout
                String clusterId = clusterResult.clusterId().get(TIMEOUT.toSeconds(), TimeUnit.SECONDS);
                int nodeCount = clusterResult.nodes().get(TIMEOUT.toSeconds(), TimeUnit.SECONDS).size();
                
                log.debug("Kafka health check successful: clusterId={}, nodes={}", clusterId, nodeCount);
                
                return Health.up()
                        .withDetail("clusterId", clusterId)
                        .withDetail("nodes", nodeCount)
                        .withDetail("status", "Connected")
                        .build();
            }
        } catch (Exception e) {
            log.error("Kafka health check failed: {}", e.getMessage());
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .withDetail("errorType", e.getClass().getSimpleName())
                    .withDetail("status", "Disconnected")
                    .build();
        }
    }
}

