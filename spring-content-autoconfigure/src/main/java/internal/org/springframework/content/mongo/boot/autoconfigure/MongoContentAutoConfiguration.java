package internal.org.springframework.content.mongo.boot.autoconfigure;

import com.mongodb.client.MongoClient;
import internal.org.springframework.content.mongo.config.MongoContentStoresRegistrar;
import internal.org.springframework.content.mongo.config.MongoStoreConfiguration;
import internal.org.springframework.content.mongo.config.MongoStoreFactoryBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ConditionalOnClass({ MongoClient.class, MongoContentStoresRegistrar.class })
@ConditionalOnMissingBean(MongoStoreFactoryBean.class)
@Import({ MongoContentAutoConfigureRegistrar.class, MongoStoreConfiguration.class })
public class MongoContentAutoConfiguration {

}
