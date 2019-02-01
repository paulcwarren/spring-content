package internal.org.springframework.content.rest.links;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jConfiguration;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jSpringRunner;
import internal.org.springframework.content.rest.support.StoreConfig;
import internal.org.springframework.content.rest.support.TestEntity;
import internal.org.springframework.content.rest.support.TestEntity2;
import internal.org.springframework.content.rest.support.TestEntity2Repository;
import internal.org.springframework.content.rest.support.TestEntity3;
import internal.org.springframework.content.rest.support.TestEntity3ContentRepository;
import internal.org.springframework.content.rest.support.TestEntity3Repository;
import internal.org.springframework.content.rest.support.TestEntityChild;
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

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;

@RunWith(Ginkgo4jSpringRunner.class)
@Ginkgo4jConfiguration(threads = 1)
@WebAppConfiguration
@ContextConfiguration(classes = {
		StoreConfig.class,
		DelegatingWebMvcConfiguration.class,
		RepositoryRestMvcConfiguration.class,
		RestConfiguration.class,
		HypermediaConfiguration.class })
@Transactional
@ActiveProfiles("store")
public class ContentLinksIntegrationTest {

	@Autowired
	TestEntityRepository repository;
	@Autowired
	TestEntityContentRepository contentRepository;
	@Autowired
	TestEntity2Repository repository2;
	@Autowired
	TestEntityChildContentRepository contentRepository2;
	@Autowired
	TestEntity3Repository repository3;
	@Autowired
	TestEntity3ContentRepository contentRepository3;

	@Autowired
	private WebApplicationContext context;

	private MockMvc mvc;

	private TestEntity testEntity;
	private TestEntity2 testEntity2;
	private TestEntity3 testEntity3;

	private EntityContentLinkTests entityContentLinkTests;
	private EntityPropertyContentLinkTests entityPropertyContentLinkTests;

	{
		Describe("given no baseUri are set", () -> {
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
					entityContentLinkTests.setUrl("/testEntity3s/" + testEntity3.getId());
					entityContentLinkTests.setLinkRel("testEntity3s");
					entityContentLinkTests.setExpectedLinkRegex("http://localhost/testEntity3s/[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}");
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
					entityContentLinkTests.setUrl("/testEntities/" + testEntity.getId());
					entityContentLinkTests.setLinkRel("testEntitiesContent");
					entityContentLinkTests.setExpectedLinkRegex("http://localhost/testEntitiesContent/[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}");
				});
				entityContentLinkTests = new EntityContentLinkTests();
			});

			Context("given an Entity with content properties", () -> {
				BeforeEach(() -> {
					testEntity2 = repository2.save(new TestEntity2());
				});
				Context("given a single content property with content", () -> {
					BeforeEach(() -> {
						TestEntityChild child = new TestEntityChild();
						child.mimeType = "text/plain";
						contentRepository2.setContent(child, new ByteArrayInputStream(
								"Hello Spring Content World!".getBytes()));

						testEntity2.setChild(child);

						TestEntityChild child1 = new TestEntityChild();
						child1.mimeType = "text/plain";
						contentRepository2.setContent(child1,
								new ByteArrayInputStream("Content 1".getBytes()));

						TestEntityChild child2 = new TestEntityChild();
						child2.mimeType = "text/plain";
						contentRepository2.setContent(child2,
								new ByteArrayInputStream("Content 2".getBytes()));

						List<TestEntityChild> children = new ArrayList<>();
						children.add(child1);
						children.add(child2);

						testEntity2.setChildren(children);
						repository2.save(testEntity2);

						entityPropertyContentLinkTests.setMvc(mvc);
						entityPropertyContentLinkTests.setRepository(repository2);
						entityPropertyContentLinkTests.setStore(contentRepository2);
						entityPropertyContentLinkTests.setTestEntity(testEntity2);
						entityPropertyContentLinkTests.setUrl("/files/" + testEntity2.getId());
						entityPropertyContentLinkTests.setLinkRel("child");
						entityPropertyContentLinkTests.setExpectedLinkRegex("http://localhost/files/" + testEntity2.getId()
								+ "/child/[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}");
					});
					entityPropertyContentLinkTests = new EntityPropertyContentLinkTests();
				});
			});
		});
	}

	@Test
	public void noop() {
	}
}
