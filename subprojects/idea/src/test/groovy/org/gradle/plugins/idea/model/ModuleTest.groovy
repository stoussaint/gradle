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
package org.gradle.plugins.idea.model

import org.gradle.api.internal.XmlTransformer
import spock.lang.Specification

/**
 * @author Hans Dockter
 */
class ModuleTest extends Specification {
    final PathFactory pathFactory = new PathFactory()
    final XmlTransformer xmlTransformer = new XmlTransformer()
    final customSourceFolders = [path('file://$MODULE_DIR$/src')] as LinkedHashSet
    final customTestSourceFolders = [path('file://$MODULE_DIR$/srcTest')] as LinkedHashSet
    final customExcludeFolders = [path('file://$MODULE_DIR$/target')] as LinkedHashSet
    final customDependencies = [
            new ModuleLibrary([path('file://$MODULE_DIR$/gradle/lib')] as Set,
                    [path('file://$MODULE_DIR$/gradle/javadoc')] as Set, [path('file://$MODULE_DIR$/gradle/src')] as Set,
                    [] as Set, null),
            new ModuleLibrary([path('file://$MODULE_DIR$/ant/lib'), path('jar://$GRADLE_CACHE$/gradle.jar!/')] as Set, [] as Set, [] as Set,
                    [new JarDirectory(path('file://$MODULE_DIR$/ant/lib'), false)] as Set, "RUNTIME"),
            new ModuleDependency('someModule', null)]

    Module module = new Module(xmlTransformer)

    def loadFromReader() {
        when:
        module.load(customModuleReader)

        then:
        module.javaVersion == "1.6"
        module.sourceFolders == customSourceFolders
        module.testSourceFolders == customTestSourceFolders
        module.excludeFolders == customExcludeFolders
        module.outputDir == path('file://$MODULE_DIR$/out')
        module.testOutputDir == path('file://$MODULE_DIR$/outTest')
        (module.dependencies as List) == customDependencies
    }

    def configureOverwritesDependenciesAndAppendsAllOtherEntries() {
        def constructorSourceFolders = [path('a')] as Set
        def constructorTestSourceFolders = [path('b')] as Set
        def constructorExcludeFolders = [path('c')] as Set
        def constructorInheritOutputDirs = false
        def constructorOutputDir = path('someOut')
        def constructorJavaVersion = '1.6'
        def constructorTestOutputDir = path('someTestOut')
        def constructorModuleDependencies = [
                customDependencies[0],
                new ModuleLibrary([path('x')], [], [], [new JarDirectory(path('y'), false)], null)] as LinkedHashSet
	    def constructorFacets = [new FacetConfiguration('web')] as Set

        when:
        module.load(customModuleReader)
        module.configure(null, constructorSourceFolders, constructorTestSourceFolders, constructorExcludeFolders,
                constructorInheritOutputDirs, constructorOutputDir, constructorTestOutputDir, constructorModuleDependencies, constructorJavaVersion, constructorFacets)

        then:
        module.sourceFolders == customSourceFolders + constructorSourceFolders
        module.testSourceFolders == customTestSourceFolders + constructorTestSourceFolders
        module.excludeFolders == customExcludeFolders + constructorExcludeFolders
        module.outputDir == constructorOutputDir
        module.testOutputDir == constructorTestOutputDir
        module.javaVersion == constructorJavaVersion
        module.dependencies == constructorModuleDependencies
        module.facets == constructorFacets
    }

    def loadDefaults() {
        when:
        module.loadDefaults()

        then:
        module.javaVersion == Module.INHERITED
        module.inheritOutputDirs
        module.sourceFolders == [] as Set
        module.dependencies.size() == 0
    }

    def generatedXmlShouldContainCustomValues() {
        def constructorSourceFolders = [new Path('a')] as Set
        def constructorOutputDir = new Path('someOut')
        def constructorTestOutputDir = new Path('someTestOut')

        when:
        module.loadDefaults()
        module.configure(null, constructorSourceFolders, [] as Set, [] as Set, false, constructorOutputDir, constructorTestOutputDir, [] as Set, null, [] as Set)
        def xml = toXmlReader
        def newModule = new Module(xmlTransformer)
        newModule.load(xml)

        then:
        this.module == newModule
    }

    def generatedXmlShouldContainWebFacet() {
        def constructorSourceFolders = [path('file://$MODULE_DIR$/src/main/java'), path('file://$MODULE_DIR$/src/main/resources')] as Set
        def constructorOutputDir = new Path('someOut')
        def constructorTestOutputDir = new Path('someTestOut')
        def constructorFacets = [new FacetConfiguration('web', ['WEBAPP_DIR' : path('file://$MODULE_DIR$/src/main/webapp')])] as Set

        when:
        module.loadDefaults()
        module.configure(null, constructorSourceFolders, [] as Set, [] as Set, false, constructorOutputDir, constructorTestOutputDir, [] as Set, null, constructorFacets)
        def xml = new XmlSlurper().parse(toXmlReader)

        then:
        def facetManager = xml.component.find { it.@name == 'FacetManager' }
        assert facetManager

        def webFacet = facetManager.facet[0]
        assert webFacet.@name == 'Web' && webFacet.@type == 'web'

        def wfConfiguration = webFacet.configuration
       
        assert wfConfiguration.descriptors.deploymentDescriptor
        def deploymentDescriptor = wfConfiguration.descriptors.deploymentDescriptor
        assert deploymentDescriptor.@name == 'web.xml'
        assert deploymentDescriptor.@url == 'file://$MODULE_DIR$/src/main/webapp/WEB-INF/web.xml'

        assert wfConfiguration.webroots.root.@url == 'file://$MODULE_DIR$/src/main/webapp'

        assert wfConfiguration.sourceRoots.root.find { it.@url == 'file://$MODULE_DIR$/src/main/java' }
        assert wfConfiguration.sourceRoots.root.find { it.@url == 'file://$MODULE_DIR$/src/main/resources'}
    }

    private InputStream getToXmlReader() {
        ByteArrayOutputStream toXmlText = new ByteArrayOutputStream()
        module.store(toXmlText)
        return new ByteArrayInputStream(toXmlText.toByteArray())
    }

    private InputStream getCustomModuleReader() {
        return getClass().getResourceAsStream('customModule.xml')
    }

    private Path path(String url) {
        pathFactory.path(url)
    }
}
