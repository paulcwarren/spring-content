package org.springframework.content.elasticsearch.boot;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jConfiguration;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;
import internal.org.springframework.content.elasticsearch.ElasticsearchIndexServiceImpl;
import internal.org.springframework.content.elasticsearch.ElasticsearchIndexer;
import internal.org.springframework.content.elasticsearch.IndexManager;
import internal.org.springframework.content.elasticsearch.boot.autoconfigure.ElasticsearchAutoConfiguration;
import org.elasticsearch.client.RestHighLevelClient;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.content.elasticsearch.EnableElasticsearchFulltextIndexing;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@RunWith(Ginkgo4jRunner.class)
@Ginkgo4jConfiguration(threads=1)
public class ElasticsearchAutoConfigurationTest {

    private static RestHighLevelClient client;

    static {
        client = mock(RestHighLevelClient.class);
    }

    {
        Describe("given a context without a rest high level client configured", () -> {
            It("should create a client", () -> {
                final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
                        .withConfiguration(AutoConfigurations.of(ElasticsearchAutoConfiguration.class));

                contextRunner.withUserConfiguration(ContextWithoutClientBean.class).run((context) -> {
                    assertThat(context).hasSingleBean(RestHighLevelClient.class);
                    assertThat(context).getBean(RestHighLevelClient.class).isNotEqualTo(client);
                    assertThat(context).hasSingleBean(ElasticsearchAutoConfiguration.ElasticsearchProperties.class);
                    assertThat(context).hasSingleBean(ElasticsearchIndexer.class);

                    assertThat(context).hasSingleBean(ElasticsearchIndexServiceImpl.class);
                    assertThat(context).hasSingleBean(IndexManager.class);
                });
            });
        });

        Describe("given a context with a rest high level client configured", () -> {
            It("should use that client", () -> {
                final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
                        .withConfiguration(AutoConfigurations.of(ElasticsearchAutoConfiguration.class));

                contextRunner.withUserConfiguration(ContextWithClientBean.class).run((context) -> {
                    assertThat(context).getBean(RestHighLevelClient.class).isEqualTo(client);
                });

            });
        });

        Describe("given a context with auto indexing disabled", () -> {
            BeforeEach(() -> {
                System.setProperty("spring.content.elasticsearch.autoindex", "false");
            });
            AfterEach(() -> {
                System.clearProperty("spring.content.elasticsearch.autoindex");
            });

            It("should not configure the indexing event handler", () -> {
                final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
                        .withConfiguration(AutoConfigurations.of(ElasticsearchAutoConfiguration.class));

                contextRunner.withUserConfiguration(ContextWithClientBean.class).run((context) -> {
                    assertThat(context).doesNotHaveBean(ElasticsearchIndexer.class);
                });

            });
        });

        Describe("given a context with auto-indexing configured", () -> {
            BeforeEach(() -> {
                System.setProperty("spring.content.elasticsearch.autoindex", "true");
            });
            AfterEach(() -> {
                System.clearProperty("spring.content.elasticsearch.autoindex");
            });

            It("should load the context", () -> {
                final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
                        .withConfiguration(AutoConfigurations.of(ElasticsearchAutoConfiguration.class));

                contextRunner.withUserConfiguration(ContextWithClientBean.class).run((context) -> {
                    assertThat(context).hasSingleBean(ElasticsearchIndexer.class);
                });

            });
        });


        Describe("given a context that already enables elasticsearch fulltext indexing", () -> {

            It("should load the context and not throw a BeanDefinitionOverrideException", () -> {
                final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
                        .withConfiguration(AutoConfigurations.of(ElasticsearchAutoConfiguration.class));

                contextRunner.withUserConfiguration(ContextWithEnablement.class).run((context) -> {
                    assertThat(context).hasSingleBean(RestHighLevelClient.class);
                });

            });
        });
    }

    @Configuration
    public static class ContextWithoutClientBean {
    }

    @Configuration
    public static class ContextWithClientBean {

        @Bean
        public RestHighLevelClient client() {
            return client;
        }
    }

    @Configuration
    @EnableElasticsearchFulltextIndexing
    public static class ContextWithEnablement {

        @Bean
        public RestHighLevelClient client() {
            return client;
        }
    }
}
