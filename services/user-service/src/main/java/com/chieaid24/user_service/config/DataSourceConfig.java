package com.chieaid24.user_service.config;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.metrics.micrometer.MicrometerMetricsTrackerFactory;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;
import org.springframework.util.StringUtils;

@Configuration
public class DataSourceConfig {

  @Bean
  @ConfigurationProperties("spring.datasource")
  public DataSourceProperties primaryProperties() {
    return new DataSourceProperties();
  }

  @Bean
  @ConfigurationProperties("spring.datasource.replica")
  public DataSourceProperties replicaProperties() {
    return new DataSourceProperties();
  }

  // Primary pool — writes + reads outside readOnly transactions. Also the Flyway target.
  @Bean(name = {"primaryDataSource", "flywayDataSource"})
  public HikariDataSource primaryDataSource(MeterRegistry meterRegistry) {
    HikariDataSource ds =
        primaryProperties().initializeDataSourceBuilder().type(HikariDataSource.class).build();
    ds.setPoolName("primary");
    ds.setMaximumPoolSize(8);
    ds.setMetricsTrackerFactory(new MicrometerMetricsTrackerFactory(meterRegistry));
    return ds;
  }

  @Bean
  public HikariDataSource replicaDataSource(MeterRegistry meterRegistry) {
    DataSourceProperties replica = replicaProperties();
    DataSourceProperties primary = primaryProperties();
    if (!StringUtils.hasText(replica.getUsername())) {
      replica.setUsername(primary.getUsername());
    }
    if (!StringUtils.hasText(replica.getPassword())) {
      replica.setPassword(primary.getPassword());
    }
    HikariDataSource ds =
        replica.initializeDataSourceBuilder().type(HikariDataSource.class).build();
    ds.setPoolName("replica");
    ds.setMaximumPoolSize(4);
    ds.setMetricsTrackerFactory(new MicrometerMetricsTrackerFactory(meterRegistry));
    return ds;
  }

  @Bean
  public RoutingDataSource routingDataSource(
      HikariDataSource primaryDataSource, HikariDataSource replicaDataSource) {
    RoutingDataSource routing = new RoutingDataSource();
    Map<Object, Object> targets = new HashMap<>();
    targets.put(RoutingDataSource.PRIMARY, primaryDataSource);
    targets.put(RoutingDataSource.REPLICA, replicaDataSource);
    routing.setTargetDataSources(targets);
    routing.setDefaultTargetDataSource(primaryDataSource);
    routing.afterPropertiesSet();
    return routing;
  }

  // The @Primary bean handed to JPA / repositories. Lazy proxy lets the routing key resolve
  // *after* the @Transactional(readOnly=…) flag has been applied to the current thread.
  @Bean
  @Primary
  public DataSource dataSource(RoutingDataSource routingDataSource) {
    return new LazyConnectionDataSourceProxy(routingDataSource);
  }
}
