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

package org.springframework.boot.context.properties.source;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.springframework.boot.origin.Origin;
import org.springframework.boot.origin.PropertySourceOrigin;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.util.Assert;

/**
 * {@link ConfigurationPropertySource} backed by a non-enumerable Spring
 * {@link PropertySource} or a restricted {@link EnumerablePropertySource} implementation
 * (such as a security restricted {@code systemEnvironment} source). A
 * {@link PropertySource} is adapted with the help of a {@link PropertyMapper} which
 * provides the mapping rules for individual properties.
 * <p>
 * Each {@link ConfigurationPropertySource#getConfigurationProperty
 * getConfigurationProperty} call attempts to
 * {@link PropertyMapper#map(PropertySource, ConfigurationPropertyName) map} the
 * {@link ConfigurationPropertyName} to one or more {@code String} based names. This
 * allows fast property resolution for well formed property sources.
 * <p>
 * If at all possible the {@link PropertySourceIterableConfigurationPropertySource} should
 * be used in preference to this implementation since it supports "relaxed" style
 * resolution.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @see PropertyMapper
 * @see PropertySourceIterableConfigurationPropertySource
 */
class PropertySourceConfigurationPropertySource implements ConfigurationPropertySource {

	private final PropertySource<?> propertySource;

	private final PropertyMapper mapper;

	/**
	 * Create a new {@link PropertySourceConfigurationPropertySource} implementation.
	 * @param propertySource the source property source
	 * @param mapper the property mapper
	 */
	PropertySourceConfigurationPropertySource(PropertySource<?> propertySource,
			PropertyMapper mapper) {
		Assert.notNull(propertySource, "PropertySource must not be null");
		Assert.notNull(mapper, "Mapper must not be null");
		this.propertySource = propertySource;
		this.mapper = new ExceptionSwallowingPropertyMapper(mapper);
	}

	@Override
	public ConfigurationProperty getConfigurationProperty(
			ConfigurationPropertyName name) {
		List<PropertyMapping> mappings = getMapper().map(getPropertySource(), name);
		return find(mappings, name);
	}

	protected final ConfigurationProperty find(List<PropertyMapping> mappings,
			ConfigurationPropertyName name) {
		return mappings.stream().filter((m) -> m.isApplicable(name)).map(this::find)
				.filter(Objects::nonNull).findFirst().orElse(null);
	}

	private ConfigurationProperty find(PropertyMapping mapping) {
		String propertySourceName = mapping.getPropertySourceName();
		Object value = getPropertySource().getProperty(propertySourceName);
		if (value == null) {
			return null;
		}
		value = mapping.getValueExtractor().apply(value);
		ConfigurationPropertyName configurationPropertyName = mapping
				.getConfigurationPropertyName();
		Origin origin = PropertySourceOrigin.get(this.propertySource, propertySourceName);
		return ConfigurationProperty.of(configurationPropertyName, value, origin);
	}

	protected PropertySource<?> getPropertySource() {
		return this.propertySource;
	}

	protected final PropertyMapper getMapper() {
		return this.mapper;
	}

	/**
	 * {@link PropertyMapper} that swallows exceptions when the mapping fails.
	 */
	private static class ExceptionSwallowingPropertyMapper implements PropertyMapper {

		private final PropertyMapper mapper;

		ExceptionSwallowingPropertyMapper(PropertyMapper mapper) {
			this.mapper = mapper;
		}

		@Override
		public List<PropertyMapping> map(PropertySource<?> propertySource,
				ConfigurationPropertyName configurationPropertyName) {
			try {
				return this.mapper.map(propertySource, configurationPropertyName);
			}
			catch (Exception ex) {
				return Collections.emptyList();
			}
		}

		@Override
		public List<PropertyMapping> map(PropertySource<?> propertySource,
				String propertySourceName) {
			try {
				return this.mapper.map(propertySource, propertySourceName);
			}
			catch (Exception ex) {
				return Collections.emptyList();
			}
		}

	}

}
