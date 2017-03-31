package internal.org.springframework.content.fs.repository;

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
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.placementstrategy.PlacementService;
import org.springframework.core.io.WritableResource;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;

import internal.org.springframework.content.fs.operations.FileResourceTemplate;

@RunWith(Ginkgo4jRunner.class)
public class DefaultFilesystemContentRepositoryImplTest {
    private DefaultFileSystemContentRepositoryImpl<TestEntity, String> filesystemContentRepoImpl;
    private FileResourceTemplate fileResourceTemplate;
    private PlacementService placement;
    private TestEntity entity;
    private WritableResource resource;

    private InputStream content;
    private OutputStream output;

    {
//    	Describe("how does java.io.File really work?", () -> {
//    		FIt("does something", () -> {
//    			File f = new File("/tmp/something/else");
//    			FileOutputStream out = new FileOutputStream(f);
//    			out.write("some bytes".getBytes());
//    			out.close();
//    		});
//    	});
    	
        Describe("DefaultFilesystemContentRepositoryImpl", () -> {
            BeforeEach(() -> {
                resource = mock(WritableResource.class);
                fileResourceTemplate = mock(FileResourceTemplate.class);
                placement = mock(PlacementService.class);
                filesystemContentRepoImpl = new DefaultFileSystemContentRepositoryImpl<TestEntity, String>(fileResourceTemplate, placement);
                
                when(fileResourceTemplate.get(anyObject())).thenReturn(resource);
                output = mock(OutputStream.class);
                when(resource.getOutputStream()).thenReturn(output);
            });
            Context("#setContent", () -> {
                BeforeEach(() -> {
                    entity = new TestEntity();
                    content = new ByteArrayInputStream("Hello content world!".getBytes());
                    when(resource.contentLength()).thenReturn(1L);
                    when(placement.getLocation(anyObject())).thenReturn("/some/deep/location");
                });

                JustBeforeEach(() -> {
                    filesystemContentRepoImpl.setContent(entity, content);
                });

//                It("should get a location from the placement service and use that to fetch the resource", () -> {
//                	verify(placement).getLocation(anyObject());
//                	verify(fileResourceTemplate).get(eq("/some/deep/location"));
//                });
                
                It("should change the content length", () -> {
                    assertThat(entity.getContentLen(), is(1L));
                });

                Context("#when the content already exists", () -> {
                    BeforeEach(() -> {
                        entity.setContentId("abcd");
                    });

//                    It("should use the existing UUID", () -> {
//                        assertThat(entity.getContentId(), is("abcd"));
//                    });

//                    It("should not create a new resource", () -> {
//                    	verify(fileResourceTemplate, never()).create();
//                    });
                    
                    It("should write to the resource's outputstream", () -> {
                        verify(resource).getOutputStream();
                        verify(output, times(1)).write(Matchers.<byte[]>any(), eq(0), eq(20));
                    });
                });

                Context("when the content does not already exist", () -> {
                    BeforeEach(() -> {
                        assertThat(entity.getContentId(), is(nullValue()));
                    });
                    It("should make a new UUID", () -> {
                        assertThat(entity.getContentId(), is(not(nullValue())));
                    });
//                    It("should create a new resource", () -> {
//                    	verify(fileResourceTemplate).createResource(anyObject());
//                    });
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
                    entity.setContentId("abcd");
                    when(fileResourceTemplate.get(anyObject())).thenReturn(resource);
                    when(resource.getInputStream()).thenReturn(content);
                });

                Context("when the resource exists", () -> {
                    BeforeEach(() -> {
                        when(resource.exists()).thenReturn(true);
                    });

                    It("should get content", () -> {
                        assertThat(filesystemContentRepoImpl.getContent(entity), is(content));
                    });
                });
                Context("when the resource does not exists", () -> {
                    BeforeEach(() -> {
                        when(resource.exists()).thenReturn(false);
                    });

                    It("should not find the content", () -> {
                        assertThat(filesystemContentRepoImpl.getContent(entity), is(nullValue()));
                    });
                });
            });

            Context("#unsetContent", () -> {
                BeforeEach(() -> {
                    entity = new TestEntity();
                    entity.setContentId("abcd");
                    when(fileResourceTemplate.get(anyObject())).thenReturn(resource);
                    when(resource.exists()).thenReturn(true);
                });

                JustBeforeEach(() -> {
                	filesystemContentRepoImpl.unsetContent(entity);
                });

                It("should unset content", () -> {
                    verify(fileResourceTemplate).delete(anyObject());
                    assertThat(entity.getContentId(), is(nullValue()));
                    assertThat(entity.getContentLen(), is(0L));
                });
            });
        });
    }

    @Test
    public void test() {
    	//noop
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
