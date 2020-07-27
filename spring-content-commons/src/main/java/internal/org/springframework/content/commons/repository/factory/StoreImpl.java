package internal.org.springframework.content.commons.repository.factory;

import lombok.Getter;
import org.springframework.content.commons.io.FileRemover;
import org.springframework.content.commons.io.ObservableInputStream;
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.file.Files;

public class StoreImpl implements ContentStore<Object, Serializable> {

    private final ContentStore<Object, Serializable> delegate;
    private final ApplicationEventPublisher publisher;

    public StoreImpl(ContentStore<Object, Serializable> delegate, ApplicationEventPublisher publisher) {
        this.delegate = delegate;
        this.publisher = publisher;
    }

    @Override
    public Object setContent(Object property, InputStream content) {

        Object result = null;

        try {
            File tmpStreamFile = Files.createTempFile("sc", "bsce").toFile();
            TeeInputStream eventStream = new TeeInputStream(content, new FileOutputStream(tmpStreamFile), true);
            BeforeSetContentEvent before = new BeforeSetContentEvent(property, delegate, eventStream);
            AfterSetContentEvent after = new AfterSetContentEvent(property, delegate);

            publisher.publishEvent(before);

            if (eventStream != null && eventStream.isDirty()) {
                while (eventStream.read(new byte[4096]) != -1) {
                }
                eventStream.close();
                content = new ObservableInputStream(new FileInputStream(tmpStreamFile), new FileRemover(tmpStreamFile));
            }

            try {
                result = delegate.setContent(property, content);
            }
            catch (Exception e) {
                throw e;
            }

            if (after != null) {
                after.setResult(result);
                publisher.publishEvent(after);
            }
        } catch (FileNotFoundException fileNotFoundException) {
            fileNotFoundException.printStackTrace();
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }

        return result;
    }

    @Override
    public Object setContent(Object property, Resource resourceContent) {

        BeforeSetContentEvent before = new BeforeSetContentEvent(property, delegate, resourceContent);
        AfterSetContentEvent after = new AfterSetContentEvent(property, delegate);

        publisher.publishEvent(before);

        Object result;
        try {
            result = delegate.setContent(property, resourceContent);
        }
        catch (Exception e) {
            throw e;
        }

        if (after != null) {
            after.setResult(result);
            publisher.publishEvent(after);
        }

        return result;
    }

    @Override
    public Object unsetContent(Object property) {
        BeforeUnsetContentEvent before = new BeforeUnsetContentEvent(property, delegate);
        AfterUnsetContentEvent after = new AfterUnsetContentEvent(property, delegate);

        publisher.publishEvent(before);

        Object result;
        try {
            result = delegate.unsetContent(property);
        }
        catch (Exception e) {
            throw e;
        }

        if (after != null) {
            after.setResult(result);
            publisher.publishEvent(after);
        }

        return result;
    }

    @Override
    public InputStream getContent(Object property) {
        BeforeGetContentEvent before = new BeforeGetContentEvent(property, delegate);
        AfterGetContentEvent after = new AfterGetContentEvent(property, delegate);

        publisher.publishEvent(before);

        InputStream result;
        try {
            result = delegate.getContent(property);
        }
        catch (Exception e) {
            throw e;
        }

        if (after != null) {
            after.setResult(result);
            publisher.publishEvent(after);
        }

        return result;
    }

    @Override
    public Resource getResource(Object entity) {

        BeforeGetResourceEvent before = new BeforeGetResourceEvent(entity, delegate);
        AfterGetResourceEvent after = new AfterGetResourceEvent(entity, delegate);

        publisher.publishEvent(before);

        Resource result;
        try {
            result = delegate.getResource(entity);
        }
        catch (Exception e) {
            throw e;
        }

        if (after != null) {
            after.setResult(result);
            publisher.publishEvent(after);
        }

        return result;
    }

    @Override
    public Resource getResource(Serializable id) {

        BeforeGetResourceEvent before = new BeforeGetResourceEvent(id, delegate);
        AfterGetResourceEvent after = new AfterGetResourceEvent(id, delegate);

        publisher.publishEvent(before);

        Resource result;
        try {
            result = delegate.getResource(id);
        }
        catch (Exception e) {
            throw e;
        }

        if (after != null) {
            after.setResult(result);
            publisher.publishEvent(after);
        }

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
    }
}
