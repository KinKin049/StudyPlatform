package com.cupk.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC配置类，配置CORS跨域映射和静态资源处理器。
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    private final StoragePathProvider storagePathProvider;

    /**
     * 构造函数，注入存储路径提供者。
     *
     * @param storagePathProvider 存储路径提供者
     */
    public WebMvcConfig(StoragePathProvider storagePathProvider) {
        this.storagePathProvider = storagePathProvider;
    }

    /**
     * 配置CORS跨域映射，允许所有路径的跨域请求。
     *
     * @param registry CORS注册器
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("*")
                .allowCredentials(false);
    }

    /**
     * 配置静态资源处理器，将/files/**路径映射到文件存储目录。
     *
     * @param registry 资源处理器注册器
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/files/**")
                .addResourceLocations(storagePathProvider.storageRoot().toUri().toString());
    }
}
