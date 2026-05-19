package com.kb;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;

import java.net.InetAddress;

/**
 * 个人知识库系统 - 启动类
 */
@Slf4j
@SpringBootApplication
@MapperScan("com.kb.mapper")
public class KnowledgeBaseApplication {

    public static void main(String[] args) {
        SpringApplication.run(KnowledgeBaseApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    @Order(2)
    public void onReady(ApplicationReadyEvent event) {
        Environment env = event.getApplicationContext().getEnvironment();
        try {
            String name = env.getProperty("spring.application.name", "知识库");
            String ip = InetAddress.getLocalHost().getHostAddress();
            String port = env.getProperty("server.port", "8080");
            String[] activeProfiles = env.getActiveProfiles();
            String profile = activeProfiles.length > 0 ? String.join(",", activeProfiles) : "default";

            System.out.println();
            System.out.println("----------------------------------------------------------");
            System.out.println("\t" + name + " 启动成功！");
            System.out.println("\t访问地址：http://" + ip + ":" + port);
            System.out.println("\t当前环境：" + profile);
            System.out.println("----------------------------------------------------------");
            System.out.println();
        } catch (Exception e) {
            log.warn("获取本机IP失败", e);
        }
    }
}
