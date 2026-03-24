package org.heior.miniclaw;

import org.heior.miniclaw.config.MiniClawProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@ConfigurationPropertiesScan("org.heior.miniclaw")
public class MiniclawApplication {
    public static void main(String[] args) {
        SpringApplication.run(MiniclawApplication.class, args);
    }
}
