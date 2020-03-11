package internal.org.springframework.content.rest.links;

import java.util.UUID;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jConfiguration;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jSpringRunner;
import internal.org.springframework.content.rest.support.BaseUriConfig;
import internal.org.springframework.content.rest.support.TestEntity3;
import internal.org.springframework.content.rest.support.TestEntity5;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.rest.config.HypermediaConfiguration;
import org.springframework.content.rest.config.RestConfiguration;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.webmvc.PersistentEntityResource;
import org.springframework.data.rest.webmvc.config.RepositoryRestMvcConfiguration;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.config.annotation.DelegatingWebMvcConfiguration;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.AfterEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.JustBeforeEach;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.beans.HasPropertyWithValue.hasProperty;

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
public class ContentLinksResourceProcessorIT {

	@Autowired
	private Repositories repositories;

	@Autowired
	private ContentLinksResourceProcessor processor;

	@Autowired
	private WebApplicationContext context;

	private MockMvc mvc;

	private PersistentEntityResource resource;

	{
		Describe("given the spring content baseUri property is set to contentApi", () -> {
			BeforeEach(() -> {
				mvc = MockMvcBuilders.webAppContextSetup(context).build();
			});

			JustBeforeEach(() -> {
				MockHttpServletRequest request = new MockHttpServletRequest();
				RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

				processor.process(resource);
			});

			Context("given an entity with a single @ContentId property", () -> {

				BeforeEach(() -> {
					PersistentEntity<?, ?> persistentEntity = repositories.getPersistentEntity(TestEntity3.class);

					TestEntity3 obj = new TestEntity3();
					obj.setId(999L);
					obj.setContentId(UUID.randomUUID());

					PersistentEntityResource.Builder build = PersistentEntityResource.build(obj, persistentEntity);
					resource = build.build();
				});

				It("should add an entity content links", () -> {
					assertThat(resource.getLinks("testEntity3"), hasItem(hasProperty("href", is("http://localhost/contentApi/testEntity3s/999"))));
					assertThat(resource.getLinks("testEntity3s"), hasItem(hasProperty("href", is("http://localhost/contentApi/testEntity3s/999"))));
				});

				Context("when fully qualified links property is true", () -> {
					BeforeEach(() -> {
						processor.getRestConfiguration().setFullyQualifiedLinks(true);
					});

					AfterEach(() -> {
						processor.getRestConfiguration().setFullyQualifiedLinks(false);
					});

					It("should add an entity content link against a 'content' linkrel", () -> {
						assertThat(resource.getLinks("content"), hasItem(hasProperty("href", is("http://localhost/contentApi/testEntity3s/999/content"))));
					});
				});
			});

			Context("given an entity with multiple @ContentId properties", () -> {

				BeforeEach(() -> {
					PersistentEntity<?, ?> persistentEntity = repositories.getPersistentEntity(TestEntity5.class);

					TestEntity5 obj = new TestEntity5();
					obj.setId(999L);
					UUID contentId = UUID.randomUUID();
					obj.setContentId(contentId);
					obj.setRenditionId(UUID.randomUUID());

					PersistentEntityResource.Builder build = PersistentEntityResource.build(obj, persistentEntity);
					resource = build.build();
				});

				It("should add content property links", () -> {
					assertThat(resource.getLinks("content"), hasItems(hasProperty("href", is("http://localhost/contentApi/testEntity5s/999/content"))));
					assertThat(resource.getLinks("rendition"), hasItems(hasProperty("href", is("http://localhost/contentApi/testEntity5s/999/rendition"))));
				});
			});
		});
	}

	@Test
	public void noop() {
	}
}
