// src/main/java/org/example/zeniqbackend/config/StaticResourceConfig.java
package org.example.eshopbackend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.*;

import java.util.concurrent.TimeUnit;

@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // mapuje URL /static/** na lokální adresář ./static/
        registry.addResourceHandler("/static/**")
                .addResourceLocations("file:./static/")
                .setCacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic().immutable());
    }
}
