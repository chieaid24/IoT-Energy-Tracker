package com.chieaid24.alert_service.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
public class RoutingDataSource extends AbstractRoutingDataSource {

  static final String PRIMARY = "PRIMARY";
  static final String REPLICA = "REPLICA";

  @Override
  protected Object determineCurrentLookupKey() {
    String key =
        TransactionSynchronizationManager.isCurrentTransactionReadOnly() ? REPLICA : PRIMARY;
    log.debug("Routing datasource lookup -> {}", key);
    return key;
  }
}
