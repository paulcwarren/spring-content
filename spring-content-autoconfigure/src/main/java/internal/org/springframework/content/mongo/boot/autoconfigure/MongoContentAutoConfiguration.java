package internal.org.springframework.content.mongo.boot.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.mongodb.client.MongoClient;

import internal.org.springframework.content.mongo.config.MongoContentStoresRegistrar;
import internal.org.springframework.content.mongo.config.MongoStoreConfiguration;
import internal.org.springframework.content.mongo.config.MongoStoreFactoryBean;
import internal.org.springframework.versions.jpa.boot.autoconfigure.JpaVersionsAutoConfiguration;

@Configuration
@AutoConfigureAfter({ JpaVersionsAutoConfiguration.class })
@ConditionalOnClass({ MongoClient.class, MongoContentStoresRegistrar.class })
@ConditionalOnProperty(
        prefix="spring.content.storage.type",
        name = "default",
        havingValue = "gridfs",
        matchIfMissing=true)
@ConditionalOnMissingBean(MongoStoreFactoryBean.class)
@Import({ MongoContentAutoConfigureRegistrar.class, MongoStoreConfiguration.class })
public class MongoContentAutoConfiguration {

}
