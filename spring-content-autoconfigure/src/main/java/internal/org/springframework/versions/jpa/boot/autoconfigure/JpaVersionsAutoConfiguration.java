package internal.org.springframework.versions.jpa.boot.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ResourceLoader;
import org.springframework.versions.jpa.config.JpaLockingAndVersioningConfig;

import javax.sql.DataSource;

@Configuration
@AutoConfigureAfter(DataSourceAutoConfiguration.class)
@ConditionalOnClass({ DataSource.class, JpaLockingAndVersioningConfig.class })
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
    public JpaVersionsDatabaseInitializer jpaVersionsDatabaseInitializer(DataSource dataSource,
                                                             ResourceLoader resourceLoader) {
        return new JpaVersionsDatabaseInitializer(dataSource, resourceLoader, this.properties);
    }
}
