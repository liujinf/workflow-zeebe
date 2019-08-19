/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a commercial license.
 * You may not use this file except in compliance with the commercial license.
 */
package org.camunda.optimize.upgrade.steps.schema;

import org.camunda.optimize.service.es.schema.IndexMappingCreator;
import org.camunda.optimize.upgrade.es.ESIndexAdjuster;
import org.camunda.optimize.upgrade.steps.UpgradeStep;


public class UpdateMappingIndexStep implements UpgradeStep {
  private final IndexMappingCreator index;

  public UpdateMappingIndexStep(final IndexMappingCreator index) {
    this.index = index;
  }

  @Override
  public void execute(final ESIndexAdjuster esIndexAdjuster) {
    esIndexAdjuster.updateIndexDynamicSettingsAndMappings(index);
  }
}
