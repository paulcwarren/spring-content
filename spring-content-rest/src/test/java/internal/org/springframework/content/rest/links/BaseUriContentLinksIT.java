package internal.org.springframework.content.rest.links;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jConfiguration;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jSpringRunner;
import internal.org.springframework.content.rest.support.BaseUriConfig;
import internal.org.springframework.content.rest.support.TestEntity;
import internal.org.springframework.content.rest.support.TestEntity2;
import internal.org.springframework.content.rest.support.TestEntity2Repository;
import internal.org.springframework.content.rest.support.TestEntity3;
import internal.org.springframework.content.rest.support.TestEntity3ContentRepository;
import internal.org.springframework.content.rest.support.TestEntity3Repository;
import internal.org.springframework.content.rest.support.TestEntityChildContentRepository;
import internal.org.springframework.content.rest.support.TestEntityContentRepository;
import internal.org.springframework.content.rest.support.TestEntityRepository;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.rest.config.HypermediaConfiguration;
import org.springframework.content.rest.config.RestConfiguration;
import org.springframework.data.rest.webmvc.config.RepositoryRestMvcConfiguration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.DelegatingWebMvcConfiguration;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;

@RunWith(Ginkgo4jSpringRunner.class)
@Ginkgo4jConfiguration(threads = 1)
@WebAppConfiguration
@ContextConfiguration(classes = {
		BaseUriConfig.class,
		DelegatingWebMvcConfiguration.class,
		RepositoryRestMvcConfiguration.class,
		RestConfiguration.class,
		HypermediaConfiguration.class })
@Transactional
@ActiveProfiles("store")
public class BaseUriContentLinksIT {

	@Autowired
	TestEntityRepository repository;
	@Autowired
	TestEntityContentRepository contentRepository;
	@Autowired
	TestEntity3Repository repository3;
	@Autowired
	TestEntity3ContentRepository contentRepository3;

	@Autowired
	private WebApplicationContext context;

	private MockMvc mvc;

	private TestEntity testEntity;
	private TestEntity3 testEntity3;

	private EntityContentLinkTests entityContentLinkTests;

	{
		Describe("given the spring content baseUri property is set to contentApi", () -> {
			BeforeEach(() -> {
				mvc = MockMvcBuilders.webAppContextSetup(context).build();
			});

			Context("given an Entity and a Store with a default store path", () -> {
				BeforeEach(() -> {
					testEntity3 = repository3.save(new TestEntity3());

					entityContentLinkTests.setMvc(mvc);
					entityContentLinkTests.setRepository(repository3);
					entityContentLinkTests.setStore(contentRepository3);
					entityContentLinkTests.setTestEntity(testEntity3);
					entityContentLinkTests.setUrl("/api/testEntity3s/" + testEntity3.getId());
					entityContentLinkTests.setLinkRel("testEntity3s");
					entityContentLinkTests.setExpectedLinkRegex("http://localhost/contentApi/testEntity3s/" + testEntity3.getId());
				});
				entityContentLinkTests = new EntityContentLinkTests();
			});

			Context("given an Entity and a Store specifying a store path", () -> {
				BeforeEach(() -> {
					testEntity = repository.save(new TestEntity());

					entityContentLinkTests.setMvc(mvc);
					entityContentLinkTests.setRepository(repository);
					entityContentLinkTests.setStore(contentRepository);
					entityContentLinkTests.setTestEntity(testEntity);
					entityContentLinkTests.setUrl("/api/testEntities/" + testEntity.getId());
					entityContentLinkTests.setLinkRel("testEntity");
					entityContentLinkTests.setExpectedLinkRegex("http://localhost/contentApi/testEntitiesContent/" + testEntity.getId());
				});
				entityContentLinkTests = new EntityContentLinkTests();
			});
		});
	}

	@Test
	public void noop() {
	}
}
