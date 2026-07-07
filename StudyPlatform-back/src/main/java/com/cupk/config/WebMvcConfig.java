package com.cupk.config;

import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置类
 * 配置静态资源处理，将 /files/** 请求映射到文件存储目录
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    /**
     * 添加资源处理器
     * 将 /files/** 请求路径映射到实际文件存储位置
     *
     * @param registry ResourceHandlerRegistry资源处理器注册器
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String storageLocation = resolveStoragePath().toUri().toString();
        registry.addResourceHandler("/files/**")
                .addResourceLocations(storageLocation);
    }

    /**
     * 解析文件存储路径
     * 优先查找当前目录下的 storage 文件夹，其次查找后端模块下的 storage 文件夹
     *
     * @return 文件存储路径
     */
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
