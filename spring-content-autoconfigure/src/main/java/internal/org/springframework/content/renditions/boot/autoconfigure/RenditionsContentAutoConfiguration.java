package internal.org.springframework.content.renditions.boot.autoconfigure;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.content.docx4j.config.Docx4jConfiguration;
import org.springframework.content.renditions.config.RenditionsConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(RenditionsConfiguration.class)
public class RenditionsContentAutoConfiguration {

    @Configuration
    @ConditionalOnClass(Docx4jConfiguration.class)
    @Import(Docx4jConfiguration.class)
    public static class Docx4jAutoConfiguration {
        //
    }

}
