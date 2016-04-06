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
package org.camunda.bpm.engine.impl.jobexecutor;

import org.camunda.bpm.engine.ProcessEngineException;
import org.camunda.bpm.engine.impl.DefaultPriorityProvider;
import org.camunda.bpm.engine.impl.ProcessEngineLogger;
import org.camunda.bpm.engine.impl.bpmn.parser.BpmnParse;
import org.camunda.bpm.engine.impl.context.Context;
import org.camunda.bpm.engine.impl.core.variable.mapping.value.ParameterValueProvider;
import org.camunda.bpm.engine.impl.persistence.entity.ExecutionEntity;
import org.camunda.bpm.engine.impl.persistence.entity.JobDefinitionEntity;
import org.camunda.bpm.engine.impl.pvm.process.ProcessDefinitionImpl;


/**
 * @author Thorben Lindhauer
 *
 */
public class DefaultJobPriorityProvider extends DefaultPriorityProvider<JobDeclaration<?, ?>> {

  private final static JobExecutorLogger LOG = ProcessEngineLogger.JOB_EXECUTOR_LOGGER;

  @Override
  public long determinePriority(ExecutionEntity execution, JobDeclaration<?, ?> jobDeclaration) {

    Long jobDefinitionPriority = getJobDefinitionPriority(execution, jobDeclaration);
    if (jobDefinitionPriority != null) {
      return jobDefinitionPriority;
    }

    Long activityPriority = getActivityPriority(execution, jobDeclaration);
    if (activityPriority != null) {
      return activityPriority;
    }

    Long processDefinitionPriority = getProcessDefinitionPriority(execution, jobDeclaration);
    if (processDefinitionPriority != null) {
      return processDefinitionPriority;
    }

    return getDefaultPriority();
  }

  protected Long getJobDefinitionPriority(ExecutionEntity execution, JobDeclaration<?, ?> jobDeclaration) {
    JobDefinitionEntity jobDefinition = getJobDefinitionFor(jobDeclaration);

    if (jobDefinition != null) {
      return jobDefinition.getOverridingJobPriority();
    }

    return null;
  }

  protected Long getProcessDefinitionPriority(ExecutionEntity execution, JobDeclaration<?, ?> jobDeclaration) {
    ProcessDefinitionImpl processDefinition = jobDeclaration.getProcessDefinition();

    if (processDefinition != null) {
      ParameterValueProvider priorityProvider = (ParameterValueProvider) processDefinition.getProperty(BpmnParse.PROPERTYNAME_JOB_PRIORITY);

      if (priorityProvider != null) {
        return evaluateValueProvider(priorityProvider, execution, describeContext(jobDeclaration, execution));
      }
    }

    return null;
  }

  protected JobDefinitionEntity getJobDefinitionFor(JobDeclaration<?, ?> jobDeclaration) {
    if (jobDeclaration.getJobDefinitionId() != null) {
      return Context.getCommandContext()
          .getJobDefinitionManager()
          .findById(jobDeclaration.getJobDefinitionId());
    }
    else {
      return null;
    }
  }

  protected Long getActivityPriority(ExecutionEntity execution, JobDeclaration<?, ?> jobDeclaration) {
    if (jobDeclaration != null) {
      ParameterValueProvider priorityProvider = jobDeclaration.getJobPriorityProvider();
      if (priorityProvider != null) {
        return evaluateValueProvider(priorityProvider, execution, describeContext(jobDeclaration, execution));
      }
    }

    return null;
  }

  @Override
  protected void logNotDeterminingPriority(ExecutionEntity execution, Object value, ProcessEngineException e) {
    LOG.couldNotDeterminePriority(execution, value, e);
  }
  
  protected String describeContext(JobDeclaration<?, ?> jobDeclaration, ExecutionEntity executionEntity) {
    return "Job " + jobDeclaration.getActivityId()
            + "/" + jobDeclaration.getJobHandlerType() + " instantiated "
            + "in context of " + executionEntity;
  }
}
