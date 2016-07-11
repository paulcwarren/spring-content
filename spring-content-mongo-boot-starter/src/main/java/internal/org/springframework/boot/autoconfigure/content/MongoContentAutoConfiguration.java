package internal.org.springframework.boot.autoconfigure.content;

import com.mongodb.Mongo;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ConditionalOnClass(Mongo.class)
@Import(MongoContentAutoConfigureRegistrar.class)
public class MongoContentAutoConfiguration {

}
