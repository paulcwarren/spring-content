package internal.org.springframework.content.s3.boot.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;

import internal.org.springframework.content.s3.config.S3StoreConfiguration;
import internal.org.springframework.content.s3.config.S3StoreFactoryBean;
import internal.org.springframework.versions.jpa.boot.autoconfigure.JpaVersionsAutoConfiguration;

@Configuration
@AutoConfigureAfter({ JpaVersionsAutoConfiguration.class })
@ConditionalOnClass(AmazonS3Client.class)
public class S3ContentAutoConfiguration {

	@Configuration
	@ConditionalOnMissingBean(S3StoreFactoryBean.class)
	@Import({ S3StoreConfiguration.class, S3ContentAutoConfigureRegistrar.class })
	public static class EnableS3StoresConfig {}

	@Bean
	@ConditionalOnMissingBean()
	public AmazonS3 amazonS3() {
		AmazonS3 s3Client = new AmazonS3Client();
		return s3Client;
	}
}
