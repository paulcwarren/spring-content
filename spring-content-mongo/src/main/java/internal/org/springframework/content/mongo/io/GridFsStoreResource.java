package internal.org.springframework.content.mongo.io;

import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.gridfs.GridFSFile;
import org.springframework.content.commons.io.DeletableResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.URI;
import java.net.URL;
import java.sql.SQLException;

import static org.springframework.data.mongodb.core.query.Query.query;
import static org.springframework.data.mongodb.gridfs.GridFsCriteria.whereFilename;

public class GridFsStoreResource implements Resource, WritableResource, DeletableResource {

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
        this.location = location;
        this.gridfs = gridfs;
    }

    public long contentLength() throws IOException {
        GridFSFile file = gridfs.findOne(query(whereFilename().is(location)));
        if (file == null) {
            return 0L;
        }
        return file.getLength();
    }

    public String getFilename() throws IllegalStateException {
        return location;
    }

    public long lastModified() throws IOException {
        return delegate.lastModified();
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
        return delegate.isOpen();
    }

    public InputStream getInputStream() throws IOException, IllegalStateException {
        GridFSFile file = gridfs.findOne(query(whereFilename().is(location)));
        if (file == null) {
            return null;
        }
        return ((GridFSDBFile)file).getInputStream();
    }

    public String getDescription() {
        return delegate.getDescription();
    }

    @Override
    public boolean equals(Object obj) {
        return delegate.equals(obj);
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    public boolean isReadable() {
        return delegate.isReadable();
    }

    public URL getURL() throws IOException {
        return delegate.getURL();
    }

    public URI getURI() throws IOException {
        return delegate.getURI();
    }

    public File getFile() throws IOException {
        return delegate.getFile();
    }

    public Resource createRelative(String relativePath) throws IOException {
        return delegate.createRelative(relativePath);
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

        Thread t = new Thread(
                () -> {
                    synchronized (resource) {
                        gridfs.store(is, resource.getFilename());
                    }
                }
        );
        t.start();

        OutputStream synchronizedOutputStream = new PipedOutputStream(is) {
            private boolean firstWrite = true;

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                firstWrite();
                super.write(b, off, len);
            }

            @Override
            public void write(byte[] b) throws IOException {
//                firstWrite();
                super.write(b);
            }

            @Override
            public void write(int b) throws IOException {
//                firstWrite();
                super.write(b);
            }

            protected void firstWrite() {
                if (firstWrite) {
                    try {
                        System.out.println("------->deleted existing content");
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
