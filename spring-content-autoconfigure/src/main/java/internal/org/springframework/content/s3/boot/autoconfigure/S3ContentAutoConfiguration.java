package internal.org.springframework.content.s3.boot.autoconfigure;

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

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

import internal.org.springframework.content.s3.config.S3StoreConfiguration;
import internal.org.springframework.content.s3.config.S3StoreFactoryBean;
import internal.org.springframework.versions.jpa.boot.autoconfigure.JpaVersionsAutoConfiguration;

@Configuration
@AutoConfigureAfter({ JpaVersionsAutoConfiguration.class })
@ConditionalOnClass(AmazonS3Client.class)
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
	public AmazonS3 amazonS3(S3Properties props) {
	    AmazonS3ClientBuilder builder = AmazonS3ClientBuilder
                .standard();

		if (StringUtils.hasText(props.endpoint)) {
		    builder.withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(props.endpoint, null));
		}

        if (StringUtils.hasText(props.accessKey) && StringUtils.hasText(props.secretKey)) {
            AWSCredentialsProvider provider = new AWSStaticCredentialsProvider(new BasicAWSCredentials(props.accessKey, props.secretKey));
            builder.withCredentials(provider);
        }

        if (props.pathStleAccess) {
            builder.withPathStyleAccessEnabled(true);
        }

		return builder.build();
	}

    @Component
    @ConfigurationProperties(prefix = "spring.content.s3")
    public static class S3Properties {

        public String endpoint;
        public String accessKey;
        public String secretKey;
        public boolean pathStleAccess;

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public void setAccessKey(String accessKey) {
            this.accessKey = accessKey;
        }

        public void setSecretKey(String secretKey) {
            this.secretKey = secretKey;
        }

        public void setPathStyleAccess(boolean pathStleAccess) {
            this.pathStleAccess = pathStleAccess;
        }
    }
}
