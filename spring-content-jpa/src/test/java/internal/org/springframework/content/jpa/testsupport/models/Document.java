package internal.org.springframework.content.jpa.testsupport.models;

import java.util.UUID;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.annotations.MimeType;

import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Document {

    @Id
//    @GeneratedValue
    @org.springframework.data.annotation.Id
    private String id = UUID.randomUUID().toString();

    @ContentId
    private String contentId;

    @ContentLength
    private Long contentLen;

    @MimeType
    private String contentMimeType;

    @ContentId
    private String renditionId;

    @ContentLength
    private long renditionLen;
}
