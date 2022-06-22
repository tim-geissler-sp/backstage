/*
 * Copyright (C) 2020 SailPoint Technologies, Inc. All rights reserved.
 */
package com.sailpoint.sp.identity.event;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;

/**
 * IdentityEventServerApplication
 */
@SpringBootApplication
@EnableResourceServer
@EnableConfigurationProperties(IdentityEventProperties.class)
@EnableScheduling
public class IdentityEventServerApplication {

	/**
	 * The application entry-point.
	 *
	 * @param args The command-line arguments.
	 */
	public static void main(String[] args) {
		SpringApplication.run(IdentityEventServerApplication.class, args);
	}
}
