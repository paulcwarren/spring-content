//package internal.org.springframework.content.fs.store;
//
//import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.*;
//import static org.hamcrest.CoreMatchers.containsString;
//import static org.hamcrest.CoreMatchers.instanceOf;
//import static org.hamcrest.CoreMatchers.is;
//import static org.hamcrest.CoreMatchers.not;
//import static org.hamcrest.CoreMatchers.nullValue;
//import static org.hamcrest.MatcherAssert.assertThat;
//import static org.mockito.ArgumentMatchers.anyObject;
//import static org.mockito.ArgumentMatchers.eq;
//import static org.mockito.ArgumentMatchers.matches;
//import static org.mockito.Mockito.atLeastOnce;
//import static org.mockito.Mockito.mock;
//import static org.mockito.Mockito.never;
//import static org.mockito.Mockito.spy;
//import static org.mockito.Mockito.times;
//import static org.mockito.Mockito.verify;
//import static org.mockito.Mockito.when;
//import static org.mockito.hamcrest.MockitoHamcrest.argThat;
//
//import java.io.ByteArrayInputStream;
//import java.io.File;
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.OutputStream;
//
//import org.junit.runner.RunWith;
//import org.mockito.InOrder;
//import org.mockito.Matchers;
//import org.mockito.Mockito;
//import org.springframework.content.commons.annotations.ContentId;
//import org.springframework.content.commons.annotations.ContentLength;
//import org.springframework.content.commons.io.DeletableResource;
//import org.springframework.content.commons.repository.StoreAccessException;
//import org.springframework.content.commons.utils.FileService;
//import org.springframework.content.commons.utils.PlacementService;
//import org.springframework.content.commons.utils.PlacementServiceImpl;
//import org.springframework.content.fs.io.FileSystemResourceLoader;
//import org.springframework.core.io.FileSystemResource;
//import org.springframework.core.io.InputStreamResource;
//import org.springframework.core.io.Resource;
//import org.springframework.core.io.WritableResource;
//
//import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;
//
//@RunWith(Ginkgo4jRunner.class)
//public class DefaultFilesystemStoresImplTest {
//	private DefaultFilesystemStoreImpl<ContentProperty, String> filesystemContentRepoImpl;
//	private FileSystemResourceLoader loader;
//	private PlacementService placer;
//	private ContentProperty entity;
//
//	private Resource resource, inputResource;
//	private WritableResource writeableResource;
//	private DeletableResource deletableResource;
//	private DeletableResource nonExistentResource;
//	private FileService fileService;
//
//	private InputStream content;
//	private OutputStream output;
//
//	private File parent;
//	private File root;
//
//	private String id;
//
//	private InputStream result;
//	private Exception e;
//
//	{
//		Describe("DefaultFilesystemContentRepositoryImpl", () -> {
//
//			BeforeEach(() -> {
//				loader = mock(FileSystemResourceLoader.class);
//				placer = mock(PlacementService.class);
//				fileService = mock(FileService.class);
//
//				filesystemContentRepoImpl = spy(new DefaultFilesystemStoreImpl<ContentProperty, String>(
//						loader, null, placer, fileService));
//			});
//
//			Describe("Store", () -> {
//				Context("#getResource", () -> {
//					BeforeEach(() -> {
//						id = "12345-67890";
//
//						when(placer.convert(eq("12345-67890"), eq(String.class)))
//								.thenReturn("12345-67890");
//					});
//					JustBeforeEach(() -> {
//						resource = filesystemContentRepoImpl.getResource(id);
//					});
//					It("should use the placer service to get a resource path", () -> {
//						verify(placer).convert(eq("12345-67890"), eq(String.class));
//						verify(loader).getResource(eq("12345-67890"));
//					});
//				});
//			});
//			Describe("AssociativeStore", () -> {
//				Context("#getResource", () -> {
//					JustBeforeEach(() -> {
//						resource = filesystemContentRepoImpl.getResource(entity);
//					});
//					Context("when the entity is not already associated with a resource", () -> {
//						BeforeEach(() -> {
//							entity = new TestEntity();
//						});
//						It("should not return a resource",
//							() -> {
//								assertThat(resource, is(nullValue()));
//							});
//					});
//					Context("when the entity is already associated with a resource", () -> {
//						BeforeEach(() -> {
//							entity = new TestEntity();
//							entity.setContentId("12345-67890");
//
//							when(placer.convert(eq("12345-67890"),
//									eq(String.class))).thenReturn("/12345/67890");
//						});
//						It("should use the placer service to get a resource path", () -> {
//							verify(placer).convert(eq("12345-67890"),
//									eq(String.class));
//							verify(loader)
//									.getResource(eq("/12345/67890"));
//						});
//					});
//					Context("when there is an entity converter", () -> {
//						BeforeEach(() -> {
//							entity = new TestEntity();
//
//							deletableResource = mock(DeletableResource.class);
//
//							when(placer.canConvert(eq(entity.getClass()), eq(String.class))).thenReturn(true);
//							when(placer.convert(eq(entity), eq(String.class))).thenReturn("/abcd/efgh");
//							when(loader.getResource("/abcd/efgh")).thenReturn(deletableResource);
//						});
//						It("should not need to convert the id", () -> {
//							verify(placer, never()).convert(argThat(not(entity)), eq(String.class));
//						});
//						It("should return the resource", () -> {
//							assertThat(resource, is(deletableResource));
//						});
//					});
//					Context("when the entity has a String-arg constructor - Issue #57", () ->{
//						BeforeEach(() -> {
//							PlacementService placementService = new PlacementServiceImpl();
//							placer = spy(placementService);
//
//							entity = new TestEntity();
//						});
//						It("should not call the placement service trying to convert the entity to a string", () -> {
//							verify(placer, never()).convert(eq(entity), eq(String.class));
//						});
//					});
//				});
//				Context("#associate", () -> {
//					BeforeEach(() -> {
//						id = "12345-67890";
//
//						entity = new TestEntity();
//
//						when(placer.convert(eq("12345-67890"), eq(String.class)))
//								.thenReturn("/12345/67890");
//
//						deletableResource = mock(DeletableResource.class);
//						when(loader.getResource(eq("/12345/67890")))
//								.thenReturn(deletableResource);
//
//						when(deletableResource.contentLength()).thenReturn(20L);
//					});
//					JustBeforeEach(() -> {
//						filesystemContentRepoImpl.associate(entity, id);
//					});
//					It("should set the entity's content ID attribute", () -> {
//						assertThat(entity.getContentId(), is("12345-67890"));
//					});
//				});
//				Context("#unassociate", () -> {
//					BeforeEach(() -> {
//						entity = new TestEntity();
//						entity.setContentId("12345-67890");
//					});
//					JustBeforeEach(() -> {
//						filesystemContentRepoImpl.unassociate(entity);
//					});
//					It("should reset the entity's content ID attribute", () -> {
//						assertThat(entity.getContentId(), is(nullValue()));
//					});
//				});
//			});
//			Describe("ContentStore", () -> {
//				BeforeEach(() -> {
//					writeableResource = mock(WritableResource.class);
//				});
//
//				Context("#setContent", () -> {
//					BeforeEach(() -> {
//						entity = new TestEntity();
//						content = new ByteArrayInputStream(
//								"Hello content world!".getBytes());
//					});
//
//					JustBeforeEach(() -> {
//						try {
//							filesystemContentRepoImpl.setContent(entity, content);
//						} catch (Exception e) {
//							this.e = e;
//						}
//					});
//
//					Context("given an entity converter", () -> {
//						Context("when the content doesn't yet exist", () -> {
//							BeforeEach(() -> {
//								when(placer.convert(matches(
//										"[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}"),
//										eq(String.class)))
//										.thenReturn("12345-67890");
//
//								when(loader.getResource(eq("12345-67890")))
//										.thenReturn(writeableResource);
//								output = mock(OutputStream.class);
//								when(writeableResource.getOutputStream()).thenReturn(output);
//
//								File resourceFile = mock(File.class);
//								parent = mock(File.class);
//								when(writeableResource.getFile()).thenReturn(resourceFile);
//								when(resourceFile.getParentFile()).thenReturn(parent);
//							});
//							It("creates a directory for the parent", () -> {
//								verify(fileService).mkdirs(eq(parent));
//							});
//							It("should make a new UUID", () -> {
//								assertThat(entity.getContentId(), is(not(nullValue())));
//							});
//							FIt("should create a new resource", () -> {
//								verify(loader).getResource(eq("12345-67890"));
//							});
//							It("should write to the resource's outputstream", () -> {
//								verify(writeableResource).getOutputStream();
//								verify(output, times(1)).write(Matchers.<byte[]>any(), eq(0),
//										eq(20));
//							});
//						});
//						Context("when the content already exists", () -> {
//							BeforeEach(() -> {
//								when(placer.canConvert(eq(entity.getClass()), eq(String.class))).thenReturn(true);
//								when(placer.convert(eq(entity), eq(String.class))).thenReturn("/abcd/efgh");
//								when(loader.getResource(eq("/abcd/efgh"))).thenReturn(writeableResource);
//
//								when(writeableResource.exists()).thenReturn(true);
//
//								output = mock(OutputStream.class);
//								when(writeableResource.getOutputStream()).thenReturn(output);
//
//								when(writeableResource.contentLength()).thenReturn(20L);
//							});
//
//							It("should write to the resource's outputstream", () -> {
//								verify(output, times(1)).write(Matchers.<byte[]>any(), eq(0),
//										eq(20));
//								verify(output).close();
//							});
//
//							It("should change the content length", () -> {
//								assertThat(entity.getContentLen(), is(20L));
//							});
//						});
//					});
//
//					Context("given just the default ID converters", () -> {
//						BeforeEach(() -> {
//							when(placer.convert(eq(entity), eq(String.class))).thenReturn(null);
//
//                            when(placer.convert(eq("12345-67890"),
//                                    eq(String.class)))
//                                    .thenReturn("12345-67890");
//
//							when(placer.convert(matches(
//									"[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}"),
//									eq(String.class)))
//									.thenReturn("12345-67890");
//
//							when(loader.getResource(eq("12345-67890")))
//									.thenReturn(writeableResource);
//							output = mock(OutputStream.class);
//							when(writeableResource.getOutputStream()).thenReturn(output);
//
//							when(writeableResource.contentLength()).thenReturn(20L);
//						});
//
//						Context("when the content already exists", () -> {
//							BeforeEach(() -> {
//								entity.setContentId("12345-67890");
//								when(writeableResource.exists()).thenReturn(true);
//							});
//
//							It("should use the placer service to get a resource path",
//									() -> {
//										verify(placer, atLeastOnce()).convert(anyObject(), anyObject());
//										verify(loader).getResource(eq("12345-67890"));
//									});
//
//							It("should change the content length", () -> {
//								assertThat(entity.getContentLen(), is(20L));
//							});
//
//							It("should write to the resource's outputstream", () -> {
//								verify(writeableResource).getOutputStream();
//								verify(output, times(1)).write(Matchers.<byte[]>any(), eq(0),
//										eq(20));
//							});
//						});
//
//						Context("when the content does not already exist", () -> {
//							BeforeEach(() -> {
//								assertThat(entity.getContentId(), is(nullValue()));
//
//								File resourceFile = mock(File.class);
//								parent = mock(File.class);
//
//								when(writeableResource.getFile()).thenReturn(resourceFile);
//								when(resourceFile.getParentFile()).thenReturn(parent);
//							});
//
//							It("creates a directory for the parent", () -> {
//								verify(fileService).mkdirs(eq(parent));
//							});
//
//							It("should make a new UUID", () -> {
//								assertThat(entity.getContentId(), is(not(nullValue())));
//							});
//							It("should create a new resource", () -> {
//								verify(loader).getResource(eq("12345-67890"));
//							});
//							It("should write to the resource's outputstream", () -> {
//								verify(writeableResource).getOutputStream();
//								verify(output, times(1)).write(Matchers.<byte[]>any(), eq(0),
//										eq(20));
//							});
//						});
//
//						Context("when getting the resource output stream throws an IOException", () -> {
//							BeforeEach(() -> {
//								File resourceFile = mock(File.class);
//								parent = mock(File.class);
//
//								when(writeableResource.getFile()).thenReturn(resourceFile);
//								when(resourceFile.getParentFile()).thenReturn(parent);
//
//								when(writeableResource.getOutputStream()).thenThrow(new IOException());
//							});
//							It("should return a StoreAccessException wrapping the IOException", () -> {
//								assertThat(e, is(instanceOf(StoreAccessException.class)));
//								assertThat(e.getCause(), is(instanceOf(IOException.class)));
//							});
//						});
//					});
//				});
//
//				Context("#setContent from Resource", () -> {
//
//					BeforeEach(() -> {
//						entity = new TestEntity();
//						content = new ByteArrayInputStream("Hello content world!".getBytes());
//						inputResource = new InputStreamResource(content);
//					});
//
//					JustBeforeEach(() -> {
//						try {
//							filesystemContentRepoImpl.setContent(entity, inputResource);
//						} catch (Exception e) {
//							this.e = e;
//						}
//					});
//
//					It("should delegate to setContent from InputStream", () -> {
//						verify(filesystemContentRepoImpl).setContent(eq(entity), eq(content));
//					});
//
//					Context("when the resource throws an IOException", () -> {
//						BeforeEach(() -> {
//							inputResource = mock(Resource.class);
//							when(inputResource.getInputStream()).thenThrow(new IOException("setContent badness"));
//						});
//						It("should throw a StoreAccessException", () -> {
//							assertThat(e, is(instanceOf(StoreAccessException.class)));
//							assertThat(e.getCause().getMessage(), containsString("setContent badness"));
//						});
//					});
//				});
//
//				Context("#getContent", () -> {
//					BeforeEach(() -> {
//						entity = new TestEntity();
//						content = mock(InputStream.class);
//						entity.setContentId("abcd-efgh");
//
//						when(placer.convert(eq(entity), eq(String.class)))
//								.thenReturn(null);
//
//						when(placer.convert(eq("abcd-efgh"), eq(String.class)))
//								.thenReturn("abcd-efgh");
//
//						when(loader.getResource(eq("abcd-efgh")))
//								.thenReturn(writeableResource);
//						when(writeableResource.getInputStream()).thenReturn(content);
//					});
//
//					JustBeforeEach(() -> {
//						try {
//							result = filesystemContentRepoImpl.getContent(entity);
//						} catch (Exception e) {
//							this.e = e;
//						}
//					});
//
//					Context("given an entity converter", () -> {
//						BeforeEach(() -> {
//							when(placer.canConvert(eq(entity.getClass()), eq(String.class))).thenReturn(true);
//							when(placer.convert(eq(entity), eq(String.class)))
//									.thenReturn("/abcd/efgh");
//						});
//						Context("when the resource does not exists", () -> {
//							BeforeEach(() -> {
//								nonExistentResource = mock(DeletableResource.class);
//
//								when(loader.getResource(eq("/abcd/efgh")))
//										.thenReturn(nonExistentResource);
//
//								when(writeableResource.exists()).thenReturn(false);
//							});
//
//							It("should not return the content", () -> {
//								assertThat(result, is(nullValue()));
//							});
//						});
//
//						Context("when the resource exists", () -> {
//							BeforeEach(() -> {
//								when(loader.getResource(eq("/abcd/efgh")))
//										.thenReturn(writeableResource);
//
//								when(writeableResource.exists()).thenReturn(true);
//
//								when(writeableResource.getInputStream()).thenReturn(content);
//							});
//							It("should get content", () -> {
//								assertThat(result, is(content));
//							});
//						});
//					});
//
//					Context("given just the default ID converter", () -> {
//						BeforeEach(() -> {
//							when(placer.convert(eq(entity), eq(String.class))).thenReturn(null);
//						});
//						Context("when the resource does not exists", () -> {
//							BeforeEach(() -> {
//								nonExistentResource = mock(DeletableResource.class);
//								when(writeableResource.exists()).thenReturn(true);
//
//								when(loader.getResource(eq("/abcd/efgh")))
//										.thenReturn(nonExistentResource);
//								when(loader.getResource(eq("abcd-efgh")))
//										.thenReturn(nonExistentResource);
//							});
//
//							It("should not find the content", () -> {
//								assertThat(result, is(nullValue()));
//							});
//						});
//
//						Context("when the resource exists", () -> {
//							BeforeEach(() -> {
//								when(writeableResource.exists()).thenReturn(true);
//							});
//
//							It("should get content", () -> {
//								assertThat(result, is(content));
//							});
//
//							Context("when getting the resource inputstream throws an IOException", () -> {
//								BeforeEach(() -> {
//									when(writeableResource.getInputStream()).thenThrow(new IOException("test-ioexception"));
//								});
//								It("should return a StoreAccessException wrapping the IOException", () -> {
//									assertThat(result, is(nullValue()));
//									assertThat(e, is(instanceOf(StoreAccessException.class)));
//									assertThat(e.getCause().getMessage(), is("test-ioexception"));
//								});
//							});
//						});
//
//						Context("when the resource exists but in the old location", () -> {
//							BeforeEach(() -> {
//								nonExistentResource = mock(DeletableResource.class);
//								when(loader.getResource(eq("/abcd/efgh")))
//										.thenReturn(nonExistentResource);
//								when(nonExistentResource.exists()).thenReturn(false);
//
//								when(loader.getResource(eq("abcd-efgh")))
//										.thenReturn(writeableResource);
//								when(writeableResource.exists()).thenReturn(true);
//							});
//							It("should check the new location and then the old", () -> {
//								InOrder inOrder = Mockito.inOrder(loader);
//
//								inOrder.verify(loader).getResource(eq("abcd-efgh"));
//								inOrder.verifyNoMoreInteractions();
//							});
//							It("should get content", () -> {
//								assertThat(result, is(content));
//							});
//						});
//					});
//				});
//
//				Context("#unsetContent", () -> {
//					BeforeEach(() -> {
//						entity = new TestEntity();
//						entity.setContentId("abcd-efgh");
//						entity.setContentLen(100L);
//						deletableResource = mock(DeletableResource.class);
//					});
//
//					JustBeforeEach(() -> {
//						filesystemContentRepoImpl.unsetContent(entity);
//					});
//
//					Context("given an entity converter", () -> {
//						BeforeEach(() -> {
//							when(placer.canConvert(eq(entity.getClass()), eq(String.class))).thenReturn(true);
//							when(placer.convert(eq(entity), eq(String.class)))
//									.thenReturn("/abcd/efgh");
//						});
//						Context("given the resource does not exist", () -> {
//							BeforeEach(() -> {
//								nonExistentResource = mock(DeletableResource.class);
//
//								when(loader.getResource(eq("/abcd/efgh")))
//										.thenReturn(nonExistentResource);
//
//								when(nonExistentResource.exists()).thenReturn(false);
//							});
//							It("should not delete the resource", () -> {
//								verify(nonExistentResource, never()).delete();
//							});
//						});
//						Context("given the resource exists", () -> {
//							BeforeEach(() -> {
//								deletableResource = mock(DeletableResource.class);
//
//								when(loader.getResource(eq("/abcd/efgh")))
//										.thenReturn(deletableResource);
//
//								File resourceFile = mock(File.class);
//								parent = mock(File.class);
//								when(deletableResource.getFile()).thenReturn(resourceFile);
//								when(resourceFile.getParentFile()).thenReturn(parent);
//								when(deletableResource.exists()).thenReturn(true);
//
//								FileSystemResource rootResource = mock(FileSystemResource.class);
//								when(loader.getRootResource()).thenReturn(rootResource);
//								root = mock(File.class);
//								when(rootResource.getFile()).thenReturn(root);
//							});
//							It("should delete the resource", () -> {
//								verify(deletableResource, times(1)).delete();
//							});
//						});
//					});
//
//					Context("given just the default ID converter", () -> {
//						BeforeEach(() -> {
//							when(placer.convert(eq(entity), eq(String.class))).thenReturn(null);
//						});
//						Context("when the content exists in the new location", () -> {
//							BeforeEach(() -> {
//								when(placer.convert(eq("abcd-efgh"), eq(String.class)))
//										.thenReturn("abcd-efgh");
//
//								when(loader.getResource(eq("abcd-efgh")))
//										.thenReturn(deletableResource);
//
//								File resourceFile = mock(File.class);
//								parent = mock(File.class);
//								when(deletableResource.getFile()).thenReturn(resourceFile);
//								when(resourceFile.getParentFile()).thenReturn(parent);
//								when(deletableResource.exists()).thenReturn(true);
//
//								FileSystemResource rootResource = mock(FileSystemResource.class);
//								when(loader.getRootResource()).thenReturn(rootResource);
//								root = mock(File.class);
//								when(rootResource.getFile()).thenReturn(root);
//							});
//
//							It("should delete the resource", () -> {
//								verify(deletableResource, times(1)).delete();
//							});
//
//							Context("when the property has a dedicated ContentId field", () -> {
//								It("should reset the metadata", () -> {
//									assertThat(entity.getContentId(), is(nullValue()));
//									assertThat(entity.getContentLen(), is(0L));
//								});
//							});
//							Context("when the property's ContentId field also is the javax persistence Id field", () -> {
//								BeforeEach(() -> {
//									entity = new SharedIdContentIdEntity();
//									entity.setContentId("abcd-efgh");
//								});
//								It("should not reset the content id metadata", () -> {
//									assertThat(entity.getContentId(), is("abcd-efgh"));
//									assertThat(entity.getContentLen(), is(0L));
//								});
//							});
//							Context("when the property's ContentId field also is the Spring Id field", () -> {
//								BeforeEach(() -> {
//									entity = new SharedSpringIdContentIdEntity();
//									entity.setContentId("abcd-efgh");
//								});
//								It("should not reset the content id metadata", () -> {
//									assertThat(entity.getContentId(), is("abcd-efgh"));
//									assertThat(entity.getContentLen(), is(0L));
//								});
//							});
//						});
//
//						Context("when the content doesnt exist", () -> {
//							BeforeEach(() -> {
//								when(placer.convert(eq("abcd-efgh"), eq(String.class)))
//										.thenReturn("abcd-efgh");
//
//								nonExistentResource = mock(DeletableResource.class);
//								when(loader.getResource(eq("abcd-efgh")))
//										.thenReturn(nonExistentResource);
//								when(nonExistentResource.exists()).thenReturn(false);
//							});
//							It("should unset the content", () -> {
//								verify(nonExistentResource, never()).delete();
//								assertThat(entity.getContentId(), is(nullValue()));
//								assertThat(entity.getContentLen(), is(0L));
//							});
//						});
//					});
//				});
//			});
//		});
//	}
//
//	public interface ContentProperty {
//		String getContentId();
//
//		void setContentId(String contentId);
//
//		long getContentLen();
//
//		void setContentLen(long contentLen);
//	}
//
//	public static class TestEntity implements ContentProperty {
//		@ContentId
//		private String contentId;
//
//		@ContentLength
//		private long contentLen;
//
//		public TestEntity() {
//			this.contentId = null;
//		}
//
//		public TestEntity(String contentId) {
//			this.contentId = new String(contentId);
//		}
//
//		@Override
//        public String getContentId() {
//			return this.contentId;
//		}
//
//		@Override
//        public void setContentId(String contentId) {
//			this.contentId = contentId;
//		}
//
//		@Override
//        public long getContentLen() {
//			return contentLen;
//		}
//
//		@Override
//        public void setContentLen(long contentLen) {
//			this.contentLen = contentLen;
//		}
//	}
//
//	public static class SharedIdContentIdEntity implements ContentProperty {
//
//		@jakarta.persistence.Id
//		@ContentId
//		private String contentId;
//
//		@ContentLength
//		private long contentLen;
//
//		public SharedIdContentIdEntity() {
//			this.contentId = null;
//		}
//
//		@Override
//        public String getContentId() {
//			return this.contentId;
//		}
//
//		@Override
//        public void setContentId(String contentId) {
//			this.contentId = contentId;
//		}
//
//		@Override
//        public long getContentLen() {
//			return contentLen;
//		}
//
//		@Override
//        public void setContentLen(long contentLen) {
//			this.contentLen = contentLen;
//		}
//	}
//
//	public static class SharedSpringIdContentIdEntity implements ContentProperty {
//
//		@org.springframework.data.annotation.Id
//		@ContentId
//		private String contentId;
//
//		@ContentLength
//		private long contentLen;
//
//		public SharedSpringIdContentIdEntity() {
//			this.contentId = null;
//		}
//
//		@Override
//        public String getContentId() {
//			return this.contentId;
//		}
//
//		@Override
//        public void setContentId(String contentId) {
//			this.contentId = contentId;
//		}
//
//		@Override
//        public long getContentLen() {
//			return contentLen;
//		}
//
//		@Override
//        public void setContentLen(long contentLen) {
//			this.contentLen = contentLen;
//		}
//	}
//}
