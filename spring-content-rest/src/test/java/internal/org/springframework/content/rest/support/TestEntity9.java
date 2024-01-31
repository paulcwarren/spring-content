package internal.org.springframework.content.rest.support;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
public class TestEntity9 {
    public @Id @GeneratedValue Long id;
    public String name;
    @JsonIgnore private String hidden;
    public @ContentId UUID contentId;
    public @ContentLength Long contentLen;
    public @MimeType String contentMimeType;
}
