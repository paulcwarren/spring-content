package org.springframework.versions.jpa.config;

import javax.persistence.EntityManager;
import javax.sql.DataSource;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.versions.LockingAndVersioningProxyFactory;

import internal.org.springframework.versions.AuthenticationFacade;
import internal.org.springframework.versions.jpa.CloningService;
import internal.org.springframework.versions.jpa.EntityInformationFacade;
import internal.org.springframework.versions.jpa.JpaAutoVersioningProxyFactoryImpl;
import internal.org.springframework.versions.jpa.JpaCloningServiceImpl;
import internal.org.springframework.versions.jpa.JpaVersioningServiceImpl;
import internal.org.springframework.versions.jpa.VersioningService;

@Configuration
public class JpaAutoVersioningConfig {

    @Bean
    public AuthenticationFacade auth() {
        return new AuthenticationFacade();
    }

    @Bean
    public EntityInformationFacade entityInformation() {
        return new EntityInformationFacade();
    }

//    @Bean
//    public LockingService lockingService(DataSource dataSource) {
//        return new JpaLockingServiceImpl(new JdbcTemplate(dataSource));
//    }

    @Bean
    public VersioningService versioningService(EntityManager em) {
        return new JpaVersioningServiceImpl(em);
    }

    @Bean
    public CloningService cloningService() {
        return new JpaCloningServiceImpl();
    }

    @Bean
    public LockingAndVersioningProxyFactory lockingAndVersioningService(BeanFactory bf, DataSource ds, PlatformTransactionManager txn, EntityManager em) {
        return new JpaAutoVersioningProxyFactoryImpl(bf, txn, em, auth());
    }
}
