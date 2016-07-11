package internal.org.springframework.boot.autoconfigure.content;

import com.amazonaws.services.s3.AmazonS3Client;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ConditionalOnClass(AmazonS3Client.class)
@Import(S3ContentAutoConfigureRegistrar.class)
public class S3ContentAutoConfiguration {

}
