package internal.org.springframework.content.rest.support;

import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.annotations.MimeType;

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
