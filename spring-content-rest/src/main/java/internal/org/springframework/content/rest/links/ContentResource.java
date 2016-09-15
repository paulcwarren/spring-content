package internal.org.springframework.content.rest.links;

import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;

public class ContentResource extends Resource<Object> {

	public ContentResource(Object content, Link... links) {
		super(content, links);
	}

}
