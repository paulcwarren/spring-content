package internal.org.springframework.content.s3.store;

import com.amazonaws.services.s3.AmazonS3;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.s3.Bucket;
import org.springframework.content.s3.S3ContentId;
import org.springframework.content.s3.S3ContentIdHelper;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.WritableResource;
import org.springframework.util.Assert;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
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
import static org.mockito.Matchers.endsWith;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

@RunWith(Ginkgo4jRunner.class)
//@Ginkgo4jConfiguration(threads=1)
public class DefaultS3StoreImplTest {
    private DefaultS3StoreImpl<ContentProperty, String> s3StoreImpl;
    private DefaultS3StoreImpl<ContentProperty, S3ContentId> s3ContentIdBasedStore;
    private DefaultS3StoreImpl<ContentProperty, CustomS3ContentId> customS3ContentIdBasedStore;
    private ResourceLoader loader;
    private ConversionService converter;
    private AmazonS3 client;
    private String bucket;

    private ContentProperty entity;

    private WritableResource resource;
    private Resource nonExistentResource;
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
                bucket = "default-bucket";
            });
            JustBeforeEach(() -> {
                s3StoreImpl = new DefaultS3StoreImpl<ContentProperty, String>(loader, converter, client, bucket);
            });
            Context("#getResource", () -> {
                Context("when the resource's ID is an S3ContentId", () -> {
                    BeforeEach(() -> {
                        s3ContentIdBasedStore = new DefaultS3StoreImpl<ContentProperty, S3ContentId>(loader, converter, client, "default-bucket");
                        s3ContentIdBasedStore.setContentIdHelper(
                        		S3ContentIdHelper.createS3ContentIdHelper(
                        				S3ContentId::getBucket,
                        				S3ContentId::getObjectId,
                        				id -> {
                        					Assert.notNull(id.getBucket(), "Bucket must not be null");
                        					Assert.notNull(id.getObjectId(), "ObjectId must not be null");
                        				}
                        		)
                        );
                        when(converter.convert(eq("some-object-id"), eq(String.class))).thenReturn("/some/object/id");
                    });
                    JustBeforeEach(() -> {
                        s3ContentIdBasedStore.getResource(new S3ContentId("some-bucket", "some-object-id"));
                    });
                    It("should fetch the resource", () -> {
                        verify(loader).getResource(eq("s3://some-bucket/some/object/id"));
                    });
                });
                Context("when the resource's ID is a custom content ID", () -> {
                    BeforeEach(() -> {
                        customS3ContentIdBasedStore = new DefaultS3StoreImpl<ContentProperty, CustomS3ContentId>(loader, converter, client, "default-customer");
                        customS3ContentIdBasedStore.setContentIdHelper(
                                S3ContentIdHelper.createS3ContentIdHelper(
                                        CustomS3ContentId::getCustomer,
                                        CustomS3ContentId::getObjectId,
                                        id -> {
                                            Assert.notNull(id.getCustomer(), "Bucket must not be null");
                                            Assert.notNull(id.getObjectId(), "ObjectId must not be null");
                                        }
                                )
                        );
                        when(converter.convert(eq("some-object-id"), eq(String.class))).thenReturn("/some/object/id");
                    });
                    JustBeforeEach(() -> {
                        customS3ContentIdBasedStore.getResource(new CustomS3ContentId("some-customer", "some-object-id"));
                    });
                    It("should fetch the resource", () -> {
                        verify(loader).getResource(eq("s3://some-customer/some/object/id"));
                    });
                });
                Context("when the argument is an entity", () -> {
                    BeforeEach(() -> {
                        // converter that converts new ud to take contentId type
                        when(converter.convert(argThat(is(instanceOf(UUID.class))), argThat(is(instanceOf(TypeDescriptor.class))), argThat(is(instanceOf(TypeDescriptor.class))))).thenReturn("converted-uuid");

                        // converter that converts the object id for storage placement
                        when(converter.convert(eq("converted-uuid"), eq(String.class))).thenReturn("/converted/uuid");

                        entity = new TestEntity();
                    });
                    JustBeforeEach(() -> {
                        s3StoreImpl.getResource(entity);
                    });
                    It("should fetch the resource", () -> {
                        verify(loader).getResource(matches("^s3://default-bucket/converted/uuid"));
                    });
                    Context("when the entity has a customer annotation", () -> {
                        BeforeEach(() -> {
                            entity = new TestEntityWithBucketAnnotation("some-other-bucket");
                        });
                        It("should fetch the correct resource", () -> {
                            verify(loader).getResource(matches("^s3://some-other-bucket/converted/uuid"));
                        });
                    });
                });
                Context("when env:BUCKET is not set", () -> {
                    BeforeEach(() -> {
                        bucket = null;
                    });
                    JustBeforeEach(() -> {
                        try {
                            s3StoreImpl.getResource("some-object-id");
                        } catch (Exception e) {
                            this.e = e;
                        }
                    });
                    Context("when the resource's ID is not an S3ContentId", () -> {
                        BeforeEach(() -> {
                            when(converter.convert(eq("some-object-id"), eq(String.class))).thenReturn("some-object-id");
                        });
                        It("should throw an exception", () -> {
                            assertThat(e, is(not(nullValue())));
                        });
                    });
                });
            });

            Context("#setContent", () -> {
                BeforeEach(() -> {
                    entity = new TestEntity();
                    content = new ByteArrayInputStream("Hello content world!".getBytes());
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
                        verify(converter).convert(eq("abcd-efgh"),eq(String.class));
                    });

                    It("should fetch the resource", () -> {
                    	verify(loader).getResource(eq("s3://default-bucket/abcd-efgh"));
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

                        when(converter.convert(anyObject(), argThat(instanceOf(TypeDescriptor.class)), eq(TypeDescriptor.valueOf(String.class)))).thenReturn("abcd-efgh");
                        when(converter.convert(eq("abcd-efgh"), eq(String.class))).thenReturn("abcd-efgh");

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
                    	verify(loader).getResource(eq("s3://default-bucket/abcd-efgh"));
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
                      verify(converter).convert(eq("abcd-efgh"),eq(String.class));
                    });

	                It("should fetch the resource", () -> {
	                	verify(loader).getResource(eq("s3://default-bucket/abcd-efgh"));
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
                        verify(converter).convert(eq("abcd-efgh"),eq(String.class));
                      });

  	                It("should fetch the resource", () -> {
  	                	verify(loader).getResource(eq("s3://default-bucket/abcd-efgh"));
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
                        when(converter.convert(eq("abcd-efgh"), eq(String.class))).thenReturn("abcd-efgh");

	            		when(loader.getResource(endsWith("abcd-efgh"))).thenReturn(resource);
	            		when(resource.exists()).thenReturn(true);
                	});

                    It("should use the converter to establish a resource path", () -> {
                        verify(converter).convert(eq("abcd-efgh"),eq(String.class));
                      });

  	                It("should fetch the resource", () -> {
  	                	verify(loader).getResource(eq("s3://default-bucket/abcd-efgh"));
  	                });

                    Context("when the property has a dedicated ContentId field", () -> {
                        It("should reset the metadata", () -> {
                            assertThat(entity.getContentId(), is(nullValue()));
                            assertThat(entity.getContentLen(), is(0L));
                        });
                    });
                    Context("when the property's ContentId field also is the javax persistence Id field", () ->{
                        BeforeEach(() -> {
                            entity = new SharedIdContentIdEntity();
                            entity.setContentId("abcd-efgh");
                        });
                        It("should not reset the content id metadata", () -> {
                            assertThat(entity.getContentId(), is("abcd-efgh"));
                            assertThat(entity.getContentLen(), is(0L));
                        });
                    });
                    Context("when the property's ContentId field also is the Spring Id field", () ->{
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
                        when(converter.convert(eq("abcd-efgh"), eq(String.class))).thenReturn("abcd-efgh");

                		nonExistentResource = mock(Resource.class);
                        when(loader.getResource(endsWith("abcd-efgh"))).thenReturn(nonExistentResource);
                        when(nonExistentResource.exists()).thenReturn(false);
                	});

                    It("should use the converter to establish a resource path", () -> {
                        verify(converter).convert(eq("abcd-efgh"),eq(String.class));
                      });

  	                It("should fetch the resource", () -> {
  	                	verify(loader).getResource(eq("s3://default-bucket/abcd-efgh"));
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

        public SharedIdContentIdEntity() {this.contentId = null;}

        public String getContentId() { return this.contentId; }

        public void setContentId(String contentId) { this.contentId = contentId; }

        public long getContentLen() { return contentLen; }

        public void setContentLen(long contentLen) { this.contentLen = contentLen; }
    }

    public static class SharedSpringIdContentIdEntity implements ContentProperty {

        @org.springframework.data.annotation.Id
        @ContentId
        private String contentId;

        @ContentLength
        private long contentLen;

        public SharedSpringIdContentIdEntity() { this.contentId = null; }

        public String getContentId() { return this.contentId; }

        public void setContentId(String contentId) { this.contentId = contentId; }

        public long getContentLen() { return contentLen; }

        public void setContentLen(long contentLen) { this.contentLen = contentLen; }
    }

    public class CustomS3ContentId implements Serializable {
        private String customer;
        private String objectId;

        public CustomS3ContentId(String bucket, String objectId) {
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
}
