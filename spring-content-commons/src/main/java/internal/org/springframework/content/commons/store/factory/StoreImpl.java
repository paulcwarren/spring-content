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
import java.util.function.Supplier;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.repository.Store;
import org.springframework.content.commons.store.*;
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

public class StoreImpl implements org.springframework.content.commons.repository.ContentStore<Object, Serializable>, ContentStore<Object, Serializable> {

    private static final Log logger = LogFactory.getLog(StoreImpl.class);

    private final Store<Serializable> delegate;
    private final ApplicationEventPublisher publisher;
    private final Path copyContentRootPath;

    public StoreImpl(Store<Serializable> delegate, ApplicationEventPublisher publisher, Path copyContentRootPath) {
        this.delegate = delegate;
        this.publisher = publisher;
        this.copyContentRootPath = copyContentRootPath;
    }

    @Override
    public Object setContent(Object entity, InputStream content) {
        return this.internalSetContent(entity, null, content, () -> {
            try {
                if (delegate instanceof org.springframework.content.commons.store.ContentStore) {
                    return ((org.springframework.content.commons.store.ContentStore)(delegate)).setContent(entity, content);
                } else {
                    return ((org.springframework.content.commons.repository.ContentStore)(delegate)).setContent(entity, content);
                }
            }
            catch (Exception e) {
                throw e;
            }
        });
    }

    @Override
    public Object setContent(Object entity, PropertyPath propertyPath, InputStream content) {
        return this.internalSetContent(entity, propertyPath, content, () -> {
            try {
                if (delegate instanceof org.springframework.content.commons.store.ContentStore) {
                    return ((org.springframework.content.commons.store.ContentStore)(delegate)).setContent(entity, propertyPath, content);
                } else {
                    return ((org.springframework.content.commons.repository.ContentStore)(delegate)).setContent(entity, propertyPath, content);
                }
            }
            catch (Exception e) {
                throw e;
            }
        });
    }

    @Override
    public Object setContent(Object entity, PropertyPath propertyPath, InputStream content, long contentLen) {
        return this.internalSetContent(entity, propertyPath, content, () -> {
            try {
                if (delegate instanceof org.springframework.content.commons.store.ContentStore) {
                    return ((org.springframework.content.commons.store.ContentStore)(delegate)).setContent(entity, propertyPath, content, contentLen);
                } else {
                    return ((org.springframework.content.commons.repository.ContentStore)(delegate)).setContent(entity, propertyPath, content, contentLen);
                }
            }
            catch (Exception e) {
                throw e;
            }
        });
    }

    @Override
    public Object setContent(Object entity, PropertyPath propertyPath, InputStream content, org.springframework.content.commons.repository.SetContentParams params) {
        return this.internalSetContent(entity, propertyPath, content, () -> {
            try {
                return ((org.springframework.content.commons.repository.ContentStore)(delegate)).setContent(entity, propertyPath, content, params);
            }
            catch (Exception e) {
                throw e;
            }
        });
    }

    @Override
    public Object setContent(Object entity, PropertyPath propertyPath, InputStream content, SetContentParams params) {
        return this.internalSetContent(entity, propertyPath, content, () -> {
            try {
                return ((org.springframework.content.commons.store.ContentStore) delegate).setContent(entity, propertyPath, content, params);
            }
            catch (Exception e) {
                throw e;
            }
        });
    }

    public Object internalSetContent(Object property, PropertyPath propertyPath, InputStream content, Supplier invocation) {
        Object result = null;

        File contentCopy = null;
        TeeInputStream contentCopyStream = null;
        try {
            contentCopy = Files.createTempFile(copyContentRootPath, "contentCopy", ".tmp").toFile();
            contentCopyStream = new TeeInputStream(content, new FileOutputStream(contentCopy), true);

            org.springframework.content.commons.repository.events.BeforeSetContentEvent oldBefore = null;
            BeforeSetContentEvent before = null;

            oldBefore = new org.springframework.content.commons.repository.events.BeforeSetContentEvent(property, propertyPath, delegate, contentCopyStream);
            publisher.publishEvent(oldBefore);

            ContentStore contentStore = castToContentStore(delegate);
            if (contentStore != null) {
                before = new BeforeSetContentEvent(property, propertyPath, contentStore, contentCopyStream);
                publisher.publishEvent(before);
            }

            // inputstream was processed and replaced
            if (oldBefore != null && oldBefore.getInputStream() != null && oldBefore.getInputStream().equals(contentCopyStream) == false) {
                content = oldBefore.getInputStream();
            }
            else if (before != null && before.getInputStream() != null && before.getInputStream().equals(contentCopyStream) == false) {
                content = before.getInputStream();
            }
            // content was processed but not replaced
            else if (contentCopyStream != null && contentCopyStream.isDirty()) {
                while (contentCopyStream.read(new byte[4096]) != -1) {
                }
                content = new FileInputStream(contentCopy);
            }

            result = invocation.get();

            org.springframework.content.commons.repository.events.AfterSetContentEvent oldAfter = new org.springframework.content.commons.repository.events.AfterSetContentEvent(property, propertyPath, delegate);
            oldAfter.setResult(result);
            publisher.publishEvent(oldAfter);

            if (contentStore != null) {
                AfterSetContentEvent after = new AfterSetContentEvent(property, propertyPath, contentStore);
                after.setResult(result);
                publisher.publishEvent(after);
            }
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
    public Object setContent(Object entity, Resource resourceContent) {
        return this.internalSetContent(entity, null, resourceContent, () -> {
            try {
                if (delegate instanceof org.springframework.content.commons.store.ContentStore) {
                    return ((org.springframework.content.commons.store.ContentStore)(delegate)).setContent(entity, resourceContent);
                } else {
                    return ((org.springframework.content.commons.repository.ContentStore)(delegate)).setContent(entity, resourceContent);
                }
            }
            catch (Exception e) {
                throw e;
            }
        });
    }

    @Override
    public Object setContent(Object entity, PropertyPath propertyPath, Resource resourceContent) {
        return this.internalSetContent(entity, propertyPath, resourceContent, () -> {
            try {
                if (delegate instanceof org.springframework.content.commons.store.ContentStore) {
                    return ((org.springframework.content.commons.store.ContentStore)(delegate)).setContent(entity, propertyPath, resourceContent);
                } else {
                    return ((org.springframework.content.commons.repository.ContentStore)(delegate)).setContent(entity, propertyPath, resourceContent);
                }
            }
            catch (Exception e) {
                throw e;
            }
        });
    }

    public Object internalSetContent(Object property, PropertyPath propertyPath, Resource resourceContent, Supplier invocation) {
        org.springframework.content.commons.repository.events.BeforeSetContentEvent oldBefore = new org.springframework.content.commons.repository.events.BeforeSetContentEvent(property, propertyPath, delegate, resourceContent);
        publisher.publishEvent(oldBefore);

        ContentStore contentStore = castToContentStore(delegate);
        if (contentStore != null) {
            BeforeSetContentEvent before = new BeforeSetContentEvent(property, propertyPath, contentStore, resourceContent);
            publisher.publishEvent(before);
        }

        Object result = invocation.get();

        org.springframework.content.commons.repository.events.AfterSetContentEvent oldAfter = new org.springframework.content.commons.repository.events.AfterSetContentEvent(property, propertyPath, delegate);
        oldAfter.setResult(result);
        publisher.publishEvent(oldAfter);

        if (contentStore != null) {
            AfterSetContentEvent after = new AfterSetContentEvent(property, propertyPath, contentStore);
            after.setResult(result);
            publisher.publishEvent(after);
        }
        return result;
    }

    @Override
    public Object unsetContent(Object entity) {
        return this.internalUnsetContent(entity, null,
            () -> {
                Object result;
                try {
                    if (delegate instanceof org.springframework.content.commons.store.ContentStore) {
                        return ((org.springframework.content.commons.store.ContentStore)(delegate)).unsetContent(entity);
                    } else {
                        return ((org.springframework.content.commons.repository.ContentStore)(delegate)).unsetContent(entity);
                    }
                }
                catch (Exception e) {
                    throw e;
                }
            });
    }

    @Override
    public Object unsetContent(Object entity, PropertyPath propertyPath) {
        return this.internalUnsetContent(entity, propertyPath,
            () -> {
                Object result;
                try {
                    if (delegate instanceof org.springframework.content.commons.store.ContentStore) {
                        return ((org.springframework.content.commons.store.ContentStore)(delegate)).unsetContent(entity, propertyPath);
                    } else {
                        return ((org.springframework.content.commons.repository.ContentStore)(delegate)).unsetContent(entity, propertyPath);
                    }
                }
                catch (Exception e) {
                    throw e;
                }
            });
    }

    @Override
    public Object unsetContent(Object entity, PropertyPath propertyPath, org.springframework.content.commons.repository.UnsetContentParams params) {
        return this.internalUnsetContent(entity, propertyPath,
        () -> {
            Object result;
            try {
                if (delegate instanceof org.springframework.content.commons.store.ContentStore) {
                    int ordinal = params.getDisposition().ordinal();
                    UnsetContentParams params1 = UnsetContentParams.builder()
                            .disposition(UnsetContentParams.Disposition.values()[ordinal])
                            .build();
                    return ((org.springframework.content.commons.store.ContentStore)(delegate)).unsetContent(entity, propertyPath, params1);
                } else {
                    return ((org.springframework.content.commons.repository.ContentStore)(delegate)).unsetContent(entity, propertyPath, params);
                }
            }
            catch (Exception e) {
                throw e;
            }
        });
    }

    @Override
    public Object unsetContent(Object entity, PropertyPath propertyPath, UnsetContentParams params) {
        return this.internalUnsetContent(entity, propertyPath,
        () -> {
            Object result;
            try {
                if (delegate instanceof org.springframework.content.commons.store.ContentStore) {
                    return ((org.springframework.content.commons.store.ContentStore)(delegate)).unsetContent(entity, propertyPath, params);
                } else {
                    int ordinal = params.getDisposition().ordinal();
                    org.springframework.content.commons.repository.UnsetContentParams params1 = org.springframework.content.commons.repository.UnsetContentParams.builder()
                            .disposition(org.springframework.content.commons.repository.UnsetContentParams.Disposition.values()[ordinal])
                            .build();
                    return ((org.springframework.content.commons.repository.ContentStore)(delegate)).unsetContent(entity, propertyPath, params1);
                }
            }
            catch (Exception e) {
                throw e;
            }
        });
    }

    public Object internalUnsetContent(Object entity, PropertyPath propertyPath, Supplier invocation) {

        org.springframework.content.commons.repository.events.BeforeUnsetContentEvent oldBefore = new org.springframework.content.commons.repository.events.BeforeUnsetContentEvent(entity, propertyPath, delegate);
        publisher.publishEvent(oldBefore);

        ContentStore contentStore = castToContentStore(delegate);
        if (contentStore != null) {
            BeforeUnsetContentEvent before = new BeforeUnsetContentEvent(entity, propertyPath, contentStore);
            publisher.publishEvent(before);
        }

        Object result = invocation.get();

        org.springframework.content.commons.repository.events.AfterUnsetContentEvent oldAfter = new org.springframework.content.commons.repository.events.AfterUnsetContentEvent(entity, propertyPath, delegate);
        oldAfter.setResult(result);
        publisher.publishEvent(oldAfter);

        if (contentStore != null) {
            AfterUnsetContentEvent after = new AfterUnsetContentEvent(entity, propertyPath, contentStore);
            after.setResult(result);
            publisher.publishEvent(after);
        }

        return result;
    }

    @Override
    public InputStream getContent(Object entity) {
        return this.internalGetContent(entity, null, () -> {
            try {
                if (delegate instanceof org.springframework.content.commons.store.ContentStore) {
                    return ((org.springframework.content.commons.store.ContentStore)(delegate)).getContent(entity);
                } else {
                    return ((org.springframework.content.commons.repository.ContentStore)(delegate)).getContent(entity);
                }
            } catch (Exception e) {
                throw e;
            }
        });
    }

    @Override
    public InputStream getContent(Object entity, PropertyPath propertyPath) {
        return this.internalGetContent(entity, propertyPath, () -> {
            try {
                if (delegate instanceof org.springframework.content.commons.store.ContentStore) {
                    return ((org.springframework.content.commons.store.ContentStore)(delegate)).getContent(entity, propertyPath);
                } else {
                    return ((org.springframework.content.commons.repository.ContentStore)(delegate)).getContent(entity, propertyPath);
                }
            } catch (Exception e) {
                throw e;
            }
        });
    }

    public InputStream internalGetContent(Object entity, PropertyPath propertyPath, Supplier<InputStream> invocation) {
        org.springframework.content.commons.repository.events.BeforeGetContentEvent oldBefore = new org.springframework.content.commons.repository.events.BeforeGetContentEvent(entity, propertyPath, delegate);
        publisher.publishEvent(oldBefore);

        ContentStore contentStore = castToContentStore(delegate);
        if (contentStore != null) {
            BeforeGetContentEvent before = new BeforeGetContentEvent(entity, propertyPath, contentStore);
            publisher.publishEvent(before);
        }

        InputStream result = invocation.get();

        org.springframework.content.commons.repository.events.AfterGetContentEvent oldAfter = new org.springframework.content.commons.repository.events.AfterGetContentEvent(entity, propertyPath, delegate);
        oldAfter.setResult(result);
        publisher.publishEvent(oldAfter);
        if (oldAfter.getResult() != null) {
            result = (InputStream) oldAfter.getResult();
        }

        if (contentStore != null) {
            AfterGetContentEvent after = new AfterGetContentEvent(entity, propertyPath, contentStore);
            after.setResult(result);
            publisher.publishEvent(after);
            if (after.getResult() != null) {
                result = (InputStream) after.getResult();
            }
        }

        return result;
    }

    @Override
    public Resource getResource(Object entity) {
        return this.internalGetResource(entity, null, () -> {
            try {
                if (delegate instanceof org.springframework.content.commons.store.Store) {
                    return ((org.springframework.content.commons.store.AssociativeStore)(delegate)).getResource(entity);
                } else {
                    return ((org.springframework.content.commons.repository.AssociativeStore) (delegate)).getResource(entity);
                }
            }
            catch (Exception e) {
                throw e;
            }
        });
    }

    @Override
    public Resource getResource(Object entity, PropertyPath propertyPath) {
        return this.internalGetResource(entity, propertyPath, () -> {
            try {
                if (delegate instanceof org.springframework.content.commons.store.AssociativeStore) {
                    return ((org.springframework.content.commons.store.AssociativeStore)(delegate)).getResource(entity, propertyPath);
                } else {
                    return ((org.springframework.content.commons.repository.AssociativeStore)(delegate)).getResource(entity, propertyPath);
                }
            }
            catch (Exception e) {
                throw e;
            }
        });
    }

    @Override
    public Resource getResource(Object entity, PropertyPath propertyPath, org.springframework.content.commons.repository.GetResourceParams oldParams) {
        return this.internalGetResource(entity, propertyPath, () -> {
            try {
                return ((org.springframework.content.commons.repository.AssociativeStore)(delegate)).getResource(entity, propertyPath, oldParams);
            }
            catch (Exception e) {
                throw e;
            }
        });
    }

    @Override
    public Resource getResource(Object entity, PropertyPath propertyPath, GetResourceParams params) {
        return this.internalGetResource(entity, propertyPath, () -> {
            try {
                return ((org.springframework.content.commons.store.AssociativeStore) delegate).getResource(entity, propertyPath, params);
            }
            catch (Exception e) {
                throw e;
            }
        });
    }

    public Resource internalGetResource(Object entity, PropertyPath propertyPath, Supplier<Resource> invocation) {
        org.springframework.content.commons.repository.events.BeforeGetResourceEvent oldBefore = new org.springframework.content.commons.repository.events.BeforeGetResourceEvent(entity, propertyPath, delegate);
        publisher.publishEvent(oldBefore);

        ContentStore contentStore = castToContentStore(delegate);
        if (contentStore != null) {
            BeforeGetResourceEvent before = new BeforeGetResourceEvent(entity, propertyPath, contentStore);
            publisher.publishEvent(before);
        }

        Resource result = invocation.get();

        org.springframework.content.commons.repository.events.AfterGetResourceEvent oldAfter = new org.springframework.content.commons.repository.events.AfterGetResourceEvent(entity, propertyPath, delegate);
        oldAfter.setResult(result);
        publisher.publishEvent(oldAfter);
        if (oldAfter.getResult() != null) {
            result = (Resource) oldAfter.getResult();
        }

        if (contentStore != null) {
            AfterGetResourceEvent after = new AfterGetResourceEvent(entity, propertyPath, contentStore);
            after.setResult(result);
            publisher.publishEvent(after);
            if (after.getStore() != null) {
                result = (Resource) after.getResult();
            }
        }

        return result;
    }

    @Override
    public Resource getResource(Serializable id) {

        org.springframework.content.commons.repository.events.BeforeGetResourceEvent oldBefore = new org.springframework.content.commons.repository.events.BeforeGetResourceEvent(id, delegate);
        publisher.publishEvent(oldBefore);

        ContentStore contentStore = castToContentStore(delegate);
        if (contentStore != null) {
            BeforeGetResourceEvent before = new BeforeGetResourceEvent(id, contentStore);
            publisher.publishEvent(before);
        }

        Resource result;
        try {
            if (delegate instanceof org.springframework.content.commons.store.Store) {
                result = ((org.springframework.content.commons.store.Store)(delegate)).getResource(id);
            } else {
                result = ((org.springframework.content.commons.repository.Store)(delegate)).getResource(id);
            }
        }
        catch (Exception e) {
            throw e;
        }

        org.springframework.content.commons.repository.events.AfterGetResourceEvent oldAfter = new org.springframework.content.commons.repository.events.AfterGetResourceEvent(id, delegate);
        oldAfter.setResult(result);
        publisher.publishEvent(oldAfter);

        if (contentStore != null) {
            AfterGetResourceEvent after = new AfterGetResourceEvent(id, contentStore);
            after.setResult(result);
            publisher.publishEvent(after);
        }

        return result;
    }

    @Override
    public void associate(Object entity, Serializable id) {

        org.springframework.content.commons.repository.events.BeforeAssociateEvent oldBefore = new org.springframework.content.commons.repository.events.BeforeAssociateEvent(entity, delegate);
        publisher.publishEvent(oldBefore);

        ContentStore contentStore = castToContentStore(delegate);
        if (contentStore != null) {
            BeforeAssociateEvent before = new BeforeAssociateEvent(entity, contentStore);
            publisher.publishEvent(before);
        }


        try {
            if (delegate instanceof org.springframework.content.commons.store.AssociativeStore) {
                ((org.springframework.content.commons.store.AssociativeStore)(delegate)).associate(entity, id);
            } else {
                ((org.springframework.content.commons.repository.AssociativeStore)(delegate)).associate(entity, id);
            }
        }
        catch (Exception e) {
            throw e;
        }

        org.springframework.content.commons.repository.events.AfterAssociateEvent oldAfter = new org.springframework.content.commons.repository.events.AfterAssociateEvent(entity, delegate);
        publisher.publishEvent(oldAfter);

        if (contentStore != null) {
            AfterAssociateEvent after = new AfterAssociateEvent(entity, contentStore);
            publisher.publishEvent(after);
        }
    }

    @Override
    public void associate(Object entity, PropertyPath propertyPath, Serializable id) {

        org.springframework.content.commons.repository.events.BeforeAssociateEvent oldBefore = new org.springframework.content.commons.repository.events.BeforeAssociateEvent(entity, propertyPath, delegate);
        publisher.publishEvent(oldBefore);

        ContentStore contentStore = castToContentStore(delegate);
        if (contentStore != null) {
            BeforeAssociateEvent before = new BeforeAssociateEvent(entity, propertyPath, contentStore);
            publisher.publishEvent(before);
        }

        try {
            if (delegate instanceof org.springframework.content.commons.store.AssociativeStore) {
                ((org.springframework.content.commons.store.AssociativeStore)(delegate)).associate(entity, propertyPath, id);
            } else {
                ((org.springframework.content.commons.repository.AssociativeStore)(delegate)).associate(entity, propertyPath, id);
            }
        }
        catch (Exception e) {
            throw e;
        }

        org.springframework.content.commons.repository.events.AfterAssociateEvent oldAfter = new org.springframework.content.commons.repository.events.AfterAssociateEvent(entity, propertyPath, delegate);
        publisher.publishEvent(oldAfter);

        if (contentStore != null) {
            AfterAssociateEvent after = new AfterAssociateEvent(entity, propertyPath, contentStore);
            publisher.publishEvent(after);
        }
    }

    @Override
    public void unassociate(Object entity) {

        org.springframework.content.commons.repository.events.BeforeUnassociateEvent oldBefore = new org.springframework.content.commons.repository.events.BeforeUnassociateEvent(entity, delegate);
        publisher.publishEvent(oldBefore);

        ContentStore contentStore = castToContentStore(delegate);
        if (contentStore != null) {
            BeforeUnassociateEvent before = new BeforeUnassociateEvent(entity, contentStore);
            publisher.publishEvent(before);
        }

        try {
            if (delegate instanceof org.springframework.content.commons.store.AssociativeStore) {
                ((org.springframework.content.commons.store.AssociativeStore)(delegate)).unassociate(entity);
            } else {
                ((org.springframework.content.commons.repository.AssociativeStore)(delegate)).unassociate(entity);
            }
        }
        catch (Exception e) {
            throw e;
        }

        org.springframework.content.commons.repository.events.AfterUnassociateEvent oldAfter = new org.springframework.content.commons.repository.events.AfterUnassociateEvent(entity, delegate);
        publisher.publishEvent(oldAfter);

        if (contentStore != null) {
            AfterUnassociateEvent after = new AfterUnassociateEvent(entity, contentStore);
            publisher.publishEvent(after);
        }
    }

    @Override
    public void unassociate(Object entity, PropertyPath propertyPath) {

        org.springframework.content.commons.repository.events.BeforeUnassociateEvent oldBefore = new org.springframework.content.commons.repository.events.BeforeUnassociateEvent(entity, propertyPath, delegate);
        publisher.publishEvent(oldBefore);

        ContentStore contentStore = castToContentStore(delegate);
        if (contentStore != null) {
            BeforeUnassociateEvent before = new BeforeUnassociateEvent(entity, propertyPath, contentStore);
            publisher.publishEvent(before);
        }

        try {
            if (delegate instanceof org.springframework.content.commons.store.AssociativeStore) {
                ((org.springframework.content.commons.store.AssociativeStore)(delegate)).unassociate(entity, propertyPath);
            } else {
                ((org.springframework.content.commons.repository.AssociativeStore)(delegate)).unassociate(entity, propertyPath);
            }
        }
        catch (Exception e) {
            throw e;
        }

        org.springframework.content.commons.repository.events.AfterUnassociateEvent oldAfter = new org.springframework.content.commons.repository.events.AfterUnassociateEvent(entity, propertyPath, delegate);
        publisher.publishEvent(oldAfter);

        if (contentStore != null) {
            AfterUnassociateEvent after = new AfterUnassociateEvent(entity, propertyPath, contentStore);
            publisher.publishEvent(after);
        }
    }

    private <SID extends Serializable> ContentStore<Object, SID> castToContentStore(Store<Serializable> delegate) {
        if (delegate instanceof ContentStore == false) {
            return null;
        }
        return (ContentStore)delegate;
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
