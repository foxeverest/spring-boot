/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.test;

import org.springframework.boot.SpringApplication;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.context.ContextLoader;

/**
 * A {@link ContextLoader} that can be used to test Spring Boot applications (those that
 * normally startup using {@link SpringApplication}). Can be used to test non-web features
 * (like a repository layer) or start an fully-configured embedded servlet container.
 * <p>
 * Use {@code @WebIntegrationTest} (or {@code @IntegrationTest} with
 * {@code @WebAppConfiguration}) to indicate that you want to use a real servlet container
 * or {@code @WebAppConfiguration} alone to use a {@link MockServletContext}.
 * <p>
 * If {@code @ActiveProfiles} are provided in the test class they will be used to create
 * the application context.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @see IntegrationTest
 * @see WebIntegrationTest
 * @see TestRestTemplate
 * @deprecated since 1.4.0 in favor of
 * {@link org.springframework.boot.test.context.SpringApplicationContextLoader}
 */
@Deprecated
public class SpringApplicationContextLoader
		extends org.springframework.boot.test.context.SpringApplicationContextLoader {

}
