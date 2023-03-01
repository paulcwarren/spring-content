package internal.org.springframework.content.rest.support;

import java.util.UUID;

import jakarta.persistence.Embeddable;

import org.hibernate.annotations.Formula;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.annotations.MimeType;
import org.springframework.content.commons.annotations.OriginalFileName;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.content.rest.RestResource;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class TestEntity10Child {

    @ContentId public UUID contentId;
	@ContentLength public Long contentLen;
	@MimeType public String contentMimeType;
	@OriginalFileName public String contentFileName = "";

    @ContentId public UUID previewId;
    @ContentLength public Long previewLen;
    @MimeType public String previewMimeType;

	// prevent TestEntity8Child from being return by hibernate as null
	@Formula("1")
	private int workaroundForBraindeadJpaImplementation;

}
