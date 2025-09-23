package com.genifast.dms.config;

import javax.sql.DataSource;

import org.camunda.bpm.engine.spring.SpringProcessEngineConfiguration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import com.zaxxer.hikari.HikariDataSource;

import jakarta.persistence.EntityManagerFactory;

@Configuration
public class CamundaDataSourceConfiguration {

    // 1. Cấu hình các thuộc tính cho datasource của Camunda
    @Bean(name = "camundaDataSourceProperties")
    @ConfigurationProperties(prefix = "camunda.datasource")
    public DataSourceProperties camundaDataSourceProperties() {
        return new DataSourceProperties();
    }

    // 2. Tạo bean DataSource riêng cho Camunda từ các thuộc tính trên
    @Bean(name = "camundaBpmDataSource")
    public DataSource camundaBpmDataSource(@Qualifier("camundaDataSourceProperties") DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }

    // 3. Cấu hình Process Engine của Camunda để sử dụng DataSource riêng
    @Bean
    public SpringProcessEngineConfiguration processEngineConfiguration(
            @Qualifier("camundaBpmDataSource") DataSource dataSource,
            @Qualifier("transactionManager") PlatformTransactionManager transactionManager) {

        SpringProcessEngineConfiguration config = new SpringProcessEngineConfiguration();

        config.setDataSource(dataSource);
        config.setTransactionManager(transactionManager); // Dùng chung transaction manager chính
        config.setDatabaseSchemaUpdate("true");
        config.setHistory("full");
        config.setJobExecutorActivate(true);
        config.setDbIdentityUsed(false);

        // Chỉ định schema một lần nữa để chắc chắn
        config.setDatabaseSchema("camunda");
        config.setDatabaseTablePrefix("camunda.");

        return config;
    }

    // 4. Đảm bảo Transaction Manager chính của ứng dụng được ưu tiên
    @Bean
    @Primary
    public PlatformTransactionManager transactionManager(EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }
}