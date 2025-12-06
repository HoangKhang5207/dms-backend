package com.genifast.dms.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class StaticResourcesWebConfiguration implements WebMvcConfigurer {

    @Value("${preview.location:file:./uploads/}")
    private String baseURI;

    @Value("${storage.local.base-url:/storage}")
    private String baseUrl;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Map storage URL to local file system
        registry.addResourceHandler(baseUrl + "/**")
               .addResourceLocations(baseURI);
    }
}
