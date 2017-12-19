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

package org.keycloak.testsuite.util;

import org.jboss.arquillian.graphene.context.GrapheneContext;
import org.openqa.selenium.WebDriver;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Stack;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public final class DroneUtils {
    private static Queue<WebDriver> driverQueue = new LinkedList<>();

    public static WebDriver getCurrentDriver() {
        if (driverQueue.isEmpty()) {
            return GrapheneContext.lastContext().getWebDriver();
        }

        return driverQueue.peek();
    }

    public static void addWebDriver(WebDriver driver) {
        driverQueue.add(driver);
    }

    public static void removeWebDriver() {
        driverQueue.poll();
    }

    public static void resetQueue() {
        driverQueue = new LinkedList<>();
    }
}
