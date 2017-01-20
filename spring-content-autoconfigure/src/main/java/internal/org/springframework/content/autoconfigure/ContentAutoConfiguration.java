package internal.org.springframework.content.autoconfigure;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.format.support.DefaultFormattingConversionService;

@Configuration
public class ContentAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(ConversionService.class)
    public ConversionService conversionService() {
        return new DefaultFormattingConversionService();
    }

}
