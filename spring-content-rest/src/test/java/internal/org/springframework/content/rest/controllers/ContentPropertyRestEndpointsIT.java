package internal.org.springframework.content.rest.controllers;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.fileUpload;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Locale;
import java.util.Optional;
import java.util.TimeZone;

import org.apache.commons.io.IOUtils;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.rest.config.RestConfiguration;
import org.springframework.data.rest.webmvc.config.RepositoryRestMvcConfiguration;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.DelegatingWebMvcConfiguration;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jSpringRunner;

import internal.org.springframework.content.rest.support.StoreConfig;
import internal.org.springframework.content.rest.support.TestEntity2;
import internal.org.springframework.content.rest.support.TestEntity2Repository;
import internal.org.springframework.content.rest.support.TestEntityChild;
import internal.org.springframework.content.rest.support.TestEntityChildContentRepository;

@RunWith(Ginkgo4jSpringRunner.class)
// @Ginkgo4jConfiguration(threads=1)
@WebAppConfiguration
@ContextConfiguration(classes = {
		StoreConfig.class,
		DelegatingWebMvcConfiguration.class,
		RepositoryRestMvcConfiguration.class,
		RestConfiguration.class })
@Transactional
@ActiveProfiles("store")
public class ContentPropertyRestEndpointsIT {

	@Autowired
	private TestEntity2Repository repository2;

	@Autowired
	private TestEntityChildContentRepository contentRepository2;

	@Autowired
	private WebApplicationContext context;

	private Version versionTests;
	private LastModifiedDate lastModifiedDateTests;

	private MockMvc mvc;

	private TestEntity2 testEntity2;

	{
		Describe("Content/Content Collection REST Endpoints", () -> {
			BeforeEach(() -> {
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
//										.andExpect(header().string("last-modified", is(toHeaderDateFormat(testEntity2.getModifiedDate()))))
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

						versionTests = Version.tests();
						lastModifiedDateTests = LastModifiedDate.tests();
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
										.andExpect(header().string("last-modified", isWithinASecond(testEntity2.getModifiedDate())))
										.andExpect(content().string(is("Hello Spring Content World!")));
							});
						});
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

						versionTests = Version.tests();
						lastModifiedDateTests = LastModifiedDate.tests();
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
															"tests-file.txt", "text/plain",
															content.getBytes())))
											.andExpect(status().is2xxSuccessful());

									Optional<TestEntity2> fetched = repository2
											.findById(testEntity2.getId());
									assertThat(fetched.isPresent(), is(true));
									assertThat(fetched.get().getChildren().size(), is(1));
									assertThat(fetched.get().getChildren().get(0).contentLen,
											is(31L));
									assertThat(fetched.get().getChildren().get(0).fileName,
											is("tests-file.txt"));
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

	public static Matcher<String> isWithinASecond(final Date expectedDate) {
		return new TypeSafeMatcher<String>() {

			@Override
			protected void describeMismatchSafely(String foo, Description description) {
				description.appendText("was ").appendValue(foo);
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("Date ").appendValue(expectedDate);
			}

			@Override
			protected boolean matchesSafely(String actualDate) {
				// Your custom matching logic goes here
				Instant instant = Instant.ofEpochMilli(expectedDate.getTime());
				LocalDateTime expectedDateTime = LocalDateTime.ofInstant(instant, ZoneId.of("GMT"));

				LocalDateTime actualDateTime = LocalDateTime.parse(actualDate, DateTimeFormatter.ofPattern("EEE, d MMM yyyy HH:mm:ss z", Locale.ENGLISH));

				long diff = ChronoUnit.SECONDS.between(expectedDateTime, actualDateTime);
				return diff <= 1;
			}
		};
	}
}
