package org.springframework.versions.jpa.config;

import internal.org.springframework.versions.AuthenticationFacade;
import internal.org.springframework.versions.LockingService;
import internal.org.springframework.versions.jpa.JpaLockingAndVersioningServiceImpl;
import internal.org.springframework.versions.jpa.JpaLockingServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
public class JpaVersionsConfig {

    @Bean
    public AuthenticationFacade auth() {
        return new AuthenticationFacade();
    }

    @Bean
    public LockingService lockingService(DataSource dataSource, PlatformTransactionManager txnMgr, AuthenticationFacade auth) {
        return new JpaLockingServiceImpl(new JdbcTemplate(dataSource), txnMgr, auth);
    }

    @Bean
    public JpaLockingAndVersioningServiceImpl lockingAndVersioningService() {
        return new JpaLockingAndVersioningServiceImpl();
    }
}
