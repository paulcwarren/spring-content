package it.internal.org.springframework.content.rest.controllers;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.File;
import java.io.StringReader;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.fs.config.EnableFilesystemStores;
import org.springframework.content.fs.io.FileSystemResourceLoader;
import org.springframework.content.rest.config.ContentRestConfigurer;
import org.springframework.content.rest.config.RestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.rest.webmvc.config.RepositoryRestMvcConfiguration;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.DelegatingWebMvcConfiguration;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jSpringRunner;
import com.theoryinpractise.halbuilder.api.ReadableRepresentation;
import com.theoryinpractise.halbuilder.api.RepresentationFactory;
import com.theoryinpractise.halbuilder.standard.StandardRepresentationFactory;

import internal.org.springframework.content.rest.support.TestEntity3;
import internal.org.springframework.content.rest.support.TestEntity3Repository;
import internal.org.springframework.content.rest.support.config.JpaInfrastructureConfig;

@RunWith(Ginkgo4jSpringRunner.class)
//@Ginkgo4jConfiguration(threads=1)
@WebAppConfiguration
@ContextConfiguration(classes = {
		ShortcutExclusionsIT.Config.class,
		DelegatingWebMvcConfiguration.class,
		RepositoryRestMvcConfiguration.class,
		RestConfiguration.class })
@Transactional
public class ShortcutExclusionsIT {

	@Autowired
	private TestEntity3Repository repo;

	@Autowired
	private WebApplicationContext context;

	private MockMvc mvc;

	private TestEntity3 testEntity;

	{
		Describe("Excluded Shortcut Link Tests", () -> {
			BeforeEach(() -> {
				mvc = MockMvcBuilders.webAppContextSetup(context).build();

				testEntity = repo.save(new TestEntity3());
                testEntity = repo.save(testEntity);
			});
			Context("when all content property shortcut GETs are excluded and a non-json request is made", () -> {
			    It("should return the entity", () -> {
	                MockHttpServletResponse response = mvc
	                        .perform(get("/testEntity3s/" + testEntity.getId())
	                                .accept("*/*"))
	                        .andExpect(status().isOk())
	                        .andReturn().getResponse();

	                RepresentationFactory representationFactory = new StandardRepresentationFactory();
	                ReadableRepresentation halResponse = representationFactory
	                        .readRepresentation("application/hal+json",
	                                new StringReader(response.getContentAsString()));
	                assertThat(halResponse.getLinks().size(), is(greaterThan(2)));
	                assertThat(halResponse.getLinksByRel("testEntity3"), is(not(nullValue())));
			    });
			});
		});
	}

	@Test
	public void noop() {}

	@Configuration
	@EnableJpaRepositories(basePackages = "internal.org.springframework.content.rest.support")
	@EnableTransactionManagement
	@EnableFilesystemStores(basePackages = "internal.org.springframework.content.rest.support")
	public static class Config extends JpaInfrastructureConfig {

	    @Bean
	    public ContentRestConfigurer contentRestConfigurer() {

	        return new ContentRestConfigurer() {

	            @Override
	            public void configure(RestConfiguration config) {
	                config.shortcutExclusions().exclude("GET", MediaType.parseMediaType("*/*"));
	            }
	        };
	    }

	    @Bean
	    FileSystemResourceLoader fileSystemResourceLoader() {
	        return new FileSystemResourceLoader(filesystemRoot().getAbsolutePath());
	    }

	    @Bean
	    public File filesystemRoot() {
	        File baseDir = new File(System.getProperty("java.io.tmpdir"));
	        File filesystemRoot = new File(baseDir, "spring-content-excluded-shortcut-links");
	        filesystemRoot.mkdirs();
	        return filesystemRoot;
	    }
	}
}
