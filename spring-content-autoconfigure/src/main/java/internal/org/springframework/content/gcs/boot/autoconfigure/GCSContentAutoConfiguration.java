package internal.org.springframework.content.gcs.boot.autoconfigure;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import internal.org.springframework.content.gcs.config.GCSStoreConfiguration;

@Configuration
@Import({ GCSStoreConfiguration.class, GCSContentAutoConfigureRegistrar.class })
public class GCSContentAutoConfiguration {

}
