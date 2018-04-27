package internal.org.springframework.content.jpa.store;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.JustBeforeEach;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyObject;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;

import org.hamcrest.CoreMatchers;
import org.junit.runner.RunWith;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.io.DeletableResource;
import org.springframework.content.jpa.io.BlobResource;
import org.springframework.content.jpa.io.BlobResourceLoader;
import org.springframework.core.io.Resource;

import internal.org.springframework.content.jpa.io.GenericBlobResource;
import internal.org.springframework.content.jpa.repository.DefaultJpaStoreImpl;

@RunWith(Ginkgo4jRunner.class)
public class DefaultJpaStoreImplTest {

	private DefaultJpaStoreImpl<Object, Integer> store;

	private BlobResourceLoader blobResourceLoader;

	private BaseTestEntity entity;
	private InputStream stream;
	private InputStream inputStream;
	private OutputStream outputStream;
	private Resource resource;
	private DeletableResource deletableResource;
	private Integer id;
	private Exception e;

	{
		Describe("DefaultJpaStoreImpl", () -> {
			JustBeforeEach(() -> {
				store = new DefaultJpaStoreImpl(blobResourceLoader);
			});

			Describe("Store", () -> {
				BeforeEach(() -> {
					blobResourceLoader = mock(BlobResourceLoader.class);
				});
				Context("#getResource", () -> {
					Context("given an id", () -> {
						BeforeEach(() -> {
							id = 1;
						});
						JustBeforeEach(() -> {
							resource = store.getResource(id);
						});
						It("should use the blob resource loader to load a blob resource", () -> {
							verify(blobResourceLoader).getResource(id.toString());
						});
					});
				});
				Context("#associate", () -> {
					BeforeEach(() -> {
						id = 12345;

						entity = new TestEntity();

						resource = mock(BlobResource.class);
						when(blobResourceLoader.getResource(eq("12345"))).thenReturn(resource);
						when(resource.contentLength()).thenReturn(20L);
					});
					JustBeforeEach(() -> {
						store.associate(entity, id);
					});
					It("should use the conversion service to get a resource path", () -> {
						verify(blobResourceLoader).getResource(eq("12345"));
					});
					It("should set the entity's content ID attribute", () -> {
						assertThat(entity.getContentId(), CoreMatchers.is(12345));
					});
					It("should set the entity's content length attribute", () -> {
						assertThat(entity.getContentLen(), CoreMatchers.is(20L));
					});
				});

				Context("#unassociate", () -> {
					BeforeEach(() -> {
						entity = new TestEntity();
						entity.setContentId(12345);
						entity.setContentLen(999L);
					});
					JustBeforeEach(() -> {
						store.unassociate(entity);
					});
					It("should reset the entity's content ID attribute", () -> {
						assertThat(entity.getContentId(), is(nullValue()));
					});
					It("should set the entity's content length attribute", () -> {
						assertThat(entity.getContentLen(), is(0L));
					});
				});
			});

			Context("#getContent", () -> {
				BeforeEach(() -> {
					blobResourceLoader = mock(BlobResourceLoader.class);
					resource = mock(GenericBlobResource.class);

					entity = new TestEntity(12345);

					when(blobResourceLoader.getResource(entity.getContentId().toString())).thenReturn(resource);
				});
				JustBeforeEach(() -> {
					inputStream = store.getContent(entity);
				});
				Context("given content", () -> {
					BeforeEach(() -> {
						stream = new ByteArrayInputStream("hello content world!".getBytes());

						when(resource.getInputStream()).thenReturn(stream);
					});

					It("should use the blob resource factory to create a new blob resource", () -> {
						verify(blobResourceLoader).getResource(entity.getContentId().toString());
					});

					It("should return an inputstream", () -> {
						assertThat(inputStream, is(not(nullValue())));
					});
				});
				Context("given fetching the input stream fails", () -> {
					BeforeEach(() -> {
						when(resource.getInputStream()).thenThrow(new IOException());
					});
					It("should return null", () -> {
						assertThat(inputStream, is(nullValue()));
					});
				});
			});
			Context("#setContent", () -> {
				JustBeforeEach(() -> {
					try {
						store.setContent(entity, inputStream);
					} catch (Exception e) {
						this.e = e;
					}
				});
				Context("when the row does not exist", () -> {
					BeforeEach(() -> {
						blobResourceLoader = mock(BlobResourceLoader.class);

						entity = new TestEntity();
						byte[] content = new byte[5000];
						new Random().nextBytes(content);
						inputStream = new ByteArrayInputStream(content);

						resource = mock(BlobResource.class);
						when(blobResourceLoader.getResource("-1")).thenReturn(resource);
						outputStream = mock(OutputStream.class);
						when(((BlobResource) resource).getOutputStream()).thenReturn(outputStream);
						when(((BlobResource) resource).getId()).thenReturn(12345);
					});
					It("should write the contents of the inputstream to the resource's outputstream", () -> {
						verify(outputStream, atLeastOnce()).write(anyObject(), anyInt(), anyInt());
					});
					It("should update the @ContentId field", () -> {
						assertThat(entity.getContentId(), is(12345));
					});
					It("should update the @ContentLength field", () -> {
						assertThat(entity.getContentLen(), is(5000L));
					});
				});
			});

			Context("#unsetContent", () -> {
				BeforeEach(() -> {
					blobResourceLoader = mock(BlobResourceLoader.class);
					entity = new TestEntity();
					entity.setContentId(12345);
					deletableResource = mock(DeletableResource.class);

					when(blobResourceLoader.getResource(entity.getContentId().toString()))
							.thenReturn(deletableResource);
					doNothing().when(deletableResource).delete();
				});

				JustBeforeEach(() -> {
					store.unsetContent(entity);
				});

				It("sshould fetch the resource with id", () -> {
					verify(blobResourceLoader).getResource("12345");
				});

				It("should unset content", () -> {
					verify(deletableResource).delete();
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
						entity.setContentId(1234);
					});
					It("should not reset the content id metadata", () -> {
						assertThat(entity.getContentId(), is(1234));
						assertThat(entity.getContentLen(), is(0L));
					});
				});
				Context("when the property's ContentId field also is the Spring Id field", () -> {
					BeforeEach(() -> {
						entity = new SharedSpringIdContentIdEntity();
						entity.setContentId(1234);
					});
					It("should not reset the content id metadata", () -> {
						assertThat(entity.getContentId(), is(1234));
						assertThat(entity.getContentLen(), is(0L));
					});
				});
			});
		});
	}

	public interface BaseTestEntity {
		Integer getContentId();

		void setContentId(Integer contentId);

		long getContentLen();

		void setContentLen(long contentLen);
	}

	public static class TestEntity implements BaseTestEntity {
		@ContentId
		private Integer contentId;
		@ContentLength
		private long contentLen;

		public TestEntity() {
			this.contentId = null;
		}

		public TestEntity(int contentId) {
			this.contentId = new Integer(contentId);
		}

		@Override
		public Integer getContentId() {
			return this.contentId;
		}

		@Override
		public void setContentId(Integer contentId) {
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

	public static class SharedIdContentIdEntity implements BaseTestEntity {

		@javax.persistence.Id
		@ContentId
		private Integer contentId;

		@ContentLength
		private long contentLen;

		public SharedIdContentIdEntity() {
			this.contentId = null;
		}

		@Override
		public Integer getContentId() {
			return this.contentId;
		}

		@Override
		public void setContentId(Integer contentId) {
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

	public static class SharedSpringIdContentIdEntity implements BaseTestEntity {

		@org.springframework.data.annotation.Id
		@ContentId
		private Integer contentId;

		@ContentLength
		private long contentLen;

		public SharedSpringIdContentIdEntity() {
			this.contentId = null;
		}

		@Override
		public Integer getContentId() {
			return this.contentId;
		}

		@Override
		public void setContentId(Integer contentId) {
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
