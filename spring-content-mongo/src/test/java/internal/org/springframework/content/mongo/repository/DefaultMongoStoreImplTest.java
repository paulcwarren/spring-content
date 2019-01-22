package internal.org.springframework.content.mongo.repository;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;
import com.mongodb.gridfs.GridFSFile;
import org.bson.types.ObjectId;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.repository.StoreAccessException;
import org.springframework.content.commons.utils.PlacementService;
import org.springframework.content.commons.utils.PlacementServiceImpl;
import org.springframework.core.io.Resource;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.JustBeforeEach;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(Ginkgo4jRunner.class)
public class DefaultMongoStoreImplTest {
	private DefaultMongoStoreImpl<Object, String> mongoContentRepoImpl;
	private GridFsTemplate gridFsTemplate;
	private GridFSFile gridFSFile;
	private ObjectId gridFSId;
	private ContentProperty property;
	private GridFsResource resource;
	private Resource genericResource;
	private PlacementService placer;

	private InputStream content;
	private InputStream result;
	private Exception e;

	{
		Describe("DefaultMongoStoreImpl", () -> {
			Describe("Store", () -> {
				BeforeEach(() -> {
					placer = mock(PlacementService.class);
					gridFsTemplate = mock(GridFsTemplate.class);
					resource = mock(GridFsResource.class);
					mongoContentRepoImpl = new DefaultMongoStoreImpl<Object, String>(
							gridFsTemplate, placer);
				});
				Context("#getResource", () -> {
					Context("with an id", () -> {
						BeforeEach(() -> {
							when(placer.convert(eq("abcd"), eq(String.class)))
									.thenReturn("abcd");
							when(gridFsTemplate.getResource(eq("abcd")))
									.thenReturn(resource);
						});
						JustBeforeEach(() -> {
							genericResource = mongoContentRepoImpl.getResource("abcd");
						});
						It("should use the mongoStorePlacementService to find the resource path",
								() -> {
									verify(placer).convert(eq("abcd"),
											eq(String.class));
								});
						It("should get Resource", () -> {
							assertThat(genericResource, is(instanceOf(Resource.class)));
						});
					});
				});
			});
			Describe("AssociativeStore", () -> {
				BeforeEach(() -> {
					placer = mock(PlacementService.class);
					gridFsTemplate = mock(GridFsTemplate.class);
					resource = mock(GridFsResource.class);
					mongoContentRepoImpl = new DefaultMongoStoreImpl<Object, String>(
							gridFsTemplate, placer);
				});
				Context("getResource", () -> {
					BeforeEach(() -> {
						property = new TestEntity();
					});
					JustBeforeEach(() -> {
						genericResource = mongoContentRepoImpl.getResource(property);
					});
					Context("given no resource is associated", () -> {
						BeforeEach(() -> {
							when(placer.convert(matches(
									"[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}"),
									eq(String.class))).thenReturn("abcd");
							when(gridFsTemplate.getResource(eq("abcd")))
									.thenReturn(resource);
						});
						It("should return null",
							() -> {
								verify(placer, never()).convert(matches(
										"[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}"),
										eq(String.class));
								assertThat(genericResource, is(nullValue()));
							});
					});
					Context("with existing content", () -> {
						BeforeEach(() -> {
							property.setContentId("abcd");

							when(placer.convert(eq("abcd"), eq(String.class)))
									.thenReturn("abcd");
							when(gridFsTemplate.getResource(eq("abcd")))
									.thenReturn(resource);
						});
						It("should use the mongoStorePlacementService to find the resource path",
								() -> {
									verify(placer).convert(eq("abcd"),
											eq(String.class));
								});
						It("should get Resource", () -> {
							assertThat(genericResource, is(not(nullValue())));
						});
					});
					Context("when the entity has a String-arg constructor - Issue #57", () ->{
						BeforeEach(() -> {
							PlacementService placementService = new PlacementServiceImpl();
							mongoContentRepoImpl = new DefaultMongoStoreImpl<>(gridFsTemplate, placer);

							property = new TestEntity();
						});
						It("should not call the placement service by default", () -> {
							verify(placer, never()).convert(property, String.class);
						});
					});
				});

				Context("#associate", () -> {
					BeforeEach(() -> {
						property = new TestEntity();

						when(placer.convert(eq("12345"), eq(String.class)))
								.thenReturn("12345");
					});
					JustBeforeEach(() -> {
						mongoContentRepoImpl.associate(property, "12345");
					});
					It("should use the mongoStorePlacementService to find the resource path",
							() -> {
								verify(placer).convert(eq("12345"), eq(String.class));
							});
					It("should set the entity's content ID attribute", () -> {
						assertThat(property.getContentId(), is("12345"));
					});
				});

				Context("#unassociate", () -> {
					BeforeEach(() -> {
						property = new TestEntity();
						property.setContentId("12345");
					});
					JustBeforeEach(() -> {
						mongoContentRepoImpl.unassociate(property);
					});
					It("should reset the entity's content ID attribute", () -> {
						assertThat(property.getContentId(), is(nullValue()));
					});
					Context("when the entity has a shared @Id @ContentId field", () -> {
						BeforeEach(() -> {
							property = new SharedIdContentIdEntity();
							property.setContentId("12345");
						});
						It("should not reset the entity's @ContentId (because it is also the @Id)",
								() -> {
									assertThat(property.getContentId(), is("12345"));
								});
					});
				});
			});
			Describe("ContentStore", () -> {
				BeforeEach(() -> {
					placer = mock(PlacementService.class);
					gridFsTemplate = mock(GridFsTemplate.class);
					gridFSFile = mock(GridFSFile.class);
					resource = mock(GridFsResource.class);
					mongoContentRepoImpl = new DefaultMongoStoreImpl<Object, String>(
							gridFsTemplate, placer);
				});

				Context("#setContent", () -> {
					BeforeEach(() -> {
						property = new TestEntity();

						content = mock(InputStream.class);
					});

					JustBeforeEach(() -> {
						try {
							mongoContentRepoImpl.setContent(property, content);
						} catch (Exception e) {
							this.e = e;
						}
					});

					Context("when content is new", () -> {
						BeforeEach(() -> {
							when(placer.convert(isA(UUID.class), eq(String.class)))
									.thenReturn("12345-67890");
							when(gridFsTemplate.getResource(anyString())).thenReturn(null)
									.thenReturn(resource);
							// when(gridFsTemplate.store(anyObject(),
							// anyString())).thenReturn(gridFSFile);
							when(resource.contentLength()).thenReturn(1L);
						});

						It("should use the mongoStorePlacementService to find the resource path",
								() -> {
									verify(placer).convert(isA(UUID.class),
											eq(String.class));
								});

						It("should store content in GridFS", () -> {
							verify(gridFsTemplate).store(eq(content), eq("12345-67890"));
						});

						It("should update the content length", () -> {
							assertThat(property.getContentLen(), is(1L));
						});
					});

					Context("#when the content already exists", () -> {
						BeforeEach(() -> {
							property.setContentId("abcd-efghi");

							when(placer.convert(eq("abcd-efghi"), eq(String.class)))
									.thenReturn("abcd-efghi");
							when(gridFsTemplate.getResource("abcd-efghi")).thenReturn(resource);
							// when(gridFsTemplate.store(anyObject(),
							// anyString())).thenReturn(gridFSFile);
							when(resource.exists()).thenReturn(true);
							when(resource.contentLength()).thenReturn(1L);
						});

						It("should use the mongoStorePlacementService to find the resource path",
								() -> {
									verify(placer).convert(eq("abcd-efghi"),
											eq(String.class));
								});

						It("should delete the exsting resource", () -> {
							verify(gridFsTemplate).delete(anyObject());
						});

						It("should store content in GridFS", () -> {
							verify(gridFsTemplate).store(eq(content), eq("abcd-efghi"));
						});

						It("should update the content length", () -> {
							assertThat(property.getContentLen(), is(1L));
						});

						Context("when the gridfs store throws an exception", () -> {
							BeforeEach(() -> {
								when(gridFsTemplate.store(anyObject(), anyString())).thenThrow(new RuntimeException("set-exception"));
							});
							It("should throw a StoreAccessException", () -> {
								assertThat(e, is(instanceOf(StoreAccessException.class)));
								assertThat(e.getCause().getMessage(), is("set-exception"));
							});
						});
					});
				});

				Context("#getContent", () -> {
					BeforeEach(() -> {
						property = new TestEntity();
						property.setContentId("abcd");

						content = mock(InputStream.class);

						when(placer.convert(eq("abcd"), eq(String.class)))
								.thenReturn("abcd");
						when(gridFsTemplate.getResource("abcd")).thenReturn(resource);
						when(resource.getInputStream()).thenReturn(content);
					});

					JustBeforeEach(() -> {
						try {
							result = mongoContentRepoImpl.getContent(property);
						} catch (Exception e) {
							this.e = e;
						}
					});

					Context("when the resource exists", () -> {
						BeforeEach(() -> {
							when(resource.exists()).thenReturn(true);
						});

						It("should use the placer to get the resource path", () -> {
							verify(placer).convert(eq("abcd"), eq(String.class));
						});

						It("should fetch the resource from that path", () -> {
							verify(gridFsTemplate).getResource("abcd");
						});

						It("should get content", () -> {
							assertThat(result, is(content));
						});

						Context("when the resource outputstream throws an IOException", () -> {
							BeforeEach(() -> {
								when(resource.getInputStream()).thenThrow(new IOException("get-ioexception"));
							});
							It("should throw a StoreAccessException", () -> {
								assertThat(e, is(instanceOf(StoreAccessException.class)));
								assertThat(e.getCause().getMessage(), is("get-ioexception"));
							});
						});
					});

					Context("when the resource does not exists", () -> {
						BeforeEach(() -> {
							when(resource.exists()).thenReturn(false);
						});

						It("should not find the content", () -> {
							assertThat(result, is(nullValue()));
						});
					});
				});

				Context("#unsetContent", () -> {
					BeforeEach(() -> {
						property = new TestEntity();
						property.setContentId("abcd");

						when(placer.convert(eq("abcd"), eq(String.class)))
								.thenReturn("abcd");
						when(gridFsTemplate.getResource("abcd")).thenReturn(resource);
						when(resource.exists()).thenReturn(true);
					});

					JustBeforeEach(() -> {
						try {
							mongoContentRepoImpl.unsetContent(property);
						} catch (Exception e) {
							this.e = e;
						}
					});

					It("should use the placer to get the resource path", () -> {
						verify(placer).convert(eq("abcd"), eq(String.class));
					});

					It("should fetch the resource from that path", () -> {
						verify(gridFsTemplate).getResource(eq("abcd"));
					});

					It("should unset content", () -> {
						verify(gridFsTemplate).delete(anyObject());
					});

					Context("when gridfs deletion throws an exception", () -> {
						BeforeEach(() -> {
							doThrow(new RuntimeException("unset-exception")).when(gridFsTemplate).delete(anyObject());
						});
						It("should throw a StoreAccessException", () -> {
							assertThat(e, is(instanceOf(StoreAccessException.class)));
							assertThat(e.getCause().getMessage(), is("unset-exception"));
						});
					});

					Context("when the property has a dedicated ContentId field", () -> {
						It("should reset the metadata", () -> {
							assertThat(property.getContentId(), is(nullValue()));
							assertThat(property.getContentLen(), is(0L));
						});
					});
					Context("when the property's ContentId field also is the javax persistence Id field",
							() -> {
								BeforeEach(() -> {
									property = new SharedIdContentIdEntity();
									property.setContentId("abcd");
								});
								It("should not reset the content id metadata", () -> {
									assertThat(property.getContentId(), is("abcd"));
									assertThat(property.getContentLen(), is(0L));
								});
							});
					Context("when the property's ContentId field also is the Spring Id field",
							() -> {
								BeforeEach(() -> {
									property = new SharedSpringIdContentIdEntity();
									property.setContentId("abcd");
								});
								It("should not reset the content id metadata", () -> {
									assertThat(property.getContentId(), is("abcd"));
									assertThat(property.getContentLen(), is(0L));
								});
							});
				});
			});
		});
	}

	@Test
	public void test() {
		// noop
	}

	public interface ContentProperty {
		String getContentId();

		void setContentId(String contentId);

		long getContentLen();

		void setContentLen(long contentLen);
	}

	public static class TestEntity implements ContentProperty {

		@ContentId
		private String contentId;

		@ContentLength
		private long contentLen;

		public TestEntity() {
			this.contentId = null;
		}

		public TestEntity(String contentId) {
			this.contentId = new String(contentId);
		}

		public String getContentId() {
			return this.contentId;
		}

		public void setContentId(String contentId) {
			this.contentId = contentId;
		}

		public long getContentLen() {
			return contentLen;
		}

		public void setContentLen(long contentLen) {
			this.contentLen = contentLen;
		}
	}

	public static class SharedIdContentIdEntity implements ContentProperty {

		@javax.persistence.Id
		@ContentId
		private String contentId;

		@ContentLength
		private long contentLen;

		public SharedIdContentIdEntity() {
			this.contentId = null;
		}

		public String getContentId() {
			return this.contentId;
		}

		public void setContentId(String contentId) {
			this.contentId = contentId;
		}

		public long getContentLen() {
			return contentLen;
		}

		public void setContentLen(long contentLen) {
			this.contentLen = contentLen;
		}
	}

	public static class SharedSpringIdContentIdEntity implements ContentProperty {

		@org.springframework.data.annotation.Id
		@ContentId
		private String contentId;

		@ContentLength
		private long contentLen;

		public SharedSpringIdContentIdEntity() {
			this.contentId = null;
		}

		public String getContentId() {
			return this.contentId;
		}

		public void setContentId(String contentId) {
			this.contentId = contentId;
		}

		public long getContentLen() {
			return contentLen;
		}

		public void setContentLen(long contentLen) {
			this.contentLen = contentLen;
		}
	}
}
