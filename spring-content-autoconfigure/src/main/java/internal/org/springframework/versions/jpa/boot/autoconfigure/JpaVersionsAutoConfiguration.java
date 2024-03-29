package internal.org.springframework.versions.jpa.boot.autoconfigure;

import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.versions.jpa.config.JpaLockingAndVersioningConfig;

@Configuration
@ConditionalOnClass({ DataSource.class, JpaLockingAndVersioningConfig.class })
@AutoConfigureAfter(DataSourceAutoConfiguration.class)
@Import({ JpaLockingAndVersioningConfig.class })
@EnableConfigurationProperties(JpaVersionsProperties.class)
public class JpaVersionsAutoConfiguration {

    private final JpaVersionsProperties properties;

    public JpaVersionsAutoConfiguration(JpaVersionsProperties properties) {
        this.properties = properties;
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(DataSource.class)
    public JpaVersionsDatabaseInitializer jpaVersionsDatabaseInitializer(DataSource dataSource) {
        return new JpaVersionsDatabaseInitializer(dataSource, properties);
    }

    @Configuration
    @Import(JpaVersionsAutoConfigureRegistrar.class)
    public static class JpaVersionAutoConfig {}
}
