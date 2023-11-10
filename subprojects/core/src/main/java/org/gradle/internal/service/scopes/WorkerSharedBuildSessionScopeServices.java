/*
 * Copyright 2021 the original author or authors.
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

package org.gradle.internal.service.scopes;

import org.gradle.api.internal.StartParameterInternal;
import org.gradle.cache.internal.BuildScopeCacheDir;
import org.gradle.initialization.GradleUserHomeDirProvider;
import org.gradle.initialization.layout.BuildLayoutConfiguration;
import org.gradle.initialization.layout.BuildLayoutFactory;
import org.gradle.initialization.layout.BuildLocations;
import org.gradle.initialization.layout.ProjectCacheDir;
import org.gradle.internal.file.Deleter;
import org.gradle.internal.hash.ClassLoaderHierarchyHasher;
import org.gradle.internal.logging.progress.ProgressLoggerFactory;
import org.gradle.internal.snapshot.ValueSnapshotter;
import org.gradle.internal.snapshot.impl.DefaultValueSnapshotter;
import org.gradle.internal.snapshot.impl.ValueSnapshotterSerializerRegistry;

import java.util.List;

public class WorkerSharedBuildSessionScopeServices {
    protected final StartParameterInternal startParameter;

    public WorkerSharedBuildSessionScopeServices(StartParameterInternal startParameter) {
        this.startParameter = startParameter;
    }

    ValueSnapshotter createValueSnapshotter(
        List<ValueSnapshotterSerializerRegistry> valueSnapshotterSerializerRegistryList,
        ClassLoaderHierarchyHasher classLoaderHierarchyHasher
    ) {
        return new DefaultValueSnapshotter(
            valueSnapshotterSerializerRegistryList,
            classLoaderHierarchyHasher
        );
    }

    ProjectCacheDir createProjectCacheDir(
        GradleUserHomeDirProvider userHomeDirProvider,
        BuildLocations buildLocations,
        Deleter deleter,
        ProgressLoggerFactory progressLoggerFactory
    ) {
        BuildScopeCacheDir cacheDir = new BuildScopeCacheDir(userHomeDirProvider, buildLocations, startParameter);
        return new ProjectCacheDir(cacheDir.getDir(), progressLoggerFactory, deleter);
    }

    BuildLocations createBuildLocations(BuildLayoutFactory buildLayoutFactory) {
        return buildLayoutFactory.getLayoutFor(new BuildLayoutConfiguration(startParameter));
    }

    BuildLayoutFactory createBuildLayoutFactory() {
        return new BuildLayoutFactory();
    }
}
