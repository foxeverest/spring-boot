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

package org.springframework.boot.actuate.health;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link DefaultHealthIndicatorRegistry}.
 *
 * @author Vedran Pavic
 */
public class DefaultHealthIndicatorRegistryTest {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private HealthIndicator one = mock(HealthIndicator.class);

	private HealthIndicator two = mock(HealthIndicator.class);

	private DefaultHealthIndicatorRegistry registry;

	@Before
	public void setUp() {
		given(this.one.health()).willReturn(new Health.Builder().up().build());
		given(this.two.health()).willReturn(new Health.Builder().unknown().build());

		this.registry = new DefaultHealthIndicatorRegistry();
	}

	@Test
	public void register() {
		this.registry.register("one", this.one);
		this.registry.register("two", this.two);
		assertThat(this.registry.getAll()).hasSize(2);
		assertThat(this.registry.get("one")).isSameAs(this.one);
		assertThat(this.registry.get("two")).isSameAs(this.two);
	}

	@Test
	public void registerAlreadyUsedName() {
		this.thrown.expect(IllegalStateException.class);
		this.thrown.expectMessage("HealthIndicator with name 'one' already registered");
		this.registry.register("one", this.one);
		this.registry.register("one", this.two);
	}

	@Test
	public void unregister() {
		this.registry.register("one", this.one);
		this.registry.register("two", this.two);
		assertThat(this.registry.getAll()).hasSize(2);
		HealthIndicator two = this.registry.unregister("two");
		assertThat(two).isSameAs(this.two);
		assertThat(this.registry.getAll()).hasSize(1);
	}

	@Test
	public void unregisterNotKnown() {
		this.registry.register("one", this.one);
		assertThat(this.registry.getAll()).hasSize(1);
		HealthIndicator two = this.registry.unregister("two");
		assertThat(two).isNull();
		assertThat(this.registry.getAll()).hasSize(1);
	}

}
