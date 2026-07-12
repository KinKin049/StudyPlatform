package com.cupk.config;

import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 文件存储路径提供者，解析并管理文件存储根目录。
 * 支持从配置文件读取自定义路径，并自动探测项目结构中的存储目录。
 */
@Component
public class StoragePathProvider {
    private final Path configuredRoot;

    /**
     * 构造函数，从配置文件读取存储根目录并规范化为绝对路径。
     *
     * @param storageRoot 存储根目录路径，默认为"storage"
     */
    public StoragePathProvider(@Value("${app.storage.root:storage}") String storageRoot) {
        this.configuredRoot = Path.of(storageRoot == null || storageRoot.isBlank() ? "storage" : storageRoot)
                .toAbsolutePath()
                .normalize();
    }

    /**
     * 获取文件存储根目录。
     * 优先返回配置的目录，若不存在则探测项目结构中的StudyPlatform-back/storage目录。
     *
     * @return 存储根目录的绝对路径
     */
    public Path storageRoot() {
        if (Files.isDirectory(configuredRoot)) {
            return configuredRoot;
        }

        Path currentDirectory = Path.of("").toAbsolutePath().normalize();
        Path backendStorage = currentDirectory.resolve("StudyPlatform-back").resolve("storage").normalize();
        if (Files.isDirectory(backendStorage)) {
            return backendStorage;
        }

        return configuredRoot;
    }
}
