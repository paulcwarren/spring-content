package internal.org.springframework.content.mongo.boot.autoconfigure;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.mongodb.Mongo;

import internal.org.springframework.content.mongo.config.MongoContentStoresRegistrar;
import internal.org.springframework.content.mongo.config.MongoStoreConfiguration;

@Configuration
@ConditionalOnClass({Mongo.class, MongoContentStoresRegistrar.class})
@Import({MongoContentAutoConfigureRegistrar.class, MongoStoreConfiguration.class})
public class MongoContentAutoConfiguration {

}
