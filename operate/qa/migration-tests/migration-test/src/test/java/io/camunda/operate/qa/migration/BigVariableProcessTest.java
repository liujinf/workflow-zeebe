/*
 * Copyright Camunda Services GmbH
 *
 * BY INSTALLING, DOWNLOADING, ACCESSING, USING, OR DISTRIBUTING THE SOFTWARE (“USE”), YOU INDICATE YOUR ACCEPTANCE TO AND ARE ENTERING INTO A CONTRACT WITH, THE LICENSOR ON THE TERMS SET OUT IN THIS AGREEMENT. IF YOU DO NOT AGREE TO THESE TERMS, YOU MUST NOT USE THE SOFTWARE. IF YOU ARE RECEIVING THE SOFTWARE ON BEHALF OF A LEGAL ENTITY, YOU REPRESENT AND WARRANT THAT YOU HAVE THE ACTUAL AUTHORITY TO AGREE TO THE TERMS AND CONDITIONS OF THIS AGREEMENT ON BEHALF OF SUCH ENTITY.
 * “Licensee” means you, an individual, or the entity on whose behalf you receive the Software.
 *
 * Permission is hereby granted, free of charge, to the Licensee obtaining a copy of this Software and associated documentation files to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject in each case to the following conditions:
 * Condition 1: If the Licensee distributes the Software or any derivative works of the Software, the Licensee must attach this Agreement.
 * Condition 2: Without limiting other conditions in this Agreement, the grant of rights is solely for non-production use as defined below.
 * "Non-production use" means any use of the Software that is not directly related to creating products, services, or systems that generate revenue or other direct or indirect economic benefits.  Examples of permitted non-production use include personal use, educational use, research, and development. Examples of prohibited production use include, without limitation, use for commercial, for-profit, or publicly accessible systems or use for commercial or revenue-generating purposes.
 *
 * If the Licensee is in breach of the Conditions, this Agreement, including the rights granted under it, will automatically terminate with immediate effect.
 *
 * SUBJECT AS SET OUT BELOW, THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 * NOTHING IN THIS AGREEMENT EXCLUDES OR RESTRICTS A PARTY’S LIABILITY FOR (A) DEATH OR PERSONAL INJURY CAUSED BY THAT PARTY’S NEGLIGENCE, (B) FRAUD, OR (C) ANY OTHER LIABILITY TO THE EXTENT THAT IT CANNOT BE LAWFULLY EXCLUDED OR RESTRICTED.
 */
package io.camunda.operate.qa.migration;

import static io.camunda.operate.qa.migration.util.TestConstants.DEFAULT_TENANT_ID;
import static io.camunda.operate.qa.util.VariablesUtil.VAR_SUFFIX;
import static org.assertj.core.api.Assertions.assertThat;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;
import static org.elasticsearch.index.query.QueryBuilders.termsQuery;

import io.camunda.operate.entities.ProcessEntity;
import io.camunda.operate.entities.VariableEntity;
import io.camunda.operate.property.ImportProperties;
import io.camunda.operate.qa.migration.util.AbstractMigrationTest;
import io.camunda.operate.qa.migration.util.EntityReader;
import io.camunda.operate.qa.migration.v100.BigVariableDataGenerator;
import io.camunda.operate.schema.templates.EventTemplate;
import io.camunda.operate.schema.templates.VariableTemplate;
import io.camunda.operate.util.ThreadUtil;
import java.util.List;
import org.assertj.core.api.Condition;
import org.elasticsearch.action.search.SearchRequest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class BigVariableProcessTest extends AbstractMigrationTest {

  private String bpmnProcessId = BigVariableDataGenerator.PROCESS_BPMN_PROCESS_ID;

  @Autowired private EntityReader entityReader;

  @Test
  public void testBigVariablesHasPreviewAndFullValue() {
    assumeThatProcessIsUnderTest(bpmnProcessId);

    ThreadUtil.sleepFor(5_000);
    final SearchRequest searchRequest = new SearchRequest(variableTemplate.getAlias());
    searchRequest
        .source()
        .query(
            termsQuery(
                VariableTemplate.NAME,
                bpmnProcessId + "_var0",
                bpmnProcessId + "_var1",
                bpmnProcessId + "_var2"));
    final List<VariableEntity> vars =
        entityReader.searchEntitiesFor(searchRequest, VariableEntity.class);

    assertThat(vars).hasSize(3);
    // then "value" contains truncated value
    final Condition<String> suffix = new Condition<>(s -> s.contains(VAR_SUFFIX), "contains");
    final Condition<String> length =
        new Condition<>(
            s -> s.length() == ImportProperties.DEFAULT_VARIABLE_SIZE_THRESHOLD, "length");
    final Condition<String> lengthGt =
        new Condition<>(
            s -> s.length() > ImportProperties.DEFAULT_VARIABLE_SIZE_THRESHOLD, "length");
    assertThat(vars).extracting(VariableEntity::getValue).areNot(suffix).are(length);
    assertThat(vars).extracting(VariableEntity::getFullValue).are(suffix).are(lengthGt);
    assertThat(vars).extracting(VariableEntity::getIsPreview).containsOnly(true);
  }

  @Test
  public void testProcess() {
    final SearchRequest searchRequest = new SearchRequest(processTemplate.getAlias());
    searchRequest.source().query(termQuery(EventTemplate.BPMN_PROCESS_ID, bpmnProcessId));
    final List<ProcessEntity> processes =
        entityReader.searchEntitiesFor(searchRequest, ProcessEntity.class);
    assertThat(processes).hasSize(1);
    assertThat(processes.get(0).getTenantId()).isEqualTo(DEFAULT_TENANT_ID);
  }
}