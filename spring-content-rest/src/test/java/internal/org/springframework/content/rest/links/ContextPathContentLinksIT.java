package internal.org.springframework.content.rest.links;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.renditions.RenditionProvider;
import org.springframework.content.fs.config.EnableFilesystemStores;
import org.springframework.content.fs.io.FileSystemResourceLoader;
import org.springframework.content.rest.config.HypermediaConfiguration;
import org.springframework.content.rest.config.RestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.rest.webmvc.config.RepositoryRestMvcConfiguration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.DelegatingWebMvcConfiguration;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jConfiguration;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jSpringRunner;

import internal.org.springframework.content.rest.support.TestEntity3;
import internal.org.springframework.content.rest.support.TestEntity3ContentRepository;
import internal.org.springframework.content.rest.support.TestEntity3Repository;
import internal.org.springframework.content.rest.support.config.JpaInfrastructureConfig;

@RunWith(Ginkgo4jSpringRunner.class)
@Ginkgo4jConfiguration(threads = 1)
@WebAppConfiguration
@ContextConfiguration(classes = {
		ContextPathContentLinksIT.ContextPathConfig.class,
		DelegatingWebMvcConfiguration.class,
		RepositoryRestMvcConfiguration.class,
		RestConfiguration.class,
		HypermediaConfiguration.class })
@Transactional
@ActiveProfiles("store")
public class ContextPathContentLinksIT {

	@Autowired
	TestEntity3Repository repository3;
	@Autowired
	TestEntity3ContentRepository contentRepository3;

	@Autowired
	private WebApplicationContext context;

	private MockMvc mvc;

	private TestEntity3 testEntity3;

	private ContentLinkTests contentLinkTests;

	{
		Describe("given the spring content baseUri property is set to contentApi", () -> {
			BeforeEach(() -> {
				mvc = MockMvcBuilders.webAppContextSetup(context).build();
			});

			Context("given an Entity and a Store with a default store path", () -> {
				BeforeEach(() -> {
					testEntity3 = repository3.save(new TestEntity3());

					contentLinkTests.setMvc(mvc);
					contentLinkTests.setRepository(repository3);
					contentLinkTests.setStore(contentRepository3);
					contentLinkTests.setTestEntity(testEntity3);
					contentLinkTests.setUrl("/contextPath/testEntity3s/" + testEntity3.getId());
					contentLinkTests.setContextPath("/contextPath");
					contentLinkTests.setLinkRel("testEntity3");
					contentLinkTests.setExpectedLinkRegex("http://localhost/contextPath/testEntity3s/" + testEntity3.getId() );
				});
				contentLinkTests = new ContentLinkTests();
			});
		});
	}
	
	@Configuration
	@EnableJpaRepositories(basePackages = "internal.org.springframework.content.rest.support")
	@EnableTransactionManagement
	@EnableFilesystemStores(basePackages = "internal.org.springframework.content.rest.support")
	@Profile("store")
	public static class ContextPathConfig extends JpaInfrastructureConfig {

		@Bean
		FileSystemResourceLoader fileSystemResourceLoader() {
			return new FileSystemResourceLoader(filesystemRoot().getAbsolutePath());
		}

		@Bean
		public File filesystemRoot() {
			File baseDir = new File(System.getProperty("java.io.tmpdir"));
			File filesystemRoot = new File(baseDir, "spring-content-controller-tests");
			filesystemRoot.mkdirs();
			return filesystemRoot;
		}
		
		@Bean
		public RenditionProvider textToHtml() {
			return new RenditionProvider() {

				@Override
				public String consumes() {
					return "text/plain";
				}

				@Override
				public String[] produces() {
					return new String[] { "text/html" };
				}

				@Override
				public InputStream convert(InputStream fromInputSource, String toMimeType) {
					String input = null;
					try {
						input = IOUtils.toString(fromInputSource);
					}
					catch (IOException e) {
					}
					return new ByteArrayInputStream(
							String.format("<html><body>%s</body></html>", input).getBytes());
				}
			};
		}
	}
	
	@Test
	public void noop() {}
}
