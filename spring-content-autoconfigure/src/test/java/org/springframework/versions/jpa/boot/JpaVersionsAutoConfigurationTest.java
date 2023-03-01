package org.springframework.versions.jpa.boot;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;

import internal.org.springframework.versions.jpa.boot.autoconfigure.JpaVersionsDatabaseInitializer;
import org.hamcrest.MatcherAssert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.repository.CrudRepository;
import org.springframework.versions.LockingAndVersioningRepository;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jConfiguration;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;

import internal.org.springframework.content.elasticsearch.boot.autoconfigure.ElasticsearchAutoConfiguration;
import internal.org.springframework.content.fs.boot.autoconfigure.FilesystemContentAutoConfiguration;
import internal.org.springframework.content.mongo.boot.autoconfigure.MongoContentAutoConfiguration;
//import internal.org.springframework.content.renditions.boot.autoconfigure.RenditionsContentAutoConfiguration;
import internal.org.springframework.content.s3.boot.autoconfigure.S3ContentAutoConfiguration;

@RunWith(Ginkgo4jRunner.class)
@Ginkgo4jConfiguration(threads = 1)
public class JpaVersionsAutoConfigurationTest {

    private AnnotationConfigApplicationContext context;

    {
        Describe("JpaVersionsAutoConfiguration", () -> {
            Context("given an application context that relies on auto configuration", () -> {
                BeforeEach(() -> {
                    context = new AnnotationConfigApplicationContext();
                    context.register(StarterConfig.class);
                    context.refresh();
                });
                
                It("should include the repository bean", () -> {
                    MatcherAssert.assertThat(context, is(not(nullValue())));
                    MatcherAssert.assertThat(context.getBean(JpaVersionsDatabaseInitializer.class), is(not(nullValue())));
                });
            });
            Context("given an application context with a EnableJpaRepositories annotation", () -> {
                BeforeEach(() -> {
                    context = new AnnotationConfigApplicationContext();
                    context.register(StarterWithAnnotationConfig.class);
                    context.refresh();
                });

                It("should include the repository bean", () -> {
                    MatcherAssert.assertThat(context, is(not(nullValue())));
//                    MatcherAssert.assertThat(context.getBean(NestedTestEntityRepository.class), is(not(nullValue())));
                });
            });
        });
    }

    @Test
    public void test() {
    }

    @Configuration
    @PropertySource("classpath:default.properties")
    @EnableAutoConfiguration(exclude= {
            ElasticsearchAutoConfiguration.class,
            MongoAutoConfiguration.class,
            FilesystemContentAutoConfiguration.class,
            MongoContentAutoConfiguration.class,
//            RenditionsContentAutoConfiguration.class,
            S3ContentAutoConfiguration.class})
    public static class BaseConfig {}
    
    @Configuration
    public static class StarterConfig extends BaseConfig {
    }

    @Configuration
    @EnableJpaRepositories(basePackages="org.springframework.versions",
                           considerNestedRepositories=true)
    public static class StarterWithAnnotationConfig extends BaseConfig {
    }

    public interface NestedTestEntityRepository extends CrudRepository<TestEntityVersioned, Long>, LockingAndVersioningRepository<TestEntityVersioned, Long> {}
}
