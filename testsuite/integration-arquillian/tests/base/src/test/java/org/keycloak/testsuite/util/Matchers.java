/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

import org.keycloak.testsuite.util.matchers.ResponseBodyMatcher;
import org.keycloak.testsuite.util.matchers.ResponseHeaderMatcher;
import org.keycloak.testsuite.util.matchers.ResponseStatusCodeMatcher;

import java.util.Map;
import javax.ws.rs.core.Response;
import org.hamcrest.Matcher;

/**
 * Additional hamcrest matchers for use in {@link org.junit.Assert#assertThat}.
 * @author hmlnarik
 */
public class Matchers {

    /**
     * Matcher on HTTP status code of a {@link Response} instance.
     * @param matcher
     * @return
     */
    public static Matcher<Response> body(Matcher<String> matcher) {
        return new ResponseBodyMatcher(matcher);
    }

    /**
     * Matcher on HTTP status code of a {@link Response} instance.
     * @param matcher
     * @return
     */
    public static Matcher<Response> statusCode(Matcher<? extends Number> matcher) {
        return new ResponseStatusCodeMatcher(matcher);
    }

    /**
     * Matches when the HTTP status code of a {@link Response} instance is equal to the given code.
     * @param expectedStatusCode
     * @return
     */
    public static Matcher<Response> statusCodeIs(Response.Status expectedStatusCode) {
        return new ResponseStatusCodeMatcher(org.hamcrest.Matchers.is(expectedStatusCode.getStatusCode()));
    }

    /**
     * Matches when the HTTP status code of a {@link Response} instance is equal to the given code.
     * @param expectedStatusCode
     * @return
     */
    public static Matcher<Response> statusCodeIs(int expectedStatusCode) {
        return new ResponseStatusCodeMatcher(org.hamcrest.Matchers.is(expectedStatusCode));
    }

    /**
     * Matches when the HTTP status code of a {@link Response} instance is equal to the given code.
     * @param expectedStatusCode
     * @return
     */
    public static <T> Matcher<Response> header(Matcher<Map<String, T>> matcher) {
        return new ResponseHeaderMatcher(matcher);
    }
}
