package org.springframework.content.rest.config;

import java.io.IOException;
import java.nio.file.Files;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jConfiguration;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;
import org.junit.runner.RunWith;

import org.springframework.content.commons.annotations.Content;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.fs.config.EnableFilesystemStores;
import org.springframework.content.fs.io.FileSystemResourceLoader;
import org.springframework.content.fs.store.FilesystemContentStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.rest.webmvc.config.RepositoryRestMvcConfiguration;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.DelegatingWebMvcConfiguration;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(Ginkgo4jRunner.class)
@Ginkgo4jConfiguration(threads = 1)
public class RestConfigurationTest {

   private AnnotationConfigWebApplicationContext context;

   // mocks
   private static ContentRestConfigurer configurer;

   {
      Describe("RestConfiguration", () -> {
         BeforeEach(() -> {
            configurer = mock(ContentRestConfigurer.class);
         });
         Context("given a context with a ContentRestConfiguration", () -> {
            BeforeEach(() -> {
               context = new AnnotationConfigWebApplicationContext();
               context.setServletContext(new MockServletContext());
               context.register(TestConfig.class,
                     DelegatingWebMvcConfiguration.class,
                     RepositoryRestMvcConfiguration.class,
                     RestConfiguration.class);
               context.refresh();
            });

            It("should have a content handler mapping bean", () -> {
               assertThat(context.getBean("contentHandlerMapping"),
                     is(not(nullValue())));
            });

            It("should have the content rest controllers", () -> {
               assertThat(
                     context.getBean("storeRestController"), is(not(nullValue())));
            });

            It("should be configurable", () -> {
               RestConfiguration config = context.getBean(RestConfiguration.class);
               assertThat(config, is(not(nullValue())));

               verify(configurer).configure(config);
            });
         });
      });
   }

   @Configuration
   @EnableFilesystemStores
   public static class TestConfig {

      @Bean
      public FileSystemResourceLoader filesystemRoot() throws IOException {
         return new FileSystemResourceLoader(Files.createTempDirectory("").toFile().getAbsolutePath());
      }

      @Bean
      public ContentRestConfigurer configurer() {
         return configurer;
      }
   }

   @Document
   @Content
   public class TestEntity {
      @Id
      private String id;
      @ContentId
      private String contentId;
   }

   public interface TestEntityRepository extends MongoRepository<TestEntity, String> {
   }

   public interface TestEntityContentRepository extends FilesystemContentStore<TestEntity, String> {
   }
}
