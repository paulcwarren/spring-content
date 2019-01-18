package internal.org.springframework.content.rest.controllers;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.*;
import static java.lang.String.format;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Optional;
import java.util.TimeZone;

import internal.org.springframework.content.rest.support.LastModifiedDateTests;
import internal.org.springframework.content.rest.support.VersionHeaderTests;
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
// @Ginkgo4jConfiguration(threads=1)
@WebAppConfiguration
@ContextConfiguration(classes = { StoreConfig.class, DelegatingWebMvcConfiguration.class,
		RepositoryRestMvcConfiguration.class, RestConfiguration.class })
@Transactional
@ActiveProfiles("store")
public class ContentPropertyRestControllerIntegrationTest {

	@Autowired
	Repositories repositories;
	@Autowired
	RepositoryInvokerFactory invokerFactory;
	@Autowired
	ResourceMappings mappings;

	@Autowired
	TestEntityRepository repository;
	@Autowired
	TestEntityContentRepository contentRepository;
	@Autowired
	TestEntity2Repository repository2;
	@Autowired
	TestEntityChildContentRepository contentRepository2;

	@Autowired
	ContentPropertyCollectionRestController collectionCtrlr;
	@Autowired
	ContentPropertyRestController propCtrlr;

	@Autowired
	private WebApplicationContext context;

	private VersionHeaderTests versionTests;
	private LastModifiedDateTests lastModifiedDateTests;

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

				mvc = MockMvcBuilders.webAppContextSetup(context).build();
			});
			Context("given an Entity with a simple content property", () -> {
				BeforeEach(() -> {
					testEntity2 = repository2.save(new TestEntity2());
				});
				Context("given that is has no content", () -> {
					Context("a GET to /{repository}/{id}/{contentProperty}", () -> {
						It("should return 404", () -> {
							mvc.perform(
									get("/files/" + testEntity2.getId() + "/child"))
									.andExpect(status().isNotFound());
						});
					});
					Context("a PUT to /{repository}/{id}/{contentProperty}", () -> {
						It("should create the content", () -> {
							mvc.perform(
									put("/files/" + testEntity2.getId() + "/child")
											.content("Hello New Spring Content World!")
											.contentType("text/plain"))
									.andExpect(status().is2xxSuccessful());

							Optional<TestEntity2> fetched = repository2
									.findById(testEntity2.getId());
							assertThat(fetched.isPresent(), is(true));
							assertThat(fetched.get().getChild().contentId,
									is(not(nullValue())));
							assertThat(fetched.get().getChild().contentLen, is(31L));
							assertThat(fetched.get().getChild().mimeType, is("text/plain"));
							assertThat(
									IOUtils.toString(contentRepository2
											.getContent(fetched.get().getChild())),
									is("Hello New Spring Content World!"));

						});
					});
				});
				Context("given that it has content", () -> {
					BeforeEach(() -> {
						String content = "Hello Spring Content World!";

						testEntity2.setChild(new TestEntityChild());
						testEntity2.getChild().mimeType = "text/plain";
						contentRepository2.setContent(testEntity2.getChild(),new ByteArrayInputStream(content.getBytes()));
						testEntity2 = repository2.save(testEntity2);

						versionTests.setMvc(mvc);
						versionTests.setUrl("/files/" + testEntity2.getId() + "/child");
						versionTests.setRepo(repository2);
						versionTests.setStore(contentRepository2);
						versionTests.setEtag(format("\"%s\"", testEntity2.getVersion()));

						lastModifiedDateTests.setMvc(mvc);
						lastModifiedDateTests.setUrl("/files/" + testEntity2.getId() + "/child");
						lastModifiedDateTests.setLastModifiedDate(testEntity2.getModifiedDate());
						lastModifiedDateTests.setEtag(testEntity2.getVersion().toString());
						lastModifiedDateTests.setContent(content);
					});
					Context("given the content property is accessed via the /{repository}/{id}/{contentProperty} endpoint", () -> {
						Context("a GET to /{repository}/{id}/{contentProperty}", () -> {
							It("should return the content", () -> {
								MockHttpServletResponse response = mvc
										.perform(get("/files/" + testEntity2.getId() + "/child")
												.accept("text/plain"))
										.andExpect(status().isOk())
										.andExpect(header().string("etag", is("\"1\"")))
										.andExpect(header().string("last-modified", is(toHeaderDateFormat(testEntity2.getModifiedDate()))))
										.andReturn().getResponse();

								assertThat(response, is(not(nullValue())));
								assertThat(response.getContentAsString(), is("Hello Spring Content World!"));
							});
						});
						Context("a GET to /{repository}/{id}/{contentProperty} with a mime type that matches a renderer", () -> {
							It("should return the rendition and 200", () -> {
								MockHttpServletResponse response = mvc
										.perform(get(
												"/files/" + testEntity2.getId()
														+ "/child")
																.accept("text/html"))
										.andExpect(status().isOk()).andReturn()
										.getResponse();

								assertThat(response, is(not(nullValue())));
								assertThat(response.getContentAsString(), is(
										"<html><body>Hello Spring Content World!</body></html>"));
							});
						});
						Context("a GET to /{repository}/{id}/{contentProperty} with multiple mime types the last of which matches the content", () -> {
							It("should return the original content and 200", () -> {
								MockHttpServletResponse response = mvc
										.perform(get("/files/"
												+ testEntity2.getId()
												+ "/child").accept(
														new String[] { "text/xml",
																"text/*" }))
										.andExpect(status().isOk()).andReturn()
										.getResponse();

								assertThat(response, is(not(nullValue())));
								assertThat(response.getContentAsString(),
										is("Hello Spring Content World!"));
							});
						});
						Context("a PUT to /{repository}/{id}/{contentProperty}", () -> {
							It("should create the content", () -> {
								mvc.perform(
										put("/files/" + testEntity2.getId() + "/child")
												.content("Hello New Spring Content World!")
												.contentType("text/plain"))
										.andExpect(status().is2xxSuccessful());

								Optional<TestEntity2> fetched = repository2
										.findById(testEntity2.getId());
								assertThat(fetched.isPresent(), is(true));
								assertThat(fetched.get().getChild().contentId,
										is(not(nullValue())));
								assertThat(fetched.get().getChild().contentLen, is(31L));
								assertThat(fetched.get().getChild().mimeType, is("text/plain"));
							});
						});
						Context("a DELETE to /{repository}/{id}/{contentProperty}", () -> {
							It("should delete the content", () -> {
								mvc.perform(delete(
										"/files/" + testEntity2.getId() + "/child"))
										.andExpect(status().isNoContent());

								Optional<TestEntity2> fetched = repository2
										.findById(testEntity2.getId());
								assertThat(fetched.isPresent(), is(true));
								assertThat(fetched.get().getChild().contentId, is(nullValue()));
								assertThat(fetched.get().getChild().contentLen, is(0L));
							});
						});

						versionTests = new VersionHeaderTests();
						lastModifiedDateTests = new LastModifiedDateTests();
					});

					Context("given the content property is accessed via the /{repository}/{id}/{contentProperty}/{contentId} endpoint", () -> {
						BeforeEach(() -> {
							versionTests.setUrl("/files/" + testEntity2.getId() + "/child/" + testEntity2.getChild().contentId);
							lastModifiedDateTests.setUrl("/files/" + testEntity2.getId() + "/child/" + testEntity2.getChild().contentId);
						});
						Context("a GET to /{repository}/{id}/{contentProperty}/{contentId}", () -> {
							It("should return the content", () -> {
								mvc.perform(get("/files/" + testEntity2.getId() + "/child/" + testEntity2.getChild().contentId)
										.accept("text/plain"))
										.andExpect(status().isOk())
										.andExpect(header().string("etag", is("\"1\"")))
										.andExpect(header().string("last-modified", is(toHeaderDateFormat(testEntity2.getModifiedDate()))))
										.andExpect(content().string(is("Hello Spring Content World!")));
							});
						});
//					Context("a GET to /{repository}/{id}/{contentProperty} with a matching if-none-match header", () -> {
//						It("should return the content", () -> {
//							mvc.perform(get("/files/" + testEntity2.getId() + "/child/" + testEntity2.getChild().contentId)
//									.accept("text/plain")
//									.header("if-none-match", "\"1\""))
//									.andExpect(status().isNotModified())
//									.andExpect(header().string("last-modified", toHeaderDateFormat(testEntity2.getModifiedDate())));
//						});
//					});
//					Context("a GET to /{repository}/{id}/{contentProperty} with an unmatching if-none-match header", () -> {
//						It("should return the content", () -> {
//							mvc.perform(get("/files/" + testEntity2.getId() + "/child/" + testEntity2.getChild().contentId)
//									.accept("text/plain")
//									.header("if-none-match", "\"0\""))
//									.andExpect(status().isOk())
//									.andExpect(header().string("last-modified", toHeaderDateFormat(testEntity2.getModifiedDate())))
//									.andExpect(content().string(is("Hello Spring Content World!")));
//						});
//					});
//					Context("a GET to /{repository}/{id}/{contentProperty}/{contentId} with an if-unmodified-since date before the entity's modified date", () -> {
//						It("should respond with 412", () -> {
//							mvc.perform(get("/files/" + testEntity2.getId() + "/child/" + testEntity2.getChild().contentId)
//									.accept("text/plain")
//									.header("if-unmodified-since", toHeaderDateFormat(addDays(testEntity2.getModifiedDate(), -1))))
//									.andExpect(status().isPreconditionFailed());
//						});
//					});
//					Context("a GET to /{repository}/{id}/{contentProperty}/{contentId} with an if-unmodified-since date the same as the entity's modified date", () -> {
//						It("should respond with 200 and the content", () -> {
//							mvc.perform(get("/files/" + testEntity2.getId() + "/child/" + testEntity2.getChild().contentId)
//									.accept("text/plain")
//									.header("if-unmodified-since", toHeaderDateFormat(testEntity2.getModifiedDate())))
//									.andExpect(status().isOk())
//									.andExpect(content().string(is("Hello Spring Content World!")));
//						});
//					});
//					Context("a GET to /{repository}/{id}/{contentProperty}/{contentId} with an if-modified-since date before the entity's modified date", () -> {
//						It("should respond with 200 and the content", () -> {
//							mvc.perform(get("/files/" + testEntity2.getId() + "/child/" + testEntity2.getChild().contentId)
//									.accept("text/plain")
//									.header("if-modified-since", toHeaderDateFormat(addDays(testEntity2.getModifiedDate(), -1))))
//									.andExpect(status().isOk())
//									.andExpect(header().string("last-modified", toHeaderDateFormat(testEntity2.getModifiedDate())))
//									.andExpect(content().string(is("Hello Spring Content World!")));
//						});
//					});
//					Context("a GET to /{repository}/{id}/{contentProperty}/{contentId} with an if-modified-since date the same as the entity's modified date", () -> {
//						It("should respond with 200 and the content", () -> {
//							mvc.perform(get("/files/" + testEntity2.getId() + "/child/" + testEntity2.getChild().contentId)
//									.accept("text/plain")
//									.header("if-modified-since", toHeaderDateFormat(testEntity2.getModifiedDate())))
//									.andExpect(status().isNotModified())
//									.andExpect(header().string("last-modified", toHeaderDateFormat(testEntity2.getModifiedDate())));
//						});
//					});
//					Context("a GET to /{repository}/{id}/{contentProperty}/{contentId} with a range", () -> {
//						It("should return the content", () -> {
//							MockHttpServletResponse response = mvc
//									.perform(get(
//											"/files/" + testEntity2.getId()
//													+ "/child/"
//													+ testEntity2.getChild().contentId)
//															.accept("text/plain")
//															.header("range",
//																	"bytes=6-19"))
//									.andExpect(status().isPartialContent())
//									.andReturn().getResponse();
//							assertThat(response, is(not(nullValue())));
//
//							assertThat(response.getContentAsString(),
//									is("Spring Content"));
//						});
//					});
						Context("a GET to /{repository}/{id}/{contentProperty}/{contentId} with a mime type that matches a renderer", () -> {
							It("should return the rendition and 200", () -> {
								MockHttpServletResponse response = mvc
										.perform(get(
												"/files/" + testEntity2.getId()
														+ "/child/"
														+ testEntity2.getChild().contentId)
												.accept("text/html"))
										.andExpect(status().isOk()).andReturn()
										.getResponse();

								assertThat(response, is(not(nullValue())));
								assertThat(response.getContentAsString(), is(
										"<html><body>Hello Spring Content World!</body></html>"));
							});
						});
						Context("a GET to /{repository}/{id}/{contentProperty}/{contentId} with multiple mime types the last of which matches the content", () -> {
							It("should return the original content and 200", () -> {
								MockHttpServletResponse response = mvc
										.perform(get("/files/"
												+ testEntity2.getId()
												+ "/child/"
												+ testEntity2.getChild().contentId).accept(
												new String[] { "text/xml",
														"text/*" }))
										.andExpect(status().isOk()).andReturn()
										.getResponse();

								assertThat(response, is(not(nullValue())));
								assertThat(response.getContentAsString(),
										is("Hello Spring Content World!"));
							});
						});
						Context("a PUT to /{repository}/{id}/{contentProperty}/{contentId}", () -> {
							It("should overwrite the content", () -> {
								mvc.perform(put("/files/"
										+ testEntity2.getId() + "/child/"
										+ testEntity2.getChild().contentId).content(
										"Hello Modified Spring Content World!")
										.contentType("text/plain"))
										.andExpect(status().isOk());

								assertThat(
										IOUtils.toString(contentRepository2
												.getContent(testEntity2.getChild())),
										is("Hello Modified Spring Content World!"));
							});
						});
						Context("a DELETE to /{repository}/{id}/{contentProperty}/{contentId}", () -> {
							It("should delete the content", () -> {
								mvc.perform(delete("/files/"
										+ testEntity2.getId() + "/child/"
										+ testEntity2.getChild().contentId))
										.andExpect(status().isNoContent());

								Optional<TestEntity2> fetched = repository2
										.findById(testEntity2.getId());
								assertThat(fetched.isPresent(), is(true));
							});
						});

						versionTests = new VersionHeaderTests();
						lastModifiedDateTests = new LastModifiedDateTests();
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
							mvc.perform(get(
									"/files/" + testEntity2.getId() + "/children/"))
									.andExpect(status().isMethodNotAllowed());
						});
					});
					Context("a PUT to /{repository}/{id}/{contentProperty}", () -> {
						It("should append the content to the entity's content property collection",
								() -> {
									mvc.perform(put("/files/" + testEntity2.getId()
											+ "/children/").content(
													"Hello New Spring Content World!")
													.contentType("text/plain"))
											.andExpect(status().is2xxSuccessful());

									Optional<TestEntity2> fetched = repository2
											.findById(testEntity2.getId());
									assertThat(fetched.isPresent(), is(true));
									assertThat(fetched.get().getChildren().size(), is(1));
									assertThat(fetched.get().getChildren().get(0).contentLen,
											is(31L));
									assertThat(fetched.get().getChildren().get(0).mimeType,
											is("text/plain"));
								});
					});
					Context("a POST to /{repository}/{id}/{contentProperty}", () -> {
						It("should append the content to the entity's content property collection",
								() -> {

									String content = "Hello New Spring Content World!";

									mvc.perform(fileUpload("/files/"
											+ testEntity2.getId() + "/children/")
													.file(new MockMultipartFile("file",
															"test-file.txt", "text/plain",
															content.getBytes())))
											.andExpect(status().is2xxSuccessful());

									Optional<TestEntity2> fetched = repository2
											.findById(testEntity2.getId());
									assertThat(fetched.isPresent(), is(true));
									assertThat(fetched.get().getChildren().size(), is(1));
									assertThat(fetched.get().getChildren().get(0).contentLen,
											is(31L));
									assertThat(fetched.get().getChildren().get(0).fileName,
											is("test-file.txt"));
									assertThat(fetched.get().getChildren().get(0).mimeType,
											is("text/plain"));
								});
					});
					Context("a DELETE to /{repository}/{id}/{contentProperty}", () -> {
						It("should return a 405 MethodNotAllowed", () -> {
							mvc.perform(delete(
									"/files/" + testEntity2.getId() + "/children/"))
									.andExpect(status().isMethodNotAllowed());
						});
					});
				});
				Context("given that is has content", () -> {
					BeforeEach(() -> {
						testEntity2 = repository2.save(new TestEntity2());

						child1 = new TestEntityChild();
						child1.mimeType = "text/plain";
						contentRepository2.setContent(child1,
								new ByteArrayInputStream("Hello".getBytes()));

						child2 = new TestEntityChild();
						child2.mimeType = "text/plain";
						contentRepository2.setContent(child2, new ByteArrayInputStream(
								"Spring Content World!".getBytes()));

						testEntity2.setChildren(new ArrayList<TestEntityChild>());
						testEntity2.getChildren().add(child1);
						testEntity2.getChildren().add(child2);

						repository2.save(testEntity2);
					});
					Context("a GET to /{repository}/{id}/{contentCollectionProperty}", () -> {
						It("should return a 406 Method Not Allowed", () -> {
							mvc.perform(get("/files/" + testEntity2.getId() + "/children/")
									.accept("text/plain"))
							.andExpect(status().isMethodNotAllowed());
						});
					});
					Context("a GET to /{repository}/{id}/{contentCollectionProperty}/{contentId}", () -> {
						It("should return the content", () -> {
							MockHttpServletResponse response = mvc
									.perform(get("/files/"
											+ testEntity2.getId()
											+ "/children/" + child2.contentId)
													.accept("text/plain"))
									.andExpect(status().isOk()).andReturn()
									.getResponse();

							assertThat(response, is(not(nullValue())));
							assertThat(response.getContentAsString(),
									is("Spring Content World!"));
						});
					});
					Context("a PUT to /{repository}/{id}/{contentCollectionProperty}/{contentId}", () -> {
						It("should set the content", () -> {
							mvc.perform(put("/files/" + testEntity2.getId()
									+ "/children/" + child2.contentId)
											.content("Modified Content World!")
											.contentType("text/plain"))
									.andExpect(status().isOk());

							assertThat(
									IOUtils.toString(contentRepository2
											.getContent(child2)),
									is("Modified Content World!"));
						});
					});
					Context("a POST to /{repository}/{id}/{contentCollectionProperty}/{contentId}", () -> {
						It("should set the content", () -> {

							String content = "Modified Content World!";

							mvc.perform(fileUpload("/files/"
									+ testEntity2.getId() + "/children/"
									+ child2.contentId)
											.file(new MockMultipartFile("file",
													"test-file.txt", "text/plain",
													content.getBytes())))
									.andExpect(status().isOk());

							Optional<TestEntity2> fetched = repository2
									.findById(testEntity2.getId());
							assertThat(fetched.isPresent(), is(true));
							for (TestEntityChild child : fetched.get().getChildren()) {
								if (child.contentId.equals(child2.contentId)) {
									assertThat(child.contentId,
											is(not(nullValue())));
									assertThat(child.fileName,
											is("test-file.txt"));
									assertThat(child.mimeType, is("text/plain"));
									assertThat(child.contentLen,
											is(new Long(content.length())));
								}
							}
							assertThat(
									IOUtils.toString(contentRepository2
											.getContent(child2)),
									is("Modified Content World!"));
						});
					});
					Context("a DELETE to /{repository}/{id}/{contentCollectionProperty}", () -> {
						It("should return a 406 Method Not Allowed", () -> {
							mvc.perform(
								delete("/files/" + testEntity2.getId() + "/children/"))
								.andExpect(status().isMethodNotAllowed());
						});
					});
					Context("a DELETE to /{repository}/{id}/{contentCollectionProperty}/{contentId}", () -> {
						It("should delete the content", () -> {
							mvc.perform(
									delete("/files/" + testEntity2.getId()
											+ "/children/" + child2.contentId))
									.andExpect(status().isNoContent());

							assertThat(contentRepository2.getContent(child2),
									is(nullValue()));

							Optional<TestEntity2> fetched = repository2
									.findById(testEntity2.getId());
							assertThat(fetched.isPresent(), is(true));
							assertThat(fetched.get().getChildren().size(), is(2));
						});
					});
				});
			});
		});
	}

	@Test
	public void noop() {
	}

	private static String toHeaderDateFormat(Date dt) {
		SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
		format.setTimeZone(TimeZone.getTimeZone("GMT"));
		return format.format(dt);
	}

	private static String toHeaderDateFormat(String dt) {
		SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
		format.setTimeZone(TimeZone.getTimeZone("GMT"));
		return format.format(new Date(dt));
	}

	private static Date addDays(Date dt, int n) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(dt);
		cal.add(Calendar.DATE, n);
		return cal.getTime();

	}

	private static Date addDays(String dt, int n) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(new Date(dt));
		cal.add(Calendar.DATE, n);
		return cal.getTime();

	}
}
