/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.webapps.schema.entities.tasklist;

import java.util.Objects;

public class FormEntity extends TasklistEntity<FormEntity> {
  private String bpmnId;
  private String schema;
  private Long version;
  private Boolean isDeleted;

  public FormEntity() {}

  public FormEntity(
      final long key,
      final String tenantId,
      final String bpmnId,
      final String schema,
      final Long version,
      final Boolean isDeleted) {
    super(String.valueOf(key), key, tenantId);
    this.bpmnId = bpmnId;
    this.schema = schema;
    this.version = version;
    this.isDeleted = isDeleted;
  }

  public String getBpmnId() {
    return bpmnId;
  }

  public void setBpmnId(final String bpmnId) {
    this.bpmnId = bpmnId;
  }

  public String getSchema() {
    return schema;
  }

  public void setSchema(final String schema) {
    this.schema = schema;
  }

  public Long getVersion() {
    return version;
  }

  public void setVersion(final Long version) {
    this.version = version;
  }

  public Boolean getIsDeleted() {
    return isDeleted;
  }

  public void setIsDeleted(final Boolean deleted) {
    isDeleted = deleted;
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), bpmnId, schema, version, isDeleted);
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    final FormEntity that = (FormEntity) o;
    return Objects.equals(bpmnId, that.bpmnId)
        && Objects.equals(schema, that.schema)
        && Objects.equals(version, that.version)
        && Objects.equals(isDeleted, that.isDeleted);
  }

  @Override
  public String toString() {
    return "FormEntity{"
        + "bpmnId='"
        + bpmnId
        + '\''
        + ", schema='"
        + schema
        + '\''
        + ", version="
        + version
        + ", isDeleted="
        + isDeleted
        + '}';
  }
}