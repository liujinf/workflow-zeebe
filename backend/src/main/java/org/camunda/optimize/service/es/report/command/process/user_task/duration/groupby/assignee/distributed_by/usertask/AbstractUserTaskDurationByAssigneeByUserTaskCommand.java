/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package org.camunda.optimize.service.es.report.command.process.user_task.duration.groupby.assignee.distributed_by.usertask;

import org.camunda.optimize.dto.optimize.query.report.single.configuration.FlowNodeExecutionState;
import org.camunda.optimize.dto.optimize.query.report.single.process.ProcessReportDataDto;
import org.camunda.optimize.dto.optimize.query.report.single.process.result.ProcessReportHyperMapResult;
import org.camunda.optimize.dto.optimize.query.report.single.result.HyperMapResultEntryDto;
import org.camunda.optimize.dto.optimize.query.report.single.result.MapResultEntryDto;
import org.camunda.optimize.service.es.report.command.aggregations.AggregationStrategy;
import org.camunda.optimize.service.es.report.command.process.user_task.UserTaskDistributedByUserTaskCommand;
import org.camunda.optimize.service.es.report.command.util.ExecutionStateAggregationUtil;
import org.camunda.optimize.service.es.report.result.process.SingleProcessHyperMapReportResult;
import org.camunda.optimize.service.exceptions.OptimizeRuntimeException;
import org.camunda.optimize.service.security.util.LocalDateUtil;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.script.Script;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.BucketOrder;
import org.elasticsearch.search.aggregations.bucket.filter.Filter;
import org.elasticsearch.search.aggregations.bucket.nested.Nested;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.camunda.optimize.service.es.report.command.util.ExecutionStateAggregationUtil.addExecutionStateFilter;
import static org.camunda.optimize.service.es.schema.type.ProcessInstanceType.USER_TASKS;
import static org.camunda.optimize.service.es.schema.type.ProcessInstanceType.USER_TASK_ACTIVITY_ID;
import static org.camunda.optimize.service.es.schema.type.ProcessInstanceType.USER_TASK_ASSIGNEE;
import static org.camunda.optimize.service.es.schema.type.ProcessInstanceType.USER_TASK_END_DATE;
import static org.camunda.optimize.upgrade.es.ElasticsearchConstants.PROC_INSTANCE_TYPE;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.search.aggregations.AggregationBuilders.filter;
import static org.elasticsearch.search.aggregations.AggregationBuilders.nested;

public abstract class AbstractUserTaskDurationByAssigneeByUserTaskCommand extends UserTaskDistributedByUserTaskCommand {

  private static final String USER_TASK_ID_TERMS_AGGREGATION = "tasks";
  private static final String USER_TASK_ASSIGNEE_TERMS_AGGREGATION = "assignees";
  private static final String USER_TASKS_AGGREGATION = "userTasks";
  private static final String FILTERED_USER_TASKS_AGGREGATION = "filteredUserTasks";

  protected AggregationStrategy aggregationStrategy;

  AbstractUserTaskDurationByAssigneeByUserTaskCommand(AggregationStrategy strategy) {
    aggregationStrategy = strategy;
  }


  @Override
  protected SingleProcessHyperMapReportResult evaluate() {
    final ProcessReportDataDto processReportData = getReportData();
    logger.debug(
      "Evaluating user task duration grouped by assignee distributed by user task report " +
        "for process definition key [{}] and versions [{}]",
      processReportData.getProcessDefinitionKey(),
      processReportData.getProcessDefinitionVersions()
    );

    final BoolQueryBuilder query = setupBaseQuery(processReportData);
    final SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
      .query(query)
      .fetchSource(false)
      .aggregation(createAggregation(processReportData.getConfiguration().getFlowNodeExecutionState()))
      .size(0);
    final SearchRequest searchRequest = new SearchRequest(PROC_INSTANCE_TYPE)
      .types(PROC_INSTANCE_TYPE)
      .source(searchSourceBuilder);

    try {
      final SearchResponse response = esClient.search(searchRequest, RequestOptions.DEFAULT);
      final ProcessReportHyperMapResult resultDto = mapToReportResult(response);
      return new SingleProcessHyperMapReportResult(resultDto, reportDefinition);
    } catch (IOException e) {
      final String reason = String.format(
        "Could not evaluate user task duration grouped by assignee report distributed by user task" +
          "for process definition key [%s] and versions [%s]",
        processReportData.getProcessDefinitionKey(),
        processReportData.getProcessDefinitionVersions()
      );
      logger.error(reason, e);
      throw new OptimizeRuntimeException(reason, e);
    }
  }

  protected abstract String getDurationFieldName();

  private AggregationBuilder createAggregation(FlowNodeExecutionState flowNodeExecutionState) {
    return nested(USER_TASKS, USER_TASKS_AGGREGATION)
      .subAggregation(
        filter(
          FILTERED_USER_TASKS_AGGREGATION,
          addExecutionStateFilter(
            boolQuery(),
            flowNodeExecutionState,
            USER_TASKS + "." + USER_TASK_END_DATE
          )
        )
          .subAggregation(
            AggregationBuilders
              .terms(USER_TASK_ASSIGNEE_TERMS_AGGREGATION)
              .size(configurationService.getEsAggregationBucketLimit())
              .order(BucketOrder.key(true))
              .field(USER_TASKS + "." + USER_TASK_ASSIGNEE)
              .subAggregation(
                AggregationBuilders
                  .terms(USER_TASK_ID_TERMS_AGGREGATION)
                  .size(configurationService.getEsAggregationBucketLimit())
                  .order(BucketOrder.key(true))
                  .field(USER_TASKS + "." + USER_TASK_ACTIVITY_ID)
                  .subAggregation(
                    aggregationStrategy.getAggregationBuilder()
                      .script(getScriptedAggregationField())

                  )
              )

          )
      );
  }


  private Script getScriptedAggregationField() {
    return ExecutionStateAggregationUtil.getDurationAggregationScript(
      LocalDateUtil.getCurrentDateTime().toInstant().toEpochMilli(),
      USER_TASKS + "." + getDurationFieldName(),
      USER_TASKS + "." + getReferenceDateFieldName()
    );
  }

  protected abstract String getReferenceDateFieldName();

  private ProcessReportHyperMapResult mapToReportResult(final SearchResponse response) {
    final ProcessReportHyperMapResult resultDto = new ProcessReportHyperMapResult();

    final Aggregations aggregations = response.getAggregations();
    final Nested userTasks = aggregations.get(USER_TASKS_AGGREGATION);
    final Filter filteredUserTasks = userTasks.getAggregations().get(FILTERED_USER_TASKS_AGGREGATION);
    final Terms byAssigneeAggregation = filteredUserTasks.getAggregations().get(USER_TASK_ASSIGNEE_TERMS_AGGREGATION);

    final List<HyperMapResultEntryDto<Long>> resultData = new ArrayList<>();
    for (Terms.Bucket assigneeBucket : byAssigneeAggregation.getBuckets()) {

      final List<MapResultEntryDto<Long>> byAssigneeEntry = new ArrayList<>();
      final Terms byTaskIdAggregation = assigneeBucket.getAggregations().get(USER_TASK_ID_TERMS_AGGREGATION);
      for (Terms.Bucket taskBucket : byTaskIdAggregation.getBuckets()) {
        final Long value = aggregationStrategy.getValue(taskBucket.getAggregations());
        byAssigneeEntry.add(new MapResultEntryDto<>(taskBucket.getKeyAsString(), value));
      }
      resultData.add(new HyperMapResultEntryDto<>(assigneeBucket.getKeyAsString(), byAssigneeEntry));
    }

    resultDto.setData(resultData);
    resultDto.setIsComplete(byAssigneeAggregation.getSumOfOtherDocCounts() == 0L);
    resultDto.setProcessInstanceCount(response.getHits().getTotalHits());

    return resultDto;
  }

}
