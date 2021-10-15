package it.rest.embeddedid;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.hateoas.HypermediaAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.annotations.MimeType;
import org.springframework.content.fs.config.EnableFilesystemStores;
import org.springframework.content.fs.io.FileSystemResourceLoader;
import org.springframework.content.fs.store.FilesystemContentStore;
import org.springframework.content.rest.config.ContentRestConfigurer;
import org.springframework.content.rest.config.RestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jConfiguration;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jSpringRunner;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@RunWith(Ginkgo4jSpringRunner.class)
@Ginkgo4jConfiguration(threads = 1)
@SpringBootTest(classes = {EmbeddedIdTest.Application.class},
                webEnvironment=WebEnvironment.RANDOM_PORT)
@EnableAutoConfiguration(exclude = { HypermediaAutoConfiguration.class, SecurityAutoConfiguration.class })
public class EmbeddedIdTest {

    @LocalServerPort
    private int serverPort;

    @Autowired
    private TestEntityRepository repo;

    @Autowired
    private TestEntityContentRepository store;

    @Autowired
    private WebApplicationContext context;

    private MockMvc mvc;

    {
        Describe("EmbeddedId", () -> {

            BeforeEach(() -> {
                mvc = MockMvcBuilders.webAppContextSetup(context).build();
            });

            It("should have a content handler mapping bean", () -> {

                String content = "this is some content";

                TestEntity entity = repo.save(new TestEntity());
                entity = store.setContent(entity, new ByteArrayInputStream(content.getBytes()));
                entity = repo.save(entity);

//                String command = "curl -H 'Accept: text/plain' http://localhost:" + serverPort + "/testEntities/" + entity.getId().toString();
//                Process process = Runtime.getRuntime().exec(command);
//                while (process.isAlive() == true) {
//                    Thread.sleep(1000);
//                }
//                assertThat(process.exitValue(), is(0));
//                assertThat(IOUtils.toString(process.getInputStream()), is(content));

                MockHttpServletResponse response =
                        mvc.perform(
                            get("/testEntities/" + entity.getId()).
                                accept("text/plain")).
                            andExpect(status().isOk()).
                            andReturn().getResponse();

                    assertThat(response, is(not(nullValue())));
                    assertThat(response.getContentAsString(), is(content));

            });
       });
    }

    @SpringBootApplication
    @EnableJpaRepositories(considerNestedRepositories = true, basePackages={"it.rest.embeddedid"})
    @EnableFilesystemStores(basePackages = "it.rest.embeddedid")
    @Import({RestConfiguration.class})
    public static class Application {

       public static void main(String[] args) {
           SpringApplication.run(Application.class, args);
       }

       @Bean
       public FileSystemResourceLoader filesystemRoot() throws IOException {
          return new FileSystemResourceLoader(Files.createTempDirectory("").toFile().getAbsolutePath());
       }

       @Bean
       public ContentRestConfigurer configureConversionService() {
           return new ContentRestConfigurer() {

               @Override
               public void configure(RestConfiguration config) {

                   config.converters().addConverter(new Converter<String, TestEntityId>() {
                       @Override
                       public TestEntityId convert(String source) {
                           String[] segments = source.split("_");
                           return new TestEntityId(segments[0], segments[1]);
                       }
                   });
               }
           };
       }
    }

    @Entity
    @Getter
    @Setter
    @ToString
    public static class TestEntity {

       @EmbeddedId
       private TestEntityId id = new TestEntityId();

       @ContentId
       @Column(name = "content_id")
       private String contentId;

       @ContentLength
       @Column(name = "content_length")
       private long contentLength;

       @MimeType
       @Column(name = "mime_type")
       private String mimeType = "text/plain";
    }

    public static class TestEntityId implements Serializable {

       private static final long serialVersionUID = -1710555467685181030L;

       private String first;

       private String last;

       public TestEntityId() {
           this.first = UUID.randomUUID().toString();
           this.last = UUID.randomUUID().toString();
       }

       public TestEntityId(String mediumId, String fileId) {
           this.first = mediumId;
           this.last = fileId;
       }

       @Override
       public String toString() {
           return first + "_" + last;
       }
    }

    public interface TestEntityRepository extends JpaRepository<TestEntity, TestEntityId> {
    }

    public interface TestEntityContentRepository extends FilesystemContentStore<TestEntity, TestEntityId> {
    }

    @Test
    public void noop() {}
}
