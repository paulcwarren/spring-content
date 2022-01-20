package internal.org.springframework.content.rest.io;

import static java.lang.String.format;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.time.Instant;
import java.util.Date;
import java.util.stream.Stream;

import org.springframework.content.commons.mappingcontext.ContentProperty;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.renditions.Renderable;
import org.springframework.content.commons.repository.AssociativeStore;
import org.springframework.content.commons.storeservice.StoreInfo;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;

/**
 * Represents a Spring Content Resource that is associated with a Spring Data Entity.
 *
 * Overrides the ContentLength and LastModifiedDate with the values stored on the
 * Spring Data entity, rather than the values of the actual content itself (these are
 * used as fallback values though).
 *
 * Also sets appropriate headers to pass the Spring Data Entity recorded filename, if exists.
 */
public class AssociatedStorePropertyPathResourceImpl<S> extends AssociatedStoreResourceImpl<S> implements AssociatedStorePropertyResource<S> {

//    private Object property;
    private ContentProperty property;
    private boolean embedded;

    public AssociatedStorePropertyPathResourceImpl(StoreInfo info, ContentProperty property, S entity, Resource original) {

        super(info, entity, original);
        this.property = property;
        this.embedded = true;
    }

    @Override
    public S getAssociation() {

        if (embedded) {
            return super.getAssociation();
        }

//        return (S)property;
        throw new UnsupportedOperationException();
    }

    public ContentProperty getContentProperty() {
        return this.property;
    }

    @Override
    public boolean embedded() {

        return embedded;
    }

    @Override
    public Object getProperty() {

//        return property;
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isRenderableAs(org.springframework.util.MimeType mimeType) {

        if (Renderable.class.isAssignableFrom(this.getStoreInfo().getInterface())) {

            Renderable renderer = (Renderable)this.getStoreInfo().getImplementation(AssociativeStore.class);
            return renderer.hasRendition(getAssociation(), PropertyPath.from(property.getContentPropertyPath()), mimeType.toString());
        }

        return false;
    }

    @Override
    public InputStream renderAs(org.springframework.util.MimeType mimeType) {

        if (Renderable.class.isAssignableFrom(this.getStoreInfo().getInterface())) {

            Renderable renderer = (Renderable)this.getStoreInfo().getImplementation(AssociativeStore.class);
            return renderer.getRendition(getAssociation(), PropertyPath.from(property.getContentPropertyPath()), mimeType.toString());
        }

        return null;
    }

    @Override
    public Object getETag() {

        Object etag = null;

// TODO: can we remove this is all properties are effectively embedded?
//        if (property != null) {
//            etag = BeanUtils.getFieldWithAnnotation(property, Version.class);
//        }

        if (etag == null) {
            etag = super.getETag();
        }

        return etag.toString();
    }

    @Override
    public MediaType getMimeType() {

        Object mimeType = null;

        mimeType = this.property.getMimeType(this.getAssociation());

        return MediaType.valueOf(mimeType != null ? mimeType.toString() : MediaType.ALL_VALUE);
    }

    @Override
    public long contentLength() throws IOException {

// TODO: can we remove this is all properties are effectively embedded?
//        Long contentLength = null; (Long) BeanUtils.getFieldWithAnnotation(property, ContentLength.class);
        Long contentLength = (Long) this.property.getContentLength(this.getAssociation());
        if (contentLength == null) {
            contentLength = getDelegate().contentLength();
        }
        return contentLength;
    }

    @Override
    public long lastModified() throws IOException {

// TODO: can we remove this is all properties are effectively embedded?
        Object lastModified = null; //BeanUtils.getFieldWithAnnotation(property, LastModifiedDate.class);
        if (lastModified == null) {
            return getDelegate().lastModified();
        }

        return Stream.of(lastModified)
        .map(it -> getConversionService().convert(it, Date.class))//
        .map(it -> getConversionService().convert(it, Instant.class))//
        .map(it -> it.toEpochMilli())
        .findFirst().orElseThrow(() -> new IllegalArgumentException(format("Invalid data type for @LastModifiedDate on Entity %s", this.getAssociation())));
    }

    @Override
    public HttpHeaders getResponseHeaders() {

        HttpHeaders headers = new HttpHeaders();

        // Modified to show download
        Object originalFileName = this.property.getOriginalFileName(this.getAssociation());
        if (originalFileName != null && StringUtils.hasText(originalFileName.toString())) {
            ContentDisposition.Builder builder = ContentDisposition.builder("form-data").name( "attachment").filename((String)originalFileName, Charset.defaultCharset());
            headers.setContentDisposition(builder.build());
        }
        return headers;
    }
}
