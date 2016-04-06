/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.camunda.bpm.engine.impl.bpmn.behavior;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.impl.core.variable.mapping.value.ParameterValueProvider;
import org.camunda.bpm.engine.impl.externaltask.DefaultExternalTaskPriorityProvider;
import org.camunda.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.camunda.bpm.engine.impl.persistence.entity.ExternalTaskEntity;
import org.camunda.bpm.engine.impl.pvm.delegate.ActivityExecution;

/**
 * Implements behavior of external task activities, i.e. all service-task-like
 * activities that have camunda:type="external".
 *
 * @author Thorben Lindhauer
 * @author Christopher Zell
 */
public class ExternalTaskActivityBehavior extends AbstractBpmnActivityBehavior {

  protected String topicName;
  protected ParameterValueProvider priorityValueProvider;

  public ExternalTaskActivityBehavior(String topicName, ParameterValueProvider paramValueProvider) {
    this.topicName = topicName;
    this.priorityValueProvider = paramValueProvider;    
  }

  @Override
  public void execute(ActivityExecution execution) throws Exception {
    ExecutionEntity executionEntity = (ExecutionEntity) execution;
    DefaultExternalTaskPriorityProvider provider = new DefaultExternalTaskPriorityProvider();      
    long priority = provider.determinePriority(executionEntity, this);
    ExternalTaskEntity.createAndInsert(executionEntity, topicName, priority);
  }

  @Override
  public void signal(ActivityExecution execution, String signalName, Object signalData) throws Exception {
    leave(execution);
  }

  public ParameterValueProvider getPriorityValueProvider() {
    return priorityValueProvider;
  }  
  
  /**
   * Overrides the propagateBpmnError method to made it public.
   * Is used to propagate the bpmn error from an external task.
   * @param error the error which should be propagated
   * @param execution the current activity execution
   * @throws Exception throwsn an exception if no handler was found
   */
  @Override  
  public void propagateBpmnError(BpmnError error, ActivityExecution execution) throws Exception {
    super.propagateBpmnError(error, execution);
  }
}
