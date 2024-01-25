package internal.org.springframework.content.rest.support;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.annotations.MimeType;
import org.springframework.content.commons.annotations.OriginalFileName;

import java.util.UUID;

@Entity
@Getter
@Setter
public class TestEntity3 implements ContentEntity {
	public @Id @GeneratedValue Long id;
	public String name;
	@JsonIgnore private String hidden;
	public @ContentId UUID contentId;
	public @ContentLength Long len;
	public @MimeType String mimeType;
	private @OriginalFileName String originalFileName;
	private String title;

	@OneToOne
	@JoinColumn(name = "testEntity4", nullable = true)
	private TestEntity4 testEntity4;
}
