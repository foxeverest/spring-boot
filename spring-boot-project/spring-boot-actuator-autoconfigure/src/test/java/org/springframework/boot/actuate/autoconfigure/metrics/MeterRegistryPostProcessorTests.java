/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.metrics;

import java.util.ArrayList;
import java.util.List;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.MeterRegistry.Config;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.core.instrument.config.MeterFilter;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

/**
 * Tests for {@link MeterRegistryPostProcessor}.
 *
 * @author Phillip Webb
 */
public class MeterRegistryPostProcessorTests {

	private List<MeterBinder> binders = new ArrayList<>();

	private List<MeterFilter> filters = new ArrayList<>();

	private List<MeterRegistryCustomizer<?>> customizers = new ArrayList<>();

	@Mock
	private MeterBinder mockBinder;

	@Mock
	private MeterFilter mockFilter;

	@Mock
	private MeterRegistryCustomizer<MeterRegistry> mockCustomizer;

	@Mock
	private MeterRegistry mockRegistry;

	@Mock
	private Config mockConfig;

	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		given(this.mockRegistry.config()).willReturn(this.mockConfig);
	}

	@Test
	public void postProcessWhenNotRegistryShouldReturnBean() {
		MeterRegistryPostProcessor processor = new MeterRegistryPostProcessor(
				this.binders, this.filters, this.customizers, false);
		Object bean = new Object();
		String beanName = "name";
		assertThat(processor.postProcessBeforeInitialization(bean, beanName))
				.isEqualTo(bean);
		assertThat(processor.postProcessAfterInitialization(bean, beanName))
				.isEqualTo(bean);
	}

	@Test
	public void postProcessorWhenCompositeShouldSkip() {
		this.binders.add(this.mockBinder);
		this.customizers.add(this.mockCustomizer);
		MeterRegistryPostProcessor processor = new MeterRegistryPostProcessor(
				this.binders, this.filters, this.customizers, false);
		processor.postProcessAfterInitialization(new CompositeMeterRegistry(), "name");
		verifyZeroInteractions(this.mockBinder, this.mockCustomizer);
	}

	@Test
	public void postProcessShouldApplyCustomizer() {
		this.customizers.add(this.mockCustomizer);
		MeterRegistryPostProcessor processor = new MeterRegistryPostProcessor(
				this.binders, this.filters, this.customizers, false);
		processor.postProcessAfterInitialization(this.mockRegistry, "name");
		verify(this.mockCustomizer).customize(this.mockRegistry);
	}

	@Test
	public void postProcessShouldApplyFilter() {
		this.filters.add(this.mockFilter);
		MeterRegistryPostProcessor processor = new MeterRegistryPostProcessor(
				this.binders, this.filters, this.customizers, false);
		processor.postProcessAfterInitialization(this.mockRegistry, "name");
		verify(this.mockConfig).meterFilter(this.mockFilter);
	}

	@Test
	public void postProcessShouldApplyBinder() {
		this.binders.add(this.mockBinder);
		MeterRegistryPostProcessor processor = new MeterRegistryPostProcessor(
				this.binders, this.filters, this.customizers, false);
		processor.postProcessAfterInitialization(this.mockRegistry, "name");
		verify(this.mockBinder).bindTo(this.mockRegistry);
	}

	@Test
	public void postProcessShouldBeCallInOrderCustomizeFilterBinder() {
		this.customizers.add(this.mockCustomizer);
		this.filters.add(this.mockFilter);
		this.binders.add(this.mockBinder);
		MeterRegistryPostProcessor processor = new MeterRegistryPostProcessor(
				this.binders, this.filters, this.customizers, false);
		processor.postProcessAfterInitialization(this.mockRegistry, "name");
		InOrder ordered = inOrder(this.mockBinder, this.mockConfig, this.mockCustomizer);
		ordered.verify(this.mockCustomizer).customize(this.mockRegistry);
		ordered.verify(this.mockConfig).meterFilter(this.mockFilter);
		ordered.verify(this.mockBinder).bindTo(this.mockRegistry);
	}

	@Test
	public void postProcessWhenAddToGlobalRegistryShouldAddToGlobalRegistry() {
		MeterRegistryPostProcessor processor = new MeterRegistryPostProcessor(
				this.binders, this.filters, this.customizers, true);
		try {
			processor.postProcessAfterInitialization(this.mockRegistry, "name");
			assertThat(Metrics.globalRegistry.getRegistries())
					.contains(this.mockRegistry);
		}
		finally {
			Metrics.removeRegistry(this.mockRegistry);
		}
	}

	@Test
	public void postProcessWhenNotAddToGlobalRegistryShouldAddToGlobalRegistry() {
		MeterRegistryPostProcessor processor = new MeterRegistryPostProcessor(
				this.binders, this.filters, this.customizers, false);
		processor.postProcessAfterInitialization(this.mockRegistry, "name");
		assertThat(Metrics.globalRegistry.getRegistries())
				.doesNotContain(this.mockRegistry);
	}

}
