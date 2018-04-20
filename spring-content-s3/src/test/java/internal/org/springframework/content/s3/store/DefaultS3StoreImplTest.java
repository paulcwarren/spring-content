package internal.org.springframework.content.s3.store;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.JustBeforeEach;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.anyObject;
import static org.mockito.ArgumentMatchers.endsWith;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

import com.amazonaws.services.s3.AmazonS3;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jConfiguration;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;

import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.WritableResource;

@RunWith(Ginkgo4jRunner.class)
@Ginkgo4jConfiguration(threads = 1)
public class DefaultS3StoreImplTest {
	private DefaultS3StoreImpl<ContentProperty, String> s3StoreImpl;
	private ResourceLoader loader;
	private ConversionService converter;
	private AmazonS3 client;
	private ContentProperty entity;

	private WritableResource resource;
	private Resource genericResource;
	private Resource nonExistentResource;

	private InputStream content;
	private OutputStream output;

	private File parent;

	private InputStream result;

	{
		Describe("Store", () -> {
			BeforeEach(() -> {
				genericResource = mock(Resource.class);
				loader = mock(ResourceLoader.class);
				converter = mock(ConversionService.class);
				client = mock(AmazonS3.class);

				s3StoreImpl = new DefaultS3StoreImpl<ContentProperty, String>(loader, converter, client, "some-bucket");
			});
			Context("#getResource", () -> {
				BeforeEach(() -> {

					when(converter.convert(eq("12345-67890"), eq(String.class))).thenReturn("12345-67890");
				});
				JustBeforeEach(() -> {
					genericResource = s3StoreImpl.getResource("12345-67890");
				});
				It("should use the conversion service to get a resource path", () -> {
					verify(converter).convert(eq("12345-67890"), eq(String.class));
					verify(loader).getResource(eq("s3://some-bucket/12345-67890"));
				});
			});
			Context("#associate", () -> {
				BeforeEach(() -> {
					entity = new TestEntity();

					when(converter.convert(eq("12345-67890"), eq(String.class))).thenReturn("12345-67890");

					when(loader.getResource(eq("s3://some-bucket/12345-67890"))).thenReturn(genericResource);
					when(genericResource.contentLength()).thenReturn(20L);
				});
				JustBeforeEach(() -> {
					s3StoreImpl.associate(entity, "12345-67890");
				});
				It("should use the conversion service to get a resource path", () -> {
					verify(converter).convert(eq("12345-67890"), eq(String.class));
					verify(loader).getResource(eq("s3://some-bucket/12345-67890"));
				});
				It("should set the entity's content ID attribute", () -> {
					assertThat(entity.getContentId(), is("s3://some-bucket/12345-67890"));
				});
				It("should set the entity's content length attribute", () -> {
					assertThat(entity.getContentLen(), is(20L));
				});
			});
			Context("#unassociate", () -> {
				BeforeEach(() -> {
					entity = new TestEntity();
					entity.setContentId("12345-67890");
					entity.setContentLen(999L);
				});
				JustBeforeEach(() -> {
					s3StoreImpl.unassociate(entity);
				});
				Context("when the property has a dedicated ContentId field", () -> {
					It("should reset the metadata", () -> {
						assertThat(entity.getContentId(), is(nullValue()));
						assertThat(entity.getContentLen(), is(0L));
					});
				});
				Context("when the property's ContentId field also is the javax persistence Id field", () -> {
					BeforeEach(() -> {
						entity = new SharedIdContentIdEntity();
						entity.setContentId("abcd-efgh");
					});
					It("should not reset the content id metadata", () -> {
						assertThat(entity.getContentId(), is("abcd-efgh"));
						assertThat(entity.getContentLen(), is(0L));
					});
				});
				Context("when the property's ContentId field also is the Spring Id field", () -> {
					BeforeEach(() -> {
						entity = new SharedSpringIdContentIdEntity();
						entity.setContentId("abcd-efgh");
					});
					It("should not reset the content id metadata", () -> {
						assertThat(entity.getContentId(), is("abcd-efgh"));
						assertThat(entity.getContentLen(), is(0L));
					});
				});
			});
		});

		Describe("DefaultS3StoreImplTest", () -> {
			BeforeEach(() -> {
				resource = mock(WritableResource.class);
				loader = mock(ResourceLoader.class);
				converter = mock(ConversionService.class);
				client = mock(AmazonS3.class);

				s3StoreImpl = new DefaultS3StoreImpl<ContentProperty, String>(loader, converter, client, "some-bucket");
			});
			Context("#setContent", () -> {
				BeforeEach(() -> {
					entity = new TestEntity();
					content = new ByteArrayInputStream("Hello content world!".getBytes());

					// when(placement.getLocation(anyObject())).thenReturn("/some/deeply/located/content");
				});

				JustBeforeEach(() -> {
					s3StoreImpl.setContent(entity, content);
				});

				Context("#when the content already exists", () -> {
					BeforeEach(() -> {
						entity.setContentId("abcd-efgh");

						when(converter.convert(eq("abcd-efgh"), eq(String.class))).thenReturn("abcd-efgh");

						when(loader.getResource(endsWith("abcd-efgh"))).thenReturn(resource);
						output = mock(OutputStream.class);
						when(resource.getOutputStream()).thenReturn(output);

						when(resource.contentLength()).thenReturn(20L);

						when(resource.exists()).thenReturn(true);
					});

					It("should use the converter to establish a resource path", () -> {
						// verify(placement).getLocation(anyObject());
						verify(converter).convert(eq("abcd-efgh"), eq(String.class));
					});

					It("should fetch the resource", () -> {
						verify(loader).getResource(eq("s3://some-bucket/abcd-efgh"));
					});

					It("should change the content length", () -> {
						assertThat(entity.getContentLen(), is(20L));
					});

					It("should write to the resource's outputstream", () -> {
						verify(resource).getOutputStream();
						verify(output, times(1)).write(Matchers.<byte[]>any(), eq(0), eq(20));
					});
				});

				Context("when the content does not already exist", () -> {
					BeforeEach(() -> {
						assertThat(entity.getContentId(), is(nullValue()));

						when(converter.convert(anyObject(), eq(String.class))).thenReturn("abcd-efgh");

						when(loader.getResource(endsWith("abcd-efgh"))).thenReturn(resource);
						output = mock(OutputStream.class);
						when(resource.getOutputStream()).thenReturn(output);

						when(resource.contentLength()).thenReturn(20L);

						File resourceFile = mock(File.class);
						parent = mock(File.class);

						when(resource.getFile()).thenReturn(resourceFile);
						when(resourceFile.getParentFile()).thenReturn(parent);
					});

					It("should make a new UUID", () -> {
						assertThat(entity.getContentId(), is(not(nullValue())));
					});

					It("should create a new resource", () -> {
						verify(loader).getResource(eq("s3://some-bucket/abcd-efgh"));
					});

					It("should write to the resource's outputstream", () -> {
						verify(resource).getOutputStream();
						verify(output, times(1)).write(Matchers.<byte[]>any(), eq(0), eq(20));
					});
				});
			});

			Context("#getContent", () -> {
				BeforeEach(() -> {
					entity = new TestEntity();
					content = mock(InputStream.class);
					entity.setContentId("abcd-efgh");

					// when(placement.getLocation(eq("abcd-efgh"))).thenReturn("/abcd/efgh");
					when(converter.convert(eq("abcd-efgh"), eq(String.class))).thenReturn("abcd-efgh");

					when(loader.getResource(endsWith("abcd-efgh"))).thenReturn(resource);
					when(resource.getInputStream()).thenReturn(content);
				});

				JustBeforeEach(() -> {
					result = s3StoreImpl.getContent(entity);
				});
				Context("when the resource exists", () -> {
					BeforeEach(() -> {
						when(resource.exists()).thenReturn(true);
					});

					It("should use the converter to establish a resource path", () -> {
						verify(converter).convert(eq("abcd-efgh"), eq(String.class));
					});

					It("should fetch the resource", () -> {
						verify(loader).getResource(eq("s3://some-bucket/abcd-efgh"));
					});

					It("should get content", () -> {
						assertThat(result, is(content));
					});
				});
				Context("when the resource does not exist", () -> {
					BeforeEach(() -> {
						nonExistentResource = mock(Resource.class);
						when(resource.exists()).thenReturn(true);

						when(loader.getResource(endsWith("abcd-efgh"))).thenReturn(nonExistentResource);
					});

					It("should use the converter to establish a resource path", () -> {
						verify(converter).convert(eq("abcd-efgh"), eq(String.class));
					});

					It("should fetch the resource", () -> {
						verify(loader).getResource(eq("s3://some-bucket/abcd-efgh"));
					});

					It("should not find the content", () -> {
						assertThat(result, is(nullValue()));
					});
				});
			});

			Context("#unsetContent", () -> {
				BeforeEach(() -> {
					entity = new TestEntity();
					entity.setContentId("abcd-efgh");
					entity.setContentLen(100L);
					resource = mock(WritableResource.class);
				});

				JustBeforeEach(() -> {
					s3StoreImpl.unsetContent(entity);
				});

				Context("when the content exists", () -> {

					BeforeEach(() -> {
						// when(placement.getLocation("abcd-efgh")).thenReturn("/abcd/efgh");
						when(converter.convert(eq("abcd-efgh"), eq(String.class))).thenReturn("abcd-efgh");

						when(loader.getResource(endsWith("abcd-efgh"))).thenReturn(resource);
						when(resource.exists()).thenReturn(true);
					});

					It("should use the converter to establish a resource path", () -> {
						verify(converter).convert(eq("abcd-efgh"), eq(String.class));
					});

					It("should fetch the resource", () -> {
						verify(loader).getResource(eq("s3://some-bucket/abcd-efgh"));
					});

					Context("when the property has a dedicated ContentId field", () -> {
						It("should reset the metadata", () -> {
							assertThat(entity.getContentId(), is(nullValue()));
							assertThat(entity.getContentLen(), is(0L));
						});
					});
					Context("when the property's ContentId field also is the javax persistence Id field", () -> {
						BeforeEach(() -> {
							entity = new SharedIdContentIdEntity();
							entity.setContentId("abcd-efgh");
						});
						It("should not reset the content id metadata", () -> {
							assertThat(entity.getContentId(), is("abcd-efgh"));
							assertThat(entity.getContentLen(), is(0L));
						});
					});
					Context("when the property's ContentId field also is the Spring Id field", () -> {
						BeforeEach(() -> {
							entity = new SharedSpringIdContentIdEntity();
							entity.setContentId("abcd-efgh");
						});
						It("should not reset the content id metadata", () -> {
							assertThat(entity.getContentId(), is("abcd-efgh"));
							assertThat(entity.getContentLen(), is(0L));
						});
					});
				});

				Context("when the content doesnt exist", () -> {
					BeforeEach(() -> {
						// when(placement.getLocation("abcd-efgh")).thenReturn("/abcd/efgh");
						when(converter.convert(eq("abcd-efgh"), eq(String.class))).thenReturn("abcd-efgh");

						nonExistentResource = mock(Resource.class);
						when(loader.getResource(endsWith("abcd-efgh"))).thenReturn(nonExistentResource);
						when(nonExistentResource.exists()).thenReturn(false);
					});

					It("should use the converter to establish a resource path", () -> {
						verify(converter).convert(eq("abcd-efgh"), eq(String.class));
					});

					It("should fetch the resource", () -> {
						verify(loader).getResource(eq("s3://some-bucket/abcd-efgh"));
					});

					It("should unset the content", () -> {
						verify(client, never()).deleteObject(anyObject());
						assertThat(entity.getContentId(), is(nullValue()));
						assertThat(entity.getContentLen(), is(0L));
					});
				});
			});
		});
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

		@Override
		public String getContentId() {
			return this.contentId;
		}

		@Override
		public void setContentId(String contentId) {
			this.contentId = contentId;
		}

		@Override
		public long getContentLen() {
			return contentLen;
		}

		@Override
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

		@Override
		public String getContentId() {
			return this.contentId;
		}

		@Override
		public void setContentId(String contentId) {
			this.contentId = contentId;
		}

		@Override
		public long getContentLen() {
			return contentLen;
		}

		@Override
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

		@Override
		public String getContentId() {
			return this.contentId;
		}

		@Override
		public void setContentId(String contentId) {
			this.contentId = contentId;
		}

		@Override
		public long getContentLen() {
			return contentLen;
		}

		@Override
		public void setContentLen(long contentLen) {
			this.contentLen = contentLen;
		}
	}
}
