/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.service.db.os.report.filter.util;

import static io.camunda.optimize.dto.optimize.query.report.single.filter.data.date.DateUnit.QUARTERS;
import static io.camunda.optimize.service.db.DatabaseConstants.OPTIMIZE_DATE_FORMAT;
import static io.camunda.optimize.service.db.os.externalcode.client.dsl.QueryDSL.json;

import io.camunda.optimize.dto.optimize.query.report.single.filter.data.date.DateFilterDataDto;
import io.camunda.optimize.dto.optimize.query.report.single.filter.data.date.RelativeDateFilterStartDto;
import io.camunda.optimize.dto.optimize.query.report.single.filter.data.date.RollingDateFilterStartDto;
import io.camunda.optimize.service.exceptions.OptimizeValidationException;
import io.camunda.optimize.service.security.util.LocalDateUtil;
import io.camunda.optimize.service.util.DateFilterUtil;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch._types.query_dsl.RangeQuery;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DateFilterQueryUtilOS {
  private static final DateTimeFormatter formatter =
      DateTimeFormatter.ofPattern(OPTIMIZE_DATE_FORMAT);

  public static List<Query> filterQueries(
      final List<DateFilterDataDto<?>> dates, final String dateField, final ZoneId timezone) {
    return dates == null
        ? List.of()
        : dates.stream()
            .flatMap(dateDto -> createRangeQuery(dateDto, dateField, timezone).stream())
            .toList();
  }

  public static List<Query> createRangeQueries(
      final List<DateFilterDataDto<?>> dates, final String dateField, final ZoneId timezone) {
    return dates == null
        ? List.of()
        : dates.stream()
            .flatMap(dateDto -> createRangeQuery(dateDto, dateField, timezone).stream())
            .toList();
  }

  public static Optional<Query> createRangeQuery(
      final DateFilterDataDto<?> dateFilterDto, final String dateField, final ZoneId timezone) {
    switch (dateFilterDto.getType()) {
      case FIXED:
        return createFixedDateFilter(
            (OffsetDateTime) dateFilterDto.getStart(), dateFilterDto.getEnd(), dateField, timezone);
      case ROLLING:
        return createRollingDateFilter(
            (RollingDateFilterStartDto) dateFilterDto.getStart(), dateField, timezone);
      case RELATIVE:
        return createRelativeDateFilter(
            (RelativeDateFilterStartDto) dateFilterDto.getStart(), dateField, timezone);
      default:
        log.warn("Cannot execute date filter. Unknown type [{}]", dateFilterDto.getType());
        return Optional.empty();
    }
  }

  private static Optional<Query> createFixedDateFilter(
      final OffsetDateTime start,
      final OffsetDateTime end,
      final String dateField,
      final ZoneId timezone) {
    if (end == null && start == null) {
      return Optional.empty();
    }

    final RangeQuery.Builder rangeQueryBuilder = new RangeQuery.Builder().field(dateField);
    if (end != null) {
      final OffsetDateTime endDateWithCorrectTimezone =
          end.atZoneSameInstant(timezone).toOffsetDateTime();
      rangeQueryBuilder.lte(json(formatter.format(endDateWithCorrectTimezone)));
    }
    if (start != null) {
      final OffsetDateTime startDateWithCorrectTimezone =
          start.atZoneSameInstant(timezone).toOffsetDateTime();
      rangeQueryBuilder.gte(json(formatter.format(startDateWithCorrectTimezone)));
    }
    rangeQueryBuilder.format(OPTIMIZE_DATE_FORMAT);
    return Optional.of(rangeQueryBuilder.build().toQuery());
  }

  private static Optional<Query> createRollingDateFilter(
      final RollingDateFilterStartDto startDto, final String dateField, final ZoneId timezone) {
    if (startDto == null || startDto.getUnit() == null || startDto.getValue() == null) {
      return Optional.empty();
    }

    final RangeQuery.Builder rangeQueryBuilder = new RangeQuery.Builder().field(dateField);
    final OffsetDateTime now = LocalDateUtil.getCurrentTimeWithTimezone(timezone);
    rangeQueryBuilder.lte(json(formatter.format(now)));

    if (QUARTERS.equals(startDto.getUnit())) {
      log.warn(
          "Cannot create date filter: {} is not supported for rolling date filters",
          startDto.getUnit());
      throw new OptimizeValidationException(
          String.format("%s is not supported for rolling date filters", startDto.getUnit()));
    }

    final OffsetDateTime dateBeforeGivenFilter =
        now.minus(
            startDto.getValue(), ChronoUnit.valueOf(startDto.getUnit().getId().toUpperCase()));
    rangeQueryBuilder.gte(json(formatter.format(dateBeforeGivenFilter)));
    rangeQueryBuilder.format(OPTIMIZE_DATE_FORMAT);
    return Optional.of(rangeQueryBuilder.build().toQuery());
  }

  private static Optional<Query> createRelativeDateFilter(
      final RelativeDateFilterStartDto startDto, final String dateField, final ZoneId timezone) {
    if (startDto == null || startDto.getUnit() == null || startDto.getValue() == null) {
      return Optional.empty();
    }

    final RangeQuery.Builder rangeQueryBuilder = new RangeQuery.Builder().field(dateField);
    final OffsetDateTime now = LocalDateUtil.getCurrentTimeWithTimezone(timezone);
    if (startDto.getValue() == 0) {
      rangeQueryBuilder.lte(json(formatter.format(now)));
      rangeQueryBuilder.gte(
          json(
              formatter.format(DateFilterUtil.getStartOfCurrentInterval(now, startDto.getUnit()))));
    } else {
      final OffsetDateTime startOfCurrentInterval =
          DateFilterUtil.getStartOfCurrentInterval(now, startDto.getUnit());
      final OffsetDateTime startOfPreviousInterval =
          DateFilterUtil.getStartOfPreviousInterval(
              startOfCurrentInterval, startDto.getUnit(), startDto.getValue());
      rangeQueryBuilder.lt(json(formatter.format(startOfCurrentInterval)));
      rangeQueryBuilder.gte(json(formatter.format(startOfPreviousInterval)));
    }
    rangeQueryBuilder.format(OPTIMIZE_DATE_FORMAT);
    return Optional.of(rangeQueryBuilder.build().toQuery());
  }
}