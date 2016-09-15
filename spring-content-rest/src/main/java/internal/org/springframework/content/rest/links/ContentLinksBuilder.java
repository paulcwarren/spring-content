package internal.org.springframework.content.rest.links;

import org.springframework.hateoas.core.LinkBuilderSupport;
import org.springframework.web.util.UriComponentsBuilder;

public class ContentLinksBuilder extends
		LinkBuilderSupport<ContentLinksBuilder> {

	public ContentLinksBuilder(UriComponentsBuilder builder) {
		super(builder);
	}

	@Override
	protected ContentLinksBuilder getThis() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected ContentLinksBuilder createNewInstance(UriComponentsBuilder builder) {
		return new ContentLinksBuilder(builder);
	}

}
