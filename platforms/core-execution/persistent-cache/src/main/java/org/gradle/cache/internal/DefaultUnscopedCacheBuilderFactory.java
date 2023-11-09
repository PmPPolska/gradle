/*
 * Copyright 2009 the original author or authors.
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
package org.gradle.cache.internal;

import org.gradle.api.Action;
import org.gradle.cache.CacheBuilder;
import org.gradle.cache.CacheCleanupStrategy;
import org.gradle.cache.UnscopedCacheBuilderFactory;
import org.gradle.cache.FileLockManager;
import org.gradle.cache.LockOptions;
import org.gradle.cache.PersistentCache;
import org.gradle.cache.internal.filelock.LockOptionsBuilder;

import java.io.File;
import java.util.Collections;
import java.util.Map;

public class DefaultUnscopedCacheBuilderFactory implements UnscopedCacheBuilderFactory {
    private final CacheScopeMapping cacheScopeMapping;
    private final CacheFactory factory;

    public DefaultUnscopedCacheBuilderFactory(CacheScopeMapping cacheScopeMapping, CacheFactory factory) {
        this.cacheScopeMapping = cacheScopeMapping;
        this.factory = factory;
    }

    @Override
    public CacheBuilder cache(String key) {
        return new PersistentCacheBuilder(key);
    }

    @Override
    public CacheBuilder cache(File baseDir) {
        return new PersistentCacheBuilder(baseDir);
    }

    private class PersistentCacheBuilder implements CacheBuilder {
        final String key;
        final File baseDir;
        Map<String, ?> properties = Collections.emptyMap();
        Action<? super PersistentCache> initializer;
        CacheCleanupStrategy cacheCleanupStrategy;
        LockOptions lockOptions = new LockOptionsBuilder(FileLockManager.LockMode.Shared);
        String displayName;
        VersionStrategy versionStrategy = VersionStrategy.CachePerVersion;

        PersistentCacheBuilder(String key) {
            this.key = key;
            this.baseDir = null;
        }

        PersistentCacheBuilder(File baseDir) {
            this.key = null;
            this.baseDir = baseDir;
        }

        @Override
        public CacheBuilder withProperties(Map<String, ?> properties) {
            this.properties = properties;
            return this;
        }

        @Override
        public CacheBuilder withCrossVersionCache() {
            this.versionStrategy = VersionStrategy.SharedCache;
            return this;
        }

        @Override
        public CacheBuilder withDisplayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        @Override
        public CacheBuilder withLockOptions(LockOptions lockOptions) {
            this.lockOptions = lockOptions;
            return this;
        }

        @Override
        public CacheBuilder withInitializer(Action<? super PersistentCache> initializer) {
            this.initializer = initializer;
            return this;
        }

        @Override
        public CacheBuilder withCleanupStrategy(CacheCleanupStrategy cacheCleanupStrategy) {
            this.cacheCleanupStrategy = cacheCleanupStrategy;
            return this;
        }

        @Override
        public PersistentCache open() {
            File cacheBaseDir;
            if (baseDir != null) {
                cacheBaseDir = baseDir;
            } else {
                cacheBaseDir = cacheScopeMapping.getBaseDirectory(null, key, versionStrategy);
            }
            return factory.open(cacheBaseDir, displayName, properties, lockOptions, initializer, cacheCleanupStrategy);
        }
    }
}
