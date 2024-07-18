package internal.org.springframework.content.rest.support;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.annotations.MimeType;
import org.springframework.content.commons.annotations.OriginalFileName;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.Date;
import java.util.UUID;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class TestEntity4 implements ContentEntity {
	public @Id @GeneratedValue Long id;

	public String name;
	public @ContentId UUID contentId;
	public @ContentLength Long len;
	public @MimeType String mimeType;
	private @Version Long version = 0L;
	private @CreatedDate Date createdDate;
	private @LastModifiedDate Date modifiedDate;
	private @OriginalFileName String originalFileName;
	private String title;

	@OneToOne(mappedBy="testEntity4")
	private TestEntity3 testEntity3;
}
