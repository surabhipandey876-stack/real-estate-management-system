package com.realestate.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
public class PropertyImageWebConfig implements WebMvcConfigurer {

    private final String resourceLocation;

    public PropertyImageWebConfig(
            @Value("${app.property-images.directory:uploads/property-images}") String directory
    ) {
        String location = Path.of(directory).toAbsolutePath().normalize().toUri().toString();
        this.resourceLocation = location.endsWith("/") ? location : location + "/";
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/property-images/**")
                .addResourceLocations(resourceLocation);
    }
}
