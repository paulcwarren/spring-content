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
import org.springframework.core.io.WritableResource;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;

import internal.org.springframework.content.fs.operations.FileResourceTemplate;

@RunWith(Ginkgo4jRunner.class)
public class DefaultFilesystemContentRepositoryImplTest {
    private DefaultFileSystemContentRepositoryImpl<TestEntity, String> filesystemContentRepoImpl;
    private FileResourceTemplate fileResourceTemplate;
    private TestEntity property;
    private WritableResource resource;

    private InputStream content;
    private OutputStream output;

    {
        Describe("DefaultFilesystemContentRepositoryImpl", () -> {
            BeforeEach(() -> {
                resource = mock(WritableResource.class);
                fileResourceTemplate = mock(FileResourceTemplate.class);
                filesystemContentRepoImpl = new DefaultFileSystemContentRepositoryImpl<TestEntity, String>(fileResourceTemplate);
                
                when(fileResourceTemplate.get(anyObject())).thenReturn(resource);
                output = mock(OutputStream.class);
                when(resource.getOutputStream()).thenReturn(output);
            });
            Context("#setContent", () -> {
                BeforeEach(() -> {
                    property = new TestEntity();
                    content = new ByteArrayInputStream("Hello content world!".getBytes());
                    when(resource.contentLength()).thenReturn(1L);
                });

                JustBeforeEach(() -> {
                    filesystemContentRepoImpl.setContent(property, content);
                });

                It("should write to the resource's outputstream", () -> {
                    verify(resource).getOutputStream();
                    verify(output, times(1)).write(Matchers.<byte[]>any(), eq(0), eq(20));
                });

                It("should change the content length", () -> {
                    assertThat(property.getContentLen(), is(1L));
                });

                Context("#when the content already exists", () -> {
                    BeforeEach(() -> {
                        property.setContentId("abcd");
                    });

                    It("should use the existing UUID", () -> {
                        assertThat(property.getContentId(), is("abcd"));
                    });
                });

                Context("when the content does not already exist", () -> {
                    BeforeEach(() -> {
                        assertThat(property.getContentId(), is(nullValue()));
                    });
                    It("should make a new UUID", () -> {
                        assertThat(property.getContentId(), is(not(nullValue())));
                    });
                });
            });

            Context("#getContent", () -> {
                BeforeEach(() -> {
                    property = new TestEntity();
                    content = mock(InputStream.class);
                    property.setContentId("abcd");
                    when(fileResourceTemplate.get(anyObject())).thenReturn(resource);
                    when(resource.getInputStream()).thenReturn(content);
                });

                Context("when the resource exists", () -> {
                    BeforeEach(() -> {
                        when(resource.exists()).thenReturn(true);
                    });

                    It("should get content", () -> {
                        assertThat(filesystemContentRepoImpl.getContent(property), is(content));
                    });
                });
                Context("when the resource does not exists", () -> {
                    BeforeEach(() -> {
                        when(resource.exists()).thenReturn(false);
                    });

                    It("should not find the content", () -> {
                        assertThat(filesystemContentRepoImpl.getContent(property), is(nullValue()));
                    });
                });
            });

            Context("#unsetContent", () -> {
                BeforeEach(() -> {
                    property = new TestEntity();
                    property.setContentId("abcd");
                    when(fileResourceTemplate.get(anyObject())).thenReturn(resource);
                    when(resource.exists()).thenReturn(true);
                });

                JustBeforeEach(() -> {
                	filesystemContentRepoImpl.unsetContent(property);
                });

                It("should unset content", () -> {
                    verify(fileResourceTemplate).delete(anyObject());
                    assertThat(property.getContentId(), is(nullValue()));
                    assertThat(property.getContentLen(), is(0L));
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
