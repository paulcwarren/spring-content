package org.springframework.versions.jpa.config;

import internal.org.springframework.versions.AuthenticationFacade;
import internal.org.springframework.versions.LockingService;
import internal.org.springframework.versions.jpa.CloningService;
import internal.org.springframework.versions.jpa.EntityInformationFacade;
import internal.org.springframework.versions.jpa.JpaCloningServiceImpl;
import internal.org.springframework.versions.jpa.JpaLockingAndVersioningProxyFactoryImpl;
import internal.org.springframework.versions.jpa.JpaLockingServiceImpl;
import internal.org.springframework.versions.jpa.JpaVersioningServiceImpl;
import internal.org.springframework.versions.jpa.VersioningService;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.versions.LockingAndVersioningProxyFactory;

import javax.persistence.EntityManager;
import javax.sql.DataSource;

@Configuration
public class JpaLockingAndVersioningConfig {

    @Bean
    public AuthenticationFacade auth() {
        return new AuthenticationFacade();
    }

    @Bean
    public EntityInformationFacade entityInformation() {
        return new EntityInformationFacade();
    }

    @Bean
    public LockingService lockingService(DataSource dataSource) {
        return new JpaLockingServiceImpl(new JdbcTemplate(dataSource));
    }

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
        return new JpaLockingAndVersioningProxyFactoryImpl(bf, txn, em, lockingService(ds), auth());
    }
}
