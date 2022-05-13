package internal.org.springframework.content.rest.contentservice;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.rest.AfterGetContentEvent;
import org.springframework.content.rest.AfterSetContentEvent;
import org.springframework.content.rest.AfterUnsetContentEvent;
import org.springframework.content.rest.BeforeGetContentEvent;
import org.springframework.content.rest.BeforeSetContentEvent;
import org.springframework.content.rest.BeforeUnsetContentEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import internal.org.springframework.content.rest.controllers.MethodNotAllowedException;
import internal.org.springframework.content.rest.io.AssociatedStoreResource;

public class EventingContentService implements ContentService {

    private ContentService contentService;
    private ApplicationEventPublisher publisher;

    public EventingContentService(ApplicationEventPublisher publisher, ContentService contentService) {
        this.publisher = publisher;
        this.contentService = contentService;
    }

    @Override
    public void getContent(HttpServletRequest request, HttpServletResponse response, HttpHeaders headers, Resource resource, MediaType resourceType)
            throws MethodNotAllowedException {

        this.publisher.publishEvent(new BeforeGetContentEvent(resource, resourceType));
        this.contentService.getContent(request, response, headers, resource, resourceType);
        this.publisher.publishEvent(new AfterGetContentEvent(resource, resourceType));
    }

    @Override
    public void setContent(HttpServletRequest request, HttpServletResponse response, HttpHeaders headers, Resource source, MediaType sourceMimeType, Resource target)
            throws IOException,
            MethodNotAllowedException {

        this.publisher.publishEvent(new BeforeSetContentEvent(source, sourceMimeType));

        this.contentService.setContent(request, response, headers, source, sourceMimeType, target);

        Object s = ((AssociatedStoreResource)target).getAssociation();
        PropertyPath path = PropertyPath.from(((AssociatedStoreResource)target).getContentProperty().getContentPropertyPath());
        this.publisher.publishEvent(new AfterSetContentEvent(s, path, target, null));
    }

    @Override
    public void unsetContent(Resource resource)
            throws MethodNotAllowedException {

        Object s = ((AssociatedStoreResource)resource).getAssociation();
        PropertyPath path = PropertyPath.from(((AssociatedStoreResource)resource).getContentProperty().getContentPropertyPath());
        this.publisher.publishEvent(new BeforeUnsetContentEvent(s, path, resource, null));

        this.contentService.unsetContent(resource);

        this.publisher.publishEvent(new AfterUnsetContentEvent(resource, null));
    }
}
