package com.cupk.oj.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * OJ 在线判题系统异步配置类
 * 启用异步处理能力，支持代码评测任务的异步执行
 */
@Configuration
@EnableAsync
public class OjAsyncConfig {
}
