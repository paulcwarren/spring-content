package internal.org.springframework.content.gcs.store;
/*package internal.org.springframework.content.gcs.store;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3ObjectId;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;
import internal.org.springframework.content.s3.config.DefaultAssociativeStoreS3ObjectIdResolver;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.repository.StoreAccessException;
import org.springframework.content.s3.Bucket;
import org.springframework.content.s3.S3ObjectIdResolver;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.WritableResource;
import org.springframework.util.Assert;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;

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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.endsWith;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

@RunWith(Ginkgo4jRunner.class)
// @Ginkgo4jConfiguration(threads=1)
public class DefaultGCSStoreImplTest {

	private DefaultGCSStoreImpl<ContentProperty, String> s3StoreImpl;
	private DefaultGCSStoreImpl<ContentProperty, S3ObjectId> s3ObjectIdBasedStore;
	private DefaultGCSStoreImpl<ContentProperty, CustomContentId> customS3ContentIdBasedStore;

	private ResourceLoader loader;
	private ConversionService converter;
	private AmazonS3 client;
	private S3ObjectIdResolver resolver;
	private String defaultBucket;

	private CustomContentId customId;
	private ContentProperty entity;

	private String id;
	private WritableResource resource;
	private Resource r, nonExistentResource;
	private InputStream content;
	private OutputStream output;
	private File parent;
	private InputStream result;
	private Exception e;

	{
		Describe("DefaultS3StoreImpl", () -> {
			BeforeEach(() -> {
				resource = mock(WritableResource.class);
				loader = mock(ResourceLoader.class);
				converter = mock(ConversionService.class);
				client = mock(AmazonS3.class);
				defaultBucket = null;
			});
			Describe("Store", () -> {
				Context("#getResourceInternal", () -> {
					Context("given the store's ID is an S3ObjectId type", () -> {
						BeforeEach(() -> {
							s3ObjectIdBasedStore = new DefaultS3StoreImpl<ContentProperty, S3ObjectId>(
									loader, converter, client,
									GCSObjectIdResolver.createS3ObjectIdResolver(
											S3ObjectId::getBucket, S3ObjectId::getKey,
											id -> {
												Assert.notNull(id.getBucket(),
														"Bucket must not be null");
												Assert.notNull(id.getKey(),
														"Key must not be null");
											}),
									"default-defaultBucket");
							when(converter.convert(eq("some-object-id"),
									eq(String.class))).thenReturn("/some/object/id");
							when(loader.getResource(anyString())).thenReturn(resource);
						});
						JustBeforeEach(() -> {
							s3ObjectIdBasedStore.getResource(new S3ObjectId(
									"some-defaultBucket", "some-object-id"));
						});
						It("should fetch the resource", () -> {
							verify(loader).getResource(
									eq("s3://some-defaultBucket/some/object/id"));
						});
					});
					Context("given the store's ID is a custom ID type", () -> {
						JustBeforeEach(() -> {
							customS3ContentIdBasedStore = new DefaultS3StoreImpl<ContentProperty, CustomContentId>(
									loader, converter, client, resolver, defaultBucket);

							try {
								customS3ContentIdBasedStore.getResource(customId);
							}
							catch (Exception e) {
								this.e = e;
							}
						});
						Context("given a default bucket is set", () -> {
							BeforeEach(() -> {
								defaultBucket = "default-customer";
							});
							Context("given the resolver is created with the static constructor function",
									() -> {
										BeforeEach(() -> {
											resolver = GCSObjectIdResolver
													.createS3ObjectIdResolver(
															CustomContentId::getCustomer,
															CustomContentId::getObjectId,
															null);
										});
										Context("given an ID", () -> {
											BeforeEach(() -> {
												customId = new CustomContentId(
														"some-customer",
														"some-object-id");

												when(converter.convert(
														eq("some-object-id"),
														eq(String.class))).thenReturn(
																"/some/object/id");
											});
											It("should fetch the resource", () -> {
												verify(loader).getResource(eq(
														"s3://some-customer/some/object/id"));
											});
										});
									});
							Context("given the resolver is an anonymous class", () -> {
								BeforeEach(() -> {
									resolver = new S3ObjectIdResolver<CustomContentId>() {
										@Override
										public String getBucket(
												CustomContentId idOrEntity,
												String defaultBucketName) {
											return idOrEntity.getCustomer();
										}

										@Override
										public String getKey(CustomContentId idOrEntity) {
											return idOrEntity.getObjectId();
										}
									};
								});
								Context("given an ID", () -> {
									BeforeEach(() -> {
										customId = new CustomContentId("some-customer",
												"some-object-id");

										when(converter.convert(eq("some-object-id"),
												eq(String.class)))
														.thenReturn("/some/object/id");
									});
									It("should fetch the resource", () -> {
										verify(loader).getResource(
												eq("s3://some-customer/some/object/id"));
									});
								});
							});
							Context("given the resolver is a validating resolver", () -> {
								BeforeEach(() -> {
									resolver = GCSObjectIdResolver
											.createS3ObjectIdResolver(
													CustomContentId::getCustomer,
													CustomContentId::getObjectId,
													id -> Assert.notNull(id.getCustomer(),
															"bad id"));
								});
								Context("given an invalid ID", () -> {
									BeforeEach(() -> {
										customId = new CustomContentId(
												 invalid ---> null, "some-object-id");
									});
									It("should throw an error", () -> {
										assertThat(e, is(not(nullValue())));
										assertThat(e.getMessage(), is("bad id"));
									});
								});
							});
						});
						Context("given a default bucket is not set", () -> {
							BeforeEach(() -> {
								defaultBucket = null;
							});
							Context("given a resolver that does not validate", () -> {
								BeforeEach(() -> {
									resolver = GCSObjectIdResolver
											.createS3ObjectIdResolver(
													CustomContentId::getCustomer,
													CustomContentId::getObjectId, null);
								});
								Context("when called with an ID that doesn't specify a bucket either",
										() -> {
											BeforeEach(() -> {
												customId = new CustomContentId(null,
														"some-object-id");
											});
											It("should throw an error", () -> {
												assertThat(e, is(not(nullValue())));
												assertThat(e.getMessage(),
														is("Bucket not set"));
											});
										});
							});
						});
					});
				});
			});
			Describe("AssociativeStore", () -> {
				JustBeforeEach(() -> {
					s3StoreImpl = new DefaultS3StoreImpl<ContentProperty, String>(loader,
							converter, client, resolver, defaultBucket);
				});
				Context("#getResource", () -> {
					JustBeforeEach(() -> {
						try {
							r = s3StoreImpl.getResource(entity);
						}
						catch (Exception e) {
							this.e = e;
						}
					});
					Context("given the default associative store id resolver", () -> {
						BeforeEach(() -> {
							resolver = new DefaultAssociativeStoreS3ObjectIdResolver(
									converter);
						});
						Context("given a default bucket", () -> {
							BeforeEach(() -> {
								defaultBucket = "default-defaultBucket";
							});
							Context("when called with an entity that doesn't have an @Bucket value",
									() -> {
										BeforeEach(() -> {
											entity = new TestEntity("12345-67890");

											// converter that converts the object id for
											// storage placement
											when(converter.convert(eq("12345-67890"),
													eq(String.class))).thenReturn(
															"/12345/67890");

											when(loader.getResource(matches("^s3://default-defaultBucket/12345/67890"))).thenReturn(mock(WritableResource.class));
										});
										It("should fetch the resource", () -> {
											verify(loader).getResource(matches(
													"^s3://default-defaultBucket/12345/67890"));
										});
									});
							Context("when called with an entity that has an @Bucket value",
									() -> {
										BeforeEach(() -> {
											entity = new TestEntityWithBucketAnnotation(
													"some-other-bucket");
											entity.setContentId("12345-67890");

											// converter that converts the object id for
											// storage placement
											when(converter.convert(eq("12345-67890"),
													eq(String.class))).thenReturn(
													"/12345/67890");

											when(loader.getResource(matches("^s3://some-other-bucket/12345/67890"))).thenReturn(mock(WritableResource.class));
										});
										It("should fetch the correct resource",
												() -> {
													verify(loader)
															.getResource(
																	matches("^s3://some-other-bucket/12345/67890"));
												});
									});
							Context("when called with an entity that has no associated resource", () -> {
								BeforeEach(() -> {
									entity = new TestEntity();
								});
								It("should return null", () -> {
									assertThat(r, is(nullValue()));
									assertThat(e, is(nullValue()));
								});
							});
						});
					});
					Context("given a custom id resolver", () -> {
						BeforeEach(() -> {
							resolver = GCSObjectIdResolver.createS3ObjectIdResolver(
									id -> "custom-bucket", id -> "custom-object-id",
									null);
						});
						Context("given a default bucket", () -> {
							BeforeEach(() -> {
								defaultBucket = "default-defaultBucket";
							});
							Context("when called with an entity", () -> {
								BeforeEach(() -> {
									entity = new TestEntity();

									when(converter.convert(eq("custom-object-id"),
											eq(String.class)))
													.thenReturn("/custom/object/id");
								});
								It("should fetch the resource", () -> {
									verify(loader).getResource(matches(
											"^s3://custom-bucket/custom/object/id$"));
								});
							});
						});
					});
					Context("given a custom id resolver that cannot resolve the bucket",
							() -> {
								BeforeEach(() -> {
									resolver = GCSObjectIdResolver
											.createS3ObjectIdResolver(id -> null,
													id -> "custom-object-id", null);
								});
								Context("given the default bucket is not set", () -> {
									BeforeEach(() -> {
										defaultBucket = null;
									});
									Context("when called with an entity", () -> {
										BeforeEach(() -> {
											entity = new TestEntity();
										});
										It("should throw an exception", () -> {
											assertThat(e, is(not(nullValue())));
											assertThat(e.getMessage(),
													is("Bucket not set"));
										});
									});
								});
							});
				});
				Context("associate", () -> {
					BeforeEach(() -> {
						resolver = new DefaultAssociativeStoreS3ObjectIdResolver(
								converter);

						id = "12345-67890";
						entity = new TestEntity();
					});
					JustBeforeEach(() -> {
						s3StoreImpl.associate(entity, id);
					});
					It("should set the entity's content ID attribute", () -> {
						assertThat(entity.getContentId(), is("12345-67890"));
					});
				});
				Context("#unassociate", () -> {
					BeforeEach(() -> {
						resolver = new DefaultAssociativeStoreS3ObjectIdResolver(
								converter);

						entity = new TestEntity();
						entity.setContentId("12345-67890");
					});
					JustBeforeEach(() -> {
						s3StoreImpl.unassociate(entity);
					});
					It("should reset the entity's content ID attribute", () -> {
						assertThat(entity.getContentId(), is(nullValue()));
					});
				});
			});
			Describe("ContentStore", () -> {
				JustBeforeEach(() -> {
					s3StoreImpl = new DefaultS3StoreImpl<ContentProperty, String>(loader,
							converter, client, resolver, defaultBucket);
				});
				Context("#setContent", () -> {
					BeforeEach(() -> {
						entity = new TestEntity();
						content = new ByteArrayInputStream(
								"Hello content world!".getBytes());
					});
					JustBeforeEach(() -> {
						try {
							s3StoreImpl.setContent(entity, content);
						} catch (Exception e) {
							this.e = e;
						}
					});
					Context("given the default associative store id resolver", () -> {
						BeforeEach(() -> {
							resolver = new DefaultAssociativeStoreS3ObjectIdResolver(
									converter);
						});
						Context("given a default bucket is set", () -> {
							BeforeEach(() -> {
								defaultBucket = "default-defaultBucket";
							});
							Context("when the content already exists", () -> {
								BeforeEach(() -> {
									entity.setContentId("abcd-efgh");

									when(converter.convert(eq("abcd-efgh"),
											eq(String.class))).thenReturn("abcd-efgh");

									when(loader.getResource(endsWith("abcd-efgh")))
											.thenReturn(resource);
									output = mock(OutputStream.class);
									when(resource.getOutputStream()).thenReturn(output);

									when(resource.contentLength()).thenReturn(20L);

									when(resource.exists()).thenReturn(true);
								});

								It("should use the converter to establish a resource path",
										() -> {
											verify(converter).convert(eq("abcd-efgh"),
													eq(String.class));
										});

								It("should fetch the resource", () -> {
									verify(loader).getResource(
											eq("s3://default-defaultBucket/abcd-efgh"));
								});

								It("should change the content length", () -> {
									assertThat(entity.getContentLen(), is(20L));
								});

								It("should write to the resource's outputstream", () -> {
									verify(resource).getOutputStream();
									verify(output, times(1)).write(Matchers.<byte[]>any(),
											eq(0), eq(20));
								});

								Context("when the resource output stream throws an IOException", () -> {
									BeforeEach(() -> {
										when(resource.getOutputStream()).thenThrow(new IOException("set-ioexception"));
									});
									It("should throw a StoreAccessException", () -> {
										assertThat(e, is(instanceOf(StoreAccessException.class)));
										assertThat(e.getCause().getMessage(), is("set-ioexception"));
									});
								});
							});

							Context("when the content does not already exist", () -> {
								BeforeEach(() -> {
									assertThat(entity.getContentId(), is(nullValue()));

									when(converter.convert(anyObject(),
											argThat(instanceOf(TypeDescriptor.class)),
											eq(TypeDescriptor.valueOf(String.class))))
													.thenReturn("abcd-efgh");
									when(converter.convert(eq("abcd-efgh"),
											eq(String.class))).thenReturn("abcd-efgh");

									when(loader.getResource(endsWith("abcd-efgh")))
											.thenReturn(resource);
									output = mock(OutputStream.class);
									when(resource.getOutputStream()).thenReturn(output);

									when(resource.contentLength()).thenReturn(20L);

									File resourceFile = mock(File.class);
									parent = mock(File.class);

									when(resource.getFile()).thenReturn(resourceFile);
									when(resourceFile.getParentFile()).thenReturn(parent);
								});

								It("should make a new UUID", () -> {
									assertThat(entity.getContentId(),
											is(not(nullValue())));
								});

								It("should create a new resource", () -> {
									verify(loader).getResource(
											eq("s3://default-defaultBucket/abcd-efgh"));
								});

								It("should write to the resource's outputstream", () -> {
									verify(resource).getOutputStream();
									verify(output, times(1)).write(Matchers.<byte[]>any(),
											eq(0), eq(20));
								});
							});
						});
					});
				});
				Context("#getContent", () -> {
					JustBeforeEach(() -> {
						try {
							result = s3StoreImpl.getContent(entity);
						} catch (Exception e) {
							this.e = e;
						}
					});
					Context("given the default associative store id resolver", () -> {
						BeforeEach(() -> {
							resolver = new DefaultAssociativeStoreS3ObjectIdResolver(converter);
						});
						Context("given a default bucket is set", () -> {
							BeforeEach(() -> {
								defaultBucket = "default-defaultBucket";
							});
							Context("when called with an entity", () -> {
								BeforeEach(() -> {
									entity = new TestEntity();
									content = mock(InputStream.class);
									entity.setContentId("abcd-efgh");

									when(converter.convert(eq("abcd-efgh"),
											eq(String.class))).thenReturn("abcd-efgh");

									when(loader.getResource(endsWith("abcd-efgh")))
											.thenReturn(resource);
									when(resource.getInputStream()).thenReturn(content);
								});
								Context("and the resource already exists", () -> {
									BeforeEach(() -> {
										when(resource.exists()).thenReturn(true);
									});

									It("should use the converter to establish a resource path",
											() -> {
												verify(converter).convert(eq("abcd-efgh"),
														eq(String.class));
											});

									It("should fetch the resource", () -> {
										verify(loader).getResource(eq(
												"s3://default-defaultBucket/abcd-efgh"));
									});

									It("should get content", () -> {
										assertThat(result, is(content));
									});

									Context("when the resource input stream throws an IOException", () -> {
										BeforeEach(() -> {
											when(resource.getInputStream()).thenThrow(new IOException("get-ioexception"));
										});
										It("should throw a StoreAccessException", () -> {
											assertThat(e, is(instanceOf(StoreAccessException.class)));
											assertThat(e.getCause().getMessage(), is("get-ioexception"));
										});
									});
								});
								Context("and the resource doesn't exist", () -> {
									BeforeEach(() -> {
										nonExistentResource = mock(
												WritableResource.class);
										when(resource.exists()).thenReturn(true);

										when(loader.getResource(endsWith("abcd-efgh")))
												.thenReturn(nonExistentResource);
									});

									It("should use the converter to establish a resource path",
											() -> {
												verify(converter).convert(eq("abcd-efgh"),
														eq(String.class));
											});

									It("should fetch the resource", () -> {
										verify(loader).getResource(eq(
												"s3://default-defaultBucket/abcd-efgh"));
									});

									It("should not find the content", () -> {
										assertThat(result, is(nullValue()));
									});
								});
							});
						});
					});
				});
				Context("#unsetContent", () -> {
					JustBeforeEach(() -> {
						try {
							s3StoreImpl.unsetContent(entity);
						} catch (Exception e) {
							this.e = e;
						}
					});
					Context("given the default associative store id resolver", () -> {
						BeforeEach(() -> {
							resolver = new DefaultAssociativeStoreS3ObjectIdResolver(
									converter);
						});
						Context("given a default bucket is set", () -> {
							BeforeEach(() -> {
								defaultBucket = "default-defaultBucket";
							});
							Context("when called with an entity", () -> {
								BeforeEach(() -> {
									entity = new TestEntity();
									entity.setContentId("abcd-efgh");
									entity.setContentLen(100L);
									resource = mock(WritableResource.class);
								});
								Context("and the content exists", () -> {
									BeforeEach(() -> {
										when(converter.convert(eq("abcd-efgh"),
												eq(String.class)))
														.thenReturn("abcd-efgh");

										when(loader.getResource(endsWith("abcd-efgh")))
												.thenReturn(resource);
										when(resource.exists()).thenReturn(true);
									});
									It("should use the converter to establish a resource path",
											() -> {
												verify(converter).convert(eq("abcd-efgh"),
														eq(String.class));
											});
									It("should fetch the resource", () -> {
										verify(loader).getResource(eq(
												"s3://default-defaultBucket/abcd-efgh"));
									});
									Context("when the property has a dedicated ContentId field",
											() -> {
												It("should reset the metadata", () -> {
													assertThat(entity.getContentId(),
															is(nullValue()));
													assertThat(entity.getContentLen(),
															is(0L));
												});
											});
									Context("when the property's ContentId field also is the javax persistence Id field",
											() -> {
												BeforeEach(() -> {
													entity = new SharedIdContentIdEntity();
													entity.setContentId("abcd-efgh");
												});
												It("should not reset the content id metadata",
														() -> {
															assertThat(
																	entity.getContentId(),
																	is("abcd-efgh"));
															assertThat(entity
																	.getContentLen(),
																	is(0L));
														});
											});
									Context("when the property's ContentId field also is the Spring Id field",
											() -> {
												BeforeEach(() -> {
													entity = new SharedSpringIdContentIdEntity();
													entity.setContentId("abcd-efgh");
												});
												It("should not reset the content id metadata",
														() -> {
															assertThat(
																	entity.getContentId(),
																	is("abcd-efgh"));
															assertThat(entity
																	.getContentLen(),
																	is(0L));
														});
											});
									Context("when the amazon client throws an AmazonClientException", () -> {
										BeforeEach(() -> {
											doThrow(new AmazonClientException("unset-exception")).when(client).deleteObject(anyObject());
										});
										It("should throw a StoreAccessException", () -> {
											assertThat(e, is(instanceOf(StoreAccessException.class)));
											assertThat(e.getCause().getMessage(), is("unset-exception"));
										});
									});
								});
								Context("and the content doesn't exist", () -> {
									BeforeEach(() -> {
										when(converter.convert(eq("abcd-efgh"),
												eq(String.class)))
														.thenReturn("abcd-efgh");

										nonExistentResource = mock(
												WritableResource.class);
										when(loader.getResource(endsWith("abcd-efgh")))
												.thenReturn(nonExistentResource);
										when(nonExistentResource.exists())
												.thenReturn(false);
									});
									It("should use the converter to establish a resource path",
											() -> {
												verify(converter).convert(eq("abcd-efgh"),
														eq(String.class));
											});
									It("should fetch the resource", () -> {
										verify(loader).getResource(eq(
												"s3://default-defaultBucket/abcd-efgh"));
									});
									It("should unset the content", () -> {
										verify(client, never()).deleteObject(anyObject());
										assertThat(entity.getContentId(),
												is(nullValue()));
										assertThat(entity.getContentLen(), is(0L));
									});
								});
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

	public class TestEntityWithBucketAnnotation extends TestEntity {
		@Bucket
		private String bucketId = null;

		public TestEntityWithBucketAnnotation(String bucketId) {
			this.bucketId = bucketId;
		}

		public String getBucketId() {
			return bucketId;
		}

		public void setBucketId(String bucketId) {
			this.bucketId = bucketId;
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

	public class CustomContentId implements Serializable {
		private String customer;
		private String objectId;

		public CustomContentId(String bucket, String objectId) {
			this.customer = bucket;
			this.objectId = objectId;
		}

		public String getCustomer() {
			return customer;
		}

		public void setCustomer(String customer) {
			this.customer = customer;
		}

		public String getObjectId() {
			return objectId;
		}

		public void setObjectId(String objectId) {
			this.objectId = objectId;
		}
	}

	static GCSObjectIdResolver<CustomContentId> customIdResolver = GCSObjectIdResolver
			.createS3ObjectIdResolver(CustomContentId::getCustomer,
					CustomContentId::getObjectId, id -> {
						Assert.notNull(id.getCustomer(),
								"Unable to determine defaultBucket.  Customer is null");
						Assert.notNull(id.getObjectId(),
								"Unable to determine key.  ObjectId is null");
					});
}
*/