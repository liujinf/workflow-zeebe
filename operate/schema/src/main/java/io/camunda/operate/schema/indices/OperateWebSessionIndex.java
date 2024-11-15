/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.operate.schema.indices;

import io.camunda.operate.conditions.DatabaseInfo;
import io.camunda.operate.property.OperateProperties;
import io.camunda.operate.schema.backup.Prio4Backup;
import io.camunda.webapps.schema.descriptors.operate.OperateIndexDescriptor;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OperateWebSessionIndex extends OperateIndexDescriptor implements Prio4Backup {

  public static final String ID = "id";
  public static final String CREATION_TIME = "creationTime";
  public static final String LAST_ACCESSED_TIME = "lastAccessedTime";
  public static final String MAX_INACTIVE_INTERVAL_IN_SECONDS = "maxInactiveIntervalInSeconds";
  public static final String ATTRIBUTES = "attributes";

  public static final String INDEX_NAME = "web-session";

  @Autowired private OperateProperties properties;

  public OperateWebSessionIndex() {
    super(null, false);
  }

  @PostConstruct
  public void init() {
    indexPrefix = properties.getIndexPrefix(DatabaseInfo.getCurrent());
    isElasticsearch = DatabaseInfo.isElasticsearch();
  }

  @Override
  public String getIndexName() {
    return INDEX_NAME;
  }

  @Override
  public String getVersion() {
    return "1.1.0";
  }

  @Override
  public String getIndexPrefix() {
    return properties.getIndexPrefix();
  }
}
