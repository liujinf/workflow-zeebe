/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package org.camunda.optimize.dto.optimize.query.report.single.process.filter.util;

import org.camunda.optimize.dto.optimize.query.report.single.process.filter.ExecutingFlowNodeFilterDto;
import org.camunda.optimize.dto.optimize.query.report.single.process.filter.data.ExecutingFlowNodeFilterDataDto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ExecutingFlowNodeFilterBuilder {

  private List<String> values = new ArrayList<>();
  private ProcessFilterBuilder filterBuilder;

  private ExecutingFlowNodeFilterBuilder(ProcessFilterBuilder processFilterBuilder) {
    filterBuilder = processFilterBuilder;
  }

  public static ExecutingFlowNodeFilterBuilder construct(ProcessFilterBuilder processFilterBuilder) {
    return new ExecutingFlowNodeFilterBuilder(processFilterBuilder);
  }

  public ExecutingFlowNodeFilterBuilder id(String flowNodeId) {
    values.add(flowNodeId);
    return this;
  }

  public ExecutingFlowNodeFilterBuilder ids(String... flowNodeIds) {
    values.addAll(Arrays.asList(flowNodeIds));
    return this;
  }

  public ProcessFilterBuilder add() {
    ExecutingFlowNodeFilterDataDto dataDto = new ExecutingFlowNodeFilterDataDto();
    dataDto.setValues(new ArrayList<>(values));
    ExecutingFlowNodeFilterDto executingFlowNodeFilterDto = new ExecutingFlowNodeFilterDto();
    executingFlowNodeFilterDto.setData(dataDto);
    filterBuilder.addFilter(executingFlowNodeFilterDto);
    return filterBuilder;
  }
}
