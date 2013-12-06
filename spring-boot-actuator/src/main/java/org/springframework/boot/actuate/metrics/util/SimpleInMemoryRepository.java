/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.boot.actuate.metrics.util;

import java.util.ArrayList;
import java.util.NavigableMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Repository utility that stores stuff in memory with period-separated String keys.
 * 
 * @author Dave Syer
 */
public class SimpleInMemoryRepository<T> {

	private ConcurrentNavigableMap<String, T> values = new ConcurrentSkipListMap<String, T>();

	private ConcurrentMap<String, Object> locks = new ConcurrentHashMap<String, Object>();

	public static interface Callback<T> {
		T modify(T current);
	}

	public T update(String name, Callback<T> callback) {
		Object lock = this.locks.putIfAbsent(name, new Object());
		if (lock == null) {
			lock = this.locks.get(name);
		}
		synchronized (lock) {
			T current = this.values.get(name);
			T value = callback.modify(current);
			if (current != null) {
				this.values.replace(name, current, value);
			}
			else {
				this.values.putIfAbsent(name, value);
			}
			return this.values.get(name);
		}
	}

	public void set(String name, T value) {
		T current = this.values.get(name);
		if (current != null) {
			this.values.replace(name, current, value);
		}
		else {
			this.values.putIfAbsent(name, value);
		}
	}

	public long count() {
		return this.values.size();
	}

	public void remove(String name) {
		this.values.remove(name);
	}

	public T findOne(String name) {
		if (this.values.containsKey(name)) {
			return this.values.get(name);
		}
		return null;
	}

	public Iterable<T> findAll() {
		return new ArrayList<T>(this.values.values());
	}

	public Iterable<T> findAllWithPrefix(String prefix) {
		if (prefix.endsWith(".*")) {
			prefix = prefix.substring(0, prefix.length() - 1);
		}
		if (!prefix.endsWith(".")) {
			prefix = prefix + ".";
		}
		return new ArrayList<T>(this.values.subMap(prefix, false, prefix + "~", true)
				.values());
	}

	protected NavigableMap<String, T> getValues() {
		return this.values;
	}

}
