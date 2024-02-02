package internal.org.springframework.content.rest.io;

import static java.lang.String.format;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.stream.Stream;

import jakarta.persistence.Version;

import org.springframework.content.commons.io.DeletableResource;
import org.springframework.content.commons.io.RangeableResource;
import org.springframework.content.commons.mappingcontext.ContentProperty;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.renditions.Renderable;
import org.springframework.content.commons.repository.AssociativeStore;
import org.springframework.content.commons.storeservice.StoreInfo;
import org.springframework.content.commons.utils.BeanUtils;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;
import org.springframework.data.convert.Jsr310Converters;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.resource.HttpResource;

/**
 * Represents a Spring Content Resource that is associated with a Spring Data Entity.
 *
 * Overrides the ContentLength and LastModifiedDate with the values stored on the
 * Spring Data entity, rather than the values of the actual content itself (these are
 * used as fallback values though).
 *
 * Also sets appropriate headers to pass the Spring Data Entity recorded filename, if exists.
 */
public class AssociatedStoreResourceImpl<S> implements HttpResource, AssociatedStoreResource<S> {

    private final ConfigurableConversionService conversionService = new DefaultConversionService();

    {
        Jsr310Converters.getConvertersToRegister().forEach(conversionService::addConverter);
    }

    private S entity;
    private Resource original;
    private StoreInfo info;

    private ContentProperty contentProperty;

    private PropertyPath propertyPath;


    public AssociatedStoreResourceImpl(StoreInfo info, S entity, PropertyPath propertyPath, ContentProperty property, Resource original) {
        this.info = info;
        this.entity = entity;
        this.propertyPath = propertyPath;
        this.contentProperty = property;
        this.original = original;
    }

    @Override
    public StoreInfo getStoreInfo() {

        return info;
    }

    @Override
    public S getAssociation() {

        return entity;
    }

    @Override
    public PropertyPath getPropertyPath() {
        return this.propertyPath;
    }

    @Override
    public ContentProperty getContentProperty() {
        return this.contentProperty;
    }

    protected Resource getDelegate() {

        return original;
    }

    protected ConversionService getConversionService() {
        return conversionService;
    }

    @Override
    public boolean isRenderableAs(org.springframework.util.MimeType mimeType) {

        if (Renderable.class.isAssignableFrom(this.getStoreInfo().getInterface())) {

            Renderable renderer = (Renderable)this.getStoreInfo().getImplementation(AssociativeStore.class);
            return renderer.hasRendition(getAssociation(), this.getPropertyPath(), mimeType.toString());
        }

        return false;
    }

    @Override
    public InputStream renderAs(org.springframework.util.MimeType mimeType) {

        if (Renderable.class.isAssignableFrom(this.getStoreInfo().getInterface())) {

            Renderable renderer = (Renderable)this.getStoreInfo().getImplementation(AssociativeStore.class);
            return renderer.getRendition(getAssociation(), this.getPropertyPath(), mimeType.toString());
        }

        return null;
    }

    @Override
    public Object getETag() {

        Object etag = null;

    //        if (property != null) {
    //            etag = BeanUtils.getFieldWithAnnotation(property, Version.class);
    //        }

        if (etag == null) {
            etag = BeanUtils.getFieldWithAnnotation(entity, Version.class);
        }

        if (etag == null) {
            etag = "";
        }

        return etag.toString();
    }

    @Override
    public MediaType getMimeType() {

        Object mimeType = null;

        mimeType = this.getContentProperty().getMimeType(this.getAssociation());

        return MediaType.valueOf(mimeType != null ? mimeType.toString() : MediaType.ALL_VALUE);
    }

    @Override
    public long contentLength() throws IOException {

     // TODO: can we remove this is all properties are effectively embedded?
//      Long contentLength = null; (Long) BeanUtils.getFieldWithAnnotation(property, ContentLength.class);
      Long contentLength = (Long) this.getContentProperty().getContentLength(this.getAssociation());
      if (contentLength == null) {
          contentLength = getDelegate().contentLength();
      }
      return contentLength;
    }

    @Override
    public long lastModified() throws IOException {

     // TODO: can we remove this is all properties are effectively embedded?
        Object lastModified = null; //BeanUtils.getFieldWithAnnotation(property, LastModifiedDate.class);
        if (lastModified == null && getDelegate() != null) {
            return getDelegate().lastModified();
        }

        if (lastModified != null) {
            return Stream.of(lastModified)
                    .map(it -> getConversionService().convert(it, Date.class))//
                    .map(it -> getConversionService().convert(it, Instant.class))//
                    .map(it -> it.toEpochMilli())
                    .findFirst().orElseThrow(() -> new IllegalArgumentException(format("Invalid data type for @LastModifiedDate on Entity %s", this.getAssociation())));
        }

        return -1L;
    }

    @Override
    public HttpHeaders getResponseHeaders() {

        HttpHeaders headers = new HttpHeaders();

        // Modified to show download
        Object originalFileName = this.getContentProperty().getOriginalFileName(this.getAssociation());
        if (originalFileName != null && StringUtils.hasText(originalFileName.toString())) {
            headers.setContentDisposition(ContentDisposition.attachment().filename((String) originalFileName, StandardCharsets.UTF_8).build());
        } else {
            headers.setContentDisposition(ContentDisposition.attachment().build());
        }
        return headers;
    }

    @Override
    public boolean exists() {
        return (original != null ? original.exists() : false);
    }

    @Override
    public boolean isReadable() {
        return original.isReadable();
    }

    @Override
    public boolean isOpen() {
        return original.isOpen();
    }

    @Override
    public boolean isFile() {
        return original.isFile();
    }

    @Override
    public URL getURL() throws IOException {
        return original.getURL();
    }

    @Override
    public URI getURI() throws IOException {
        return original.getURI();
    }

    @Override
    public File getFile() throws IOException {
        return original.getFile();
    }

    @Override
    public ReadableByteChannel readableChannel() throws IOException {
        return original.readableChannel();
    }

    @Override
    public Resource createRelative(String relativePath) throws IOException {
        return original.createRelative(relativePath);
    }

    @Override
    @Nullable
    public String getFilename() {
        return original.getFilename();
    }

    @Override
    public String getDescription() {
        return original.getDescription();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return original.getInputStream();
    }

    @Override
    public boolean isWritable() {
        return ((WritableResource)original).isWritable();
    }

    @Override
    public OutputStream getOutputStream()
            throws IOException {
        return ((WritableResource)original).getOutputStream();
    }

    @Override
    public WritableByteChannel writableChannel()
            throws IOException {
        return ((WritableResource)original).writableChannel();
    }


    @Override
    public void delete()
            throws IOException {
        ((DeletableResource)original).delete();
    }

    @Override
    public void setRange(String range) {
        if (original instanceof RangeableResource) {
            ((RangeableResource)original).setRange(range);
        }
    }
}
