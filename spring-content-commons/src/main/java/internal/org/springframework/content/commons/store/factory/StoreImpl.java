package internal.org.springframework.content.commons.store.factory;

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
import org.springframework.content.commons.repository.GetResourceParams;
import org.springframework.content.commons.store.events.AfterAssociateEvent;
import org.springframework.content.commons.store.events.AfterGetContentEvent;
import org.springframework.content.commons.store.events.AfterGetResourceEvent;
import org.springframework.content.commons.store.events.AfterSetContentEvent;
import org.springframework.content.commons.store.events.AfterUnassociateEvent;
import org.springframework.content.commons.store.events.AfterUnsetContentEvent;
import org.springframework.content.commons.store.events.BeforeAssociateEvent;
import org.springframework.content.commons.store.events.BeforeGetContentEvent;
import org.springframework.content.commons.store.events.BeforeGetResourceEvent;
import org.springframework.content.commons.store.events.BeforeSetContentEvent;
import org.springframework.content.commons.store.events.BeforeUnassociateEvent;
import org.springframework.content.commons.store.events.BeforeUnsetContentEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.Resource;

import lombok.Getter;

public class StoreImpl implements ContentStore<Object, Serializable> {

    private static final Log logger = LogFactory.getLog(StoreImpl.class);

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

            org.springframework.content.commons.repository.events.BeforeSetContentEvent oldBefore = new org.springframework.content.commons.repository.events.BeforeSetContentEvent(property, delegate, contentCopyStream);
            publisher.publishEvent(oldBefore);

            BeforeSetContentEvent before = new BeforeSetContentEvent(property, delegate, contentCopyStream);
            publisher.publishEvent(before);

            // inputstream was processed and replaced
            if (oldBefore.getInputStream() != null && oldBefore.getInputStream().equals(contentCopyStream) == false) {
                content = oldBefore.getInputStream();
            }
            else if (before.getInputStream() != null && before.getInputStream().equals(contentCopyStream) == false) {
                content = before.getInputStream();
            }
            // content was processed but not replaced
            else if (contentCopyStream != null && contentCopyStream.isDirty()) {
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

            org.springframework.content.commons.repository.events.AfterSetContentEvent oldAfter = new org.springframework.content.commons.repository.events.AfterSetContentEvent(result, delegate);
            oldAfter.setResult(result);
            publisher.publishEvent(oldAfter);

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
        return this.setContent(property, propertyPath, content, -1);
    }

    @Override
    public Object setContent(Object property, PropertyPath propertyPath, InputStream content, long contentLen) {
        Object result = null;

        File contentCopy = null;
        TeeInputStream contentCopyStream = null;
        try {
            contentCopy = Files.createTempFile(copyContentRootPath, "contentCopy", ".tmp").toFile();
            contentCopyStream = new TeeInputStream(content, new FileOutputStream(contentCopy), true);

            org.springframework.content.commons.repository.events.BeforeSetContentEvent oldBefore = new org.springframework.content.commons.repository.events.BeforeSetContentEvent(property, propertyPath, delegate, contentCopyStream);
            publisher.publishEvent(oldBefore);

            BeforeSetContentEvent before = new BeforeSetContentEvent(property, propertyPath, delegate, contentCopyStream);
            publisher.publishEvent(before);

            // inputstream was processed and replaced
            if (before.getInputStream() != null && before.getInputStream().equals(contentCopyStream) == false) {
                content = before.getInputStream();
            }
            // content was processed but not replaced
            else if (contentCopyStream != null && contentCopyStream.isDirty()) {
                while (contentCopyStream.read(new byte[4096]) != -1) {
                }
                content = new FileInputStream(contentCopy);
            }

            try {
                result = delegate.setContent(property, propertyPath, content, contentLen);
            }
            catch (Exception e) {
                throw e;
            }

            org.springframework.content.commons.repository.events.AfterSetContentEvent oldAfter = new org.springframework.content.commons.repository.events.AfterSetContentEvent(property, propertyPath, delegate);
            oldAfter.setResult(result);
            publisher.publishEvent(oldAfter);

            AfterSetContentEvent after = new AfterSetContentEvent(property, propertyPath, delegate);
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

        org.springframework.content.commons.repository.events.BeforeSetContentEvent oldBefore = new org.springframework.content.commons.repository.events.BeforeSetContentEvent(property, delegate, resourceContent);
        publisher.publishEvent(oldBefore);

        BeforeSetContentEvent before = new BeforeSetContentEvent(property, delegate, resourceContent);
        publisher.publishEvent(before);

        Object result;
        try {
            result = delegate.setContent(property, resourceContent);
        }
        catch (Exception e) {
            throw e;
        }

        org.springframework.content.commons.repository.events.AfterSetContentEvent oldAfter = new org.springframework.content.commons.repository.events.AfterSetContentEvent(property, delegate);
        oldAfter.setResult(result);
        publisher.publishEvent(oldAfter);

        AfterSetContentEvent after = new AfterSetContentEvent(property, delegate);
        after.setResult(result);
        publisher.publishEvent(after);

        return result;
    }

    @Override
    public Object setContent(Object property, PropertyPath propertyPath, Resource resourceContent) {

        org.springframework.content.commons.repository.events.BeforeSetContentEvent oldBefore = new org.springframework.content.commons.repository.events.BeforeSetContentEvent(property, propertyPath, delegate, resourceContent);
        publisher.publishEvent(oldBefore);

        BeforeSetContentEvent before = new BeforeSetContentEvent(property, propertyPath, delegate, resourceContent);
        publisher.publishEvent(before);

        Object result;
        try {
            result = delegate.setContent(property, propertyPath, resourceContent);
        }
        catch (Exception e) {
            throw e;
        }

        org.springframework.content.commons.repository.events.AfterSetContentEvent oldAfter = new org.springframework.content.commons.repository.events.AfterSetContentEvent(property, propertyPath, delegate);
        oldAfter.setResult(result);
        publisher.publishEvent(oldAfter);

        AfterSetContentEvent after = new AfterSetContentEvent(property, propertyPath, delegate);
        after.setResult(result);
        publisher.publishEvent(after);

        return result;
    }

    @Override
    public Object unsetContent(Object property) {
        org.springframework.content.commons.repository.events.BeforeUnsetContentEvent oldBefore = new org.springframework.content.commons.repository.events.BeforeUnsetContentEvent(property, delegate);
        publisher.publishEvent(oldBefore);

        BeforeUnsetContentEvent before = new BeforeUnsetContentEvent(property, delegate);
        publisher.publishEvent(before);

        Object result;
        try {
            result = delegate.unsetContent(property);
        }
        catch (Exception e) {
            throw e;
        }

        org.springframework.content.commons.repository.events.AfterUnsetContentEvent oldAfter = new org.springframework.content.commons.repository.events.AfterUnsetContentEvent(property, delegate);
        oldAfter.setResult(result);
        publisher.publishEvent(oldAfter);

        AfterUnsetContentEvent after = new AfterUnsetContentEvent(property, delegate);
        after.setResult(result);
        publisher.publishEvent(after);

        return result;
    }


    @Override
    public Object unsetContent(Object property, PropertyPath propertyPath) {
        org.springframework.content.commons.repository.events.BeforeUnsetContentEvent oldBefore = new org.springframework.content.commons.repository.events.BeforeUnsetContentEvent(property, propertyPath, delegate);
        publisher.publishEvent(oldBefore);

        BeforeUnsetContentEvent before = new BeforeUnsetContentEvent(property, propertyPath, delegate);
        publisher.publishEvent(before);

        Object result;
        try {
            result = delegate.unsetContent(property, propertyPath);
        }
        catch (Exception e) {
            throw e;
        }

        org.springframework.content.commons.repository.events.AfterUnsetContentEvent oldAfter = new org.springframework.content.commons.repository.events.AfterUnsetContentEvent(property, propertyPath, delegate);
        oldAfter.setResult(result);
        publisher.publishEvent(oldAfter);

        AfterUnsetContentEvent after = new AfterUnsetContentEvent(property, propertyPath, delegate);
        after.setResult(result);
        publisher.publishEvent(after);

        return result;
    }

    @Override
    public InputStream getContent(Object property) {
        org.springframework.content.commons.repository.events.BeforeGetContentEvent oldBefore = new org.springframework.content.commons.repository.events.BeforeGetContentEvent(property, delegate);
        publisher.publishEvent(oldBefore);

        BeforeGetContentEvent before = new BeforeGetContentEvent(property, delegate);
        publisher.publishEvent(before);

        InputStream result;
        try {
            result = delegate.getContent(property);
        }
        catch (Exception e) {
            throw e;
        }

        org.springframework.content.commons.repository.events.AfterGetContentEvent oldAfter = new org.springframework.content.commons.repository.events.AfterGetContentEvent(property, delegate);
        oldAfter.setResult(result);
        publisher.publishEvent(oldAfter);

        AfterGetContentEvent after = new AfterGetContentEvent(property, delegate);
        after.setResult(result);
        publisher.publishEvent(after);

        return result;
    }

    @Override
    public InputStream getContent(Object property, PropertyPath propertyPath) {
        org.springframework.content.commons.repository.events.BeforeGetContentEvent oldBefore = new org.springframework.content.commons.repository.events.BeforeGetContentEvent(property, propertyPath, delegate);
        publisher.publishEvent(oldBefore);

        BeforeGetContentEvent before = new BeforeGetContentEvent(property, propertyPath, delegate);
        publisher.publishEvent(before);

        InputStream result;
        try {
            result = delegate.getContent(property, propertyPath);
        }
        catch (Exception e) {
            throw e;
        }

        org.springframework.content.commons.repository.events.AfterGetContentEvent oldAfter = new org.springframework.content.commons.repository.events.AfterGetContentEvent(property, propertyPath, delegate);
        oldAfter.setResult(result);
        publisher.publishEvent(oldAfter);

        AfterGetContentEvent after = new AfterGetContentEvent(property, propertyPath, delegate);
        after.setResult(result);
        publisher.publishEvent(after);

        return (InputStream) after.getResult();
    }

    @Override
    public Resource getResource(Object entity) {

        org.springframework.content.commons.repository.events.BeforeGetResourceEvent oldBefore = new org.springframework.content.commons.repository.events.BeforeGetResourceEvent(entity, delegate);
        publisher.publishEvent(oldBefore);

        BeforeGetResourceEvent before = new BeforeGetResourceEvent(entity, delegate);
        publisher.publishEvent(before);

        Resource result;
        try {
            result = delegate.getResource(entity);
        }
        catch (Exception e) {
            throw e;
        }

        org.springframework.content.commons.repository.events.AfterGetResourceEvent oldAfter = new org.springframework.content.commons.repository.events.AfterGetResourceEvent(entity, delegate);
        oldAfter.setResult(result);
        publisher.publishEvent(oldAfter);

        AfterGetResourceEvent after = new AfterGetResourceEvent(entity, delegate);
        after.setResult(result);
        publisher.publishEvent(after);

        return result;
    }

    @Override
    public Resource getResource(Object entity, PropertyPath propertyPath) {

        org.springframework.content.commons.repository.events.BeforeGetResourceEvent oldBefore = new org.springframework.content.commons.repository.events.BeforeGetResourceEvent(entity, propertyPath, delegate);
        publisher.publishEvent(oldBefore);

        BeforeGetResourceEvent before = new BeforeGetResourceEvent(entity, propertyPath, delegate);
        publisher.publishEvent(before);

        Resource result;
        try {
            result = delegate.getResource(entity, propertyPath);
        }
        catch (Exception e) {
            throw e;
        }

        org.springframework.content.commons.repository.events.AfterGetResourceEvent oldAfter = new org.springframework.content.commons.repository.events.AfterGetResourceEvent(entity, propertyPath, delegate);
        oldAfter.setResult(result);
        publisher.publishEvent(oldAfter);

        AfterGetResourceEvent after = new AfterGetResourceEvent(entity, propertyPath, delegate);
        after.setResult(result);
        publisher.publishEvent(after);

        return (Resource) after.getResult();
    }

    @Override
    public Resource getResource(Object entity, PropertyPath propertyPath, GetResourceParams params) {
        org.springframework.content.commons.repository.events.BeforeGetResourceEvent oldBefore = new org.springframework.content.commons.repository.events.BeforeGetResourceEvent(entity, propertyPath, delegate);
        publisher.publishEvent(oldBefore);

        BeforeGetResourceEvent before = new BeforeGetResourceEvent(entity, propertyPath, delegate);
        publisher.publishEvent(before);

        Resource result;
        try {
            result = delegate.getResource(entity, propertyPath, params);
        }
        catch (Exception e) {
            throw e;
        }

        org.springframework.content.commons.repository.events.AfterGetResourceEvent oldAfter = new org.springframework.content.commons.repository.events.AfterGetResourceEvent(entity, propertyPath, delegate);
        oldAfter.setResult(result);
        publisher.publishEvent(oldAfter);

        AfterGetResourceEvent after = new AfterGetResourceEvent(entity, propertyPath, delegate);
        after.setResult(result);
        publisher.publishEvent(after);

        return (Resource) after.getResult();
    }

    @Override
    public Resource getResource(Serializable id) {

        org.springframework.content.commons.repository.events.BeforeGetResourceEvent oldBefore = new org.springframework.content.commons.repository.events.BeforeGetResourceEvent(id, delegate);
        publisher.publishEvent(oldBefore);

        BeforeGetResourceEvent before = new BeforeGetResourceEvent(id, delegate);
        publisher.publishEvent(before);

        Resource result;
        try {
            result = delegate.getResource(id);
        }
        catch (Exception e) {
            throw e;
        }

        org.springframework.content.commons.repository.events.AfterGetResourceEvent oldAfter = new org.springframework.content.commons.repository.events.AfterGetResourceEvent(id, delegate);
        oldAfter.setResult(result);
        publisher.publishEvent(oldAfter);

        AfterGetResourceEvent after = new AfterGetResourceEvent(id, delegate);
        after.setResult(result);
        publisher.publishEvent(after);

        return result;
    }

    @Override
    public void associate(Object entity, Serializable id) {

        org.springframework.content.commons.repository.events.BeforeAssociateEvent oldBefore = new org.springframework.content.commons.repository.events.BeforeAssociateEvent(entity, delegate);
        org.springframework.content.commons.repository.events.AfterAssociateEvent oldAfter = new org.springframework.content.commons.repository.events.AfterAssociateEvent(entity, delegate);

        BeforeAssociateEvent before = new BeforeAssociateEvent(entity, delegate);
        AfterAssociateEvent after = new AfterAssociateEvent(entity, delegate);

        publisher.publishEvent(before);
        publisher.publishEvent(oldBefore);

        try {
            delegate.associate(entity, id);
        }
        catch (Exception e) {
            throw e;
        }

        if (after != null) {
            publisher.publishEvent(after);
        }

        if (oldAfter != null) {
            publisher.publishEvent(oldAfter);
        }
    }

    @Override
    public void associate(Object entity, PropertyPath propertyPath, Serializable id) {

        org.springframework.content.commons.repository.events.BeforeAssociateEvent oldBefore = new org.springframework.content.commons.repository.events.BeforeAssociateEvent(entity, propertyPath, delegate);
        org.springframework.content.commons.repository.events.AfterAssociateEvent oldAfter = new org.springframework.content.commons.repository.events.AfterAssociateEvent(entity, propertyPath, delegate);

        BeforeAssociateEvent before = new BeforeAssociateEvent(entity, propertyPath, delegate);
        AfterAssociateEvent after = new AfterAssociateEvent(entity, propertyPath, delegate);

        publisher.publishEvent(before);
        publisher.publishEvent(oldBefore);

        try {
            delegate.associate(entity, propertyPath, id);
        }
        catch (Exception e) {
            throw e;
        }

        if (after != null) {
            publisher.publishEvent(after);
        }

        if (oldAfter != null) {
            publisher.publishEvent(oldAfter);
        }
    }

    @Override
    public void unassociate(Object entity) {

        org.springframework.content.commons.repository.events.BeforeUnassociateEvent oldBefore = new org.springframework.content.commons.repository.events.BeforeUnassociateEvent(entity, delegate);
        org.springframework.content.commons.repository.events.AfterUnassociateEvent oldAfter = new org.springframework.content.commons.repository.events.AfterUnassociateEvent(entity, delegate);

        BeforeUnassociateEvent before = new BeforeUnassociateEvent(entity, delegate);
        AfterUnassociateEvent after = new AfterUnassociateEvent(entity, delegate);

        publisher.publishEvent(before);
        publisher.publishEvent(oldBefore);

        try {
            delegate.unassociate(entity);
        }
        catch (Exception e) {
            throw e;
        }

        if (after != null) {
            publisher.publishEvent(after);
        }

        if (oldAfter != null) {
            publisher.publishEvent(oldAfter);
        }
    }

    @Override
    public void unassociate(Object entity, PropertyPath propertyPath) {

        org.springframework.content.commons.repository.events.BeforeUnassociateEvent oldBefore = new org.springframework.content.commons.repository.events.BeforeUnassociateEvent(entity, propertyPath, delegate);
        org.springframework.content.commons.repository.events.AfterUnassociateEvent oldAfter = new org.springframework.content.commons.repository.events.AfterUnassociateEvent(entity, propertyPath, delegate);

        BeforeUnassociateEvent before = new BeforeUnassociateEvent(entity, propertyPath, delegate);
        AfterUnassociateEvent after = new AfterUnassociateEvent(entity, propertyPath, delegate);

        publisher.publishEvent(before);
        publisher.publishEvent(oldBefore);

        try {
            delegate.unassociate(entity, propertyPath);
        }
        catch (Exception e) {
            throw e;
        }

        if (after != null) {
            publisher.publishEvent(after);
        }

        if (oldAfter != null) {
            publisher.publishEvent(oldAfter);
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
