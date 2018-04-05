package internal.org.springframework.content.rest.controllers;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Optional;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.rest.config.RestConfiguration;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.repository.support.RepositoryInvokerFactory;
import org.springframework.data.rest.core.mapping.ResourceMappings;
import org.springframework.data.rest.webmvc.RootResourceInformation;
import org.springframework.data.rest.webmvc.config.RepositoryRestMvcConfiguration;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.DelegatingWebMvcConfiguration;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jSpringRunner;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import internal.org.springframework.content.rest.support.StoreConfig;
import internal.org.springframework.content.rest.support.TestEntity;
import internal.org.springframework.content.rest.support.TestEntity2;
import internal.org.springframework.content.rest.support.TestEntity2Repository;
import internal.org.springframework.content.rest.support.TestEntityChild;
import internal.org.springframework.content.rest.support.TestEntityChildContentRepository;
import internal.org.springframework.content.rest.support.TestEntityContentRepository;
import internal.org.springframework.content.rest.support.TestEntityRepository;

@RunWith(Ginkgo4jSpringRunner.class)
//@Ginkgo4jConfiguration(threads=1)
@WebAppConfiguration
@ContextConfiguration(classes = {StoreConfig.class, DelegatingWebMvcConfiguration.class, RepositoryRestMvcConfiguration.class, RestConfiguration.class})
@Transactional
@ActiveProfiles("store")
public class ContentPropertyRestControllerIntegrationTest {
	
	@Autowired Repositories repositories;
	@Autowired RepositoryInvokerFactory invokerFactory;
	@Autowired ResourceMappings mappings;
	
	@Autowired TestEntityRepository repository;
	@Autowired TestEntityContentRepository contentRepository;
	@Autowired TestEntity2Repository repository2;
	@Autowired TestEntityChildContentRepository contentRepository2;
	
	@Autowired ContentPropertyCollectionRestController collectionCtrlr;
	@Autowired ContentPropertyRestController propCtrlr;
	
	@Autowired private WebApplicationContext context;

	private MockMvc mvc;

	private TestEntity testEntity;
	
	private TestEntity2 testEntity2;
	private TestEntityChild child1;
	private TestEntityChild child2;

	{
		Describe("ContentPropertyRestController", () -> {
			BeforeEach(() -> {
				assertThat(collectionCtrlr, is(not(nullValue())));
				assertThat(propCtrlr, is(not(nullValue())));
				
				mvc = MockMvcBuilders
						.webAppContextSetup(context)
						.build();
			});
			Context("given an Entity with a simple content property", () -> {
				BeforeEach(() -> {
					testEntity2 = repository2.save(new TestEntity2());
					repository2.save(testEntity2);
				});
				Context("given that is has no content", () -> {
					Context("a GET to /{repository}/{id}/{contentProperty}", () -> {
						It("should return 404", () -> {
							mvc.perform(get("/files/" + testEntity2.id.toString() + "/child"))
									.andExpect(status().isNotFound());
						});
					});
					Context("a PUT to /{repository}/{id}/{contentProperty}", () -> {
						It("should create the content", () -> {
							mvc.perform(put("/files/" + testEntity2.id.toString() + "/child")
									.content("Hello New Spring Content World!")
									.contentType("text/plain"))
									.andExpect(status().is2xxSuccessful());
							
							Optional<TestEntity2> fetched = repository2.findById(testEntity2.id);
							assertThat(fetched.isPresent(), is(true));
							assertThat(fetched.get().child.contentId, is(not(nullValue())));
							assertThat(fetched.get().child.contentLen, is(31L));
							assertThat(fetched.get().child.mimeType, is("text/plain"));
							assertThat(IOUtils.toString(contentRepository2.getContent(fetched.get().child)), is("Hello New Spring Content World!"));
						});
					});
				});
				Context("given that it has content", () -> {
					BeforeEach(() -> {
						testEntity2.child = new TestEntityChild();
						testEntity2.child.mimeType = "text/plain";
						contentRepository2.setContent(testEntity2.child, new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
						repository2.save(testEntity2);
					});
					Context("a GET to /{repository}/{id}/{contentProperty}", () -> {
						It("should return the content", () -> {
							MockHttpServletResponse response = mvc.perform(get("/files/" + testEntity2.id.toString() + "/child")
									.accept("text/plain"))
									.andExpect(status().isOk())
									.andReturn().getResponse();

							assertThat(response, is(not(nullValue())));
							assertThat(response.getContentAsString(), is("Hello Spring Content World!"));
						});
					});
					Context("a GET to /{repository}/{id}/{contentProperty} with a range", () -> {
						It("should return the content", () -> {
							MockHttpServletResponse response = mvc.perform(get("/files/" + testEntity2.id.toString() + "/child")
									.accept("text/plain")
									.header("range", "bytes=6-19"))
									.andExpect(status().isPartialContent())
									.andReturn().getResponse();
							assertThat(response, is(not(nullValue())));

							assertThat(response.getContentAsString(), is("Spring Content"));
						});
					});
					Context("a GET to /{repository}/{id}/{contentProperty} with a mime type that matches a renderer", () -> {
						It("should return the rendition and 200", () -> {
							MockHttpServletResponse response = mvc.perform(get("/files/" + testEntity2.id.toString() + "/child")
									.accept("text/html"))
									.andExpect(status().isOk())
									.andReturn().getResponse();

							assertThat(response, is(not(nullValue())));
							assertThat(response.getContentAsString(), is("<html><body>Hello Spring Content World!</body></html>"));
						});
					});
					Context("a GET to /{repository}/{id}/{contentProperty} with multiple mime types the last of which matches the content", () -> {
						It("should return the original content and 200", () -> {
							MockHttpServletResponse response = mvc.perform(get("/files/" + testEntity2.id.toString() + "/child")
									.accept(new String[] {"text/xml","text/*"}))
									.andExpect(status().isOk())
									.andReturn().getResponse();

							assertThat(response, is(not(nullValue())));
							assertThat(response.getContentAsString(), is("Hello Spring Content World!"));
						});
					});
					Context("a PUT to /{repository}/{id}/{contentProperty}", () -> {
						It("should create the content", () -> {
							mvc.perform(put("/files/" + testEntity2.id.toString() + "/child")
									.content("Hello New Spring Content World!")
									.contentType("text/plain"))
									.andExpect(status().is2xxSuccessful());
							
							Optional<TestEntity2> fetched = repository2.findById(testEntity2.id);
							assertThat(fetched.isPresent(), is(true));
							assertThat(fetched.get().child.contentId, is(not(nullValue())));
							assertThat(fetched.get().child.contentLen, is(31L));
							assertThat(fetched.get().child.mimeType, is("text/plain"));
							assertThat(IOUtils.toString(contentRepository2.getContent(fetched.get().child)), is("Hello New Spring Content World!"));
						});
					});
					Context("a DELETE to /{repository}/{id}/{contentProperty}", () -> {
						It("should delete the content", () -> {
							mvc.perform(delete("/files/" + testEntity2.id.toString() + "/child"))
									.andExpect(status().isNoContent());
							
							Optional<TestEntity2> fetched = repository2.findById(testEntity2.id);
							assertThat(fetched.isPresent(), is(true));
							assertThat(fetched.get().child.contentId, is(nullValue()));
							assertThat(fetched.get().child.contentLen, is(0L));
							assertThat(fetched.get().child.mimeType, is(nullValue()));
						});
					});
					Context("a GET to /{repository}/{id}/{contentProperty}/{contentId}", () -> {
						It("should return the content", () -> {
							MockHttpServletResponse response = mvc.perform(get("/files/" + testEntity2.id.toString() + "/child/" + testEntity2.child.contentId)
									.accept("text/plain"))
									.andExpect(status().isOk())
									.andReturn().getResponse();
							assertThat(response, is(not(nullValue())));

							assertThat(response.getContentAsString(), is("Hello Spring Content World!"));
						});
					});
					Context("a GET to /{repository}/{id}/{contentProperty}/{contentId} with a range", () -> {
						It("should return the content", () -> {
							MockHttpServletResponse response = mvc.perform(get("/files/" + testEntity2.id.toString() + "/child/" + testEntity2.child.contentId)
									.accept("text/plain")
									.header("range", "bytes=6-19"))
									.andExpect(status().isPartialContent())
									.andReturn().getResponse();
							assertThat(response, is(not(nullValue())));

							assertThat(response.getContentAsString(), is("Spring Content"));
						});
					});
					Context("a GET to /{repository}/{id}/{contentProperty}/{contentId} with a mime type that matches a renderer", () -> {
						It("should return the rendition and 200", () -> {
							MockHttpServletResponse response = mvc.perform(get("/files/" + testEntity2.id.toString() + "/child/" + testEntity2.child.contentId)
									.accept("text/html"))
									.andExpect(status().isOk())
									.andReturn().getResponse();

							assertThat(response, is(not(nullValue())));
							assertThat(response.getContentAsString(), is("<html><body>Hello Spring Content World!</body></html>"));
						});
					});
					Context("a GET to /{repository}/{id}/{contentProperty}/{contentId} with multiple mime types the last of which matches the content", () -> {
						It("should return the original content and 200", () -> {
							MockHttpServletResponse response = mvc.perform(get("/files/" + testEntity2.id.toString() + "/child/" + testEntity2.child.contentId)
									.accept(new String[] {"text/xml","text/*"}))
									.andExpect(status().isOk())
									.andReturn().getResponse();

							assertThat(response, is(not(nullValue())));
							assertThat(response.getContentAsString(), is("Hello Spring Content World!"));
						});
					});
					Context("a PUT to /{repository}/{id}/{contentProperty}/{contentId}", () -> {
						It("should overwrite the content", () -> {
							mvc.perform(put("/files/" + testEntity2.id.toString() + "/child/" + testEntity2.child.contentId)
									.content("Hello Modified Spring Content World!")
									.contentType("text/plain"))
									.andExpect(status().isOk());

							assertThat(IOUtils.toString(contentRepository2.getContent(testEntity2.child)), is("Hello Modified Spring Content World!"));
						});
					});
					Context("a DELETE to /{repository}/{id}/{contentProperty}/{contentId}", () -> {
						It("should delete the content", () -> {
							mvc.perform(delete("/files/" + testEntity2.id.toString() + "/child/" + testEntity2.child.contentId))
									.andExpect(status().isNoContent());
							
							Optional<TestEntity2> fetched = repository2.findById(testEntity2.id);
							assertThat(fetched.isPresent(), is(true));
							assertThat(fetched.get().child.contentId, is(nullValue()));
						});
					});
				});
			});
			
			Context("given an Entity with a collection content property", () -> {
				BeforeEach(() -> {
					testEntity2 = repository2.save(new TestEntity2());
				});
				Context("given that is has no content", () -> {
					Context("a GET to /{repository}/{id}/{contentProperty}", () -> {
						It("should return 406 MethodNotAllowed", () -> {
							mvc.perform(get("/files/" + testEntity2.id.toString() + "/children/"))
								.andExpect(status().isMethodNotAllowed());
						});
					});
					Context("a PUT to /{repository}/{id}/{contentProperty}", () -> {
						It("should append the content to the entity's content property collection", () -> {
							mvc.perform(put("/files/" + testEntity2.id.toString() + "/children/")
								.content("Hello New Spring Content World!")
								.contentType("text/plain"))
								.andExpect(status().is2xxSuccessful());
					
							Optional<TestEntity2> fetched = repository2.findById(testEntity2.id);
							assertThat(fetched.isPresent(), is(true));
							assertThat(fetched.get().children.size(), is(1));
							assertThat(fetched.get().children.get(0).contentLen, is(31L));
							assertThat(fetched.get().children.get(0).mimeType, is("text/plain"));
							assertThat(IOUtils.toString(contentRepository2.getContent(fetched.get().getChildren().get(0))), is("Hello New Spring Content World!"));
						});
					});
					Context("a POST to /{repository}/{id}/{contentProperty}", () -> {
						It("should append the content to the entity's content property collection", () -> {

							String content = "Hello New Spring Content World!";

							mvc.perform(fileUpload("/files/" + testEntity2.id.toString() + "/children/")
									.file(new MockMultipartFile("file", "test-file.txt", "text/plain", content.getBytes())))
									.andExpect(status().is2xxSuccessful());

							Optional<TestEntity2> fetched = repository2.findById(testEntity2.id);
							assertThat(fetched.isPresent(), is(true));
							assertThat(fetched.get().children.size(), is(1));
							assertThat(fetched.get().children.get(0).contentLen, is(31L));
							assertThat(fetched.get().children.get(0).fileName, is("test-file.txt"));
							assertThat(fetched.get().children.get(0).mimeType, is("text/plain"));
							assertThat(IOUtils.toString(contentRepository2.getContent(fetched.get().getChildren().get(0))), is("Hello New Spring Content World!"));
						});
					});
					Context("a DELETE to /{repository}/{id}/{contentProperty}", () -> {
						It("should return a 405 MethodNotAllowed", () -> {
							mvc.perform(delete("/files/" + testEntity2.id.toString() + "/children/"))
								.andExpect(status().isMethodNotAllowed());
						});
					});
				});
				Context("given that is has content", () -> {
					BeforeEach(() -> {
						testEntity2 = repository2.save(new TestEntity2());
						
						child1 = new TestEntityChild();
						child1.mimeType = "text/plain";
						contentRepository2.setContent(child1, new ByteArrayInputStream("Hello".getBytes()));
	
						child2 = new TestEntityChild();
						child2.mimeType = "text/plain";
						contentRepository2.setContent(child2, new ByteArrayInputStream("Spring Content World!".getBytes()));
	
						testEntity2.children = new ArrayList<TestEntityChild>();
						testEntity2.children.add(child1);
						testEntity2.children.add(child2);
						
						repository2.save(testEntity2);
					});
					Context("a GET to /{repository}/{id}/{contentCollectionProperty}/{contentId}", () -> {
						It("should return the content", () -> {
							MockHttpServletResponse response = mvc.perform(get("/files/" + testEntity2.id.toString() + "/children/" + child2.contentId)
									.accept("text/plain"))
									.andExpect(status().isOk())
									.andReturn().getResponse();
	
							assertThat(response, is(not(nullValue())));
							assertThat(response.getContentAsString(), is("Spring Content World!"));
						});
					});
					Context("a PUT to /{repository}/{id}/{contentCollectionProperty}/{contentId}", () -> {
						It("should set the content", () -> { 
							mvc.perform(put("/files/" + testEntity2.id.toString() + "/children/" + child2.contentId)
									.content("Modified Content World!")
									.contentType("text/plain"))
									.andExpect(status().isOk());
	
							assertThat(IOUtils.toString(contentRepository2.getContent(child2)), is("Modified Content World!"));
						});
					});
					Context("a POST to /{repository}/{id}/{contentCollectionProperty}/{contentId}", () -> {
						It("should set the content", () -> {

							String content = "Modified Content World!";

							mvc.perform(fileUpload("/files/" + testEntity2.id.toString() + "/children/" + child2.contentId)
									.file(new MockMultipartFile("file", "test-file.txt", "text/plain", content.getBytes())))
									.andExpect(status().isOk());

							Optional<TestEntity2> fetched = repository2.findById(testEntity2.id);
							assertThat(fetched.isPresent(), is(true));
							for (TestEntityChild child : fetched.get().children) {
								if (child.contentId.equals(child2.contentId)) {
									assertThat(child.contentId, is(not(nullValue())));
									assertThat(child.fileName, is("test-file.txt"));
									assertThat(child.mimeType, is("text/plain"));
									assertThat(child.contentLen, is(new Long(content.length())));
								}
							}
							assertThat(IOUtils.toString(contentRepository2.getContent(child2)), is("Modified Content World!"));
						});
					});
					Context("a DELETE to /{repository}/{id}/{contentCollectionProperty}/{contentId}", () -> {
						It("should delete the content", () -> { 
							mvc.perform(delete("/files/" + testEntity2.id.toString() + "/children/" + child2.contentId))
									.andExpect(status().isNoContent());
	
							assertThat(contentRepository2.getContent(child2), is(nullValue()));
	
							Optional<TestEntity2> fetched = repository2.findById(testEntity2.id);
							assertThat(fetched.isPresent(), is(true));
							assertThat(fetched.get().children.size(), is(2));
						});
					});
				});
			});
		});
	}

	@Test
	public void noop() {}
	
	protected RootResourceInformation getResourceInformation(Class<?> domainType) {

		Assert.notNull(domainType, "Domain type must not be null!");

		PersistentEntity<?, ?> entity = repositories.getPersistentEntity(domainType);

		return new RootResourceInformation(mappings.getMetadataFor(domainType), entity,
				invokerFactory.getInvokerFor(domainType));
	}
}
