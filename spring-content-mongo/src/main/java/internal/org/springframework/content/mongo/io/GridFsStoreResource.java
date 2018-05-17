package internal.org.springframework.content.mongo.io;

import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.gridfs.GridFSFile;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.content.commons.io.DeletableResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.util.Assert;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.URI;
import java.net.URL;
import java.util.concurrent.CountDownLatch;

import static org.springframework.data.mongodb.core.query.Query.query;
import static org.springframework.data.mongodb.gridfs.GridFsCriteria.whereFilename;

public class GridFsStoreResource implements Resource, WritableResource, DeletableResource {

    private static Log logger = LogFactory.getLog(GridFsStoreResource.class);

    private GridFsResource delegate;
    private String location;
    private GridFsTemplate gridfs;

    public GridFsStoreResource(Resource delegate, GridFsTemplate gridfs) {
        Assert.isInstanceOf(GridFsResource.class, "delegate must be an instance of GridFsResource");
        this.delegate = (GridFsResource) delegate;
        this.gridfs = gridfs;
    }

    public GridFsStoreResource(String location, GridFsTemplate gridfs) {
        Assert.notNull(location, "location must be specified");
        Assert.notNull(location, "gridfs must be specified");
        this.location = location;
        this.gridfs = gridfs;
    }

    public long contentLength() throws IOException {
        GridFSDBFile file = gridfs.findOne(query(whereFilename().is(location)));
        if (file == null) {
            return 0L;
        }
        return file.getLength();
    }

    public String getFilename() throws IllegalStateException {
        return location;
    }

    public long lastModified() throws IOException {
        GridFSDBFile file = gridfs.findOne(query(whereFilename().is(location)));
        if (file == null) {
            return -1L;
        }
        return file.getUploadDate().getTime();
    }

    public Object getId() {
        GridFSFile file = gridfs.findOne(query(whereFilename().is(location)));
        if (file == null) {
            return null;
        }
        return file.getId();
    }

    public String getContentType() {
        GridFSFile file = gridfs.findOne(query(whereFilename().is(location)));
        if (file == null) {
            return null;
        }
        return file.getContentType();
    }

    public boolean exists() {
        return gridfs.findOne(query(whereFilename().is(location))) != null;
    }

    public boolean isOpen() {
        return true;
    }

    public InputStream getInputStream() throws IOException, IllegalStateException {
        GridFSFile file = gridfs.findOne(query(whereFilename().is(location)));
        if (file == null) {
            return null;
        }
        return ((GridFSDBFile)file).getInputStream();
    }

    public String getDescription() {
        return "gridfsdbfile [" + location + "]";
    }

    public boolean isReadable() {
        return true;
    }

    public URL getURL() throws IOException {
        throw new UnsupportedOperationException();
    }

    public URI getURI() throws IOException {
        throw new UnsupportedOperationException();
    }

    public File getFile() throws IOException {
        throw new UnsupportedOperationException();
    }

    public Resource createRelative(String relativePath) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        return String.format("GridFsStoreResource [location = '%s']", location);
    }


    @Override
    public boolean isWritable() {
        return true;
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        final GridFsStoreResource resource = this;

        final PipedInputStream is = new PipedInputStream();
        final OutputStream synchronizedOutputStream = new PipedOutputStream(is) {
            private boolean firstWrite = true;

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                deleteOnFirstWrite();
                super.write(b, off, len);
            }

            @Override
            public void write(byte[] b) throws IOException {
                deleteOnFirstWrite();
                super.write(b);
            }

            @Override
            public void write(int b) throws IOException {
                deleteOnFirstWrite();
                super.write(b);
            }

            protected void deleteOnFirstWrite() {
                if (firstWrite) {
                    try {
                        gridfs.delete(query(whereFilename().is(resource.getFilename())));
                    } finally {
                        firstWrite = false;
                    }
                }
            }

            @Override
            public void close() throws IOException {
                super.close();

                synchronized (resource) {
                    return;
                }
            }
        };

        CountDownLatch latch = new CountDownLatch(1);
        Thread t = new Thread(
                () -> {
                    synchronized (resource) {
                        latch.countDown();
                        gridfs.store(is, resource.getFilename());
                    }
                }
        );
        t.start();

        try {
            latch.await();
        } catch (InterruptedException e) {
            logger.warn(String.format("waiting for countdown latch for resource %s", resource.getFilename()), e);
        }

        return synchronizedOutputStream;
    }

    @Override
    public void delete() {
        if (gridfs.findOne(query(whereFilename().is(location))) == null) {
            return;
        }
        gridfs.delete(query(whereFilename().is(location)));
    }
}
