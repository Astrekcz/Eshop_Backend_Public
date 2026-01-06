// src/main/java/org/example/zeniqbackend/config/WebConfig.java
package org.example.eshopbackend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path dir = Paths.get("./static/images").toAbsolutePath().normalize();
        // veřejná URL: /static/images/...
        registry.addResourceHandler("/static/images/**")
                .addResourceLocations("file:" + dir.toString() + "/")
                .setCachePeriod(31536000); // 1 rok
    }
}
