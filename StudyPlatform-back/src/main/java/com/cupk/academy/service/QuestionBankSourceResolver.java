package com.cupk.academy.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;

final class QuestionBankSourceResolver {
    private static final String RESOURCE_ROOT = "question-bank-sources";
    private static final String SOURCE_DIRECTORY_ENV = "QUESTION_BANK_SOURCE_DIR";

    private QuestionBankSourceResolver() {
    }

    static Optional<SourceFile> find(String fileName, Logger log) {
        for (Path directory : sourceDirectories()) {
            Path sourcePath = directory.resolve(fileName).toAbsolutePath().normalize();
            if (Files.isRegularFile(sourcePath)) {
                return Optional.of(SourceFile.file(sourcePath));
            }
        }

        String resourcePath = RESOURCE_ROOT + "/" + fileName;
        URL resourceUrl = contextClassLoader().getResource(resourcePath);
        if (resourceUrl != null) {
            return Optional.of(SourceFile.resource(fileName, resourcePath, resourceUrl));
        }

        log.warn("Question bank source resource {} not found", resourcePath);
        return Optional.empty();
    }

    private static List<Path> sourceDirectories() {
        List<Path> directories = new ArrayList<>();
        String configuredDirectory = System.getenv(SOURCE_DIRECTORY_ENV);
        if (configuredDirectory != null && !configuredDirectory.isBlank()) {
            directories.add(Path.of(configuredDirectory));
        }

        Path currentDirectory = Path.of("").toAbsolutePath().normalize();
        directories.add(currentDirectory.resolve("src").resolve("main").resolve("resources").resolve(RESOURCE_ROOT));
        directories.add(currentDirectory.resolve("StudyPlatform-back").resolve("src").resolve("main").resolve("resources").resolve(RESOURCE_ROOT));
        return directories;
    }

    private static ClassLoader contextClassLoader() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        return classLoader == null ? QuestionBankSourceResolver.class.getClassLoader() : classLoader;
    }

    static final class SourceFile {
        private final String fileName;
        private final String location;
        private final Path filePath;
        private final URL resourceUrl;

        private SourceFile(String fileName, String location, Path filePath, URL resourceUrl) {
            this.fileName = fileName;
            this.location = location;
            this.filePath = filePath;
            this.resourceUrl = resourceUrl;
        }

        static SourceFile file(Path filePath) {
            return new SourceFile(
                    filePath.getFileName().toString(),
                    filePath.toString(),
                    filePath,
                    null
            );
        }

        static SourceFile resource(String fileName, String resourcePath, URL resourceUrl) {
            return new SourceFile(fileName, resourcePath, null, resourceUrl);
        }

        String fileName() {
            return fileName;
        }

        String location() {
            return location;
        }

        Reader openReader() throws IOException {
            if (filePath != null) {
                return Files.newBufferedReader(filePath, StandardCharsets.UTF_8);
            }
            return new BufferedReader(new InputStreamReader(resourceUrl.openStream(), StandardCharsets.UTF_8));
        }

        String readString() throws IOException {
            try (Reader reader = openReader()) {
                StringBuilder content = new StringBuilder();
                char[] buffer = new char[4096];
                int readCount;
                while ((readCount = reader.read(buffer)) >= 0) {
                    content.append(buffer, 0, readCount);
                }
                return content.toString();
            }
        }
    }
}
