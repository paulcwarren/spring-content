package internal.org.springframework.content.rest.links;

import java.util.List;

import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.Resources;

public class ContentResources extends Resources<Resource<Object>> {

	public ContentResources(List<Resource<Object>> resources) {
		super(resources, new Link[] {});
	}

}
