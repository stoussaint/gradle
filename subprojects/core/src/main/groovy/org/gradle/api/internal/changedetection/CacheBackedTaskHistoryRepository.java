/*
 * Copyright 2011 the original author or authors.
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
package org.gradle.api.internal.changedetection;

import org.gradle.api.internal.TaskInternal;
import org.gradle.cache.CacheRepository;
import org.gradle.cache.DefaultSerializer;
import org.gradle.cache.PersistentIndexedCache;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CacheBackedTaskHistoryRepository implements TaskHistoryRepository {
    private final CacheRepository repository;
    private final FileSnapshotRepository snapshotRepository;
    private PersistentIndexedCache<String, TaskHistory> taskHistoryCache;
    private DefaultSerializer<TaskHistory> serializer;

    public CacheBackedTaskHistoryRepository(CacheRepository repository, FileSnapshotRepository snapshotRepository) {
        this.repository = repository;
        this.snapshotRepository = snapshotRepository;
    }

    public History getHistory(final TaskInternal task) {
        if (taskHistoryCache == null) {
            serializer = new DefaultSerializer<TaskHistory>();
            taskHistoryCache = repository.cache("taskArtifacts").forObject(task.getProject().getGradle()).open().openIndexedCache(serializer);
        }
        final TaskHistory history = loadHistory(task);
        final LazyTaskExecution currentExecution = new LazyTaskExecution();
        currentExecution.snapshotRepository = snapshotRepository;
        currentExecution.setOutputFiles(outputFiles(task));
        final LazyTaskExecution previousExecution = findPreviousExecution(currentExecution, history);
        if (previousExecution != null) {
            previousExecution.snapshotRepository = snapshotRepository;
        }
        history.addConfiguration(currentExecution);

        return new History() {
            public TaskExecution getPreviousExecution() {
                return previousExecution;
            }

            public TaskExecution getCurrentExecution() {
                return currentExecution;
            }

            public void update() {
                if (currentExecution.inputFilesSnapshotId == null && currentExecution.inputFilesSnapshot != null) {
                    currentExecution.inputFilesSnapshotId = snapshotRepository.add(currentExecution.inputFilesSnapshot);
                }
                if (currentExecution.outputFilesSnapshotId == null && currentExecution.outputFilesSnapshot != null) {
                    currentExecution.outputFilesSnapshotId = snapshotRepository.add(currentExecution.outputFilesSnapshot);
                }
                taskHistoryCache.put(task.getPath(), history);
            }
        };
    }

    private TaskHistory loadHistory(TaskInternal task) {
        ClassLoader original = serializer.getClassLoader();
        serializer.setClassLoader(task.getClass().getClassLoader());
        try {
            TaskHistory history = taskHistoryCache.get(task.getPath());
            return history == null ? new TaskHistory() : history;
        } finally {
            serializer.setClassLoader(original);
        }
    }

    private static Set<String> outputFiles(TaskInternal task) {
        Set<String> outputFiles = new HashSet<String>();
        for (File file : task.getOutputs().getFiles()) {
            outputFiles.add(file.getAbsolutePath());
        }
        return outputFiles;
    }

    private LazyTaskExecution findPreviousExecution(TaskExecution currentExecution, TaskHistory history) {
        Set<String> outputFiles = currentExecution.getOutputFiles();
        LazyTaskExecution bestMatch = null;
        int bestMatchOverlap = 0;
        for (LazyTaskExecution configuration : history.configurations) {
            if (outputFiles.size() == 0) {
                if (configuration.getOutputFiles().size() == 0) {
                    bestMatch = configuration;
                    break;
                }
            }

            Set<String> intersection = new HashSet<String>(outputFiles);
            intersection.retainAll(configuration.getOutputFiles());
            if (intersection.size() > bestMatchOverlap) {
                bestMatch = configuration;
                bestMatchOverlap = intersection.size();
            }
            if (bestMatchOverlap == outputFiles.size()) {
                break;
            }
        }
        return bestMatch;
    }

    private static class TaskHistory implements Serializable {
        private static final int MAX_HISTORY_ENTRIES = 3;
        private final List<LazyTaskExecution> configurations = new ArrayList<LazyTaskExecution>();

        public void addConfiguration(LazyTaskExecution configuration) {
            configurations.add(0, configuration);
            // Only keep a few of the most recent configurations
            while (configurations.size() > MAX_HISTORY_ENTRIES) {
                configurations.remove(MAX_HISTORY_ENTRIES);
            }
        }
    }

    private static class LazyTaskExecution extends TaskExecution {
        private Long inputFilesSnapshotId;
        private Long outputFilesSnapshotId;
        private transient FileSnapshotRepository snapshotRepository;
        private transient FileCollectionSnapshot inputFilesSnapshot;
        private transient FileCollectionSnapshot outputFilesSnapshot;

        @Override
        public FileCollectionSnapshot getInputFilesSnapshot() {
            if (inputFilesSnapshot == null) {
                inputFilesSnapshot = snapshotRepository.get(inputFilesSnapshotId);
            }
            return inputFilesSnapshot;
        }

        @Override
        public void setInputFilesSnapshot(FileCollectionSnapshot inputFilesSnapshot) {
            this.inputFilesSnapshot = inputFilesSnapshot;
            this.inputFilesSnapshotId = null;
        }

        @Override
        public FileCollectionSnapshot getOutputFilesSnapshot() {
            if (outputFilesSnapshot == null) {
                outputFilesSnapshot = snapshotRepository.get(outputFilesSnapshotId);
            }
            return outputFilesSnapshot;
        }

        @Override
        public void setOutputFilesSnapshot(FileCollectionSnapshot outputFilesSnapshot) {
            this.outputFilesSnapshot = outputFilesSnapshot;
            outputFilesSnapshotId = null;
        }
    }
}
