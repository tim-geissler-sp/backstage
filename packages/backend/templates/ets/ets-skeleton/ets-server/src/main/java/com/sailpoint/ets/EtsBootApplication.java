package com.sailpoint.ets;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;


@EnableScheduling
@SpringBootApplication
@EnableResourceServer
@EnableConfigurationProperties(EtsProperties.class)
public class EtsBootApplication {
	public static void main(String[] args) {
		SpringApplication.run(EtsBootApplication.class, args);
	}
}
