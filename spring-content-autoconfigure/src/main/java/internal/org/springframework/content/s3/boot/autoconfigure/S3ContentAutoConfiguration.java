package internal.org.springframework.content.s3.boot.autoconfigure;

import java.net.URI;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import internal.org.springframework.content.s3.config.S3StoreConfiguration;
import internal.org.springframework.content.s3.config.S3StoreFactoryBean;
import internal.org.springframework.versions.jpa.boot.autoconfigure.JpaVersionsAutoConfiguration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;


@AutoConfiguration
//@Configuration
@AutoConfigureAfter({ JpaVersionsAutoConfiguration.class })
@ConditionalOnClass(S3Client.class)
@ConditionalOnProperty(
        prefix="spring.content.storage.type",
        name = "default",
        havingValue = "s3",
        matchIfMissing=true)
public class S3ContentAutoConfiguration {

	@Configuration
	@ConditionalOnMissingBean(S3StoreFactoryBean.class)
	@Import({ S3StoreConfiguration.class, S3ContentAutoConfigureRegistrar.class })
	public static class EnableS3StoresConfig {}

	@Bean
	@ConditionalOnMissingBean()
	public S3Client amazonS3(S3Properties props) {
	    S3ClientBuilder builder = S3Client.builder();

		if (StringUtils.hasText(props.endpoint)) {
		    builder.endpointOverride(URI.create(props.endpoint));
		}

        if (StringUtils.hasText(props.accessKey) && StringUtils.hasText(props.secretKey)) {
            AwsCredentialsProvider provider = StaticCredentialsProvider.create(AwsBasicCredentials.create(props.accessKey, props.secretKey));
            builder.credentialsProvider(provider);
        }

        if (props.pathStyleAccess) {
            builder.serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build());
        }

		return builder.build();
	}

    @Component
    @ConfigurationProperties(prefix = "spring.content.s3")
    public static class S3Properties {

        public String endpoint;
        public String accessKey;
        public String secretKey;
        public boolean pathStyleAccess;

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public void setAccessKey(String accessKey) {
            this.accessKey = accessKey;
        }

        public void setSecretKey(String secretKey) {
            this.secretKey = secretKey;
        }

        public void setPathStyleAccess(boolean pathStyleAccess) {
            this.pathStyleAccess = pathStyleAccess;
        }
    }
}
