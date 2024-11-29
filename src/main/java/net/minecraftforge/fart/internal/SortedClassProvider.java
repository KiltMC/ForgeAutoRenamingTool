/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.fart.internal;

import net.minecraftforge.fart.api.ClassProvider;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

class SortedClassProvider implements ClassProvider {
    List<ClassProvider> classProviders;
    private final Consumer<String> log;
    private final Map<String, Optional<? extends IClassInfo>> classCache = new ConcurrentHashMap<>();

    SortedClassProvider(List<ClassProvider> classProviders, Consumer<String> log) {
        this.classProviders = classProviders;
        this.log = log;
    }

    @Override
    public Optional<? extends IClassInfo> getClass(String cls) {
        return this.classCache.computeIfAbsent(cls, this::computeClassInfo);
    }

    @Override
    public Optional<byte[]> getClassBytes(String cls) {
        for (ClassProvider provider : this.classProviders) {
            Optional<byte[]> bytes = provider.getClassBytes(cls);
            if (bytes.isPresent()) {
                return bytes;
            }
        }
        return Optional.empty();
    }

    @Override
    public @Nullable InputStream getClassStream(final String cls) throws IOException {
        for (ClassProvider provider : this.classProviders) {
            @Nullable InputStream stream = provider.getClassStream(cls);
            if (stream != null) {
                return stream;
            }
        }
        return null;
    }

    private Optional<? extends IClassInfo> computeClassInfo(String name) {
        for (ClassProvider classProvider : this.classProviders) {
            Optional<? extends IClassInfo> classInfo = classProvider.getClass(name);

            if (classInfo.isPresent())
                return classInfo;
        }

        this.log.accept("Can't Find Class: " + name);

        return Optional.empty();
    }

    void clearCache() {
        this.classCache.clear();
    }

    @Override
    public void close() throws IOException {
        for (ClassProvider classProvider : this.classProviders) {
            classProvider.close();
        }
    }
}
