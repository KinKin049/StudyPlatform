package com.cupk.config;

import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String storageLocation = resolveStoragePath().toUri().toString();
        registry.addResourceHandler("/files/**")
                .addResourceLocations(storageLocation);
    }

    private Path resolveStoragePath() {
        Path currentDirectory = Path.of("").toAbsolutePath();
        Path directStorage = currentDirectory.resolve("storage").normalize();
        if (Files.isDirectory(directStorage)) {
            return directStorage;
        }

        Path backendStorage = currentDirectory.resolve("StudyPlatform-back").resolve("storage").normalize();
        if (Files.isDirectory(backendStorage)) {
            return backendStorage;
        }

        return directStorage;
    }
}
