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

import org.gradle.api.Project
import org.gradle.api.internal.project.DefaultProject
import org.gradle.util.HelperUtil
import spock.lang.Specification

/**
 * @author Szczepan Faber, @date 03.03.11
 */
class IdeaModuleTest extends Specification {

    DefaultProject project = HelperUtil.createRootProject()
    Project childProject = HelperUtil.createChildProject(project, "child", new File("."))
    Project grandChildProject = HelperUtil.createChildProject(childProject, "grandChild", new File("."))
    IdeaPlugin ideaPlugin = new IdeaPlugin()

    def "moduleName controls outputFile"() {
        given:
        applyPluginToProjects()
        assert childProject.ideaModule.moduleName == "child"
        def existingOutputFolder = childProject.ideaModule.outputFile.parentFile

        when:
        childProject.ideaModule.moduleName = "foo"

        then:
        childProject.ideaModule.moduleName == "foo"
        childProject.ideaModule.outputFile.name == "foo.iml"
        childProject.ideaModule.outputFile.parentFile == existingOutputFolder
    }

    def "prepares web facet"() {
        given:
        applyPluginToProjects()

        when:
        childProject.apply plugin: 'war'

        then:
        assert childProject.ideaModule.facets.find { it.name == 'web' }
    }

    private applyPluginToProjects() {
        ideaPlugin.apply(project)
        ideaPlugin.apply(childProject)
        ideaPlugin.apply(grandChildProject)
    }
}
