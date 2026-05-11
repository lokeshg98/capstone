package com.communitybot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableJpaAuditing       // powers BaseEntity.createdAt / updatedAt
@EnableScheduling        // needed by @Scheduled tasks
@EnableAsync             // enables @Async for bot event listener
@ConfigurationPropertiesScan // auto-registers all @ConfigurationProperties beans
public class CommunityBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(CommunityBotApplication.class, args);
    }
}
