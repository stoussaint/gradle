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

/**
 * Represents a facet configuration which will wrap Facet custom properties .
 *
 * @author Stephane Toussaint
 */
class FacetConfiguration {
    /**
     * The name of the facet. Must not be null
     */
    final String name

    final Map properties

    FacetConfiguration(String name) {
        this.name = name
        this.properties = [] as Map
    }

    FacetConfiguration(String name, Map properties) {
        this.name = name
        this.properties = properties
    }

    boolean equals(o) {
        if (this.is(o)) { return true }

        if (o == null || getClass() != o.class) { return false }

        FacetConfiguration facetConfiguration = (FacetConfiguration) o;

        if (name != facetConfiguration.name) { return false }

        return true;
    }

    int hashCode() {
        return name.hashCode();
    }

    public String toString() {
        return "FacetConfiguration{" +
                "name='" + name + "'"
                "}";
    }
}
