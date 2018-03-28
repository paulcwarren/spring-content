package internal.org.springframework.content.jpa.boot.autoconfigure;

import internal.org.springframework.content.jpa.config.JpaStoreConfiguration;
import internal.org.springframework.content.jpa.config.JpaStoresRegistrar;
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

import javax.sql.DataSource;

@Configuration
@AutoConfigureAfter(DataSourceAutoConfiguration.class)
@ConditionalOnClass({DataSource.class, JpaStoresRegistrar.class})
@Import({JpaContentAutoConfigureRegistrar.class, JpaStoreConfiguration.class})
@EnableConfigurationProperties(ContentJpaProperties.class)
public class JpaContentAutoConfiguration {

    private final ContentJpaProperties properties;

    public JpaContentAutoConfiguration(ContentJpaProperties properties) {
        this.properties = properties;
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(DataSource.class)
    public ContentJpaDatabaseInitializer databaseInitializer(DataSource dataSource, ResourceLoader resourceLoader) {
        return new ContentJpaDatabaseInitializer(dataSource, resourceLoader, this.properties);
    }
}
