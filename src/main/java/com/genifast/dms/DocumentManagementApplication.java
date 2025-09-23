package com.genifast.dms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.genifast.dms.config.ApplicationProperties;

@SpringBootApplication
@EnableAsync
@EnableConfigurationProperties(ApplicationProperties.class)
@EnableScheduling
@EnableJpaAuditing(auditorAwareRef = "appAuditorAware")
@EnableCaching
public class DocumentManagementApplication {

	public static void main(String[] args) {
		SpringApplication.run(DocumentManagementApplication.class, args);
	}

}
