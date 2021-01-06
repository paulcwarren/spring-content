package it.internal.org.springframework.content.rest.controllers;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jSpringRunner;
import internal.org.springframework.content.rest.support.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.rest.config.RestConfiguration;
import org.springframework.data.rest.webmvc.config.RepositoryRestMvcConfiguration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.DelegatingWebMvcConfiguration;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.*;

@RunWith(Ginkgo4jSpringRunner.class)
//@Ginkgo4jConfiguration(threads=1)
@WebAppConfiguration
@ContextConfiguration(classes = {
		FullyQualifiedLinksConfig.class,
		DelegatingWebMvcConfiguration.class,
		RepositoryRestMvcConfiguration.class,
		RestConfiguration.class })
@Transactional
public class FullyQualifiedLinksIT {

	@Autowired
	private TestEntityRepository repo3;

	@Autowired
	private TestEntityContentRepository store3;

	@Autowired
	private WebApplicationContext context;

	private MockMvc mvc;

	private TestEntity testEntity3;

	private Content contentTests;
	
	{
		Describe("ContextPath Content Tests", () -> {
			BeforeEach(() -> {
				mvc = MockMvcBuilders.webAppContextSetup(context).build();
			});
			Context("given an entity is the subject of a repository and storage", () -> {
				BeforeEach(() -> {
					testEntity3 = repo3.save(new TestEntity());
					testEntity3 = repo3.save(testEntity3);

					contentTests.setMvc(mvc);
					contentTests.setUrl("/testEntitiesContent/" + testEntity3.getId() + "/content");
					contentTests.setEntity(testEntity3);
					contentTests.setRepository(repo3);
					contentTests.setStore(store3);

				});
				contentTests = Content.tests();
			});
		});
	}

	@Test
	public void noop() {}
}
