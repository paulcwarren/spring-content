//package internal.org.springframework.content.s3.store;
//
//import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.*;
//import static java.lang.String.format;
//import static org.hamcrest.CoreMatchers.containsString;
//import static org.hamcrest.CoreMatchers.instanceOf;
//import static org.hamcrest.CoreMatchers.is;
//import static org.hamcrest.CoreMatchers.not;
//import static org.hamcrest.CoreMatchers.nullValue;
//import static org.hamcrest.MatcherAssert.assertThat;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.endsWith;
//import static org.mockito.ArgumentMatchers.eq;
//import static org.mockito.ArgumentMatchers.matches;
//import static org.mockito.Mockito.*;
//
//import java.io.ByteArrayInputStream;
//import java.io.File;
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.OutputStream;
//import java.io.Serializable;
//import java.util.UUID;
//import java.util.function.Supplier;
//
//import org.junit.runner.RunWith;
//import org.springframework.beans.factory.config.BeanDefinitionCustomizer;
//import org.springframework.content.commons.annotations.ContentId;
//import org.springframework.content.commons.annotations.ContentLength;
//import org.springframework.content.commons.config.ContentPropertyInfo;
//import org.springframework.content.commons.io.RangeableResource;
//import org.springframework.content.commons.property.PropertyPath;
//import org.springframework.content.commons.repository.StoreAccessException;
//import org.springframework.content.commons.utils.PlacementService;
//import org.springframework.content.commons.utils.PlacementServiceImpl;
//import org.springframework.content.s3.Bucket;
//import org.springframework.content.s3.S3ObjectId;
//import org.springframework.content.s3.config.MultiTenantS3ClientProvider;
//import org.springframework.context.support.GenericApplicationContext;
//import org.springframework.core.convert.ConversionFailedException;
//import org.springframework.core.convert.converter.Converter;
//import org.springframework.core.io.DefaultResourceLoader;
//import org.springframework.core.io.InputStreamResource;
//import org.springframework.core.io.Resource;
//import org.springframework.core.io.ResourceLoader;
//import org.springframework.core.io.WritableResource;
//
//import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;
//
//import internal.org.springframework.content.s3.config.S3StoreConfiguration;
//import internal.org.springframework.content.s3.io.S3StoreResource;
//import internal.org.springframework.content.s3.io.SimpleStorageProtocolResolver;
//import software.amazon.awssdk.services.s3.S3Client;
//import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
//import software.amazon.awssdk.services.s3.model.S3Exception;
//
//@RunWith(Ginkgo4jRunner.class)
//// @Ginkgo4jConfiguration(threads=1)
//public class DefaultS3StoreImplTest {
//
//	private DefaultS3StoreImpl<ContentProperty, String> s3StoreImpl;
//	private DefaultS3StoreImpl<ContentProperty, S3ObjectId> s3ObjectIdBasedStore;
//	private DefaultS3StoreImpl<ContentProperty, CustomContentId> customS3ContentIdBasedStore;
//
//	private GenericApplicationContext context = new GenericApplicationContext();
//	private ResourceLoader loader;
//	private PlacementService placementService;
//	private S3Client client, client2;
//
//	private MultiTenantS3ClientProvider clientProvider;
//
//	private String defaultBucket;
//
//	private CustomContentId customId;
//	private ContentProperty entity;
//
//	private String id;
//	private WritableResource resource;
//	private Resource r, nonExistentResource;
//	private InputStream content;
//	private OutputStream output;
//	private File parent;
//	private InputStream result;
//	private Exception e;
//
//	{
//		Describe("DefaultS3StoreImpl", () -> {
//			BeforeEach(() -> {
//				resource = mock(WritableResource.class, withSettings().extraInterfaces(RangeableResource.class));
//				loader = mock(ResourceLoader.class);
//				placementService = mock(PlacementService.class);
//				client = mock(S3Client.class);
//				defaultBucket = null;
//
//				context.registerBean("s3Client", S3Client.class, new Supplier() {
//
//                    @Override
//                    public Object get() {
//                        return client;
//                    }
//				}, new BeanDefinitionCustomizer[]{});
//				context.refresh();
//			});
//			Describe("Store", () -> {
//				Context("#getResource", () -> {
//					Context("given the store's ID is an S3ObjectId type", () -> {
//						BeforeEach(() -> {
//							placementService = new PlacementServiceImpl();
//							S3StoreConfiguration.addDefaultS3ObjectIdConverters(placementService, defaultBucket);
//							placementService.addConverter(new Converter<String, String>() {
//								@Override
//								public String convert(String source) {
//									return "/some/object/id";
//								}
//							});
//
//							SimpleStorageProtocolResolver s3Protocol = new SimpleStorageProtocolResolver(client);
//							s3Protocol.afterPropertiesSet();
//							loader = new DefaultResourceLoader();
//							((DefaultResourceLoader)loader).addProtocolResolver(s3Protocol);
//
//							s3ObjectIdBasedStore = new DefaultS3StoreImpl<>(context, loader, null, placementService, client, null);
//						});
//						JustBeforeEach(() -> {
//							try {
//								r = s3ObjectIdBasedStore.getResource(new S3ObjectId("some-defaultBucket", "some-object-id"));
//							} catch (Exception e) {
//								this.e = e;
//							}
//						});
//						It("should return the resource", () -> {
//							assertThat(e, is(nullValue()));
//							assertThat(r, is(instanceOf(S3StoreResource.class)));
//							assertThat(((S3StoreResource)r).getClient(), is(client));
//							assertThat(r.getDescription(), is(format("Amazon s3 resource [bucket='%s' and object='%s']","some-defaultBucket", "some/object/id")));
//						});
//					});
//					Context("given the store's ID is a custom ID type", () -> {
//						JustBeforeEach(() -> {
//							customS3ContentIdBasedStore = new DefaultS3StoreImpl<>(context, loader, null, placementService, client, null);
//
//							try {
//								r = customS3ContentIdBasedStore.getResource(customId);
//							}
//							catch (Exception e) {
//								this.e = e;
//							}
//						});
//						Context("given a default bucket is set", () -> {
//							BeforeEach(() -> {
//								defaultBucket = "default-customer";
//							});
//							Context("given the resolver is created with the static constructor function", () -> {
//								BeforeEach(() -> {
//									placementService = new PlacementServiceImpl();
//									S3StoreConfiguration.addDefaultS3ObjectIdConverters(placementService, defaultBucket);
//									placementService.addConverter(new Converter<CustomContentId, S3ObjectId>() {
//
//                                        @Override
//                                        public S3ObjectId convert(CustomContentId entity) {
//                                            return new S3ObjectId(entity.getCustomer(), entity.getObjectId());
//                                        }
//									});
//
//									SimpleStorageProtocolResolver s3Protocol = new SimpleStorageProtocolResolver(client);
//									s3Protocol.afterPropertiesSet();
//									loader = new DefaultResourceLoader();
//									((DefaultResourceLoader)loader).addProtocolResolver(s3Protocol);
//								});
//								Context("given an ID", () -> {
//									BeforeEach(() -> {
//										customId = new CustomContentId(
//												"some-customer",
//												"some-object-id");
//									});
//									It("should fetch the resource", () -> {
//										assertThat(e, is(nullValue()));
//										assertThat(r, is(instanceOf(S3StoreResource.class)));
//										assertThat(((S3StoreResource)r).getClient(), is(client));
//										assertThat(r.getDescription(), is(format("Amazon s3 resource [bucket='%s' and object='%s']","some-customer", "some-object-id")));
//									});
//								});
//							});
//						});
//						Context("given a default bucket is not set", () -> {
//							BeforeEach(() -> {
//								defaultBucket = null;
//							});
//							Context("given a resolver that does not validate", () -> {
//								BeforeEach(() -> {
//									placementService = new PlacementServiceImpl();
//									S3StoreConfiguration.addDefaultS3ObjectIdConverters(placementService, defaultBucket);
//								});
//								Context("when called with an ID that doesn't specify a bucket either", () -> {
//                                    BeforeEach(() -> {
//                                        customId = new CustomContentId(null,"some-object-id");
//                                    });
//                                    It("should throw an error", () -> {
//                                        assertThat(e, is(instanceOf(ConversionFailedException.class)));
//                                    });
//                                });
//							});
//						});
//					});
//					Context("given a multi tenant configuration", () -> {
//						JustBeforeEach(() -> {
//							placementService = new PlacementServiceImpl();
//							S3StoreConfiguration.addDefaultS3ObjectIdConverters(placementService, defaultBucket);
//							s3ObjectIdBasedStore = new DefaultS3StoreImpl<>(context, loader, null, placementService, client, clientProvider);
//
//							try {
//								r = s3ObjectIdBasedStore.getResource(new S3ObjectId("some-bucket", "some-object-id"));
//							}
//							catch (Exception e) {
//								this.e = e;
//							}
//						});
//
//						BeforeEach(() -> {
//							client2 = mock(S3Client.class);
//							clientProvider = new MultiTenantS3ClientProvider() {
//								@Override
//								public S3Client getS3Client() {
//									return client2;
//								};
//							};
//						});
//
//						It("should fetch the resource using the correct client", () -> {
//							assertThat(e, is(nullValue()));
//							assertThat(r, is(instanceOf(S3StoreResource.class)));
//							assertThat(((S3StoreResource)r).getClient(), is(client2));
//							assertThat(r.getDescription(), is(format("Amazon s3 resource [bucket='%s' and object='%s']","some-bucket", "some-object-id")));
//						});
//					});
//				});
//			});
//
//			Describe("AssociativeStore", () -> {
//				JustBeforeEach(() -> {
//					s3StoreImpl = new DefaultS3StoreImpl<ContentProperty, String>(context,loader,null,placementService,client,null);
//				});
//				Context("#getResource", () -> {
//					JustBeforeEach(() -> {
//						try {
//							r = s3StoreImpl.getResource(entity);
//						}
//						catch (Exception e) {
//							this.e = e;
//						}
//					});
//					Context("given the default associative store id resolver", () -> {
//						Context("given a default bucket", () -> {
//							BeforeEach(() -> {
//								defaultBucket = "default-defaultBucket";
//							});
//							Context("when called with an entity that doesn't have an @Bucket value", () -> {
//								BeforeEach(() -> {
//									entity = new TestEntity("12345-67890");
//
//									placementService = new PlacementServiceImpl();
//                                    S3StoreConfiguration.addDefaultS3ObjectIdConverters(placementService, defaultBucket);
//									placementService.addConverter(new Converter<S3ObjectId, String>() {
//                                        @Override
//                                        public String convert(S3ObjectId source) {
//                                            return "/" + source.getKey().replaceAll("-", "/");
//                                        }
//                                    });
//
//									SimpleStorageProtocolResolver s3Protocol = new SimpleStorageProtocolResolver(client);
//									s3Protocol.afterPropertiesSet();
//									loader = new DefaultResourceLoader();
//									((DefaultResourceLoader)loader).addProtocolResolver(s3Protocol);
//                                });
//								It("should fetch the resource", () -> {
//									assertThat(e, is(nullValue()));
//									assertThat(r, is(instanceOf(S3StoreResource.class)));
//									assertThat(((S3StoreResource)r).getClient(), is(client));
//									assertThat(r.getDescription(), is(format("Amazon s3 resource [bucket='%s' and object='%s']","default-defaultBucket", "12345/67890")));
//								});
//							});
//							Context("when called with an entity that has an @Bucket value", () -> {
//								BeforeEach(() -> {
//									entity = new TestEntityWithBucketAnnotation(
//											"some-other-bucket");
//									entity.setContentId("12345-67890");
//
//                                    placementService = new PlacementServiceImpl();
//                                    S3StoreConfiguration.addDefaultS3ObjectIdConverters(placementService, defaultBucket);
//
//									SimpleStorageProtocolResolver s3Protocol = new SimpleStorageProtocolResolver(client);
//									s3Protocol.afterPropertiesSet();
//									loader = new DefaultResourceLoader();
//									((DefaultResourceLoader)loader).addProtocolResolver(s3Protocol);
//								});
//								It("should fetch the correct resource", () -> {
//									assertThat(e, is(nullValue()));
//									assertThat(r, is(instanceOf(S3StoreResource.class)));
//									assertThat(((S3StoreResource)r).getClient(), is(client));
//									assertThat(r.getDescription(), is(format("Amazon s3 resource [bucket='%s' and object='%s']","some-other-bucket", "12345-67890")));
//								});
//							});
//							Context("when called with an entity that has no associated resource", () -> {
//								BeforeEach(() -> {
//									entity = new TestEntity();
//								});
//								It("should return null", () -> {
//									assertThat(r, is(nullValue()));
//									assertThat(e, is(nullValue()));
//								});
//							});
//						});
//					});
//					Context("given a custom id resolver", () -> {
//						Context("given a default bucket", () -> {
//							BeforeEach(() -> {
//								defaultBucket = "default-defaultBucket";
//							});
//							Context("when called with an entity", () -> {
//								BeforeEach(() -> {
//									entity = new TestEntity("12345-67890");
//
//									placementService = new PlacementServiceImpl();
//									S3StoreConfiguration.addDefaultS3ObjectIdConverters(placementService, defaultBucket);
//                                    placementService.addConverter(new Converter<TestEntity, S3ObjectId>() {
//                                        @Override
//                                        public S3ObjectId convert(TestEntity source) {
//                                            return new S3ObjectId( "custom-bucket", "custom-object-id");
//                                        }
//                                    });
//
//									SimpleStorageProtocolResolver s3Protocol = new SimpleStorageProtocolResolver(client);
//									s3Protocol.afterPropertiesSet();
//									loader = new DefaultResourceLoader();
//									((DefaultResourceLoader)loader).addProtocolResolver(s3Protocol);
//								});
//								It("should fetch the resource", () -> {
//									assertThat(e, is(nullValue()));
//									assertThat(r, is(instanceOf(S3StoreResource.class)));
//									assertThat(((S3StoreResource)r).getClient(), is(client));
//									assertThat(r.getDescription(), is(format("Amazon s3 resource [bucket='%s' and object='%s']","custom-bucket", "custom-object-id")));
//								});
//							});
//						});
//					});
//					Context("given a custom id resolver that cannot resolve the bucket", () -> {
//                        Context("given the default bucket is not set", () -> {
//                            BeforeEach(() -> {
//                                defaultBucket = null;
//                            });
//                            Context("when called with an entity", () -> {
//                                BeforeEach(() -> {
//                                    entity = new TestEntity("12345-67890");
//
//                                    placementService = new PlacementServiceImpl();
//                                    S3StoreConfiguration.addDefaultS3ObjectIdConverters(placementService, defaultBucket);
//                                    placementService.addConverter(new Converter<CustomContentId, S3ObjectId>() {
//                                        @Override
//                                        public S3ObjectId convert(CustomContentId entity) {
//                                            return new S3ObjectId(null, entity.getObjectId());
//                                        }
//                                    });
//                                });
//                                It("should throw an exception", () -> {
//                                    assertThat(e, is(instanceOf(ConversionFailedException.class)));
//                                });
//                            });
//                        });
//                    });
//				});
//				Context("#getResource with PropertyPath", () -> {
//					JustBeforeEach(() -> {
//						try {
//							r = s3StoreImpl.getResource(entity, PropertyPath.from("content"));
//						}
//						catch (Exception e) {
//							this.e = e;
//						}
//					});
//
//					// the following context is (and should be) exactly the same as for "#getResource" above
//					Context("given the default associative store id resolver", () -> {
//						Context("given a default bucket", () -> {
//							BeforeEach(() -> {
//								defaultBucket = "default-defaultBucket";
//							});
//							Context("when called with an entity that doesn't have an @Bucket value", () -> {
//								BeforeEach(() -> {
//									entity = new TestEntity("12345-67890");
//
//									placementService = new PlacementServiceImpl();
//									S3StoreConfiguration.addDefaultS3ObjectIdConverters(placementService, defaultBucket);
//									placementService.addConverter(new Converter<S3ObjectId, String>() {
//										@Override
//										public String convert(S3ObjectId source) {
//											return "/" + source.getKey().replaceAll("-", "/");
//										}
//									});
//
//									SimpleStorageProtocolResolver s3Protocol = new SimpleStorageProtocolResolver(client);
//									s3Protocol.afterPropertiesSet();
//									loader = new DefaultResourceLoader();
//									((DefaultResourceLoader)loader).addProtocolResolver(s3Protocol);
//								});
//								It("should fetch the resource", () -> {
//									assertThat(e, is(nullValue()));
//									assertThat(r, is(instanceOf(S3StoreResource.class)));
//									assertThat(((S3StoreResource)r).getClient(), is(client));
//									assertThat(r.getDescription(), is(format("Amazon s3 resource [bucket='%s' and object='%s']","default-defaultBucket", "12345/67890")));
//								});
//							});
//							Context("when called with an entity that has an @Bucket value", () -> {
//								BeforeEach(() -> {
//									entity = new TestEntityWithBucketAnnotation(
//											"some-other-bucket");
//									entity.setContentId("12345-67890");
//
//									placementService = new PlacementServiceImpl();
//									S3StoreConfiguration.addDefaultS3ObjectIdConverters(placementService, defaultBucket);
//
//									SimpleStorageProtocolResolver s3Protocol = new SimpleStorageProtocolResolver(client);
//									s3Protocol.afterPropertiesSet();
//									loader = new DefaultResourceLoader();
//									((DefaultResourceLoader)loader).addProtocolResolver(s3Protocol);
//								});
//								It("should fetch the correct resource", () -> {
//									assertThat(e, is(nullValue()));
//									assertThat(r, is(instanceOf(S3StoreResource.class)));
//									assertThat(((S3StoreResource)r).getClient(), is(client));
//									assertThat(r.getDescription(), is(format("Amazon s3 resource [bucket='%s' and object='%s']","some-other-bucket", "12345-67890")));
//								});
//							});
//							Context("when called with an entity that has no associated resource", () -> {
//								BeforeEach(() -> {
//									entity = new TestEntity();
//								});
//								It("should return null", () -> {
//									assertThat(r, is(nullValue()));
//									assertThat(e, is(nullValue()));
//								});
//							});
//						});
//					});
//
//					Context("given a custom id resolver", () -> {
//						Context("given a default bucket", () -> {
//							BeforeEach(() -> {
//								defaultBucket = "default-defaultBucket";
//							});
//							Context("when called with an entity", () -> {
//								BeforeEach(() -> {
//									entity = new TestEntity("12345-67890");
//
//									placementService = new PlacementServiceImpl();
//									S3StoreConfiguration.addDefaultS3ObjectIdConverters(placementService, defaultBucket);
//
//									// Converter that matches Entity and content Id types. Expected to be invoked.
//									// Converter<ContentPropertyInfo<TestEntity, String>, S3ObjectId> instead of Converter<TestEntity, S3ObjectId> for #getResource
//									placementService.addConverter(new Converter<ContentPropertyInfo<TestEntity, String>, S3ObjectId>() {
//										@Override
//										public S3ObjectId convert(ContentPropertyInfo<TestEntity, String> source) {
//											return new S3ObjectId( "custom-bucket", "test-entity/custom-object-id-string-based");
//										}
//									});
//
//									// Converter that does not match content Id type. Should not be invoked.
//									placementService.addConverter(new Converter<ContentPropertyInfo<Object, UUID>, S3ObjectId>() {
//										@Override
//										public S3ObjectId convert(ContentPropertyInfo<Object, UUID> source) {
//											return new S3ObjectId( "custom-bucket", "object/custom-object-id-uuid-based");
//										}
//									});
//									// Converter that does not match Entity type. Should not be invoked.
//									placementService.addConverter(new Converter<ContentPropertyInfo<TestEntityWithBucketAnnotation, String>, S3ObjectId>() {
//										@Override
//										public S3ObjectId convert(ContentPropertyInfo<TestEntityWithBucketAnnotation, String> source) {
//											return new S3ObjectId( "custom-bucket", "test-entity-with-bucket/custom-object-id-string-based");
//										}
//									});
//
//									SimpleStorageProtocolResolver s3Protocol = new SimpleStorageProtocolResolver(client);
//									s3Protocol.afterPropertiesSet();
//									loader = new DefaultResourceLoader();
//									((DefaultResourceLoader)loader).addProtocolResolver(s3Protocol);
//								});
//								It("should fetch the resource", () -> {
//									assertThat(e, is(nullValue()));
//									assertThat(r, is(instanceOf(S3StoreResource.class)));
//									assertThat(((S3StoreResource)r).getClient(), is(client));
//									assertThat(r.getDescription(), is(format("Amazon s3 resource [bucket='%s' and object='%s']","custom-bucket", "test-entity/custom-object-id-string-based")));
//								});
//							});
//						});
//					});
//					Context("given a custom id resolver that cannot resolve the bucket", () -> {
//						Context("given the default bucket is not set", () -> {
//							BeforeEach(() -> {
//								defaultBucket = null;
//							});
//							Context("when called with an entity", () -> {
//								BeforeEach(() -> {
//									entity = new TestEntity("12345-67890");
//
//									placementService = new PlacementServiceImpl();
//									S3StoreConfiguration.addDefaultS3ObjectIdConverters(placementService, defaultBucket);
//									placementService.addConverter(new Converter<ContentPropertyInfo<TestEntity, String>, S3ObjectId>() {
//										@Override
//										public S3ObjectId convert(ContentPropertyInfo<TestEntity, String> source) {
//											return new S3ObjectId(null, "custom-object-id");
//										}
//									});
//								});
//								It("should throw an exception", () -> {
//									assertThat(e, is(instanceOf(ConversionFailedException.class)));
//								});
//							});
//						});
//					});
//				});
//				Context("#associate", () -> {
//					BeforeEach(() -> {
//						id = "12345-67890";
//						entity = new TestEntity();
//					});
//					JustBeforeEach(() -> {
//						s3StoreImpl.associate(entity, id);
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
//						s3StoreImpl.unassociate(entity);
//					});
//					It("should reset the entity's content ID attribute", () -> {
//						assertThat(entity.getContentId(), is(nullValue()));
//					});
//				});
//			});
//
//			Describe("ContentStore", () -> {
//				JustBeforeEach(() -> {
//					s3StoreImpl = spy(new DefaultS3StoreImpl<ContentProperty, String>(context,loader,null,placementService,client,null));
//				});
//				Context("#setContent", () -> {
//					BeforeEach(() -> {
//						entity = new TestEntity();
//						content = new ByteArrayInputStream(
//								"Hello content world!".getBytes());
//					});
//					JustBeforeEach(() -> {
//						try {
//							s3StoreImpl.setContent(entity, content);
//						} catch (Exception e) {
//							this.e = e;
//						}
//					});
//					Context("given the default associative store id resolver", () -> {
//						Context("given a default bucket is set", () -> {
//							BeforeEach(() -> {
//								defaultBucket = "default-defaultBucket";
//							});
//							Context("when the content already exists", () -> {
//								BeforeEach(() -> {
//									entity.setContentId("abcd-efgh");
//
//                                    placementService = new PlacementServiceImpl();
//                                    S3StoreConfiguration.addDefaultS3ObjectIdConverters(placementService, defaultBucket);
//
//									when(loader.getResource(endsWith("abcd-efgh"))).thenReturn(resource);
//									output = mock(OutputStream.class);
//									when(resource.getOutputStream()).thenReturn(output);
//
//									when(resource.contentLength()).thenReturn(20L);
//
//									when(resource.exists()).thenReturn(true);
//								});
//								It("should fetch the resource", () -> {
//									verify(loader).getResource(eq("s3://default-defaultBucket/abcd-efgh"));
//								});
//								It("should change the content length", () -> {
//									assertThat(entity.getContentLen(), is(20L));
//								});
//								It("should write to the resource's outputstream", () -> {
//									verify(resource).getOutputStream();
//									verify(output, times(1)).write(any(byte[].class),
//											eq(0), eq(20));
//								});
//								Context("when the resource output stream throws an IOException", () -> {
//									BeforeEach(() -> {
//										when(resource.getOutputStream()).thenThrow(new IOException("set-ioexception"));
//									});
//									It("should throw a StoreAccessException", () -> {
//										assertThat(e, is(instanceOf(StoreAccessException.class)));
//										assertThat(e.getCause().getMessage(), is("set-ioexception"));
//									});
//								});
//							});
//							Context("when the content does not already exist", () -> {
//								BeforeEach(() -> {
//									assertThat(entity.getContentId(), is(nullValue()));
//
//                                    placementService = new PlacementServiceImpl();
//                                    S3StoreConfiguration.addDefaultS3ObjectIdConverters(placementService, defaultBucket);
//
//                                    when(loader.getResource(matches("^s3://.*[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"))).thenReturn(resource);
//									output = mock(OutputStream.class);
//									when(resource.getOutputStream()).thenReturn(output);
//
//									when(resource.contentLength()).thenReturn(20L);
//
//									File resourceFile = mock(File.class);
//									parent = mock(File.class);
//
//									when(resource.getFile()).thenReturn(resourceFile);
//									when(resourceFile.getParentFile()).thenReturn(parent);
//								});
//								It("should make a new UUID", () -> {
//									assertThat(entity.getContentId(),is(not(nullValue())));
//								});
//								It("should create a new resource", () -> {
//									verify(loader).getResource(matches("^s3://.*[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"));
//								});
//								It("should write to the resource's outputstream", () -> {
//									verify(resource).getOutputStream();
//									verify(output, times(1)).write(any(byte[].class),
//											eq(0), eq(20));
//								});
//							});
//							Context("when s3 throws an S3Exception", () -> {
//							    BeforeEach(() -> {
//                                    assertThat(entity.getContentId(), is(nullValue()));
//
//                                    placementService = new PlacementServiceImpl();
//                                    S3StoreConfiguration.addDefaultS3ObjectIdConverters(placementService, defaultBucket);
//
//                                    when(loader.getResource(matches("^s3://.*[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"))).thenReturn(resource);
//                                    output = mock(OutputStream.class);
//                                    when(resource.getOutputStream()).thenReturn(output);
//
//                                    doThrow(S3Exception.builder().message("no such upload").build()).when(output).close();
//							    });
//							    It("should do something", () -> {
//							        assertThat(e, is(instanceOf(S3Exception.class)));
//							    });
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
//						r = new InputStreamResource(content);
//					});
//
//					JustBeforeEach(() -> {
//						try {
//							s3StoreImpl.setContent(entity, r);
//						} catch (Exception e) {
//							this.e = e;
//						}
//					});
//
//					It("should delegate", () -> {
//						verify(s3StoreImpl).setContent(eq(entity), eq(content));
//					});
//
//					Context("when the resource throws an IOException", () -> {
//						BeforeEach(() -> {
//							r = mock(Resource.class);
//							when(r.getInputStream()).thenThrow(new IOException("setContent badness"));
//						});
//						It("should throw a StoreAccessException", () -> {
//							assertThat(e, is(instanceOf(StoreAccessException.class)));
//							assertThat(e.getCause().getMessage(), containsString("setContent badness"));
//						});
//					});
//				});
//
//				Context("#getContent", () -> {
//					JustBeforeEach(() -> {
//						try {
//							result = s3StoreImpl.getContent(entity);
//						} catch (Exception e) {
//							this.e = e;
//						}
//					});
//					Context("given the default associative store id resolver", () -> {
//						Context("given a default bucket is set", () -> {
//							BeforeEach(() -> {
//								defaultBucket = "default-defaultBucket";
//							});
//							Context("when called with an entity", () -> {
//								BeforeEach(() -> {
//									entity = new TestEntity();
//									content = mock(InputStream.class);
//									entity.setContentId("abcd-efgh");
//
//                                    placementService = new PlacementServiceImpl();
//                                    S3StoreConfiguration.addDefaultS3ObjectIdConverters(placementService, defaultBucket);
//
//                                    when(loader.getResource(matches("^s3://default-defaultBucket/abcd-efgh"))).thenReturn(resource);
//									when(resource.getInputStream()).thenReturn(content);
//								});
//								Context("and the resource already exists", () -> {
//									BeforeEach(() -> {
//										when(resource.exists()).thenReturn(true);
//									});
//									It("should fetch the resource", () -> {
//										verify(loader).getResource(eq("s3://default-defaultBucket/abcd-efgh"));
//									});
//									It("should get content", () -> {
//										assertThat(result, is(content));
//									});
//									Context("when the resource input stream throws an IOException", () -> {
//										BeforeEach(() -> {
//											when(resource.getInputStream()).thenThrow(new IOException("get-ioexception"));
//										});
//										It("should throw a StoreAccessException", () -> {
//											assertThat(e, is(instanceOf(StoreAccessException.class)));
//											assertThat(e.getCause().getMessage(), is("get-ioexception"));
//										});
//									});
//								});
//								Context("and the resource doesn't exist", () -> {
//									BeforeEach(() -> {
//										nonExistentResource = mock(WritableResource.class);
//										when(resource.exists()).thenReturn(true);
//
//										when(loader.getResource(endsWith("abcd-efgh"))).thenReturn(nonExistentResource);
//									});
//									It("should fetch the resource", () -> {
//										verify(loader).getResource(eq("s3://default-defaultBucket/abcd-efgh"));
//									});
//									It("should not find the content", () -> {
//										assertThat(result, is(nullValue()));
//									});
//								});
//								Context("with an null @ContentId", () -> {
//									BeforeEach(() -> {
//										entity.setContentId(null);
//									});
//									It("should return null", () -> {
//										assertThat(result, is(nullValue()));
//										assertThat(e, is(nullValue()));
//									});
//								});
//							});
//						});
//					});
//				});
//				Context("#unsetContent", () -> {
//					JustBeforeEach(() -> {
//						try {
//							s3StoreImpl.unsetContent(entity);
//						} catch (Exception e) {
//							this.e = e;
//						}
//					});
//					Context("given the default associative store id resolver", () -> {
//						Context("given a default bucket is set", () -> {
//							BeforeEach(() -> {
//								defaultBucket = "default-defaultBucket";
//							});
//							Context("when called with an entity", () -> {
//								BeforeEach(() -> {
//									entity = new TestEntity();
//									entity.setContentId("abcd-efgh");
//									entity.setContentLen(100L);
//									resource = mock(WritableResource.class, withSettings().extraInterfaces(RangeableResource.class));
//								});
//								Context("and the content exists", () -> {
//									BeforeEach(() -> {
//										placementService = new PlacementServiceImpl();
//										S3StoreConfiguration.addDefaultS3ObjectIdConverters(placementService, defaultBucket);
//
//										when(loader.getResource(endsWith("abcd-efgh"))).thenReturn(resource);
//										when(resource.exists()).thenReturn(true);
//									});
//									It("should fetch the resource", () -> {
//										verify(loader).getResource(eq("s3://default-defaultBucket/abcd-efgh"));
//									});
//									Context("when the property has a dedicated ContentId field", () -> {
//										It("should reset the metadata", () -> {
//											assertThat(entity.getContentId(), is(nullValue()));
//											assertThat(entity.getContentLen(), is(0L));
//										});
//									});
//									Context("when the property's ContentId field also is the javax persistence Id field", () -> {
//										BeforeEach(() -> {
//											entity = new SharedIdContentIdEntity();
//											entity.setContentId("abcd-efgh");
//										});
//										It("should not reset the content id metadata", () -> {
//											assertThat(entity.getContentId(), is("abcd-efgh"));
//											assertThat(entity.getContentLen(), is(0L));
//										});
//									});
//									Context("when the property's ContentId field also is the Spring Id field", () -> {
//										BeforeEach(() -> {
//											entity = new SharedSpringIdContentIdEntity();
//											entity.setContentId("abcd-efgh");
//										});
//										It("should not reset the content id metadata",
//												() -> {
//													assertThat(entity.getContentId(), is("abcd-efgh"));
//													assertThat(entity.getContentLen(), is(0L));
//												});
//									});
//								});
//								Context("and the content doesn't exist", () -> {
//									BeforeEach(() -> {
//										placementService = new PlacementServiceImpl();
//										S3StoreConfiguration.addDefaultS3ObjectIdConverters(placementService, defaultBucket);
//
//										nonExistentResource = mock(WritableResource.class, withSettings().extraInterfaces(RangeableResource.class));
//										when(loader.getResource(endsWith("abcd-efgh"))).thenReturn(nonExistentResource);
//										when(nonExistentResource.exists()).thenReturn(false);
//									});
//									It("should fetch the resource", () -> {
//										verify(loader).getResource(eq("s3://default-defaultBucket/abcd-efgh"));
//									});
//									It("should unset the content", () -> {
//										verify(client, never()).deleteObject(any(DeleteObjectRequest.class));
//										assertThat(entity.getContentId(), is(nullValue()));
//										assertThat(entity.getContentLen(), is(0L));
//									});
//								});
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
//	public class TestEntityWithBucketAnnotation extends TestEntity {
//		@Bucket
//		private String bucketId = null;
//
//		public TestEntityWithBucketAnnotation(String bucketId) {
//			this.bucketId = bucketId;
//		}
//
//		public String getBucketId() {
//			return bucketId;
//		}
//
//		public void setBucketId(String bucketId) {
//			this.bucketId = bucketId;
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
//
//	public class CustomContentId implements Serializable {
//		private String customer;
//		private String objectId;
//
//		public CustomContentId(String bucket, String objectId) {
//			this.customer = bucket;
//			this.objectId = objectId;
//		}
//
//		public String getCustomer() {
//			return customer;
//		}
//
//		public void setCustomer(String customer) {
//			this.customer = customer;
//		}
//
//		public String getObjectId() {
//			return objectId;
//		}
//
//		public void setObjectId(String objectId) {
//			this.objectId = objectId;
//		}
//	}
//}
