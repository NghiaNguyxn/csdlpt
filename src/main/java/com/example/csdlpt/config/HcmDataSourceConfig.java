package com.example.csdlpt.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import com.example.csdlpt.common.JpaConfigHelper;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import jakarta.persistence.EntityManagerFactory;
import javax.sql.DataSource;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = "com.example.csdlpt.repository.site_hcm", entityManagerFactoryRef = "hcmEntityManagerFactory", transactionManagerRef = "hcmTransactionManager")
public class HcmDataSourceConfig {

    @Bean(name = "hcmDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.hcm")
    public DataSource dataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean(name = "hcmEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(
            @Qualifier("hcmDataSource") DataSource dataSource) {
        return JpaConfigHelper.createEntityManagerFactory(dataSource, "hcm");
    }

    @Bean(name = "hcmTransactionManager")
    public PlatformTransactionManager transactionManager(
            @Qualifier("hcmEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}
