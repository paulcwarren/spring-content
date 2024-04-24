//package it.internal.org.springframework.content.rest.controllers;
//
//import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
//import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
//import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
//
//import java.util.UUID;
//
//import org.junit.Test;
//import org.junit.runner.RunWith;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.content.rest.config.RestConfiguration;
//import org.springframework.data.rest.webmvc.config.RepositoryRestMvcConfiguration;
//import org.springframework.test.context.ActiveProfiles;
//import org.springframework.test.context.ContextConfiguration;
//import org.springframework.test.context.web.WebAppConfiguration;
//import org.springframework.test.web.servlet.MockMvc;
//import org.springframework.test.web.servlet.setup.MockMvcBuilders;
//import org.springframework.transaction.annotation.Transactional;
//import org.springframework.web.context.WebApplicationContext;
//import org.springframework.web.servlet.config.annotation.DelegatingWebMvcConfiguration;
//
//import com.github.paulcwarren.ginkgo4j.Ginkgo4jSpringRunner;
//
//import internal.org.springframework.content.rest.support.StoreConfig;
//import internal.org.springframework.content.rest.support.TestEntity7;
//import internal.org.springframework.content.rest.support.TestEntity7Repository;
//import internal.org.springframework.content.rest.support.TestEntity7Store;
//
//@RunWith(Ginkgo4jSpringRunner.class)
////@Ginkgo4jConfiguration(threads=1)
//@WebAppConfiguration
//@ContextConfiguration(classes = {
//		StoreConfig.class,
//		DelegatingWebMvcConfiguration.class,
//		RepositoryRestMvcConfiguration.class,
//		RestConfiguration.class })
//@Transactional
//@ActiveProfiles("store")
//public class NaturalIdIT {
//
//	@Autowired
//	private TestEntity7Repository repo7;
//	@Autowired
//	private TestEntity7Store store7;
//
//	@Autowired
//	private WebApplicationContext context;
//
//	private MockMvc mvc;
//
//	private TestEntity7 testEntity7;
//
//	private Entity entityTests;
//	private Content contentTests;
//
//	{
//		Describe("NaturalId Tests", () -> {
//			BeforeEach(() -> {
//				mvc = MockMvcBuilders.webAppContextSetup(context).build();
//			});
//			Context("given an entity is the subject of a repository and storage", () -> {
//				Context("given the repository and storage are exported to the same URI", () -> {
//					BeforeEach(() -> {
//
//						String name = UUID.randomUUID().toString();
//
//						testEntity7 = repo7.save(new TestEntity7("foo-" + name));
//						testEntity7 = repo7.save(testEntity7);
//
//						entityTests.setMvc(mvc);
//						entityTests.setUrl("/testEntity7s/foo-" + name);
//						entityTests.setEntity(testEntity7);
//						entityTests.setRepository(repo7);
//						entityTests.setLinkRel("testEntity7");
//
//						contentTests.setMvc(mvc);
//						contentTests.setUrl("/testEntity7s/foo-" + name);
//						contentTests.setEntity(testEntity7);
//						contentTests.setRepository(repo7);
//						contentTests.setStore(store7);
//
//					});
//					entityTests = Entity.tests();
//					contentTests = Content.tests();
//				});
//			});
//		});
//	}
//
//	@Test
//	public void noop() {
//	}
//}