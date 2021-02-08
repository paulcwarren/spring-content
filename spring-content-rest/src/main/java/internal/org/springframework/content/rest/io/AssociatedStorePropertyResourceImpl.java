package internal.org.springframework.content.rest.io;

import static java.lang.String.format;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.time.Instant;
import java.util.Date;
import java.util.stream.Stream;

import javax.persistence.Version;

import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.annotations.MimeType;
import org.springframework.content.commons.annotations.OriginalFileName;
import org.springframework.content.commons.renditions.Renderable;
import org.springframework.content.commons.repository.AssociativeStore;
import org.springframework.content.commons.storeservice.StoreInfo;
import org.springframework.content.commons.utils.BeanUtils;
import org.springframework.core.io.Resource;
import org.springframework.data.annotation.LastModifiedDate;
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
public class AssociatedStorePropertyResourceImpl<S> extends AssociatedStoreResourceImpl<S> implements AssociatedStorePropertyResource<S> {

    private Object property;
    private boolean embedded;

    public AssociatedStorePropertyResourceImpl(StoreInfo info, Object property, boolean embedded, S entity, Resource original) {

        super(info, entity, original);
        this.property = property;
        this.embedded = embedded;
    }

    @Override
    public S getAssociation() {

        if (embedded) {
            return super.getAssociation();
        }

        return (S)property;
    }

    @Override
    public boolean embedded() {

        return embedded;
    }

    @Override
    public Object getProperty() {

        return property;
    }

    @Override
    public boolean isRenderableAs(org.springframework.util.MimeType mimeType) {

        if (Renderable.class.isAssignableFrom(this.getStoreInfo().getInterface())) {

            Renderable renderer = (Renderable)this.getStoreInfo().getImplementation(AssociativeStore.class);
            return renderer.hasRendition(property, mimeType.toString());
        }

        return false;
    }

    @Override
    public InputStream renderAs(org.springframework.util.MimeType mimeType) {

        if (Renderable.class.isAssignableFrom(this.getStoreInfo().getInterface())) {

            Renderable renderer = (Renderable)this.getStoreInfo().getImplementation(AssociativeStore.class);
            return renderer.getRendition(property, mimeType.toString());
        }

        return null;
    }

    @Override
    public Object getETag() {

        Object etag = null;

        if (property != null) {
            etag = BeanUtils.getFieldWithAnnotation(property, Version.class);
        }

        if (etag == null) {
            etag = super.getETag();
        }

        return etag.toString();
    }

    @Override
    public MediaType getMimeType() {

        Object mimeType = null;

        mimeType = BeanUtils.getFieldWithAnnotation(property, MimeType.class);

        return MediaType.valueOf(mimeType != null ? mimeType.toString() : MediaType.ALL_VALUE);
    }

    @Override
    public long contentLength() throws IOException {

        Long contentLength = (Long) BeanUtils.getFieldWithAnnotation(property, ContentLength.class);
        if (contentLength == null) {
            contentLength = getDelegate().contentLength();
        }
        return contentLength;
    }

    @Override
    public long lastModified() throws IOException {

        Object lastModified = BeanUtils.getFieldWithAnnotation(property, LastModifiedDate.class);
        if (lastModified == null) {
            return getDelegate().lastModified();
        }

        return Stream.of(lastModified)
        .map(it -> getConversionService().convert(it, Date.class))//
        .map(it -> getConversionService().convert(it, Instant.class))//
        .map(it -> it.toEpochMilli())
        .findFirst().orElseThrow(() -> new IllegalArgumentException(format("Invalid data type for @LastModifiedDate on Entity %s", property)));
    }

    @Override
    public HttpHeaders getResponseHeaders() {

        HttpHeaders headers = new HttpHeaders();

        // Modified to show download
        Object originalFileName = BeanUtils.getFieldWithAnnotation(property, OriginalFileName.class);
        if (originalFileName != null && StringUtils.hasText(originalFileName.toString())) {
            ContentDisposition.Builder builder = ContentDisposition.builder("form-data").name( "attachment").filename((String)originalFileName, Charset.defaultCharset());
            headers.setContentDisposition(builder.build());
        }
        return headers;
    }
}
