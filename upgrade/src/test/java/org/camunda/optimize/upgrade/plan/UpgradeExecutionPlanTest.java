/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package org.camunda.optimize.upgrade.plan;

import org.camunda.optimize.service.es.schema.ElasticSearchSchemaManager;
import org.camunda.optimize.service.es.schema.ElasticsearchMetadataService;
import org.camunda.optimize.service.es.schema.IndexMappingCreator;
import org.camunda.optimize.upgrade.es.ESIndexAdjuster;
import org.camunda.optimize.upgrade.service.ValidationService;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class UpgradeExecutionPlanTest {

  @Test
  public void testGetMappings() {
    final UpgradeExecutionPlan underTest = new UpgradeExecutionPlan();
    final List<IndexMappingCreator> mappings = underTest.getMappings();

    assertThat(mappings.size(), is(18));
  }

  @Test
  public void testInitializeSchemaIsCalled() {
    final UpgradeExecutionPlan underTest = new UpgradeExecutionPlan();
    underTest.setFromVersion("1");
    underTest.setToVersion("2");
    final ElasticSearchSchemaManager schemaManager = Mockito.mock(ElasticSearchSchemaManager.class);
    underTest.setEsIndexAdjuster(Mockito.mock(ESIndexAdjuster.class));
    underTest.setSchemaManager(schemaManager);
    underTest.setValidationService(Mockito.mock(ValidationService.class));
    underTest.setMetadataService(Mockito.mock(ElasticsearchMetadataService.class));

    underTest.execute();

    verify(schemaManager, times(1)).initializeSchema(any());
  }
}
