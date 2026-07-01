package com.cupk.oj.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * External OJ settings bound from `oj.*` application properties.
 */
@Component
@ConfigurationProperties(prefix = "oj")
public class OjProperties {
    private String sandboxUrl;

    public String getSandboxUrl() {
        return sandboxUrl;
    }

    public void setSandboxUrl(String sandboxUrl) {
        this.sandboxUrl = sandboxUrl;
    }
}
