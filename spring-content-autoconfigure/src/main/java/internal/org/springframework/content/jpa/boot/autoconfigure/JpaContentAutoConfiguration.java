package internal.org.springframework.content.jpa.boot.autoconfigure;

import javax.persistence.EntityManager;
import javax.sql.DataSource;

import internal.org.springframework.content.jpa.config.JpaStoreConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.content.jpa.boot.ContentJpaDatabaseInitializer;
import org.springframework.content.jpa.boot.ContentJpaProperties;
import org.springframework.content.jpa.config.JpaStoreConfigurer;
import org.springframework.content.jpa.config.JpaStoreProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import internal.org.springframework.content.jpa.config.JpaStoresRegistrar;
import org.springframework.core.io.ResourceLoader;

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

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix="spring.content.jpa", name="commit-timeout")
    public JpaStoreConfigurer configurer() {
        return new JpaStoreConfigurer() {
            @Override
            public void configure(JpaStoreProperties store) {
                store.commitTimeout(properties.getCommitTimeout());
            }
        };
    }
}
