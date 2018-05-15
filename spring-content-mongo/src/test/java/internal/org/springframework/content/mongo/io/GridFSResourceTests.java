package internal.org.springframework.content.mongo.io;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;
import org.junit.runner.RunWith;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.JustBeforeEach;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(Ginkgo4jRunner.class)
public class GridFSResourceTests {

    private GridFsStoreResource r;

    private GridFsResource delegate;
    private GridFsTemplate gridfs;

    {
        Describe("GridFsStoreResource", () -> {
            JustBeforeEach(() -> {
                r = new GridFsStoreResource(delegate, gridfs);
            });
            Describe("Resource", () -> {
                BeforeEach(() -> {
                    delegate = mock(GridFsResource.class);
                });
                It("should delegate all methods", () -> {
                    r.contentLength();
                    verify(delegate).contentLength();
                    r.getFilename();
                    verify(delegate).getFilename();
                    r.lastModified();
                    verify(delegate).lastModified();
                    r.getId();
                    verify(delegate).getId();
                    r.getContentType();
                    verify(delegate).getContentType();
                    r.exists();
                    verify(delegate).exists();
                    r.isOpen();
                    verify(delegate).isOpen();
                    r.getInputStream();
                    verify(delegate).getInputStream();
                    r.getDescription();
                    verify(delegate).getDescription();
                    r.isReadable();
                    verify(delegate).isReadable();
                    r.getURL();
                    verify(delegate).getURL();
                    r.getURI();
                    verify(delegate).getURI();
                    r.getFile();
                    verify(delegate).getFile();
                    r.createRelative("some-path");
                    verify(delegate).createRelative("some-path");
                });
            });
            Describe("WritableResource", () -> {

            });
        });
    }
}
