/*
 * Copyright 2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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
package org.gradle.execution;

import com.google.common.collect.Multimap;
import org.gradle.api.Task;
import org.gradle.api.internal.GradleInternal;
import org.gradle.api.internal.tasks.options.OptionReader;
import org.gradle.execution.commandline.CommandLineTaskConfigurer;
import org.gradle.execution.commandline.CommandLineTaskParser;
import org.gradle.util.GUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * A {@link BuildConfigurationAction} which selects tasks which match the provided names. For each name, selects all tasks in all
 * projects whose name is the given name.
 */
public class TaskNameResolvingBuildConfigurationAction implements BuildConfigurationAction {
    private static final Logger LOGGER = LoggerFactory.getLogger(TaskNameResolvingBuildConfigurationAction.class);

    public void configure(BuildExecutionContext context) {
        GradleInternal gradle = context.getGradle();
        List<String> taskNames = gradle.getStartParameter().getTaskNames();
        Multimap<String, Task> selectedTasks = doSelect(gradle, taskNames);

        TaskGraphExecuter executer = gradle.getTaskGraph();
        for (String name : selectedTasks.keySet()) {
            executer.addTasks(selectedTasks.get(name));
        }

        if (selectedTasks.keySet().size() == 1) {
            LOGGER.info("Selected primary task {}", GUtil.toString(selectedTasks.keySet()));
        } else {
            LOGGER.info("Selected primary tasks {}", GUtil.toString(selectedTasks.keySet()));
        }

        context.proceed();
    }

    private Multimap<String, Task> doSelect(GradleInternal gradle, List<String> paths) {
        TaskSelector selector = gradle.getServices().get(TaskSelector.class);
        OptionReader optionReader = gradle.getServices().get(OptionReader.class);
        return new CommandLineTaskParser(new CommandLineTaskConfigurer(optionReader)).parseTasks(paths, selector);
    }
}
