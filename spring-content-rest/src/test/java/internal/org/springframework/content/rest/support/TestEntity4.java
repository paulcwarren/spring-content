package internal.org.springframework.content.rest.support;

import lombok.Getter;
import lombok.Setter;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.annotations.MimeType;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Version;
import java.util.Date;
import java.util.UUID;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class TestEntity4 {
	public @Id @GeneratedValue Long id;
	public String name;
	public @ContentId UUID contentId;
	public @ContentLength Long len;
	public @MimeType String mimeType;
	private @Version Long version;
	private @CreatedDate Date createdDate;
	private @LastModifiedDate Date modifiedDate;
}
