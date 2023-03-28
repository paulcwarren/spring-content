package internal.org.springframework.content.rest.support;

import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.annotations.MimeType;
import org.springframework.content.commons.annotations.OriginalFileName;

import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class TestEntity implements ContentEntity {
	private @Id @GeneratedValue Long id;
	private String name;
	private @ContentId UUID contentId;
	private @ContentLength Long len;
	private @MimeType String mimeType;
	private @OriginalFileName String originalFileName;
	private String title;
}
