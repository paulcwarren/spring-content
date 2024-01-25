package it.internal.org.springframework.content.rest.controllers;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static java.lang.String.format;

import java.io.ByteArrayInputStream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
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

import com.github.paulcwarren.ginkgo4j.Ginkgo4jSpringRunner;

import internal.org.springframework.content.rest.support.BaseUriConfig;
import internal.org.springframework.content.rest.support.EntityConfig;
import internal.org.springframework.content.rest.support.TestEntity;
import internal.org.springframework.content.rest.support.TestEntity3;
import internal.org.springframework.content.rest.support.TestEntity3ContentRepository;
import internal.org.springframework.content.rest.support.TestEntity3Repository;
import internal.org.springframework.content.rest.support.TestEntity4;
import internal.org.springframework.content.rest.support.TestEntity4ContentRepository;
import internal.org.springframework.content.rest.support.TestEntity4Repository;
import internal.org.springframework.content.rest.support.TestEntityContentRepository;
import internal.org.springframework.content.rest.support.TestEntityRepository;
import internal.org.springframework.content.rest.support.TestStore;

@RunWith(Ginkgo4jSpringRunner.class)
// @Ginkgo4jConfiguration(threads=1)
@WebAppConfiguration
@ContextConfiguration(classes = {
		BaseUriConfig.class,
		EntityConfig.class,
		DelegatingWebMvcConfiguration.class,
		RepositoryRestMvcConfiguration.class,
		RestConfiguration.class })
@Transactional
@ActiveProfiles("store")
public class BaseUriIT {

	// different exported URI
	@Autowired
	private TestEntityRepository repository;
	@Autowired
	private TestEntityContentRepository contentRepository;

	// same exported URI
	@Autowired
	private TestEntity3Repository repo3;
	@Autowired
	private TestEntity3ContentRepository store3;

	// same exported URI
	@Autowired
	private TestEntity4Repository repo4;
	@Autowired
	private TestEntity4ContentRepository store4;

	@Autowired
	private TestStore store;

	@Autowired
	private WebApplicationContext context;

	private MockMvc mvc;

	private TestEntity testEntity;
	private TestEntity3 testEntity3;
	private TestEntity4 testEntity4;

	private Version version;
	private LastModifiedDate lastModifiedDate;

	private Entity entityTests;
	private Content contentTests;
	private Cors corsTests;

	{
		Describe("BaseUri Content Tests", () -> {
			BeforeEach(() -> {
				mvc = MockMvcBuilders.webAppContextSetup(context).build();
			});
			Context("given an entity is the subject of a repository and storage", () -> {
				Context("given the repository and storage are exported to the same URI", () -> {
					BeforeEach(() -> {
						testEntity3 = repo3.save(new TestEntity3());
						testEntity3.name = "tests";
						testEntity3 = repo3.save(testEntity3);

						entityTests.setMvc(mvc);
						entityTests.setUrl("/api/testEntity3s/" + testEntity3.id);
						entityTests.setEntity(testEntity3);
						entityTests.setRepository(repo3);
						entityTests.setLinkRel("testEntity3");

						contentTests.setMvc(mvc);
						contentTests.setUrl("/contentApi/testEntity3s/" + testEntity3.getId());
						contentTests.setEntity(testEntity3);
						contentTests.setRepository(repo3);
						contentTests.setStore(store3);

					});
					entityTests = Entity.tests();
					contentTests = Content.tests();
				});
				Context("given the repository and storage are exported to different URIs", () -> {
					BeforeEach(() -> {
						testEntity = repository.save(new TestEntity());

						contentTests.setMvc(mvc);
						contentTests.setUrl("/contentApi/testEntitiesContent/" + testEntity.getId());
						contentTests.setEntity(testEntity);
						contentTests.setRepository(repository);
						contentTests.setStore(contentRepository);

						corsTests.setMvc(mvc);
						corsTests.setUrl("/contentApi/testEntitiesContent/" + testEntity.getId());
					});
					contentTests = Content.tests();
					corsTests = Cors.tests();
				});

				Context("given an entity with @Version", () -> {
					BeforeEach(() -> {
						testEntity4 = new TestEntity4();
						testEntity4 = store4.setContent(testEntity4, new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
						testEntity4.mimeType = "text/plain";
						testEntity4 = repo4.save(testEntity4);
						String url = "/contentApi/testEntity4s/" + testEntity4.getId();

						version.setEntity(testEntity4);
						version.setMvc(mvc);
						version.setUrl(url);
						version.setRepo(repo4);
						version.setStore(store4);
						version.setEtag(format("\"%s\"", testEntity4.getVersion()));
					});
					version = Version.tests();
				});

				Context("given an entity with @LastModifiedDate", () -> {
					BeforeEach(() -> {
						String content = "Hello Spring Content LastModifiedDate World!";

						testEntity4 = new TestEntity4();
						testEntity4 = store4.setContent(testEntity4, new ByteArrayInputStream(content.getBytes()));
						testEntity4.mimeType = "text/plain";
						testEntity4 = repo4.save(testEntity4);
						String url = "/contentApi/testEntity4s/" + testEntity4.getId();

						lastModifiedDate.setMvc(mvc);
						lastModifiedDate.setUrl(url);
						lastModifiedDate.setLastModifiedDate(testEntity4.getModifiedDate());
						lastModifiedDate.setEtag(testEntity4.getVersion().toString());
						lastModifiedDate.setContent(content);
					});
					lastModifiedDate = LastModifiedDate.tests();
				});
			});
		});
	}

	@Test
	public void noop() {
	}
}