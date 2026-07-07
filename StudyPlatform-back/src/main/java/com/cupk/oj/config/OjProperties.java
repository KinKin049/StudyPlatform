package com.cupk.oj.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * OJ 在线判题系统配置属性类
 * 从 `oj.*` 前缀的应用配置中绑定外部 OJ 设置
 */
@Component
@ConfigurationProperties(prefix = "oj")
public class OjProperties {

    /**
     * 代码沙箱服务地址
     */
    private String sandboxUrl;

    /**
     * 获取代码沙箱服务地址
     *
     * @return 代码沙箱服务 URL
     */
    public String getSandboxUrl() {
        return sandboxUrl;
    }

    /**
     * 设置代码沙箱服务地址
     *
     * @param sandboxUrl 代码沙箱服务 URL
     */
    public void setSandboxUrl(String sandboxUrl) {
        this.sandboxUrl = sandboxUrl;
    }
}
