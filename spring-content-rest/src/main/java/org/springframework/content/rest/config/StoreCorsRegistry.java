package org.springframework.content.rest.config;

import java.util.Map;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

public class StoreCorsRegistry extends CorsRegistry {

    @Override
    public Map<String, CorsConfiguration> getCorsConfigurations() {
        return super.getCorsConfigurations();
    }
}
