/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.fart.internal;

import net.minecraftforge.fart.api.ClassProvider;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ClassProviderBuilderImpl implements ClassProvider.Builder {
    private final List<FileSystem> fileSystems = new ArrayList<>();
    private final Map<String, Set<Path>> sources = new HashMap<>();
    private final Map<String, Optional<? extends ClassProvider.IClassInfo>> classInfos = new ConcurrentHashMap<>();
    private boolean cacheAll = false;

    public ClassProviderBuilderImpl() {}

    @Override
    public ClassProvider.Builder addLibrary(Path path) {
        try {
            Path libraryDir;
            if (Files.isDirectory(path)) {
                libraryDir = path;
            } else if (Files.isRegularFile(path)) {
                FileSystem zipFs = FileSystems.newFileSystem(path, null);
                this.fileSystems.add(zipFs);
                libraryDir = zipFs.getPath("/");
            } else {
                // We can't load it (it doesn't exist)
                return this;
            }

            Files.walkFileTree(
                libraryDir, new SimpleFileVisitor<Path>() {
                    @Override
                    public @NotNull FileVisitResult preVisitDirectory(Path dir, @NotNull BasicFileAttributes attrs) {
                        Path relativePath = libraryDir.relativize(dir);
                        if (relativePath.startsWith("META-INF")) return FileVisitResult.CONTINUE;
                        sources.computeIfAbsent(
                            relativePath.toString().replace('\\', '/'),
                            ignored -> new HashSet<>()
                        ).add(dir);
                        return FileVisitResult.CONTINUE;
                    }
                }
            );

        } catch (IOException e) {
            throw new RuntimeException("Could not add library: " + path.toAbsolutePath(), e);
        }

        return this;
    }

    @Override
    public ClassProvider.Builder addClass(String name, byte[] value) {
        this.classInfos.computeIfAbsent(name, k -> Optional.of(new ClassProviderImpl.ClassInfo(value)));

        return this;
    }

    @Override
    public ClassProvider.Builder shouldCacheAll(boolean value) {
        this.cacheAll = value;

        return this;
    }

    @Override
    public ClassProvider build() {
        return new ClassProviderImpl(this.fileSystems, this.sources, this.classInfos, this.cacheAll);
    }
}
