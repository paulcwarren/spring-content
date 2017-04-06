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
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.endsWith;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.placementstrategy.PlacementService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.WritableResource;

import com.amazonaws.services.s3.AmazonS3;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jConfiguration;
import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;


@RunWith(Ginkgo4jRunner.class)
@Ginkgo4jConfiguration(threads=1)
public class DefaultS3StoreImplTest {
    private DefaultS3StoreImpl<TestEntity, String> s3StoreImpl;
    private ResourceLoader loader;
    private PlacementService placement;
    private AmazonS3 client;
    private TestEntity entity;
    
    private WritableResource resource;
    private Resource nonExistentResource;

    private InputStream content;
    private OutputStream output;

    private File parent;

    private InputStream result;
    
    {
        Describe("DefaultS3StoreImplTest", () -> {
            BeforeEach(() -> {
                resource = mock(WritableResource.class);
                loader = mock(ResourceLoader.class);
                placement = mock(PlacementService.class);
                client = mock(AmazonS3.class);

                s3StoreImpl = new DefaultS3StoreImpl<TestEntity, String>(loader, placement, client, "some-bucket");
            });
            Context("#setContent", () -> {
                BeforeEach(() -> {
                    entity = new TestEntity();
                    content = new ByteArrayInputStream("Hello content world!".getBytes());

                    when(placement.getLocation(anyObject())).thenReturn("/some/deeply/located/content");

                    when(loader.getResource(endsWith("/some/deeply/located/content"))).thenReturn(resource);
                    output = mock(OutputStream.class);
                    when(resource.getOutputStream()).thenReturn(output);

                    when(resource.contentLength()).thenReturn(20L);
                });

                JustBeforeEach(() -> {
                    s3StoreImpl.setContent(entity, content);
                });


                Context("#when the content already exists", () -> {
                    BeforeEach(() -> {
                        entity.setContentId("abcd");
                        when(resource.exists()).thenReturn(true);
                    });

                    It("should get a location from the placement service and use that to create the resource", () -> {
                        verify(placement).getLocation(anyObject());
                        verify(loader).getResource(eq("s3://some-bucket/some/deeply/located/content"));
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

                        File resourceFile = mock(File.class);
                        parent = mock(File.class);

                        when(resource.getFile()).thenReturn(resourceFile);
                        when(resourceFile.getParentFile()).thenReturn(parent);
                    });

                    It("should make a new UUID", () -> {
                        assertThat(entity.getContentId(), is(not(nullValue())));
                    });

                    It("should create a new resource", () -> {
                    	verify(loader).getResource(eq("s3://some-bucket/some/deeply/located/content"));
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
                  
                    when(placement.getLocation(eq("abcd-efgh"))).thenReturn("/abcd/efgh");

                    when(loader.getResource(endsWith("/abcd/efgh"))).thenReturn(resource);
                    when(resource.getInputStream()).thenReturn(content);
                });

                JustBeforeEach(() -> {
                	result = s3StoreImpl.getContent(entity);
                });
                Context("when the resource exists", () -> {
                    BeforeEach(() -> {
                        when(resource.exists()).thenReturn(true);
                    });

                    It("should get content", () -> {
                        assertThat(result, is(content));
                    });
                });
                Context("when the resource does not exists", () -> {
                    BeforeEach(() -> {
                		nonExistentResource = mock(Resource.class);
                		when(resource.exists()).thenReturn(true);

                		when(loader.getResource(endsWith("/abcd/efgh"))).thenReturn(nonExistentResource);
                        when(loader.getResource(endsWith("abcd-efgh"))).thenReturn(nonExistentResource);
                    });

                    It("should not find the content", () -> {
                        assertThat(result, is(nullValue()));
                    });
                });
                Context("when the resource exists in the old location", () -> {
                	BeforeEach(() -> {
                		nonExistentResource = mock(Resource.class);
                        when(loader.getResource(endsWith("/abcd/efgh"))).thenReturn(nonExistentResource);
                        when(nonExistentResource.exists()).thenReturn(false);

                        when(loader.getResource(endsWith("abcd-efgh"))).thenReturn(resource);
                        when(resource.exists()).thenReturn(true);
                	});
                	It("should check the new location and then the old", () -> {
                		InOrder inOrder = Mockito.inOrder(loader);
                		
                		inOrder.verify(loader).getResource(eq("s3://some-bucket/abcd/efgh"));
                		inOrder.verify(loader).getResource(eq("s3://some-bucket/abcd-efgh"));
                		inOrder.verifyNoMoreInteractions();
                	});
                    It("should get content", () -> {
                        assertThat(result, is(content));
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

                Context("when the content exists in the new location", () -> {
                	BeforeEach(() -> {
                		when(placement.getLocation("abcd-efgh")).thenReturn("/abcd/efgh");
                		
	            		when(loader.getResource(endsWith("/abcd/efgh"))).thenReturn(resource);
	            		when(resource.exists()).thenReturn(true);
                	});
                	It("should unset content", () -> {
                	    verify(client).deleteObject(anyObject());
                		assertThat(entity.getContentId(), is(nullValue()));
                		assertThat(entity.getContentLen(), is(0L));
                	});
                });
                
                Context("when the content exists in the old location", () -> {
                	BeforeEach(() -> {
                        when(placement.getLocation("abcd-efgh")).thenReturn("/abcd/efgh");

                        nonExistentResource = mock(Resource.class);
                        when(loader.getResource(endsWith("/abcd/efgh"))).thenReturn(nonExistentResource);
                        when(nonExistentResource.exists()).thenReturn(false);

                        when(loader.getResource(endsWith("abcd-efgh"))).thenReturn(resource);
                        when(resource.exists()).thenReturn(true);

                	});
                	It("should unset the content", () -> {
                		assertThat(entity.getContentId(), is(nullValue()));
                		assertThat(entity.getContentLen(), is(0L));
                	});
                });
                
                Context("when the content doesnt exist", () -> {
                	BeforeEach(() -> {
                        when(placement.getLocation("abcd-efgh")).thenReturn("/abcd/efgh");

                		nonExistentResource = mock(Resource.class);
                        when(loader.getResource(endsWith("/abcd/efgh"))).thenReturn(nonExistentResource);
                        when(nonExistentResource.exists()).thenReturn(false);

                		nonExistentResource = mock(Resource.class);
                        when(loader.getResource(endsWith("abcd-efgh"))).thenReturn(nonExistentResource);
                        when(nonExistentResource.exists()).thenReturn(false);
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

    public static class TestEntity {
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
}
