package internal.org.springframework.content.rest.links;

import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.utils.BeanUtils;
import org.springframework.data.rest.webmvc.BaseUri;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.LinkBuilder;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

public class ContentLinks  {

	private String repo;
	private String id;
	private String contentProperty;
	
	private BaseUri baseUri = null;
	
	public ContentLinks(String baseUri) {
		this.baseUri = new BaseUri(baseUri);
	}
	
	public ContentLinks(String repo, String id, String contentProperty) {
		this.repo = repo;
		this.id = id;
		this.contentProperty = contentProperty;
	}

	public LinkBuilder linkFor() {
		return new ContentLinksBuilder(baseUri.getUriComponentsBuilder());
	}

	public Link linkToContent(final Object entity) {
		if (BeanUtils.hasFieldWithAnnotation(entity, ContentId.class) && BeanUtils.getFieldWithAnnotation(entity, ContentId.class) != null) {
			UriComponentsBuilder builder = ServletUriComponentsBuilder.fromCurrentServletMapping();
			UriComponents components = builder.path("/{repo}/{id}/{contentProperty}/{contentId}").buildAndExpand(this.repo, this.id, this.contentProperty, BeanUtils.getFieldWithAnnotation(entity, ContentId.class));
			Link l = new Link(components.toUriString());
			return l;
		}
		return null;
	}
}
