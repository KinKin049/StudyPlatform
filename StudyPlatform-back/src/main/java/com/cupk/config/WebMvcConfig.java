package com.cupk.config;

import java.nio.file.Path;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String storageLocation = Path.of("storage").toAbsolutePath().toUri().toString();
        registry.addResourceHandler("/files/**")
                .addResourceLocations(storageLocation);
    }
}
