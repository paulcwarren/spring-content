package internal.org.springframework.content.s3.boot.autoconfigure;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;

import internal.org.springframework.content.s3.config.S3StoreConfiguration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ConditionalOnClass(AmazonS3Client.class)
@Import({ S3StoreConfiguration.class, S3ContentAutoConfigureRegistrar.class })
public class S3ContentAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public AmazonS3 s3Client() {
		AmazonS3 s3Client = new AmazonS3Client();
		return s3Client;
	}
}
