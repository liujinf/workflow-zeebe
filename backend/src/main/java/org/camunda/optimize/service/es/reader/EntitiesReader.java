/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package org.camunda.optimize.service.es.reader;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.optimize.dto.optimize.query.collection.BaseCollectionDefinitionDto;
import org.camunda.optimize.dto.optimize.query.collection.CollectionEntity;
import org.camunda.optimize.dto.optimize.query.collection.SimpleCollectionDefinitionDto;
import org.camunda.optimize.dto.optimize.query.entity.EntityDto;
import org.camunda.optimize.dto.optimize.query.entity.EntityNameDto;
import org.camunda.optimize.dto.optimize.query.entity.EntityNameRequestDto;
import org.camunda.optimize.dto.optimize.query.entity.EntityType;
import org.camunda.optimize.service.es.OptimizeElasticsearchClient;
import org.camunda.optimize.service.exceptions.OptimizeRuntimeException;
import org.camunda.optimize.service.util.configuration.ConfigurationService;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.get.MultiGetItemResponse;
import org.elasticsearch.action.get.MultiGetRequest;
import org.elasticsearch.action.get.MultiGetResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.elasticsearch.search.aggregations.bucket.filter.Filter;
import org.elasticsearch.search.aggregations.bucket.filter.FilterAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.stereotype.Component;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.camunda.optimize.service.es.reader.ElasticsearchHelper.atLeastOneResponseExistsForMultiGet;
import static org.camunda.optimize.service.es.reader.ReportReader.REPORT_DATA_XML_PROPERTY;
import static org.camunda.optimize.service.es.schema.index.report.AbstractReportIndex.COLLECTION_ID;
import static org.camunda.optimize.service.es.schema.index.report.AbstractReportIndex.OWNER;
import static org.camunda.optimize.upgrade.es.ElasticsearchConstants.COLLECTION_INDEX_NAME;
import static org.camunda.optimize.upgrade.es.ElasticsearchConstants.COMBINED_REPORT_INDEX_NAME;
import static org.camunda.optimize.upgrade.es.ElasticsearchConstants.DASHBOARD_INDEX_NAME;
import static org.camunda.optimize.upgrade.es.ElasticsearchConstants.LIST_FETCH_LIMIT;
import static org.camunda.optimize.upgrade.es.ElasticsearchConstants.SINGLE_DECISION_REPORT_INDEX_NAME;
import static org.camunda.optimize.upgrade.es.ElasticsearchConstants.SINGLE_PROCESS_REPORT_INDEX_NAME;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.existsQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;
import static org.elasticsearch.index.query.QueryBuilders.termsQuery;
import static org.elasticsearch.search.aggregations.AggregationBuilders.filter;
import static org.elasticsearch.search.aggregations.AggregationBuilders.terms;

@RequiredArgsConstructor
@Component
@Slf4j
public class EntitiesReader {

  private static final String AGG_BY_TYPE_COUNT = "byTypeCount";
  private static final String[] ENTITY_LIST_EXCLUDES = {REPORT_DATA_XML_PROPERTY};
  private final OptimizeElasticsearchClient esClient;
  private final ConfigurationService configurationService;
  private final ObjectMapper objectMapper;

  public List<EntityDto> getAllPrivateEntities(final String userId) {
    log.debug("Fetching all available entities for user [{}]", userId);

    final QueryBuilder query = boolQuery().must(termQuery(OWNER, userId))
      .mustNot(existsQuery(COLLECTION_ID));

    SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
      .query(query)
      .size(LIST_FETCH_LIMIT)
      .fetchSource(null, ENTITY_LIST_EXCLUDES);
    SearchRequest searchRequest =
      createReportAndDashboardSearchRequest()
        .source(searchSourceBuilder)
        .scroll(new TimeValue(configurationService.getElasticsearchScrollTimeout()));

    SearchResponse scrollResp;
    try {
      scrollResp = esClient.search(searchRequest, RequestOptions.DEFAULT);
    } catch (IOException e) {
      log.error("Was not able to retrieve private entities!", e);
      throw new OptimizeRuntimeException("Was not able to retrieve private entities!", e);
    }

    return ElasticsearchHelper.retrieveAllScrollResults(
      scrollResp,
      CollectionEntity.class,
      objectMapper,
      esClient,
      configurationService.getElasticsearchScrollTimeout()
    ).stream().map(CollectionEntity::toEntityDto).collect(Collectors.toList());
  }

  public Map<String, Map<EntityType, Long>> countEntitiesForCollections(
    final List<? extends BaseCollectionDefinitionDto> collections) {
    log.debug(
      "Counting all available entities for collection ids [{}]",
      collections.stream().map(BaseCollectionDefinitionDto::getId).collect(Collectors.toList())
    );

    if (collections.size() == 0) {
      return new HashMap<>();
    }

    final SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
      .query(termsQuery(
        COLLECTION_ID, collections.stream().map(BaseCollectionDefinitionDto::getId).collect(Collectors.toList())
      ))
      .size(0);

    collections.forEach(collection -> {
      final String collectionId = collection.getId();
      final FilterAggregationBuilder byCollectionIdFilterAggregation =
        filter(collectionId, boolQuery().filter(termQuery(COLLECTION_ID, collectionId)));
      searchSourceBuilder.aggregation(byCollectionIdFilterAggregation);
      final TermsAggregationBuilder byEntityTypeAggregation = terms(AGG_BY_TYPE_COUNT).field("_type");
      byCollectionIdFilterAggregation.subAggregation(byEntityTypeAggregation);
    });

    final SearchRequest searchRequest = createReportAndDashboardSearchRequest().source(searchSourceBuilder);

    try {
      final SearchResponse searchResponse = esClient.search(searchRequest, RequestOptions.DEFAULT);
      return searchResponse.getAggregations().asList().stream()
        .map(agg -> (Filter) agg)
        .map(collectionFilterAggregation -> new AbstractMap.SimpleEntry<>(
          collectionFilterAggregation.getName(), extractEntityTypeCounts(collectionFilterAggregation)
        ))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    } catch (IOException e) {
      throw new OptimizeRuntimeException("Was not able to count collection entities!", e);
    }
  }

  public List<CollectionEntity> getAllEntitiesForCollection(final SimpleCollectionDefinitionDto collection) {
    log.debug("Fetching all available entities for collection [{}]", collection.getId());
    final SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
      .query(termQuery(COLLECTION_ID, collection.getId()))
      .size(LIST_FETCH_LIMIT);
    return runEntitiesSearchRequest(searchSourceBuilder);
  }

  public EntityNameDto getEntityNames(final EntityNameRequestDto requestDto) {
    log.debug(String.format("Performing get entity names search request %s", requestDto.toString()));
    MultiGetResponse multiGetItemResponse = runGetEntityNamesRequest(requestDto);
    if (!atLeastOneResponseExistsForMultiGet(multiGetItemResponse)) {
      String reason = String.format("Could not get entity names search request %s", requestDto.toString());
      throw new NotFoundException(reason);
    }

    EntityNameDto result = new EntityNameDto();
    for (MultiGetItemResponse itemResponse : multiGetItemResponse) {
      GetResponse response = itemResponse.getResponse();
      if (response.isExists()) {
        String entityId = response.getId();
        CollectionEntity entity = readCollectionEntity(response, entityId);
        if (entityId.equals(requestDto.getCollectionId())) {
          result.setCollectionName(entity.getName());
        } else if (entityId.equals(requestDto.getDashboardId())) {
          result.setDashboardName(entity.getName());
        } else if (entityId.equals(requestDto.getReportId())) {
          result.setReportName(entity.getName());
        }
      }
    }
    return result;
  }

  private Map<EntityType, Long> extractEntityTypeCounts(final Filter collectionFilterAggregation) {
    final Terms byTypeTerms = collectionFilterAggregation.getAggregations().get(AGG_BY_TYPE_COUNT);
    final long singleProcessReportCount = getDocCountForIndex(byTypeTerms, SINGLE_PROCESS_REPORT_INDEX_NAME);
    final long combinedProcessReportCount = getDocCountForIndex(byTypeTerms, COMBINED_REPORT_INDEX_NAME);
    final long singleDecisionReportCount = getDocCountForIndex(byTypeTerms, SINGLE_DECISION_REPORT_INDEX_NAME);
    final long dashboardCount = getDocCountForIndex(byTypeTerms, DASHBOARD_INDEX_NAME);
    return ImmutableMap.of(
      EntityType.DASHBOARD, dashboardCount,
      EntityType.REPORT, singleProcessReportCount + singleDecisionReportCount + combinedProcessReportCount
    );
  }

  private long getDocCountForIndex(final Terms byTypeTerms, final String singleProcessReportIndexName) {
    return Optional.ofNullable(byTypeTerms.getBucketByKey(singleProcessReportIndexName))
      .map(MultiBucketsAggregation.Bucket::getDocCount)
      .orElse(0L);
  }

  private CollectionEntity readCollectionEntity(final GetResponse response, final String entityId) {
    CollectionEntity entity;
    try {
      entity = objectMapper.readValue(response.getSourceAsString(), CollectionEntity.class);
    } catch (IOException e) {
      String reason = String.format("Can't read collection entity with id [%s].", entityId);
      throw new OptimizeRuntimeException(reason, e);
    }
    return entity;
  }

  private MultiGetResponse runGetEntityNamesRequest(EntityNameRequestDto requestDto) {
    MultiGetRequest request = new MultiGetRequest();
    addGetEntityToRequest(request, requestDto.getReportId(), SINGLE_PROCESS_REPORT_INDEX_NAME);
    addGetEntityToRequest(request, requestDto.getReportId(), SINGLE_DECISION_REPORT_INDEX_NAME);
    addGetEntityToRequest(request, requestDto.getReportId(), COMBINED_REPORT_INDEX_NAME);
    addGetEntityToRequest(request, requestDto.getDashboardId(), DASHBOARD_INDEX_NAME);
    addGetEntityToRequest(request, requestDto.getCollectionId(), COLLECTION_INDEX_NAME);
    if (request.getItems().isEmpty()) {
      throw new BadRequestException("No ids for entity name request provided");
    }

    MultiGetResponse multiGetItemResponses;
    try {
      multiGetItemResponses = esClient.mget(request, RequestOptions.DEFAULT);
    } catch (IOException e) {
      String reason = String.format("Could not get entity names search request %s", requestDto.toString());
      log.error(reason, e);
      throw new OptimizeRuntimeException(reason, e);
    }
    return multiGetItemResponses;
  }

  private void addGetEntityToRequest(final MultiGetRequest request, final String entityId,
                                     final String entityIndexName) {
    if (entityId != null) {
      request.add(new MultiGetRequest.Item(
        entityIndexName,
        entityIndexName,
        entityId
      ));
    }
  }

  private List<CollectionEntity> runEntitiesSearchRequest(final SearchSourceBuilder searchSourceBuilder) {
    SearchRequest searchRequest = createReportAndDashboardSearchRequest()
      .source(searchSourceBuilder)
      .scroll(new TimeValue(configurationService.getElasticsearchScrollTimeout()));

    SearchResponse scrollResp;
    try {
      scrollResp = esClient.search(searchRequest, RequestOptions.DEFAULT);
    } catch (IOException e) {
      log.error("Was not able to retrieve collection entities!", e);
      throw new OptimizeRuntimeException("Was not able to retrieve entities!", e);
    }

    return ElasticsearchHelper.retrieveAllScrollResults(
      scrollResp,
      CollectionEntity.class,
      objectMapper,
      esClient,
      configurationService.getElasticsearchScrollTimeout()
    );
  }

  private SearchRequest createReportAndDashboardSearchRequest() {
    return new SearchRequest(
      SINGLE_PROCESS_REPORT_INDEX_NAME,
      SINGLE_DECISION_REPORT_INDEX_NAME,
      COMBINED_REPORT_INDEX_NAME,
      DASHBOARD_INDEX_NAME
    );
  }
}
