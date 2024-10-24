/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.db.rdbms.write.service;

import io.camunda.db.rdbms.write.domain.ProcessDefinitionDbModel;
import io.camunda.db.rdbms.write.queue.ContextType;
import io.camunda.db.rdbms.write.queue.ExecutionQueue;
import io.camunda.db.rdbms.write.queue.QueueItem;

public class ProcessDefinitionWriter {

  private final ExecutionQueue executionQueue;

  public ProcessDefinitionWriter(final ExecutionQueue executionQueue) {
    this.executionQueue = executionQueue;
  }

  public void save(final ProcessDefinitionDbModel processDefinition) {
    executionQueue.executeInQueue(
        new QueueItem(
            ContextType.PROCESS_DEFINITION,
            processDefinition.processDefinitionKey(),
            "io.camunda.db.rdbms.sql.ProcessDefinitionMapper.insert",
            processDefinition));
  }
}