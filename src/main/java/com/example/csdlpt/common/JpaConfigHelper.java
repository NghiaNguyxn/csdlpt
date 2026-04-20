package com.example.csdlpt.common;

import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import javax.sql.DataSource;
import java.util.HashMap;

public class JpaConfigHelper {

    private JpaConfigHelper() {
        throw new IllegalStateException("Utility class");
    }

    public static LocalContainerEntityManagerFactoryBean createEntityManagerFactory(
            DataSource dataSource, String persistenceUnitName) {
        
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan("com.example.csdlpt.entity");
        em.setPersistenceUnitName(persistenceUnitName);
        
        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);
        
        HashMap<String, Object> properties = new HashMap<>();
        properties.put("hibernate.hbm2ddl.auto", "none");
        em.setJpaPropertyMap(properties);
        
        return em;
    }
}
