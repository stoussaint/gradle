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
package org.gradle.plugins.idea

import org.gradle.api.internal.ConventionTask
import org.gradle.api.internal.plugins.ide.DeduplicationTarget
import org.gradle.api.internal.plugins.ide.ProjectDeduper
import org.gradle.api.tasks.TaskAction

/**
 * @author Szczepan Faber, @date 03.03.11
 */
class IdeaConfigurer extends ConventionTask {

    ProjectDeduper deduper = new ProjectDeduper()

    @TaskAction
    void configure() {
        def ideaProjects = project.rootProject.allprojects.findAll { it.plugins.hasPlugin(IdeaPlugin) }
        deduper.dedupe(ideaProjects, { project ->
            new DeduplicationTarget(project: project, moduleName: project.ideaModule.moduleName, moduleNameSetter: { project.ideaModule.moduleName = it } )
        })
    }
}
