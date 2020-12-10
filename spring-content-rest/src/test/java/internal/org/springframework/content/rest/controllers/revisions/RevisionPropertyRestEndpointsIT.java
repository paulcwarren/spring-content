package internal.org.springframework.content.rest.controllers.revisions;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.io.File;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import org.hibernate.envers.Audited;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.annotations.MimeType;
import org.springframework.content.fs.config.EnableFilesystemStores;
import org.springframework.content.fs.io.FileSystemResourceLoader;
import org.springframework.content.fs.store.FilesystemContentStore;
import org.springframework.content.rest.config.RestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.envers.repository.support.EnversRevisionRepositoryFactoryBean;
import org.springframework.data.history.Revisions;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.repository.history.RevisionRepository;
import org.springframework.data.rest.webmvc.config.RepositoryRestMvcConfiguration;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.DelegatingWebMvcConfiguration;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jSpringRunner;

import internal.org.springframework.content.rest.support.config.JpaInfrastructureConfig;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@RunWith(Ginkgo4jSpringRunner.class)
// @Ginkgo4jConfiguration(threads=1)
@WebAppConfiguration
@ContextConfiguration(classes = { RevisionPropertyRestEndpointsIT.TestConfig.class, DelegatingWebMvcConfiguration.class, RepositoryRestMvcConfiguration.class, RestConfiguration.class })
public class RevisionPropertyRestEndpointsIT {

    @Autowired
    private RevisionPropertyRestEndpointsIT.TEntityRepository repository;

    @Autowired
    private RevisionPropertyRestEndpointsIT.TEntityContentStore store;

    @Autowired
    private WebApplicationContext context;

    private MockMvc mvc;

    private TEntity testEntity;

    {
        Describe("Revision Property REST Endpoints", () -> {
            BeforeEach(() -> {
                mvc = MockMvcBuilders.webAppContextSetup(context).build();
            });
            Context("given an Entity with revisions", () -> {
                BeforeEach(() -> {
                    testEntity = repository.save(new TEntity());
                    testEntity = store.setContent(testEntity, new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                    testEntity.setMimeType("text/plain");
                    testEntity = repository.save(testEntity);

                    assertThat(repository.findRevisions(testEntity.getId()).toList().size(), is(2));
                });

                Context("a GET to /{repository}/{id}/revisions/1/content", () -> {

                    It("should return a 404", () -> {

                        mvc.perform(
                            get("/tEntities/" + testEntity.getId() + "/revisions/1/content").
                                accept("text/plain")).
                            andExpect(status().isNotFound());
                    });
                });

                Context("a GET to /{repository}/{id}/revisions/<latest>/content", () -> {

                    It("should return the content", () -> {

                        Revisions<Integer, TEntity> revisions = repository.findRevisions(testEntity.getId());
                        Integer revisionId = revisions.getLatestRevision().getRequiredRevisionNumber();

                        MockHttpServletResponse response =
                            mvc.perform(
                                get("/tEntities/" + testEntity.getId() + "/revisions/" + revisionId + "/content").
                                    accept("text/plain")).
                                andExpect(status().isOk()).
                                andReturn().getResponse();

                        assertThat(response, is(not(nullValue())));
                        assertThat(response.getContentAsString(), is("Hello Spring Content World!"));
                    });
                });
            });
        });
    }

    @Test
    public void noop() {
    }

    @Configuration
    @EnableJpaRepositories(basePackages = "internal.org.springframework.content.rest.controllers.revisions", considerNestedRepositories = true, repositoryFactoryBeanClass = EnversRevisionRepositoryFactoryBean.class)
    @EnableFilesystemStores(basePackages = "internal.org.springframework.content.rest.controllers.revisions")
    public static class TestConfig extends JpaInfrastructureConfig {

        @Override
        protected String[] packagesToScan() {
            return new String[] { "internal.org.springframework.content.rest.controllers.revisions" };
        }

        @Bean
        FileSystemResourceLoader fileSystemResourceLoader() {
            return new FileSystemResourceLoader(filesystemRoot().getAbsolutePath());
        }

        @Bean
        public File filesystemRoot() {
            File baseDir = new File(System.getProperty("java.io.tmpdir"));
            File filesystemRoot = new File(baseDir, "spring-content-controller-revisions-tests");
            filesystemRoot.mkdirs();
            return filesystemRoot;
        }
    }

    public interface TEntityRepository extends JpaRepository<TEntity, Long>, RevisionRepository<TEntity, Long, Integer> {
    }

    public interface TEntityContentStore extends FilesystemContentStore<TEntity, String> {
    }

    @Entity
    @Audited
    @Getter
    @Setter
    @NoArgsConstructor
    public static class TEntity {

        @Id
        @GeneratedValue(strategy = GenerationType.AUTO)
        private Long id;

        @ContentId
        private String contentId;

        @ContentLength
        private Long contentLen;

        @MimeType
        private String mimeType;
    }
}
