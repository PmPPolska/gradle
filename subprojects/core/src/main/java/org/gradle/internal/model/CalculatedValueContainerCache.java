/*
 * Copyright 2023 the original author or authors.
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

package org.gradle.internal.model;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import org.gradle.api.NonNullApi;
import org.gradle.internal.DisplayName;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

@NonNullApi
public class CalculatedValueContainerCache<T extends DisplayName, V> {
    @VisibleForTesting
    final Map<T, Entry<V>> cache = Maps.newHashMap();
    private final CalculatedValueContainerFactory calculatedValueContainerFactory;

    public CalculatedValueContainerCache(CalculatedValueContainerFactory calculatedValueContainerFactory) {
        this.calculatedValueContainerFactory = calculatedValueContainerFactory;
    }

    public ValueContainerReference getReference(T key, Supplier<? extends V> supplier) {
        return new ValueContainerReference(key, supplier);
    }

    @NonNullApi
    private static class Entry<V> {
        final CalculatedValueContainer<V, ?> container;
        int inUse = 0;

        public Entry(CalculatedValueContainer<V, ?> container) {
            this.container = container;
        }
    }

    @NonNullApi
    public class ValueContainerReference {
        private final T key;
        private final Supplier<? extends V> supplier;
        CalculatedValueContainer<V, ?> container;

        public ValueContainerReference(T key, Supplier<? extends V> supplier) {
            this.key = key;
            this.supplier = supplier;
        }

        public V finalizeAndGet() {
            return apply(container -> {
                container.finalizeIfNotAlready();
                return container.get();
            });
        }

        public <R> R apply(Function<CalculatedValueContainer<V, ?>, R> function) {
            Entry<V> entry;
            synchronized (cache) {
                if (cache.containsKey(key)) {
                    entry = cache.get(key);
                    container = entry.container;
                } else {
                    if (container == null) {
                        container = calculatedValueContainerFactory.create(key, supplier);
                    }
                    entry = new Entry<>(container);
                    cache.put(key, entry);
                }
                entry.inUse++;
            }
            try {
                return function.apply(container);
            } finally {
                synchronized (cache) {
                    entry.inUse--;
                    if (entry.inUse <= 0) {
                        cache.remove(key);
                    }
                }
            }
        }
    }
}
