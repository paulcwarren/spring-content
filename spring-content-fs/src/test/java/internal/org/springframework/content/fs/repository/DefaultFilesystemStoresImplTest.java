package internal.org.springframework.content.fs.repository;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.io.DeletableResource;
import org.springframework.content.commons.repository.StoreAccessException;
import org.springframework.content.commons.utils.FileService;
import org.springframework.content.fs.io.FileSystemResourceLoader;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.JustBeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

@RunWith(Ginkgo4jRunner.class)
public class DefaultFilesystemStoresImplTest {
	private DefaultFilesystemStoreImpl<ContentProperty, String> filesystemContentRepoImpl;
	private FileSystemResourceLoader loader;
	private ConversionService conversion;
	private ContentProperty entity;

	private Resource resource;
	private WritableResource writeableResource;
	private DeletableResource deletableResource;
	private DeletableResource nonExistentResource;
	private FileService fileService;

	private InputStream content;
	private OutputStream output;

	private File parent;
	private File root;

	private String id;

	private InputStream result;
	private Exception e;

	{
		Describe("DefaultFilesystemContentRepositoryImpl", () -> {

			BeforeEach(() -> {
				loader = mock(FileSystemResourceLoader.class);
				conversion = mock(ConversionService.class);
				fileService = mock(FileService.class);

				filesystemContentRepoImpl = new DefaultFilesystemStoreImpl<ContentProperty, String>(
						loader, conversion, fileService);
			});

			Describe("Store", () -> {
				Context("#getResource", () -> {
					BeforeEach(() -> {
						id = "12345-67890";

						when(conversion.convert(eq("12345-67890"), eq(String.class)))
								.thenReturn("12345-67890");
					});
					JustBeforeEach(() -> {
						resource = filesystemContentRepoImpl.getResource(id);
					});
					It("should use the conversion service to get a resource path", () -> {
						verify(conversion).convert(eq("12345-67890"), eq(String.class));
						verify(loader).getResource(eq("12345-67890"));
					});
				});
			});
			Describe("AssociativeStore", () -> {
				Context("#getResource", () -> {
					JustBeforeEach(() -> {
						resource = filesystemContentRepoImpl.getResource(entity);
					});
					Context("when the entity is not already associated with a resource", () -> {
						BeforeEach(() -> {
							entity = new TestEntity();
						});
						It("should not return a resource",
							() -> {
								assertThat(resource, is(nullValue()));
							});
					});
					Context("when the entity is already associated with a resource", () -> {
						BeforeEach(() -> {
							entity = new TestEntity();
							entity.setContentId("12345-67890");

							when(conversion.convert(eq("12345-67890"),
									eq(String.class))).thenReturn("/12345/67890");
						});
						It("should use the conversion service to get a resource path", () -> {
							verify(conversion).convert(eq("12345-67890"),
									eq(String.class));
							verify(loader)
									.getResource(eq("/12345/67890"));
						});
					});
					Context("when there is an entity converter", () -> {
						BeforeEach(() -> {
							entity = new TestEntity();

							deletableResource = mock(DeletableResource.class);

							when(conversion.canConvert(eq(entity.getClass()), eq(String.class))).thenReturn(true);
							when(conversion.convert(eq(entity), eq(String.class))).thenReturn("/abcd/efgh");
							when(loader.getResource("/abcd/efgh")).thenReturn(deletableResource);
						});
						It("should not need to convert the id", () -> {
							verify(conversion, never()).convert(argThat(not(entity)), eq(String.class));
						});
						It("should return the resource", () -> {
							assertThat(resource, is(deletableResource));
						});
					});
				});
				Context("#associate", () -> {
					BeforeEach(() -> {
						id = "12345-67890";

						entity = new TestEntity();

						when(conversion.convert(eq("12345-67890"), eq(String.class)))
								.thenReturn("/12345/67890");

						deletableResource = mock(DeletableResource.class);
						when(loader.getResource(eq("/12345/67890")))
								.thenReturn(deletableResource);

						when(deletableResource.contentLength()).thenReturn(20L);
					});
					JustBeforeEach(() -> {
						filesystemContentRepoImpl.associate(entity, id);
					});
					It("should set the entity's content ID attribute", () -> {
						assertThat(entity.getContentId(), is("12345-67890"));
					});
				});
				Context("#unassociate", () -> {
					BeforeEach(() -> {
						entity = new TestEntity();
						entity.setContentId("12345-67890");
					});
					JustBeforeEach(() -> {
						filesystemContentRepoImpl.unassociate(entity);
					});
					It("should reset the entity's content ID attribute", () -> {
						assertThat(entity.getContentId(), is(nullValue()));
					});
				});
			});
			Describe("ContentStore", () -> {
				BeforeEach(() -> {
					writeableResource = mock(WritableResource.class);
				});

				Context("#setContent", () -> {
					BeforeEach(() -> {
						entity = new TestEntity();
						content = new ByteArrayInputStream(
								"Hello content world!".getBytes());
					});

					JustBeforeEach(() -> {
						try {
							filesystemContentRepoImpl.setContent(entity, content);
						} catch (Exception e) {
							this.e = e;
						}
					});

					Context("given an entity converter", () -> {
						Context("when the content doesn't yet exist", () -> {
							BeforeEach(() -> {
								when(conversion.convert(matches(
										"[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}"),
										eq(String.class)))
										.thenReturn("12345-67890");

								when(loader.getResource(eq("12345-67890")))
										.thenReturn(writeableResource);
								output = mock(OutputStream.class);
								when(writeableResource.getOutputStream()).thenReturn(output);

								File resourceFile = mock(File.class);
								parent = mock(File.class);
								when(writeableResource.getFile()).thenReturn(resourceFile);
								when(resourceFile.getParentFile()).thenReturn(parent);
							});
							It("creates a directory for the parent", () -> {
								verify(fileService).mkdirs(eq(parent));
							});
							It("should make a new UUID", () -> {
								assertThat(entity.getContentId(), is(not(nullValue())));
							});
							It("should create a new resource", () -> {
								verify(loader).getResource(eq("12345-67890"));
							});
							It("should write to the resource's outputstream", () -> {
								verify(writeableResource).getOutputStream();
								verify(output, times(1)).write(Matchers.<byte[]>any(), eq(0),
										eq(20));
							});
						});
						Context("when the content already exists", () -> {
							BeforeEach(() -> {
								when(conversion.canConvert(eq(entity.getClass()), eq(String.class))).thenReturn(true);
								when(conversion.convert(eq(entity), eq(String.class))).thenReturn("/abcd/efgh");
								when(loader.getResource(eq("/abcd/efgh"))).thenReturn(writeableResource);

								when(writeableResource.exists()).thenReturn(true);

								output = mock(OutputStream.class);
								when(writeableResource.getOutputStream()).thenReturn(output);

								when(writeableResource.contentLength()).thenReturn(20L);
							});

							It("should write to the resource's outputstream", () -> {
								verify(output, times(1)).write(Matchers.<byte[]>any(), eq(0),
										eq(20));
								verify(output).close();
							});

							It("should change the content length", () -> {
								assertThat(entity.getContentLen(), is(20L));
							});
						});
					});

					Context("given just the default ID converters", () -> {
						BeforeEach(() -> {
							when(conversion.convert(eq(entity), eq(String.class)))
									.thenReturn(null);

							when(conversion.convert(matches(
									"[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}"),
									eq(String.class)))
									.thenReturn("12345-67890");

							when(loader.getResource(eq("12345-67890")))
									.thenReturn(writeableResource);
							output = mock(OutputStream.class);
							when(writeableResource.getOutputStream()).thenReturn(output);

							when(writeableResource.contentLength()).thenReturn(20L);
						});

						Context("when the content already exists", () -> {
							BeforeEach(() -> {
								entity.setContentId("12345-67890");
								when(writeableResource.exists()).thenReturn(true);
							});

							It("should use the conversion service to get a resource path",
									() -> {
										verify(conversion, atLeastOnce()).convert(anyObject(), anyObject());
										verify(loader).getResource(eq("12345-67890"));
									});

							It("should change the content length", () -> {
								assertThat(entity.getContentLen(), is(20L));
							});

							It("should write to the resource's outputstream", () -> {
								verify(writeableResource).getOutputStream();
								verify(output, times(1)).write(Matchers.<byte[]>any(), eq(0),
										eq(20));
							});
						});

						Context("when the content does not already exist", () -> {
							BeforeEach(() -> {
								assertThat(entity.getContentId(), is(nullValue()));

								File resourceFile = mock(File.class);
								parent = mock(File.class);

								when(writeableResource.getFile()).thenReturn(resourceFile);
								when(resourceFile.getParentFile()).thenReturn(parent);
							});

							It("creates a directory for the parent", () -> {
								verify(fileService).mkdirs(eq(parent));
							});

							It("should make a new UUID", () -> {
								assertThat(entity.getContentId(), is(not(nullValue())));
							});
							It("should create a new resource", () -> {
								verify(loader).getResource(eq("12345-67890"));
							});
							It("should write to the resource's outputstream", () -> {
								verify(writeableResource).getOutputStream();
								verify(output, times(1)).write(Matchers.<byte[]>any(), eq(0),
										eq(20));
							});
						});

						Context("when getting the resource output stream throws an IOException", () -> {
							BeforeEach(() -> {
								File resourceFile = mock(File.class);
								parent = mock(File.class);

								when(writeableResource.getFile()).thenReturn(resourceFile);
								when(resourceFile.getParentFile()).thenReturn(parent);

								when(writeableResource.getOutputStream()).thenThrow(new IOException());
							});
							It("should return a StoreAccessException wrapping the IOException", () -> {
								assertThat(e, is(instanceOf(StoreAccessException.class)));
								assertThat(e.getCause(), is(instanceOf(IOException.class)));
							});
						});
					});
				});

				Context("#getContent", () -> {
					BeforeEach(() -> {
						entity = new TestEntity();
						content = mock(InputStream.class);
						entity.setContentId("abcd-efgh");

						when(conversion.convert(eq(entity), eq(String.class)))
								.thenReturn(null);

						when(conversion.convert(eq("abcd-efgh"), eq(String.class)))
								.thenReturn("abcd-efgh");

						when(loader.getResource(eq("abcd-efgh")))
								.thenReturn(writeableResource);
						when(writeableResource.getInputStream()).thenReturn(content);
					});

					JustBeforeEach(() -> {
						try {
							result = filesystemContentRepoImpl.getContent(entity);
						} catch (Exception e) {
							this.e = e;
						}
					});

					Context("given an entity converter", () -> {
						BeforeEach(() -> {
							when(conversion.canConvert(eq(entity.getClass()), eq(String.class))).thenReturn(true);
							when(conversion.convert(eq(entity), eq(String.class)))
									.thenReturn("/abcd/efgh");
						});
						Context("when the resource does not exists", () -> {
							BeforeEach(() -> {
								nonExistentResource = mock(DeletableResource.class);

								when(loader.getResource(eq("/abcd/efgh")))
										.thenReturn(nonExistentResource);

								when(writeableResource.exists()).thenReturn(false);
							});

							It("should not return the content", () -> {
								assertThat(result, is(nullValue()));
							});
						});

						Context("when the resource exists", () -> {
							BeforeEach(() -> {
								when(loader.getResource(eq("/abcd/efgh")))
										.thenReturn(writeableResource);

								when(writeableResource.exists()).thenReturn(true);

								when(writeableResource.getInputStream()).thenReturn(content);
							});
							It("should get content", () -> {
								assertThat(result, is(content));
							});
						});
					});

					Context("given just the default ID converter", () -> {
						BeforeEach(() -> {
							when(conversion.convert(eq(entity), eq(String.class))).thenReturn(null);
						});
						Context("when the resource does not exists", () -> {
							BeforeEach(() -> {
								nonExistentResource = mock(DeletableResource.class);
								when(writeableResource.exists()).thenReturn(true);

								when(loader.getResource(eq("/abcd/efgh")))
										.thenReturn(nonExistentResource);
								when(loader.getResource(eq("abcd-efgh")))
										.thenReturn(nonExistentResource);
							});

							It("should not find the content", () -> {
								assertThat(result, is(nullValue()));
							});
						});

						Context("when the resource exists", () -> {
							BeforeEach(() -> {
								when(writeableResource.exists()).thenReturn(true);
							});

							It("should get content", () -> {
								assertThat(result, is(content));
							});

							Context("when getting the resource inputstream throws an IOException", () -> {
								BeforeEach(() -> {
									when(writeableResource.getInputStream()).thenThrow(new IOException("test-ioexception"));
								});
								It("should return a StoreAccessException wrapping the IOException", () -> {
									assertThat(result, is(nullValue()));
									assertThat(e, is(instanceOf(StoreAccessException.class)));
									assertThat(e.getCause().getMessage(), is("test-ioexception"));
								});
							});
						});

						Context("when the resource exists but in the old location", () -> {
							BeforeEach(() -> {
								nonExistentResource = mock(DeletableResource.class);
								when(loader.getResource(eq("/abcd/efgh")))
										.thenReturn(nonExistentResource);
								when(nonExistentResource.exists()).thenReturn(false);

								when(loader.getResource(eq("abcd-efgh")))
										.thenReturn(writeableResource);
								when(writeableResource.exists()).thenReturn(true);
							});
							It("should check the new location and then the old", () -> {
								InOrder inOrder = Mockito.inOrder(loader);

								inOrder.verify(loader).getResource(eq("abcd-efgh"));
								inOrder.verifyNoMoreInteractions();
							});
							It("should get content", () -> {
								assertThat(result, is(content));
							});
						});
					});
				});

				Context("#unsetContent", () -> {
					BeforeEach(() -> {
						entity = new TestEntity();
						entity.setContentId("abcd-efgh");
						entity.setContentLen(100L);
						deletableResource = mock(DeletableResource.class);
					});

					JustBeforeEach(() -> {
						filesystemContentRepoImpl.unsetContent(entity);
					});

					Context("given an entity converter", () -> {
						BeforeEach(() -> {
							when(conversion.canConvert(eq(entity.getClass()), eq(String.class))).thenReturn(true);
							when(conversion.convert(eq(entity), eq(String.class)))
									.thenReturn("/abcd/efgh");
						});
						Context("given the resource does not exist", () -> {
							BeforeEach(() -> {
								nonExistentResource = mock(DeletableResource.class);

								when(loader.getResource(eq("/abcd/efgh")))
										.thenReturn(nonExistentResource);

								when(nonExistentResource.exists()).thenReturn(false);
							});
							It("should not delete the resource", () -> {
								verify(nonExistentResource, never()).delete();
							});
						});
						Context("given the resource exists", () -> {
							BeforeEach(() -> {
								deletableResource = mock(DeletableResource.class);

								when(loader.getResource(eq("/abcd/efgh")))
										.thenReturn(deletableResource);

								File resourceFile = mock(File.class);
								parent = mock(File.class);
								when(deletableResource.getFile()).thenReturn(resourceFile);
								when(resourceFile.getParentFile()).thenReturn(parent);
								when(deletableResource.exists()).thenReturn(true);

								FileSystemResource rootResource = mock(FileSystemResource.class);
								when(loader.getRootResource()).thenReturn(rootResource);
								root = mock(File.class);
								when(rootResource.getFile()).thenReturn(root);
							});
							It("should delete the resource", () -> {
								verify(deletableResource, times(1)).delete();
							});
							It("should remove orphaned directories", () -> {
								verify(fileService, times(1)).rmdirs(parent, root);
							});
						});
					});

					Context("given just the default ID converter", () -> {
						BeforeEach(() -> {
							when(conversion.convert(eq(entity), eq(String.class))).thenReturn(null);
						});
						Context("when the content exists in the new location", () -> {
							BeforeEach(() -> {
								when(conversion.convert(eq("abcd-efgh"), eq(String.class)))
										.thenReturn("abcd-efgh");

								when(loader.getResource(eq("abcd-efgh")))
										.thenReturn(deletableResource);

								File resourceFile = mock(File.class);
								parent = mock(File.class);
								when(deletableResource.getFile()).thenReturn(resourceFile);
								when(resourceFile.getParentFile()).thenReturn(parent);
								when(deletableResource.exists()).thenReturn(true);

								FileSystemResource rootResource = mock(FileSystemResource.class);
								when(loader.getRootResource()).thenReturn(rootResource);
								root = mock(File.class);
								when(rootResource.getFile()).thenReturn(root);
							});

							It("should delete the resource", () -> {
								verify(deletableResource, times(1)).delete();
							});

							It("should remove orphaned directories", () -> {
								verify(fileService, times(1)).rmdirs(parent, root);
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
								when(conversion.convert(eq("abcd-efgh"), eq(String.class)))
										.thenReturn("abcd-efgh");

								nonExistentResource = mock(DeletableResource.class);
								when(loader.getResource(eq("abcd-efgh")))
										.thenReturn(nonExistentResource);
								when(nonExistentResource.exists()).thenReturn(false);
							});
							It("should unset the content", () -> {
								verify(nonExistentResource, never()).delete();
								assertThat(entity.getContentId(), is(nullValue()));
								assertThat(entity.getContentLen(), is(0L));
							});
						});
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
