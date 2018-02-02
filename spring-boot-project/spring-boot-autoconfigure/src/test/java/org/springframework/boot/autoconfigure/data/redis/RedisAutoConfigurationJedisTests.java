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

package org.springframework.boot.autoconfigure.data.redis;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.testsupport.runner.classpath.ClassPathExclusions;
import org.springframework.boot.testsupport.runner.classpath.ModifiedClassPathRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.jedis.JedisClientConfiguration.JedisClientConfigurationBuilder;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link RedisAutoConfiguration} when Lettuce is not on the classpath.
 *
 * @author Mark Paluch
 * @author Stephane Nicoll
 */
@RunWith(ModifiedClassPathRunner.class)
@ClassPathExclusions("lettuce-core-*.jar")
public class RedisAutoConfigurationJedisTests {

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(RedisAutoConfiguration.class));

	@Test
	public void testOverrideRedisConfiguration() {
		this.runner.withPropertyValues("spring.redis.host:foo", "spring.redis.database:1")
				.run((context) -> {
					JedisConnectionFactory cf = context
							.getBean(JedisConnectionFactory.class);
					assertThat(cf.getHostName()).isEqualTo("foo");
					assertThat(cf.getDatabase()).isEqualTo(1);
					assertThat(cf.getPassword()).isNull();
					assertThat(cf.isUseSsl()).isFalse();
				});
	}

	@Test
	public void testCustomizeRedisConfiguration() {
		this.runner.withUserConfiguration(CustomConfiguration.class).run((context) -> {
			JedisConnectionFactory cf = context.getBean(JedisConnectionFactory.class);
			assertThat(cf.isUseSsl()).isTrue();
		});
	}

	@Test
	public void testRedisUrlConfiguration() {
		this.runner
				.withPropertyValues("spring.redis.host:foo",
						"spring.redis.url:redis://user:password@example:33")
				.run((context) -> {
					JedisConnectionFactory cf = context
							.getBean(JedisConnectionFactory.class);
					assertThat(cf.getHostName()).isEqualTo("example");
					assertThat(cf.getPort()).isEqualTo(33);
					assertThat(cf.getPassword()).isEqualTo("password");
					assertThat(cf.isUseSsl()).isFalse();
				});
	}

	@Test
	public void testOverrideUrlRedisConfiguration() {
		this.runner
				.withPropertyValues("spring.redis.host:foo", "spring.redis.password:xyz",
						"spring.redis.port:1000", "spring.redis.ssl:false",
						"spring.redis.url:rediss://user:password@example:33")
				.run((context) -> {
					JedisConnectionFactory cf = context
							.getBean(JedisConnectionFactory.class);
					assertThat(cf.getHostName()).isEqualTo("example");
					assertThat(cf.getPort()).isEqualTo(33);
					assertThat(cf.getPassword()).isEqualTo("password");
					assertThat(cf.isUseSsl()).isTrue();
				});
	}

	@Test
	public void testPasswordInUrlWithColon() {
		this.runner.withPropertyValues("spring.redis.url:redis://:pass:word@example:33")
				.run((context) -> {
					assertThat(
							context.getBean(JedisConnectionFactory.class).getHostName())
									.isEqualTo("example");
					assertThat(context.getBean(JedisConnectionFactory.class).getPort())
							.isEqualTo(33);
					assertThat(
							context.getBean(JedisConnectionFactory.class).getPassword())
									.isEqualTo("pass:word");
				});
	}

	@Test
	public void testPasswordInUrlStartsWithColon() {
		this.runner
				.withPropertyValues("spring.redis.url:redis://user::pass:word@example:33")
				.run((context) -> {
					assertThat(
							context.getBean(JedisConnectionFactory.class).getHostName())
									.isEqualTo("example");
					assertThat(context.getBean(JedisConnectionFactory.class).getPort())
							.isEqualTo(33);
					assertThat(
							context.getBean(JedisConnectionFactory.class).getPassword())
									.isEqualTo(":pass:word");
				});
	}

	@Test
	public void testRedisConfigurationWithPool() {
		this.runner.withPropertyValues("spring.redis.host:foo",
				"spring.redis.jedis.pool.min-idle:1",
				"spring.redis.jedis.pool.max-idle:4",
				"spring.redis.jedis.pool.max-active:16",
				"spring.redis.jedis.pool.max-wait:2000").run((context) -> {
					JedisConnectionFactory cf = context
							.getBean(JedisConnectionFactory.class);
					assertThat(cf.getHostName()).isEqualTo("foo");
					assertThat(cf.getPoolConfig().getMinIdle()).isEqualTo(1);
					assertThat(cf.getPoolConfig().getMaxIdle()).isEqualTo(4);
					assertThat(cf.getPoolConfig().getMaxTotal()).isEqualTo(16);
					assertThat(cf.getPoolConfig().getMaxWaitMillis()).isEqualTo(2000);
				});
	}

	@Test
	public void testRedisConfigurationWithTimeout() {
		this.runner
				.withPropertyValues("spring.redis.host:foo", "spring.redis.timeout:100")
				.run((context) -> {
					JedisConnectionFactory cf = context
							.getBean(JedisConnectionFactory.class);
					assertThat(cf.getHostName()).isEqualTo("foo");
					assertThat(cf.getTimeout()).isEqualTo(100);
				});
	}

	@Test
	public void testRedisConfigurationWithSentinel() {
		this.runner
				.withPropertyValues("spring.redis.sentinel.master:mymaster",
						"spring.redis.sentinel.nodes:127.0.0.1:26379,127.0.0.1:26380")
				.run((context) -> assertThat(context.getBean(JedisConnectionFactory.class)
						.isRedisSentinelAware()).isTrue());
	}

	@Test
	public void testRedisConfigurationWithSentinelAndPassword() {
		this.runner
				.withPropertyValues("spring.redis.password=password",
						"spring.redis.sentinel.master:mymaster",
						"spring.redis.sentinel.nodes:127.0.0.1:26379,127.0.0.1:26380")
				.run((context) -> assertThat(
						context.getBean(JedisConnectionFactory.class).getPassword())
								.isEqualTo("password"));
	}

	@Test
	public void testRedisConfigurationWithCluster() {
		this.runner
				.withPropertyValues(
						"spring.redis.cluster.nodes=127.0.0.1:27379,127.0.0.1:27380")
				.run((context) -> assertThat(context.getBean(JedisConnectionFactory.class)
						.getClusterConnection()).isNotNull());
	}

	@Configuration
	static class CustomConfiguration {

		@Bean
		JedisClientConfigurationBuilderCustomizer customizer() {
			return JedisClientConfigurationBuilder::useSsl;
		}

	}

}
