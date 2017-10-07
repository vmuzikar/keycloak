/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.approvals;

import org.keycloak.models.KeycloakSession;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class DefaultApprovalInterceptor implements ApprovalInterceptor {
    protected KeycloakSession session;
    protected ApprovalEvaluator evaluator;
    protected ApprovalHandler handler;
    protected Method protectedMethod;

    public DefaultApprovalInterceptor(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void intercept(ApprovalContext context) throws InterceptedException {
        populateProtectedMethod();
        populateEvaluator();
        populateHandler();

        if (evaluator.needsApproval(protectedMethod, context)) {
            handler.handleRequest(protectedMethod, context);
            throw new InterceptedException("Approval needed");
        }
    }

    @Override
    public ApprovalInterceptor setCustomEvaluator(ApprovalEvaluator evaluator) {
        this.evaluator = evaluator;
        return this;
    }

    @Override
    public ApprovalInterceptor setCustomHandler(ApprovalHandler handler) {
        this.handler = handler;
        return this;
    }

    @Override
    public ApprovalInterceptor setProtectedMethod(Method protectedMethod) {
        this.protectedMethod = protectedMethod;
        return this;
    }

    protected void populateEvaluator() {
        if (evaluator == null) {
            evaluator = session.getProvider(ApprovalEvaluator.class, "default");
        }
    }

    protected void populateHandler() {
        if (handler == null) {
            handler = session.getProvider(ApprovalProvider.class)
                    .getHandlerByProtectedClass(protectedMethod.getDeclaringClass());
        }
    }

    protected void populateProtectedMethod() {
        if (protectedMethod == null) {
            protectedMethod = getCallerMethod();
        }
    }

    protected Method getCallerMethod() {
        final StackTraceElement[] elements = Thread.currentThread().getStackTrace();
        StackTraceElement callerElement = null;

        // Three phases:
        //    1) move through what lies on top of the stack and is not this class
        //    2) move through elements which belongs to this class (i.e. through methods of this class)
        //    3) after that, return the next method
        boolean currentClassEncountered = false;
        for (StackTraceElement element : elements) {
            boolean isCurrentClass = element.getClassName().equals(this.getClass().getName());
            if (!currentClassEncountered && isCurrentClass) {
                currentClassEncountered = true;
            }
            else if (currentClassEncountered && !isCurrentClass) {
                callerElement = element;
                break;
            }
        }

        if (callerElement == null) {
            throw new CallerMethodException("Cannot find the caller method; stack trace ended prematurely");
        }

        final String callerClassName = callerElement.getClassName();
        final String callerMethodName = callerElement.getMethodName();

        List<Method> callerMethods;
        try {
            callerMethods = Arrays.stream(Class.forName(callerClassName).getMethods())
                    .filter(p -> p.getName().equals(callerMethodName))
                    .collect(Collectors.toList());
        }
        catch (Exception e) {
            throw new CallerMethodException(e);
        }

        if (callerMethods.size() != 1) {
            throw new CallerMethodException("Cannot find the caller method; zero or more than one method found");
        }

        return callerMethods.get(0);
    }

    @Override
    public void close() {

    }
}
