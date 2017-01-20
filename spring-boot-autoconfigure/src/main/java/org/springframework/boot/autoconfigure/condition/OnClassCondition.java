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

package org.springframework.boot.autoconfigure.condition;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionMessage.Style;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.ClassUtils;
import org.springframework.util.MultiValueMap;

/**
 * {@link Condition} that checks for the presence or absence of specific classes.
 *
 * @author Phillip Webb
 * @see ConditionalOnClass
 * @see ConditionalOnMissingClass
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
class OnClassCondition extends SpringBootCondition {

	@Override
	public ConditionOutcome getMatchOutcome(ConditionContext context,
			AnnotatedTypeMetadata metadata) {
		ConditionMessage matchMessage = ConditionMessage.empty();
		MultiValueMap<String, Object> onClasses = getAttributes(metadata,
				ConditionalOnClass.class);
		if (onClasses != null) {
			List<String> missing = getMatchingClasses(onClasses, MatchType.MISSING,
					context);
			if (!missing.isEmpty()) {
				return ConditionOutcome
						.noMatch(ConditionMessage.forCondition(ConditionalOnClass.class)
								.didNotFind("required class", "required classes")
								.items(Style.QUOTE, missing));
			}
			matchMessage = matchMessage.andCondition(ConditionalOnClass.class)
					.found("required class", "required classes").items(Style.QUOTE,
							getMatchingClasses(onClasses, MatchType.PRESENT, context));
		}
		MultiValueMap<String, Object> onMissingClasses = getAttributes(metadata,
				ConditionalOnMissingClass.class);
		if (onMissingClasses != null) {
			List<String> present = getMatchingClasses(onMissingClasses, MatchType.PRESENT,
					context);
			if (!present.isEmpty()) {
				return ConditionOutcome.noMatch(
						ConditionMessage.forCondition(ConditionalOnMissingClass.class)
								.found("unwanted class", "unwanted classes")
								.items(Style.QUOTE, present));
			}
			matchMessage = matchMessage.andCondition(ConditionalOnMissingClass.class)
					.didNotFind("unwanted class", "unwanted classes")
					.items(Style.QUOTE, getMatchingClasses(onMissingClasses,
							MatchType.MISSING, context));
		}
		return ConditionOutcome.match(matchMessage);
	}

	private MultiValueMap<String, Object> getAttributes(AnnotatedTypeMetadata metadata,
			Class<?> annotationType) {
		return metadata.getAllAnnotationAttributes(annotationType.getName(), true);
	}

	private List<String> getMatchingClasses(MultiValueMap<String, Object> attributes,
			MatchType matchType, ConditionContext context) {
		List<String> matches = new LinkedList<String>();
		addAll(matches, attributes.get("value"));
		addAll(matches, attributes.get("name"));
		Iterator<String> iterator = matches.iterator();
		while (iterator.hasNext()) {
			if (!matchType.matches(iterator.next(), context)) {
				iterator.remove();
			}
		}
		return matches;
	}

	private void addAll(List<String> list, List<Object> itemsToAdd) {
		if (itemsToAdd != null) {
			for (Object item : itemsToAdd) {
				Collections.addAll(list, (String[]) item);
			}
		}
	}

	private enum MatchType {

		PRESENT {

			@Override
			public boolean matches(String className, ConditionContext context) {
				return isPresent(className, context.getClassLoader());
			}

		},

		MISSING {

			@Override
			public boolean matches(String className, ConditionContext context) {
				return !isPresent(className, context.getClassLoader());
			}

		};

		private static boolean isPresent(String className, ClassLoader classLoader) {
			if (classLoader == null) {
				classLoader = ClassUtils.getDefaultClassLoader();
			}
			try {
				forName(className, classLoader);
				return true;
			}
			catch (Throwable ex) {
				return false;
			}
		}

		private static Class<?> forName(String className, ClassLoader classLoader)
				throws ClassNotFoundException {
			if (classLoader != null) {
				return classLoader.loadClass(className);
			}
			return Class.forName(className);
		}

		public abstract boolean matches(String className, ConditionContext context);

	}

}
