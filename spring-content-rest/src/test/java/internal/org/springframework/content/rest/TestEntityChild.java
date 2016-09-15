package internal.org.springframework.content.rest;

import javax.persistence.Embeddable;

import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.annotations.MimeType;

@Embeddable
public class TestEntityChild {
	@ContentId public String contentId;
	@ContentLength public Long contentLen;
	@MimeType public String mimeType;
}
