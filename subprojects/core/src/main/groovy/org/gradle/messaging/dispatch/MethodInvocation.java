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
package org.gradle.messaging.dispatch;

import java.lang.reflect.Method;
import java.util.Arrays;

public class MethodInvocation {
    private final Method method;
    private final Object[] arguments;

    public MethodInvocation(Method method, Object[] args) {
        this.method = method;
        arguments = args;
    }

    public Object[] getArguments() {
        return arguments;
    }

    public Method getMethod() {
        return method;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != getClass()) {
            return false;
        }

        MethodInvocation other = (MethodInvocation) obj;
        if (!method.equals(other.method)) {
            return false;
        }

        return Arrays.equals(arguments, other.arguments);
    }

    @Override
    public int hashCode() {
        return method.hashCode();
    }
}

