/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under one or more contributor license agreements.
 * Licensed under a proprietary license. See the License.txt file for more information.
 * You may not use this file except in compliance with the proprietary license.
 */
package org.camunda.optimize.service.process;

import org.camunda.optimize.AbstractIT;
import org.camunda.optimize.dto.engine.definition.ProcessDefinitionEngineDto;
import org.camunda.optimize.dto.optimize.IdentityDto;
import org.camunda.optimize.dto.optimize.IdentityType;
import org.camunda.optimize.dto.optimize.query.processoverview.ProcessOverviewResponseDto;
import org.camunda.optimize.dto.optimize.query.processoverview.ProcessOwnerDto;
import org.camunda.optimize.dto.optimize.query.processoverview.ProcessOwnerResponseDto;
import org.camunda.optimize.dto.optimize.rest.sorting.ProcessOverviewSorter;
import org.camunda.optimize.exception.OptimizeIntegrationTestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import javax.ws.rs.core.Response;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.camunda.optimize.rest.RestTestConstants.DEFAULT_USERNAME;
import static org.camunda.optimize.service.util.importing.EngineConstants.RESOURCE_TYPE_PROCESS_DEFINITION;
import static org.camunda.optimize.service.util.importing.EngineConstants.RESOURCE_TYPE_USER;
import static org.camunda.optimize.test.engine.AuthorizationClient.KERMIT_USER;
import static org.camunda.optimize.util.BpmnModels.getSimpleBpmnDiagram;

public class ProcessOwnerIT extends AbstractIT {

  protected static final String DEF_KEY = "def_key";
  private ProcessOwnerDto processOwnerDto = new ProcessOwnerDto("DEFAULT_USERNAME");

  @Test
  public void setProcessOwner_notPossibleForUnauthenticatedUser() {
    // when
    Response response = embeddedOptimizeExtension.getRequestExecutor()
      .buildSetProcessOwnerRequest(DEF_KEY, new ProcessOwnerDto(DEFAULT_USERNAME))
      .withoutAuthentication()
      .execute();

    // then
    assertThat(response.getStatus()).isEqualTo(Response.Status.UNAUTHORIZED.getStatusCode());
  }

  @Test
  public void setProcessOwner_noDefinitionExistsForKey() {
    // when
    Response response = embeddedOptimizeExtension.getRequestExecutor()
      .buildSetProcessOwnerRequest(DEF_KEY, new ProcessOwnerDto(DEFAULT_USERNAME))
      .execute();

    // then
    assertThat(response.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
  }

  private static Stream<ProcessOwnerDto> validOwners() {
    return Stream.of(new ProcessOwnerDto(null), new ProcessOwnerDto(DEFAULT_USERNAME));
  }

  @ParameterizedTest
  @MethodSource("validOwners")
  public void setProcessOwner_validOwnerSetting(final ProcessOwnerDto ownerDto) {
    // given
    deploySimpleProcessDefinition(DEF_KEY);
    importAllEngineEntitiesFromScratch();

    // when
    Response response = embeddedOptimizeExtension.getRequestExecutor()
      .buildSetProcessOwnerRequest(DEF_KEY, ownerDto)
      .execute();

    // then
    assertThat(response.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
    assertExpectedProcessOwner(DEF_KEY, ownerDto.getId());
  }

  @Test
  public void setProcessOwner_replaceOwnerAlreadyExists() {
    // given
    authorizationClient.addKermitUserAndGrantAccessToOptimize();
    deploySimpleProcessDefinition(DEF_KEY);
    importAllEngineEntitiesFromScratch();
    setProcessOwner(DEF_KEY, processOwnerDto);

    // when
    Response response = embeddedOptimizeExtension.getRequestExecutor()
      .buildSetProcessOwnerRequest(DEF_KEY, new ProcessOwnerDto(DEFAULT_USERNAME))
      .execute();

    // then
    assertThat(response.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
    assertExpectedProcessOwner(DEF_KEY, DEFAULT_USERNAME);
  }

  @Test
  public void setProcessOwner_removeExistingOwner() {
    // given
    deploySimpleProcessDefinition(DEF_KEY);
    importAllEngineEntitiesFromScratch();
    setProcessOwner(DEF_KEY, processOwnerDto);

    // when
    Response response = embeddedOptimizeExtension.getRequestExecutor()
      .buildSetProcessOwnerRequest(DEF_KEY, new ProcessOwnerDto(null))
      .execute();

    // then
    assertThat(response.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
    assertExpectedProcessOwner(DEF_KEY, null);
  }

  @Test
  public void setProcessOwner_invalidOwner() {
    // given
    deploySimpleProcessDefinition(DEF_KEY);
    importAllEngineEntitiesFromScratch();
    setProcessOwner(DEF_KEY, processOwnerDto);

    // when
    Response response = embeddedOptimizeExtension.getRequestExecutor()
      .buildSetProcessOwnerRequest(DEF_KEY, null)
      .execute();

    // then
    assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
  }

  @Test
  public void setProcessOwner_eventBasedProcess() {
    // given
    elasticSearchIntegrationTestExtension.addEventProcessDefinitionDtoToElasticsearch(
      DEF_KEY, new IdentityDto(DEFAULT_USERNAME, IdentityType.USER));

    // when
    Response response = embeddedOptimizeExtension.getRequestExecutor()
      .buildSetProcessOwnerRequest(DEF_KEY, new ProcessOwnerDto(DEFAULT_USERNAME))
      .execute();

    // then
    assertThat(response.getStatus()).isEqualTo(Response.Status.NO_CONTENT.getStatusCode());
    assertExpectedProcessOwner(DEF_KEY, DEFAULT_USERNAME);
  }

  @Test
  public void setProcessOwner_notAuthorizedToProcess() {
    // given
    engineIntegrationExtension.addUser(KERMIT_USER, KERMIT_USER);
    engineIntegrationExtension.grantUserOptimizeAccess(KERMIT_USER);
    final String defKey = "notAuthorized";
    deploySimpleProcessDefinition(defKey);
    importAllEngineEntitiesFromScratch();

    // when
    Response response = embeddedOptimizeExtension.getRequestExecutor()
      .buildSetProcessOwnerRequest(defKey, new ProcessOwnerDto(DEFAULT_USERNAME))
      .withUserAuthentication(KERMIT_USER, KERMIT_USER)
      .execute();

    // then
    assertThat(response.getStatus()).isEqualTo(Response.Status.FORBIDDEN.getStatusCode());
  }

  @Test
  public void setProcessOwner_notAuthorizedToProcessOwner() {
    // given
    final String defKey = "notAuthorized";
    deploySimpleProcessDefinition(defKey);
    authorizationClient.addKermitUserAndGrantAccessToOptimize();
    authorizationClient.addGlobalAuthorizationForResource(RESOURCE_TYPE_PROCESS_DEFINITION);
    authorizationClient.revokeSingleResourceAuthorizationsForKermit(DEFAULT_USERNAME, RESOURCE_TYPE_USER);
    importAllEngineEntitiesFromScratch();

    // when
    Response response = embeddedOptimizeExtension.getRequestExecutor()
      .buildSetProcessOwnerRequest(defKey, new ProcessOwnerDto(DEFAULT_USERNAME))
      .withUserAuthentication(KERMIT_USER, KERMIT_USER)
      .execute();

    // then
    assertThat(response.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
  }

  private ProcessDefinitionEngineDto deploySimpleProcessDefinition(String processDefinitionKey) {
    return engineIntegrationExtension.deployProcessAndGetProcessDefinition(getSimpleBpmnDiagram(processDefinitionKey));
  }

  private void assertExpectedProcessOwner(final String defKey, final String expectedOwnerId) {
    assertThat(getProcessOverView(null))
      .filteredOn(def -> def.getProcessDefinitionKey().equals(defKey))
      .extracting(ProcessOverviewResponseDto::getOwner)
      .singleElement()
      .satisfies(processOwner -> assertThat(processOwner)
        .isEqualTo(expectedOwnerId == null ? new ProcessOwnerResponseDto()
                     : new ProcessOwnerResponseDto(expectedOwnerId, embeddedOptimizeExtension.getIdentityService()
          .getIdentityNameById(expectedOwnerId)
          .orElseThrow(() -> new OptimizeIntegrationTestException("Could not find default user in cache")))));
  }

  protected List<ProcessOverviewResponseDto> getProcessOverView(final ProcessOverviewSorter processOverviewSorter) {
    return embeddedOptimizeExtension.getRequestExecutor()
      .buildGetProcessOverviewRequest(processOverviewSorter)
      .executeAndReturnList(ProcessOverviewResponseDto.class, Response.Status.OK.getStatusCode());
  }

  protected void setProcessOwner(final String processDefKey, final ProcessOwnerDto processOwnerDto) {
    embeddedOptimizeExtension.getRequestExecutor()
      .buildSetProcessOwnerRequest(processDefKey, processOwnerDto)
      .execute();
  }

}