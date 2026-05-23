package com.example.csdlpt.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class CentralDataSourceConfig {

    @Bean(name = "centralDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.central")
    public DataSource dataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean(name = "centralJdbcTemplate")
    public JdbcTemplate centralJdbcTemplate(@Qualifier("centralDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
