package org.springframework.content.elasticsearch.boot;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jConfiguration;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;
import internal.org.springframework.content.elasticsearch.ElasticsearchIndexServiceImpl;
import internal.org.springframework.content.elasticsearch.ElasticsearchIndexer;
import internal.org.springframework.content.elasticsearch.IndexManager;
import internal.org.springframework.content.elasticsearch.boot.autoconfigure.ElasticsearchAutoConfiguration;
import internal.org.springframework.content.fs.boot.autoconfigure.FilesystemContentAutoConfiguration;
import internal.org.springframework.content.jpa.boot.autoconfigure.JpaContentAutoConfiguration;
import internal.org.springframework.content.mongo.boot.autoconfigure.MongoContentAutoConfiguration;
import internal.org.springframework.content.renditions.boot.autoconfigure.RenditionsContentAutoConfiguration;
import internal.org.springframework.content.s3.boot.autoconfigure.S3ContentAutoConfiguration;
import internal.org.springframework.versions.jpa.boot.autoconfigure.JpaVersionsAutoConfiguration;
import org.elasticsearch.client.RestHighLevelClient;
import org.junit.Assert;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.content.elasticsearch.EnableElasticsearchFulltextIndexing;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableMBeanExport;
import org.springframework.context.annotation.PropertySource;
import org.springframework.jmx.support.RegistrationPolicy;
import org.springframework.test.context.TestPropertySource;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.AfterEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.FIt;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
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
                AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
                context.register(ContextWithoutClientBean.class);
                context.refresh();

                assertThat(context.getBean(RestHighLevelClient.class), is(not(nullValue())));
                assertThat(context.getBean(RestHighLevelClient.class), is(not(client)));
                assertThat(context.getBean(ElasticsearchAutoConfiguration.ElasticsearchProperties.class), is(not(nullValue())));
                assertThat(context.getBean(ElasticsearchIndexer.class), is(not(nullValue())));

                assertThat(context.getBean(ElasticsearchIndexServiceImpl.class), is(not(nullValue())));
                assertThat(context.getBean(IndexManager.class), is(not(nullValue())));

                context.close();
            });
        });

        Describe("given a context with a rest high level client configured", () -> {
            It("should use that client", () -> {
                AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
                context.register(ContextWithClientBean.class);
                 context.refresh();

                assertThat(context.getBean(RestHighLevelClient.class), is(client));

                context.close();
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
                AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
                context.register(ContextWithClientBean.class);
                context.refresh();

                try {
                    context.getBean(ElasticsearchIndexer.class);
                    Assert.fail("ElasticsearchIndexer not expected");
                } catch (NoSuchBeanDefinitionException nsbde) {}

                context.close();
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
                AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
                context.register(ContextWithClientBean.class);
                context.refresh();

                assertThat(context.getBean(ElasticsearchIndexer.class), is(not(nullValue())));

                context.close();
            });
        });


        Describe("given a context that already enables elasticsearch fulltext indexing", () -> {

            It("should load the context and not throw a BeanDefinitionOverrideException", () -> {
                AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
                context.setAllowBeanDefinitionOverriding(false);
                context.register(ContextWithEnablement.class);
                context.refresh();
                context.close();
            });
        });
    }

    @Configuration
    @EnableAutoConfiguration(exclude= {
            FilesystemContentAutoConfiguration.class,
            MongoAutoConfiguration.class,
            JpaContentAutoConfiguration.class,
            JpaVersionsAutoConfiguration.class,
            MongoContentAutoConfiguration.class,
            RenditionsContentAutoConfiguration.class,
            S3ContentAutoConfiguration.class})
    @EnableMBeanExport(registration = RegistrationPolicy.IGNORE_EXISTING)
    public static class ContextWithoutClientBean {
    }

    @Configuration
    @EnableAutoConfiguration(exclude= {
            FilesystemContentAutoConfiguration.class,
            MongoAutoConfiguration.class,
            JpaContentAutoConfiguration.class,
            JpaVersionsAutoConfiguration.class,
            MongoContentAutoConfiguration.class,
            RenditionsContentAutoConfiguration.class,
            S3ContentAutoConfiguration.class})
    @EnableMBeanExport(registration = RegistrationPolicy.IGNORE_EXISTING)
    public static class ContextWithClientBean {

        @Bean
        public RestHighLevelClient client() {
            return client;
        }
    }

    @Configuration
    @EnableElasticsearchFulltextIndexing
    @EnableAutoConfiguration(exclude= {
            FilesystemContentAutoConfiguration.class,
            MongoAutoConfiguration.class,
            JpaContentAutoConfiguration.class,
            JpaVersionsAutoConfiguration.class,
            MongoContentAutoConfiguration.class,
            RenditionsContentAutoConfiguration.class,
            S3ContentAutoConfiguration.class})
    @EnableMBeanExport(registration = RegistrationPolicy.IGNORE_EXISTING)
    public static class ContextWithEnablement {

        @Bean
        public RestHighLevelClient client() {
            return client;
        }
    }
}
