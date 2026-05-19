package com.chieaid24.usage_service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.chieaid24.kafka.event.EnergyUsageEvent;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.query.FluxTable;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.InfluxDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@Testcontainers
@EmbeddedKafka(
    partitions = 1,
    topics = {"energy-usage", "energy-alerts"},
    bootstrapServersProperty = "spring.kafka.bootstrap-servers")
class UsageConsumerPipelineTests {

  @Container
  static InfluxDBContainer<?> influx =
      new InfluxDBContainer<>(DockerImageName.parse("influxdb:2.7"))
          .withUsername("admin")
          .withPassword("password123")
          .withOrganization("test-org")
          .withBucket("test-bucket")
          .withAdminToken("test-token");

  @Container
  static GenericContainer<?> redis =
      new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

  @DynamicPropertySource
  static void props(DynamicPropertyRegistry r) {
    r.add("influx.url", influx::getUrl);
    r.add("influx.token", () -> "test-token");
    r.add("influx.org", () -> "test-org");
    r.add("influx.bucket", () -> "test-bucket");
    r.add("spring.data.redis.host", redis::getHost);
    r.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    // REST client base URLs the @Scheduled aggregator dials. Real values not needed —
    // failures inside the scheduler are caught and don't fail this test.
    r.add("device.service.url", () -> "http://localhost/devices");
    r.add("user.service.url", () -> "http://localhost/users");
    // Mirror production kafka serdes.
    r.add(
        "spring.kafka.producer.value-serializer",
        () -> "org.springframework.kafka.support.serializer.JsonSerializer");
    r.add(
        "spring.kafka.producer.key-serializer",
        () -> "org.apache.kafka.common.serialization.StringSerializer");
    r.add(
        "spring.kafka.consumer.value-deserializer",
        () -> "org.springframework.kafka.support.serializer.JsonDeserializer");
    r.add(
        "spring.kafka.consumer.key-deserializer",
        () -> "org.apache.kafka.common.serialization.StringDeserializer");
    r.add("spring.kafka.consumer.properties.spring.json.trusted.packages", () -> "*");
    r.add(
        "spring.kafka.consumer.properties.spring.json.value.default.type",
        () -> "com.chieaid24.kafka.event.EnergyUsageEvent");
    r.add("spring.kafka.consumer.properties.spring.json.use.type.headers", () -> "false");
    r.add("spring.kafka.consumer.auto-offset-reset", () -> "earliest");
  }

  @Autowired private KafkaTemplate<String, Object> kafkaTemplate;
  @Autowired private InfluxDBClient influxDBClient;

  @Test
  void consumesEnergyUsageEventAndWritesPointToInflux() {
    Instant ts = Instant.now().minusSeconds(60);
    EnergyUsageEvent event =
        EnergyUsageEvent.builder().deviceId(99L).energyConsumed(42.42).timestamp(ts).build();

    kafkaTemplate.send("energy-usage", event);

    await()
        .atMost(Duration.ofSeconds(30))
        .untilAsserted(
            () -> {
              String flux =
                  "from(bucket: \"test-bucket\")\n"
                      + "  |> range(start: -10m)\n"
                      + "  |> filter(fn: (r) => r._measurement == \"energy_usage\")\n"
                      + "  |> filter(fn: (r) => r.deviceId == \"99\")\n"
                      + "  |> filter(fn: (r) => r._field == \"energyConsumed\")";
              List<FluxTable> tables = influxDBClient.getQueryApi().query(flux, "test-org");
              assertThat(tables)
                  .as("InfluxDB should have at least one matching table")
                  .isNotEmpty();
              assertThat(tables.get(0).getRecords())
                  .as("first table should have at least one record")
                  .isNotEmpty();
              Object value = tables.get(0).getRecords().get(0).getValueByKey("_value");
              assertThat(value).isInstanceOf(Number.class);
              assertThat(((Number) value).doubleValue()).isEqualTo(42.42);
            });
  }
}
