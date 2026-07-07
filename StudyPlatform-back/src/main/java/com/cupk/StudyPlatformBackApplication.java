package com.cupk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 学习平台后端应用启动类
 */
@SpringBootApplication
public class StudyPlatformBackApplication {

    /**
     * 应用主入口方法，启动Spring Boot应用
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(StudyPlatformBackApplication.class, args);
    }

}
