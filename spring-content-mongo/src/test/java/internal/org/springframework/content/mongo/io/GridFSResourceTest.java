package internal.org.springframework.content.mongo.io;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.mongodb.gridfs.GridFSDBFile;
import org.apache.commons.io.IOUtils;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;
import org.springframework.core.io.Resource;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.FIt;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.JustBeforeEach;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyObject;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(Ginkgo4jRunner.class)
@PrepareForTest({GridFsTemplate.class, GridFSFile.class})
public class GridFSResourceTest {

    private GridFsStoreResource r;

    private String location;
    private GridFsTemplate gridfs;

    private GridFSFile file;

    private Object rc;

    {
        Describe("GridFsStoreResource", () -> {
            BeforeEach(() -> {
                location = "some-location";
                gridfs = mock(GridFsTemplate.class);
            });
            JustBeforeEach(() -> {
                r = new GridFsStoreResource(location, gridfs);
            });
            Describe("Resource", () -> {
                Context("#contentLength", () -> {
                    BeforeEach(() -> {
                        file = mock(GridFSFile.class);
                    });
                    JustBeforeEach(() -> {
                        rc = r.contentLength();
                    });
                    Context("given the file exists", () -> {
                        BeforeEach(() -> {
                            when(gridfs.findOne(anyObject())).thenReturn(file);
                        });
                        It("should return the file's length", () -> {
                            verify(file).getLength();
                        });
                    });
                    Context("given the file doesn't exist", () -> {
                        It("should return null", () -> {
                            verify(file, never()).getLength();
                            assertThat(rc, is(0L));
                        });
                    });
                });
                Context("#getFilename", () -> {
                    JustBeforeEach(() -> {
                        rc = r.getFilename();
                    });
                    It("should return the location", () -> {
                        assertThat(rc, is("some-location"));
                    });
                });
                Context("#getId", () -> {
                    BeforeEach(() -> {
                        file = mock(GridFSFile.class);
                    });
                    JustBeforeEach(() -> {
                        rc = r.getId();
                    });
                    Context("given the file exists", () -> {
                        BeforeEach(() -> {
                            file = mock(GridFSFile.class);
                            when(gridfs.findOne(anyObject())).thenReturn(file);
                        });
                        It("should return the file's id", () -> {
                            verify(file).getId();
                        });
                    });
                    Context("given the file doesn't exist", () -> {
                        It("should return null", () -> {
                            verify(file, never()).getId();
                            assertThat(rc, is(nullValue()));
                        });
                    });
                });
                Context("#getContentType", () -> {
                    BeforeEach(() -> {
                        file = mock(GridFSFile.class);
                    });
                    JustBeforeEach(() -> {
                        rc = r.getContentType();
                    });
                    Context("given the file exists", () -> {
                        BeforeEach(() -> {
                            file = mock(GridFSFile.class);
                            when(gridfs.findOne(anyObject())).thenReturn(file);
                        });
                        It("should return the file's content type", () -> {
                            verify(file).getContentType();
                        });
                    });
                    Context("given the file doesn't exist", () -> {
                        It("should return null", () -> {
                            verify(file, never()).getContentType();
                            assertThat(rc, is(nullValue()));
                        });
                    });
                });
                Context("#exists", () -> {
                    BeforeEach(() -> {
                        file = mock(GridFSFile.class);
                    });
                    JustBeforeEach(() -> {
                        rc = r.exists();
                    });
                    Context("given the file exists", () -> {
                        BeforeEach(() -> {
                            file = mock(GridFSFile.class);
                            when(gridfs.findOne(anyObject())).thenReturn(file);
                        });
                        It("should return true", () -> {
                            assertThat(rc, is(true));
                        });
                    });
                    Context("given the file doesn't exist", () -> {
                        It("should return null", () -> {
                            assertThat(rc, is(false));
                        });
                    });
                });
                Context("#isOpen", () -> {
                    JustBeforeEach(() -> {
                        rc = r.isOpen();
                    });
                    It("should return true", () -> {
                        assertThat(rc, is(true));
                    });
                });
                Context("#getInputStream", () -> {
                    JustBeforeEach(() -> {
                        rc = r.getInputStream(); });
                    Context("given the file exists", () -> {
                        BeforeEach(() -> {
                            file = mock(GridFSFile.class);
                            when(gridfs.findOne(anyObject())).thenReturn(file);
                            when(gridfs.getResource(location)).thenReturn(mock(GridFsResource.class));
                        });
                        It("should return the file's input stream", () -> {
                            verify(gridfs).getResource(location);
                        });
                    });
                    Context("given the file doesn't exist", () -> {
                        It("should return null", () -> {
                            verify(gridfs, never()).getResource(location);
                            assertThat(rc, is(nullValue()));
                        });
                    });
                });
                Context("#getDescription", () -> {
                    JustBeforeEach(() -> {
                        rc = r.getDescription();
                    });
                    It("should return something", () -> {
                        assertThat(rc, is(not(nullValue())));
                    });
                });
                Context("#isReadable", () -> {
                    JustBeforeEach(() -> {
                        rc = r.isReadable();
                    });
                    It("should return true", () -> {
                        assertThat(rc, is(true));
                    });
                });
                Context("#lastModified", () -> {
                    BeforeEach(() -> {
                        file = mock(GridFSFile.class);
                    });
                    JustBeforeEach(() -> {
                        rc = r.lastModified();
                    });
                    Context("given the file exists", () -> {
                        BeforeEach(() -> {
                            file = mock(GridFSFile.class);
                            when(gridfs.findOne(anyObject())).thenReturn(file);
                            when(file.getUploadDate()).thenReturn(new Date());
                        });
                        It("should return the file's input stream", () -> {
                            verify(file).getUploadDate();
                        });
                    });
                    Context("given the file doesn't exist", () -> {
                        It("should return null", () -> {
                            verify(file, never()).getUploadDate();
                            assertThat(rc, is(-1L));
                        });
                    });
                });
            });
            Describe("WritableResource", () -> {
                Context("#isWritable", () -> {
                    JustBeforeEach(() -> {
                        rc = r.isWritable();
                    });
                    It("should return true", () -> {
                        assertThat(rc, is(true));
                    });
                });
                Context("getOutputStream", () -> {
                    JustBeforeEach(() -> {
                        rc = r.getOutputStream();
                    });
                    Context("when content is written", () -> {
                        JustBeforeEach(() -> {
                            ((OutputStream)rc).write(new byte[]{32}, 0, 1);
                            IOUtils.closeQuietly((OutputStream)rc);
                        });
                        It("should store the content", () -> {
                            verify(gridfs).store(any(InputStream.class), eq(location));
                        });
                        It("should delete existing content", () -> {
                            verify(gridfs).delete(anyObject());
                        });
                    });
                });
            });
            Describe("DeletableResource", () -> {
                Context("#delete", () -> {
                    JustBeforeEach(() -> {
                        r.delete();
                    });
                    Context("given the file exists", () -> {
                        BeforeEach(() -> {
                            file = mock(GridFSFile.class);
                        });
                        Context("given the file exists", () -> {
                            BeforeEach(() -> {
                                when(gridfs.findOne(anyObject())).thenReturn(file);
                            });
                            It("should delete the file", () -> {
                                verify(gridfs).delete(anyObject());
                            });
                        });
                        Context("given the file doesn't exist", () -> {
                            It("should return null", () -> {
                                verify(gridfs, never()).delete(anyObject());
                            });
                        });
                    });
                });
            });
        });
    }
}
