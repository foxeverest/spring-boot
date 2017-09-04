/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.actuate.endpoint;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.actuate.endpoint.BeansEndpoint.ApplicationContextDescriptor;
import org.springframework.boot.actuate.endpoint.BeansEndpoint.BeanDescriptor;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link BeansEndpoint}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
public class BeansEndpointTests {

	@SuppressWarnings("unchecked")
	@Test
	public void beansAreFound() {
		ApplicationContextRunner contextRunner = new ApplicationContextRunner()
				.withUserConfiguration(EndpointConfiguration.class);
		contextRunner.run((context) -> {
			Map<String, Object> result = context.getBean(BeansEndpoint.class).beans();
			List<ApplicationContextDescriptor> contexts = (List<ApplicationContextDescriptor>) result
					.get("contexts");
			assertThat(contexts).hasSize(1);
			ApplicationContextDescriptor contextDescriptor = contexts.get(0);
			assertThat(contextDescriptor.getParentId()).isNull();
			assertThat(contextDescriptor.getId()).isEqualTo(context.getId());
			Map<String, BeanDescriptor> beans = contextDescriptor.getBeans();
			assertThat(beans.size())
					.isLessThanOrEqualTo(context.getBeanDefinitionCount());
			assertThat(contexts.get(0).getBeans()).containsKey("endpoint");
		});
	}

	@SuppressWarnings("unchecked")
	@Test
	public void infrastructureBeansAreOmitted() {
		ApplicationContextRunner contextRunner = new ApplicationContextRunner()
				.withUserConfiguration(EndpointConfiguration.class);
		contextRunner.run((context) -> {
			ConfigurableListableBeanFactory factory = (ConfigurableListableBeanFactory) context
					.getAutowireCapableBeanFactory();
			List<String> infrastructureBeans = Stream.of(context.getBeanDefinitionNames())
					.filter((name) -> BeanDefinition.ROLE_INFRASTRUCTURE == factory
							.getBeanDefinition(name).getRole())
					.collect(Collectors.toList());
			Map<String, Object> result = context.getBean(BeansEndpoint.class).beans();
			List<ApplicationContextDescriptor> contexts = (List<ApplicationContextDescriptor>) result
					.get("contexts");
			Map<String, BeanDescriptor> beans = contexts.get(0).getBeans();
			for (String infrastructureBean : infrastructureBeans) {
				assertThat(beans).doesNotContainKey(infrastructureBean);
			}
		});
	}

	@SuppressWarnings("unchecked")
	@Test
	public void lazyBeansAreOmitted() {
		ApplicationContextRunner contextRunner = new ApplicationContextRunner()
				.withUserConfiguration(EndpointConfiguration.class,
						LazyBeanConfiguration.class);
		contextRunner.run((context) -> {
			Map<String, Object> result = context.getBean(BeansEndpoint.class).beans();
			List<ApplicationContextDescriptor> contexts = (List<ApplicationContextDescriptor>) result
					.get("contexts");
			assertThat(context).hasBean("lazyBean");
			assertThat(contexts.get(0).getBeans()).doesNotContainKey("lazyBean");
		});
	}

	@SuppressWarnings("unchecked")
	@Test
	public void beansInParentContextAreFound() {
		ApplicationContextRunner parentRunner = new ApplicationContextRunner()
				.withUserConfiguration(BeanConfiguration.class);
		parentRunner.run((parent) -> {
			new ApplicationContextRunner()
					.withUserConfiguration(EndpointConfiguration.class).withParent(parent)
					.run(child -> {
				BeansEndpoint endpoint = child.getBean(BeansEndpoint.class);
				Map<String, Object> result = endpoint.beans();
				List<ApplicationContextDescriptor> contexts = (List<ApplicationContextDescriptor>) result
						.get("contexts");
				assertThat(contexts).hasSize(2);
				assertThat(contexts.get(1).getBeans()).containsKey("bean");
				assertThat(contexts.get(0).getBeans()).containsKey("endpoint");
			});
		});
	}

	@SuppressWarnings("unchecked")
	@Test
	public void beansInChildContextAreNotFound() {
		ApplicationContextRunner parentRunner = new ApplicationContextRunner()
				.withUserConfiguration(EndpointConfiguration.class);
		parentRunner.run((parent) -> {
			new ApplicationContextRunner().withUserConfiguration(BeanConfiguration.class)
					.withParent(parent).run(child -> {
				BeansEndpoint endpoint = child.getBean(BeansEndpoint.class);
				Map<String, Object> result = endpoint.beans();
				List<ApplicationContextDescriptor> contexts = (List<ApplicationContextDescriptor>) result
						.get("contexts");
				assertThat(contexts).hasSize(1);
				assertThat(contexts.get(0).getBeans()).containsKey("endpoint");
				assertThat(contexts.get(0).getBeans()).doesNotContainKey("bean");
			});
		});
	}

	@Configuration
	public static class EndpointConfiguration {

		@Bean
		public BeansEndpoint endpoint(ConfigurableApplicationContext context) {
			return new BeansEndpoint(context);
		}

	}

	@Configuration
	static class BeanConfiguration {

		@Bean
		public String bean() {
			return "bean";
		}

	}

	@Configuration
	static class LazyBeanConfiguration {

		@Lazy
		@Bean
		public String lazyBean() {
			return "lazyBean";
		}

	}

}
