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

package org.springframework.boot.autoconfigure.security;

import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

/**
 * Configuration for a Spring Security in-memory {@link AuthenticationManager}. Adds an
 * {@link InMemoryUserDetailsManager} with a default user and generated password. This can
 * be disabled by providing a bean of type {@link AuthenticationManager},
 * {@link AuthenticationProvider} or {@link UserDetailsService}.
 *
 * @author Dave Syer
 * @author Rob Winch
 * @author Madhura Bhave
 */
@Configuration
@ConditionalOnBean(ObjectPostProcessor.class)
@Order(0)
public class AuthenticationManagerConfiguration {

	private static final Log logger = LogFactory
			.getLog(AuthenticationManagerConfiguration.class);

	@Bean
	@ConditionalOnMissingBean({ AuthenticationManager.class, AuthenticationProvider.class,
			UserDetailsService.class })
	public InMemoryUserDetailsManager inMemoryUserDetailsManager() throws Exception {
		String password = UUID.randomUUID().toString();
		logger.info(String.format("%n%nUsing default security password: %s%n", password));
		return new InMemoryUserDetailsManager(
				User.withUsername("user").password(password).roles().build());
	}

	@Bean
	public AuthenticationManagerConfigurationListener authenticationManagerConfigurationListener() {
		return new AuthenticationManagerConfigurationListener();
	}

	/**
	 * {@link ApplicationListener} to autowire the {@link AuthenticationEventPublisher}
	 * into the {@link AuthenticationManager}.
	 */
	protected static class AuthenticationManagerConfigurationListener
			implements SmartInitializingSingleton {

		@Autowired
		private AuthenticationEventPublisher eventPublisher;

		@Autowired
		private ApplicationContext context;

		@Override
		public void afterSingletonsInstantiated() {
			try {
				configureAuthenticationManager(
						this.context.getBean(AuthenticationManager.class));
			}
			catch (NoSuchBeanDefinitionException ex) {
				// Ignore
			}
		}

		private void configureAuthenticationManager(AuthenticationManager manager) {
			if (manager instanceof ProviderManager) {
				((ProviderManager) manager)
						.setAuthenticationEventPublisher(this.eventPublisher);
			}
		}

	}

}
