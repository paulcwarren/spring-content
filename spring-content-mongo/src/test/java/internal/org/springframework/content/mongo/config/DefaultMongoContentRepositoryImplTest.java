package internal.org.springframework.content.mongo.config;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;
import internal.org.springframework.content.mongo.operations.MongoContentTemplate;
import internal.org.springframework.content.mongo.repository.DefaultMongoContentRepositoryImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import com.mongodb.gridfs.GridFSFile;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.InputStream;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.*;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.*;


@RunWith(Ginkgo4jRunner.class)
public class DefaultMongoContentRepositoryImplTest {
    private DefaultMongoContentRepositoryImpl mongoContentRepoImpl;
    private MongoContentTemplate mongoContentTemplate;
    private GridFsTemplate gridFsTemplate;
    private GridFSFile gridFSFile;
    private TestEntity property;
    private GridFsResource resource;

    private InputStream content;

    {
        Describe("DefaultMongoContentRepositoryImpl", () -> {
            BeforeEach(() -> {
                gridFsTemplate = mock(GridFsTemplate.class);
                gridFSFile = mock(GridFSFile.class);
                resource = mock(GridFsResource.class);
                mongoContentTemplate = new MongoContentTemplate(gridFsTemplate);
                mongoContentRepoImpl = new DefaultMongoContentRepositoryImpl(mongoContentTemplate);
            });
            Context("#setContent", () -> {
                BeforeEach(() -> {
                    property = new TestEntity();
                    content = mock(InputStream.class);
                    when(gridFsTemplate.store(anyObject(), anyString())).thenReturn(gridFSFile);
                    when(gridFsTemplate.getResource(anyObject())).thenReturn(resource);
                    when(resource.contentLength()).thenReturn(1L);
                });

                JustBeforeEach(() -> {
                    mongoContentRepoImpl.setContent(property, content);
                });

                It("should store content onto GridFS", () -> {
                    verify(gridFsTemplate).store(anyObject(), anyString());
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
                    when(gridFsTemplate.getResource(anyObject())).thenReturn(resource);
                    when(resource.getInputStream()).thenReturn(content);
                });

                Context("when the resource exists", () -> {
                    BeforeEach(() -> {
                        when(resource.exists()).thenReturn(true);
                    });

                    It("should get content", () -> {
                        assertThat(mongoContentRepoImpl.getContent(property), is(content));
                    });
                });


                Context("when the resource does not exists", () -> {
                    BeforeEach(() -> {
                        when(resource.exists()).thenReturn(false);
                    });

                    It("should not find the content", () -> {
                        assertThat(mongoContentRepoImpl.getContent(property), is(nullValue()));
                    });
                });
            });

            Context("#unsetContent", () -> {
                BeforeEach(() -> {
                    property = new TestEntity();
                    content = mock(InputStream.class);
                    property.setContentId("abcd");
                    when(gridFsTemplate.getResource(anyObject())).thenReturn(resource);
                    when(resource.getInputStream()).thenReturn(content);
                    when(resource.exists()).thenReturn(true);
                });

                JustBeforeEach(() -> {
                    mongoContentRepoImpl.unsetContent(property);
                });

                It("should unset content", () -> {
                    verify(gridFsTemplate).delete(anyObject());
                    assertThat(property.getContentId(), is(nullValue()));
                    assertThat(property.getContentLen(), is(0L));
                });
            });
        });
    }

    @Test
    public void test() {
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
