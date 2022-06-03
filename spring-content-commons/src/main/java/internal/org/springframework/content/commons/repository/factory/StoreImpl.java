package internal.org.springframework.content.commons.repository.factory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.repository.events.AfterAssociateEvent;
import org.springframework.content.commons.repository.events.AfterGetContentEvent;
import org.springframework.content.commons.repository.events.AfterGetResourceEvent;
import org.springframework.content.commons.repository.events.AfterSetContentEvent;
import org.springframework.content.commons.repository.events.AfterUnassociateEvent;
import org.springframework.content.commons.repository.events.AfterUnsetContentEvent;
import org.springframework.content.commons.repository.events.BeforeAssociateEvent;
import org.springframework.content.commons.repository.events.BeforeGetContentEvent;
import org.springframework.content.commons.repository.events.BeforeGetResourceEvent;
import org.springframework.content.commons.repository.events.BeforeSetContentEvent;
import org.springframework.content.commons.repository.events.BeforeUnassociateEvent;
import org.springframework.content.commons.repository.events.BeforeUnsetContentEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.Resource;

import lombok.Getter;

public class StoreImpl implements ContentStore<Object, Serializable> {

    private static Log logger = LogFactory.getLog(StoreImpl.class);

    private final ContentStore<Object, Serializable> delegate;
    private final ApplicationEventPublisher publisher;
    private final Path copyContentRootPath;

    public StoreImpl(ContentStore<Object, Serializable> delegate, ApplicationEventPublisher publisher, Path copyContentRootPath) {
        this.delegate = delegate;
        this.publisher = publisher;
        this.copyContentRootPath = copyContentRootPath;
    }

    @Override
    public Object setContent(Object property, InputStream content) {

        Object result = null;

        File contentCopy = null;
        TeeInputStream contentCopyStream = null;
        try {
            contentCopy = Files.createTempFile(copyContentRootPath, "contentCopy", ".tmp").toFile();
            contentCopyStream = new TeeInputStream(content, new FileOutputStream(contentCopy), true);
            BeforeSetContentEvent before = new BeforeSetContentEvent(property, delegate, contentCopyStream);

            publisher.publishEvent(before);

            if (contentCopyStream != null && contentCopyStream.isDirty()) {
                while (contentCopyStream.read(new byte[4096]) != -1) {
                }
                content = new FileInputStream(contentCopy);
            }

            try {
                result = delegate.setContent(property, content);
            }
            catch (Exception e) {
                throw e;
            }

            AfterSetContentEvent after = new AfterSetContentEvent(result, delegate);
            after.setResult(result);
            publisher.publishEvent(after);
        } catch (FileNotFoundException fileNotFoundException) {
            fileNotFoundException.printStackTrace();
        } catch (IOException ioException) {
            ioException.printStackTrace();
        } finally {
            if (contentCopyStream != null) {
                IOUtils.closeQuietly(contentCopyStream);
            }
            if (contentCopy != null) {
                try {
                    Files.deleteIfExists(contentCopy.toPath());
                } catch (IOException e) {
                    logger.error(String.format("Unable to delete content copy %s", contentCopy.toPath()), e);
                }
            }
        }

        return result;
    }

    @Override
    public Object setContent(Object property, PropertyPath propertyPath, InputStream content) {
        Object result = null;

        File contentCopy = null;
        TeeInputStream contentCopyStream = null;
        try {
            contentCopy = Files.createTempFile(copyContentRootPath, "contentCopy", ".tmp").toFile();
            contentCopyStream = new TeeInputStream(content, new FileOutputStream(contentCopy), true);
            BeforeSetContentEvent before = new BeforeSetContentEvent(property, delegate, contentCopyStream);

            publisher.publishEvent(before);

            if (contentCopyStream != null && contentCopyStream.isDirty()) {
                while (contentCopyStream.read(new byte[4096]) != -1) {
                }
                content = new FileInputStream(contentCopy);
            }

            try {
                result = delegate.setContent(property, propertyPath, content);
            }
            catch (Exception e) {
                throw e;
            }

            AfterSetContentEvent after = new AfterSetContentEvent(property, delegate);
            after.setResult(result);
            publisher.publishEvent(after);
        } catch (FileNotFoundException fileNotFoundException) {
            fileNotFoundException.printStackTrace();
        } catch (IOException ioException) {
            ioException.printStackTrace();
        } finally {
            if (contentCopyStream != null) {
                IOUtils.closeQuietly(contentCopyStream);
            }
            if (contentCopy != null) {
                try {
                    Files.deleteIfExists(contentCopy.toPath());
                } catch (IOException e) {
                    logger.error(String.format("Unable to delete content copy %s", contentCopy.toPath()), e);
                }
            }
        }

        return result;
    }

    @Override
    public Object setContent(Object property, Resource resourceContent) {

        BeforeSetContentEvent before = new BeforeSetContentEvent(property, delegate, resourceContent);

        publisher.publishEvent(before);

        Object result;
        try {
            result = delegate.setContent(property, resourceContent);
        }
        catch (Exception e) {
            throw e;
        }

        AfterSetContentEvent after = new AfterSetContentEvent(property, delegate);
        after.setResult(result);
        publisher.publishEvent(after);

        return result;
    }

    @Override
    public Object setContent(Object property, PropertyPath propertyPath, Resource resourceContent) {

        BeforeSetContentEvent before = new BeforeSetContentEvent(property, delegate, resourceContent);

        publisher.publishEvent(before);

        Object result;
        try {
            result = delegate.setContent(property, propertyPath, resourceContent);
        }
        catch (Exception e) {
            throw e;
        }

        AfterSetContentEvent after = new AfterSetContentEvent(property, delegate);
        after.setResult(result);
        publisher.publishEvent(after);

        return result;
    }

    @Override
    public Object unsetContent(Object property) {
        BeforeUnsetContentEvent before = new BeforeUnsetContentEvent(property, delegate);

        publisher.publishEvent(before);

        Object result;
        try {
            result = delegate.unsetContent(property);
        }
        catch (Exception e) {
            throw e;
        }

        AfterUnsetContentEvent after = new AfterUnsetContentEvent(property, delegate);
        after.setResult(result);
        publisher.publishEvent(after);

        return result;
    }


    @Override
    public Object unsetContent(Object property, PropertyPath propertyPath) {
        BeforeUnsetContentEvent before = new BeforeUnsetContentEvent(property, delegate);

        publisher.publishEvent(before);

        Object result;
        try {
            result = delegate.unsetContent(property, propertyPath);
        }
        catch (Exception e) {
            throw e;
        }

        AfterUnsetContentEvent after = new AfterUnsetContentEvent(property, delegate);
        after.setResult(result);
        publisher.publishEvent(after);

        return result;
    }

    @Override
    public InputStream getContent(Object property) {
        BeforeGetContentEvent before = new BeforeGetContentEvent(property, delegate);

        publisher.publishEvent(before);

        InputStream result;
        try {
            result = delegate.getContent(property);
        }
        catch (Exception e) {
            throw e;
        }

        AfterGetContentEvent after = new AfterGetContentEvent(property, delegate);
        after.setResult(result);
        publisher.publishEvent(after);

        return result;
    }

    @Override
    public InputStream getContent(Object property, PropertyPath propertyPath) {
        BeforeGetContentEvent before = new BeforeGetContentEvent(property, delegate);

        publisher.publishEvent(before);

        InputStream result;
        try {
            result = delegate.getContent(property, propertyPath);
        }
        catch (Exception e) {
            throw e;
        }

        AfterGetContentEvent after = new AfterGetContentEvent(property, delegate);
        after.setResult(result);
        publisher.publishEvent(after);

        return result;
    }

    @Override
    public Resource getResource(Object entity) {

        BeforeGetResourceEvent before = new BeforeGetResourceEvent(entity, delegate);

        publisher.publishEvent(before);

        Resource result;
        try {
            result = delegate.getResource(entity);
        }
        catch (Exception e) {
            throw e;
        }

        AfterGetResourceEvent after = new AfterGetResourceEvent(entity, delegate);
        after.setResult(result);
        publisher.publishEvent(after);

        return result;
    }

    @Override
    public Resource getResource(Object entity, PropertyPath propertyPath) {

        BeforeGetResourceEvent before = new BeforeGetResourceEvent(entity, propertyPath, delegate);

        publisher.publishEvent(before);

        Resource result;
        try {
            result = delegate.getResource(entity, propertyPath);
        }
        catch (Exception e) {
            throw e;
        }

        AfterGetResourceEvent after = new AfterGetResourceEvent(entity, propertyPath, delegate);
        after.setResult(result);
        publisher.publishEvent(after);

        return result;
    }

    @Override
    public Resource getResource(Serializable id) {

        BeforeGetResourceEvent before = new BeforeGetResourceEvent(id, delegate);

        publisher.publishEvent(before);

        Resource result;
        try {
            result = delegate.getResource(id);
        }
        catch (Exception e) {
            throw e;
        }

        AfterGetResourceEvent after = new AfterGetResourceEvent(id, delegate);
        after.setResult(result);
        publisher.publishEvent(after);

        return result;
    }

    @Override
    public void associate(Object entity, Serializable id) {

        BeforeAssociateEvent before = new BeforeAssociateEvent(entity, delegate);
        AfterAssociateEvent after = new AfterAssociateEvent(entity, delegate);

        publisher.publishEvent(before);

        try {
            delegate.associate(entity, id);
        }
        catch (Exception e) {
            throw e;
        }

        if (after != null) {
            publisher.publishEvent(after);
        }
    }

    @Override
    public void associate(Object entity, PropertyPath propertyPath, Serializable id) {

        BeforeAssociateEvent before = new BeforeAssociateEvent(entity, propertyPath, delegate);
        AfterAssociateEvent after = new AfterAssociateEvent(entity, propertyPath, delegate);

        publisher.publishEvent(before);

        try {
            delegate.associate(entity, propertyPath, id);
        }
        catch (Exception e) {
            throw e;
        }

        if (after != null) {
            publisher.publishEvent(after);
        }
    }

    @Override
    public void unassociate(Object entity) {

        BeforeUnassociateEvent before = new BeforeUnassociateEvent(entity, delegate);
        AfterUnassociateEvent after = new AfterUnassociateEvent(entity, delegate);

        publisher.publishEvent(before);

        try {
            delegate.unassociate(entity);
        }
        catch (Exception e) {
            throw e;
        }

        if (after != null) {
            publisher.publishEvent(after);
        }
    }

    @Override
    public void unassociate(Object entity, PropertyPath propertyPath) {

        BeforeUnassociateEvent before = new BeforeUnassociateEvent(entity, propertyPath, delegate);
        AfterUnassociateEvent after = new AfterUnassociateEvent(entity, propertyPath, delegate);

        publisher.publishEvent(before);

        try {
            delegate.unassociate(entity, propertyPath);
        }
        catch (Exception e) {
            throw e;
        }

        if (after != null) {
            publisher.publishEvent(after);
        }
    }

    @Getter
    static class TeeInputStream extends org.apache.commons.io.input.TeeInputStream {

        private boolean isDirty = false;

        public TeeInputStream(InputStream input, OutputStream branch, boolean closeBranch) {
            super(input, branch, closeBranch);
        }

        @Override
        public int read() throws IOException {
            isDirty = true;
            return super.read();
        }

        @Override
        public int read(byte[] bts, int st, int end) throws IOException {
            isDirty = true;
            return super.read(bts, st, end);
        }

        @Override
        public int read(byte[] bts) throws IOException {
            isDirty = true;
            return super.read(bts);
        }

        @Override
        public void close() throws IOException {
            try {
                super.close();
            } catch (Throwable t) {
                throw t;
            }
        }
    }
}
