package internal.org.springframework.content.rest.support;

import java.util.UUID;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import lombok.Getter;
import lombok.Setter;

import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.annotations.MimeType;

@Entity
@Getter
@Setter
public class TestEntity6 {

    @Id
    @GeneratedValue
    @ContentId
    private UUID id;

    @ContentLength
    private Long contentLen;

    @MimeType
    private String mimeType;
}
