package internal.org.springframework.content.rest.support;

import java.util.UUID;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.NaturalId;

import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.annotations.MimeType;
import org.springframework.content.commons.annotations.OriginalFileName;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class TestEntity7 implements ContentEntity {

    public TestEntity7(String name) {
        this.name = name;
    }

    @Id
    @GeneratedValue
    private Long id;

    @ContentId
    private UUID contentId;

    @NaturalId
    private String name;

    @ContentLength
    private Long len;

    @MimeType
    private String mimeType;

    @OriginalFileName
    private String originalFileName;

    private String title;
}
