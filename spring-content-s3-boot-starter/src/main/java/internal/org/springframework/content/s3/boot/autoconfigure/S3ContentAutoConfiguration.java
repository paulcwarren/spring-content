package internal.org.springframework.content.s3.boot.autoconfigure;

import com.amazonaws.services.s3.AmazonS3Client;

import internal.org.springframework.content.s3.config.S3ContentRepositoryConfiguration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cloud.aws.context.config.annotation.ContextResourceLoaderConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ConditionalOnClass(AmazonS3Client.class)
@Import({ContextResourceLoaderConfiguration.class, S3ContentRepositoryConfiguration.class, S3ContentAutoConfigureRegistrar.class})
public class S3ContentAutoConfiguration {

}
