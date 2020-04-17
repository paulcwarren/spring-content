package internal.org.springframework.content.rest.support;

import java.util.UUID;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.annotations.MimeType;
import org.springframework.data.rest.core.annotation.RestResource;

import lombok.Getter;
import lombok.Setter;

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
