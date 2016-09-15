package internal.org.springframework.content.rest;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.springframework.content.commons.annotations.Content;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.annotations.MimeType;

@Entity
@Content
public class TestEntity {
	public @Id @GeneratedValue Long id;
	public String name;
	public @ContentId String contentId;
	public @ContentLength Long len;
	public @MimeType String mimeType;
}
