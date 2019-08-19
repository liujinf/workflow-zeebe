/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package org.camunda.optimize.service.es.report.command.process.processinstance.duration.groupby.date;

import org.camunda.optimize.dto.optimize.query.report.single.filter.data.date.DateFilterDataDto;
import org.camunda.optimize.dto.optimize.query.report.single.group.GroupByDateUnit;
import org.camunda.optimize.dto.optimize.query.report.single.process.ProcessReportDataDto;
import org.camunda.optimize.dto.optimize.query.report.single.process.filter.EndDateFilterDto;
import org.camunda.optimize.service.es.report.command.aggregations.AggregationStrategy;
import org.camunda.optimize.service.es.report.command.process.util.ProcessInstanceQueryUtil;
import org.camunda.optimize.service.es.report.command.util.ExecutionStateAggregationUtil;
import org.camunda.optimize.service.es.schema.index.ProcessInstanceIndex;
import org.camunda.optimize.service.security.util.LocalDateUtil;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.script.Script;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.Aggregations;

import java.util.List;

import static org.camunda.optimize.service.es.filter.DateHistogramBucketLimiterUtil.createProcessEndDateHistogramBucketLimitingFilterFor;
import static org.camunda.optimize.service.es.schema.index.ProcessInstanceIndex.END_DATE;


public class ProcessInstanceDurationGroupByEndDateCommand extends
  AbstractProcessInstanceDurationGroupByDateCommand {

  public ProcessInstanceDurationGroupByEndDateCommand(final AggregationStrategy strategy) {
    super(strategy);
  }

  @Override
  protected Long processAggregationOperation(Aggregations aggs) {
    return aggregationStrategy.getValue(aggs);
  }

  @Override
  protected AggregationBuilder createOperationsAggregation() {
    return aggregationStrategy.getAggregationBuilder().script(getScriptedAggregationField());
  }

  private Script getScriptedAggregationField() {
    return ExecutionStateAggregationUtil.getDurationAggregationScript(
      LocalDateUtil.getCurrentDateTime().toInstant().toEpochMilli(),
      ProcessInstanceIndex.DURATION,
      ProcessInstanceIndex.START_DATE
    );
  }

  @Override
  public String getDateField() {
    return END_DATE;
  }

  @Override
  protected void addFiltersToQuery(final BoolQueryBuilder limitFilterQuery,
                                   final List<DateFilterDataDto> limitedFilters) {
    queryFilterEnhancer.getEndDateQueryFilter().addFilters(limitFilterQuery, limitedFilters);
  }

  @Override
  protected List<DateFilterDataDto> getReportDateFilter(final ProcessReportDataDto reportData) {
    return queryFilterEnhancer.extractFilters(reportData.getFilter(), EndDateFilterDto.class);
  }

  @Override
  protected BoolQueryBuilder createDefaultLimitingFilter(final GroupByDateUnit unit, final QueryBuilder query,
                                                         final ProcessReportDataDto reportData) {
    final BoolQueryBuilder limitFilterQuery;
    limitFilterQuery = createProcessEndDateHistogramBucketLimitingFilterFor(
      reportData.getFilter(),
      unit,
      configurationService.getEsAggregationBucketLimit(),
      ProcessInstanceQueryUtil.getLatestDate(query, getDateField(), esClient).orElse(null),
      queryFilterEnhancer
    );
    return limitFilterQuery;
  }
}
