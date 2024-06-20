package internal.org.springframework.content.jpa.store;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.JustBeforeEach;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyObject;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;
import java.util.UUID;

import jakarta.persistence.Id;
import org.hamcrest.CoreMatchers;
import org.junit.runner.RunWith;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.repository.StoreAccessException;
import org.springframework.content.jpa.io.BlobResource;
import org.springframework.content.jpa.io.BlobResourceLoader;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;

import internal.org.springframework.content.jpa.io.GenericBlobResource;

@RunWith(Ginkgo4jRunner.class)
public class DefaultJpaStoreImplTest {

	private DefaultJpaStoreImpl<Object, String> store;

	private BlobResourceLoader blobResourceLoader;

	private TestEntity entity;
	private JakartaTestEntity jakartaAnnotatedEntity;
	private InputStream stream;
	private InputStream inputStream;
	private Resource inputResource;
	private OutputStream outputStream;
	private Resource resource;
	private BlobResource blobResource;
	private String id;
	private Exception e;

	{
		Describe("DefaultJpaStoreImpl", () -> {
			JustBeforeEach(() -> {
				store = spy(new DefaultJpaStoreImpl(blobResourceLoader, null, 8096));
			});

			Describe("Store", () -> {
				BeforeEach(() -> {
					blobResourceLoader = mock(BlobResourceLoader.class);
				});
				Context("#getResource", () -> {
					Context("given an id", () -> {
						BeforeEach(() -> {
							id = "1";
						});
						JustBeforeEach(() -> {
							resource = store.getResource(id);
						});
						It("should use the blob resource loader to load a blob resource",
								() -> {
									verify(blobResourceLoader).getResource(id.toString());
								});
					});
				});
			});
			Describe("AssociativeStore", () -> {
				BeforeEach(() -> {
					blobResourceLoader = mock(BlobResourceLoader.class);
				});
				Context("#getResource", () -> {
					JustBeforeEach(() -> {
						resource = store.getResource(entity);
					});
					Context("when the entity is not associated with a resource",
							() -> {
								BeforeEach(() -> {
									entity = new TestEntity();
								});
								It("should return null", () -> {
									verify(blobResourceLoader, never()).getResource(anyObject());
									assertThat(resource, is(nullValue()));
								});
							});
					Context("when the entity is associated with a resource",
							() -> {
								BeforeEach(() -> {
									entity = new TestEntity();
									entity.setContentId("12345");
								});
								It("should load a new resource", () -> {
									verify(blobResourceLoader).getResource(eq("12345"));
								});
							});
				});
				Context("#associate", () -> {
					BeforeEach(() -> {
						id = "12345";

						entity = new TestEntity();

						resource = mock(BlobResource.class);
						when(blobResourceLoader.getResource(eq("12345")))
								.thenReturn(resource);
						when(resource.contentLength()).thenReturn(20L);
					});
					JustBeforeEach(() -> {
						store.associate(entity, id);
					});
					It("should set the entity's content ID attribute", () -> {
						assertThat(entity.getContentId(), CoreMatchers.is("12345"));
					});
				});
				Context("#unassociate", () -> {
					BeforeEach(() -> {
						id = "12345";

						entity = new TestEntity();
						entity.setContentId(id);
						entity.setContentLen(20L);
					});
					JustBeforeEach(() -> {
						store.unassociate(entity);
					});
					It("should reset the @ContentId", () -> {
						assertThat(entity.getContentId(), is(nullValue()));
					});
				});
			});
			Describe("ContentStore", () -> {
				Context("#getContent", () -> {
					BeforeEach(() -> {
						blobResourceLoader = mock(BlobResourceLoader.class);
						resource = mock(GenericBlobResource.class);

						entity = new TestEntity("12345");

						when(blobResourceLoader.getResource(entity.getContentId().toString()))
								.thenReturn(resource);
					});
					JustBeforeEach(() -> {
						try {
							inputStream = store.getContent(entity);
						} catch (Exception e) {
							this.e = e;
						}
					});
					Context("given content", () -> {
						BeforeEach(() -> {
							stream = new ByteArrayInputStream(
									"hello content world!".getBytes());

							when(resource.getInputStream()).thenReturn(stream);
						});

						It("should use the blob resource factory to create a new blob resource",
								() -> {
									verify(blobResourceLoader)
											.getResource(entity.getContentId().toString());
								});

						It("should return an inputstream", () -> {
							assertThat(inputStream, is(not(nullValue())));
						});
					});
					Context("given fetching the input stream fails", () -> {
						BeforeEach(() -> {
							when(resource.getInputStream()).thenThrow(new IOException("get-ioexception"));
						});
						It("should return null and throw a StoreAccessException", () -> {
							assertThat(inputStream, is(nullValue()));
							assertThat(e, is(instanceOf(StoreAccessException.class)));
							assertThat(e.getCause().getMessage(), is("get-ioexception"));
						});
					});
				});
				Context("#setContent", () -> {
					JustBeforeEach(() -> {
						try {
							store.setContent(entity, inputStream);
						}
						catch (Exception e) {
							this.e = e;
						}
					});
					BeforeEach(() -> {
						blobResourceLoader = mock(BlobResourceLoader.class);

						entity = new TestEntity();
						byte[] content = new byte[5000];
						new Random().nextBytes(content);
						inputStream = new ByteArrayInputStream(content);

						resource = mock(BlobResource.class);
						when(blobResourceLoader.getResource(matches(
								"[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}")))
										.thenReturn(resource);
						outputStream = mock(OutputStream.class);
						when(((BlobResource) resource).getOutputStream())
								.thenReturn(outputStream);
						when(((BlobResource) resource).getId()).thenReturn(12345);
					});
					It("should write the contents of the inputstream to the resource's outputstream",
							() -> {
								verify(outputStream, atLeastOnce()).write(anyObject(),
										anyInt(), anyInt());
							});
					It("should update the @ContentId field", () -> {
						assertThat(entity.getContentId(), is("12345"));
					});
					It("should update the @ContentLength field", () -> {
						assertThat(entity.getContentLen(), is(5000L));
					});
					Context("when the resource output stream throws an IOException", () -> {
						BeforeEach(() -> {
							when(((BlobResource) resource).getOutputStream()).thenThrow(new IOException("set-ioexception"));
						});
						It("should throw a StoreAccessException", () -> {
							assertThat(e, is(instanceOf(StoreAccessException.class)));
							assertThat(e.getCause().getMessage(), is("set-ioexception"));
						});
					});
				});

				Context("#setContent from Resource", () -> {

					BeforeEach(() -> {
						entity = new TestEntity();
						stream = new ByteArrayInputStream("Hello content world!".getBytes());
						inputResource = new InputStreamResource(stream);
					});

					JustBeforeEach(() -> {
						try {
							store.setContent(entity, inputResource);
						} catch (Exception e) {
							this.e = e;
						}
					});

					It("should delegate", () -> {
						verify(store).setContent(eq(entity), eq(stream));
					});

					Context("when the resource throws an IOException", () -> {
						BeforeEach(() -> {
							inputResource = mock(Resource.class);
							when(inputResource.getInputStream()).thenThrow(new IOException("setContent badness"));
						});
						It("should throw a StoreAccessException", () -> {
							assertThat(e, CoreMatchers.is(instanceOf(StoreAccessException.class)));
							assertThat(e.getCause().getMessage(), containsString("setContent badness"));
						});
					});
				});

				Context("#unsetContent", () -> {
					JustBeforeEach(() -> {
						try {
							store.unsetContent(entity);
						} catch (Exception e) {
							this.e = e;
						}
					});
					BeforeEach(() -> {
						blobResourceLoader = mock(BlobResourceLoader.class);
						blobResource = mock(GenericBlobResource.class);

						entity = new TestEntity("12345");

						when(blobResourceLoader.getResource(entity.getContentId().toString()))
								.thenReturn(blobResource);
					});
					It("should delete the content", () -> {
						verify(blobResource).delete();
					});
					Context("resource delete throws an Exception", () -> {
						BeforeEach(() -> {
							doThrow(new IOException("unset-ioexception")).when(blobResource).delete();
						});
						It("should throw a StoreAccessException", () -> {
							assertThat(e, is(instanceOf(StoreAccessException.class)));
							assertThat(e.getCause().getMessage(), is("unset-ioexception"));
						});
					});
				});
			});
		});

        Describe("DefaultJpaStoreImpl jakartaAnnotatedEntity", () -> {
            JustBeforeEach(() -> {
                store = spy(new DefaultJpaStoreImpl(blobResourceLoader, null, 8096));
            });

            Describe("Store", () -> {
                BeforeEach(() -> {
                    blobResourceLoader = mock(BlobResourceLoader.class);
                });
                Context("#getResource", () -> {
                    Context("given an id", () -> {
                        BeforeEach(() -> {
                            id = "1";
                        });
                        JustBeforeEach(() -> {
                            resource = store.getResource(id);
                        });
                        It("should use the blob resource loader to load a blob resource",
                                () -> {
                                    verify(blobResourceLoader).getResource(id.toString());
                                });
                    });
                });
            });
            Describe("AssociativeStore", () -> {
                BeforeEach(() -> {
                    blobResourceLoader = mock(BlobResourceLoader.class);
                });
                Context("#unassociate jakarta annotated entity", () -> {
                    BeforeEach(() -> {
                        id = "12345";

                        jakartaAnnotatedEntity = new JakartaTestEntity();
                        jakartaAnnotatedEntity.setContentId(id);
                        jakartaAnnotatedEntity.setContentLen(20L);
                    });
                    JustBeforeEach(() -> {
                        store.unassociate(jakartaAnnotatedEntity);
                    });
                    It("should NOT reset the @ContentId", () -> {
                        assertThat(jakartaAnnotatedEntity.getContentId(), CoreMatchers.is(id));
                    });
                });
            });
        });
	}

	public static class TestEntity {
		@ContentId
		private String contentId;
		@ContentLength
		private long contentLen;

		public TestEntity() {
			this.contentId = null;
		}

		public TestEntity(String contentId) {
			this.contentId = contentId;
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

	public static class JakartaTestEntity {
		@Id
		@ContentId
		private String contentId;
		@ContentLength
		private long contentLen;

		public JakartaTestEntity() {
			this.contentId = null;
		}

		public JakartaTestEntity(String contentId) {
			this.contentId = contentId;
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
