package internal.org.springframework.content.jpa.testsupport.models;

import jakarta.persistence.Embeddable;

import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.annotations.MimeType;

import lombok.Getter;
import lombok.Setter;

@Embeddable
@Getter
@Setter
public class ClaimForm {

	@ContentId
	private String contentId;

	@ContentLength
	private Long contentLength = 0L;

	@MimeType
	private String contentMimeType = "text/plain";

    @ContentId
    private String renditionId;

    @ContentLength
    private long renditionLen;

	// Ensure we can handle entities with "computed" getters; i.e. getters that
	// dont have an associated field
	public boolean getIsActive() {
		return true;
	}
}
