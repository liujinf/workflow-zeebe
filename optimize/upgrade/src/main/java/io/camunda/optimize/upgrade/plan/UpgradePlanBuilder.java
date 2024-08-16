/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.optimize.upgrade.plan;

import com.vdurmont.semver4j.Semver;
import io.camunda.optimize.upgrade.steps.UpgradeStep;
import java.util.List;

public class UpgradePlanBuilder {

  public static AddFromVersionBuilder createUpgradePlan() {
    UpgradePlan upgradePlan = new UpgradePlan();
    return new UpgradePlanBuilder().startUpgradePlanBuild(upgradePlan);
  }

  private AddFromVersionBuilder startUpgradePlanBuild(UpgradePlan upgradePlan) {
    return new AddFromVersionBuilder(upgradePlan);
  }

  public static class AddFromVersionBuilder {

    private final UpgradePlan upgradePlan;

    public AddFromVersionBuilder(UpgradePlan upgradePlan) {
      this.upgradePlan = upgradePlan;
    }

    public AddToVersionBuilder fromVersion(String fromVersion) {
      // LOOSE to allow for missing patch if an update applies to all patches of a minor
      upgradePlan.setFromVersion(new Semver(fromVersion, Semver.SemverType.LOOSE));
      return new AddToVersionBuilder(upgradePlan);
    }
  }

  public static class AddToVersionBuilder {
    private final UpgradePlan upgradePlan;

    public AddToVersionBuilder(UpgradePlan upgradePlan) {
      this.upgradePlan = upgradePlan;
    }

    public AddUpgradeStepBuilder toVersion(String toVersion) {
      upgradePlan.setToVersion(new Semver(toVersion));
      return new AddUpgradeStepBuilder(upgradePlan);
    }
  }

  public static class AddUpgradeStepBuilder {

    private final UpgradePlan upgradePlan;

    public AddUpgradeStepBuilder(UpgradePlan upgradePlan) {
      this.upgradePlan = upgradePlan;
    }

    public AddUpgradeStepBuilder addUpgradeStep(UpgradeStep upgradeStep) {
      upgradePlan.addUpgradeStep(upgradeStep);
      return this;
    }

    public AddUpgradeStepBuilder addUpgradeSteps(List<? extends UpgradeStep> upgradeSteps) {
      upgradePlan.addUpgradeSteps(upgradeSteps);
      return this;
    }

    public UpgradePlan build() {
      return upgradePlan;
    }
  }
}