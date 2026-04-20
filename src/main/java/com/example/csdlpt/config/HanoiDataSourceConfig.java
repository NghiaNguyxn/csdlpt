package com.example.csdlpt.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
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
@EnableJpaRepositories(basePackages = "com.example.csdlpt.repository.site_hn", entityManagerFactoryRef = "hanoiEntityManagerFactory", transactionManagerRef = "hanoiTransactionManager")
public class HanoiDataSourceConfig {

    @Primary
    @Bean(name = "hanoiDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.hn")
    public DataSource dataSource() {
        return DataSourceBuilder.create().build();
    }

    @Primary
    @Bean(name = "hanoiEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(
            @Qualifier("hanoiDataSource") DataSource dataSource) {
        return JpaConfigHelper.createEntityManagerFactory(dataSource, "hanoi");
    }

    @Primary
    @Bean(name = "hanoiTransactionManager")
    public PlatformTransactionManager transactionManager(
            @Qualifier("hanoiEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}
