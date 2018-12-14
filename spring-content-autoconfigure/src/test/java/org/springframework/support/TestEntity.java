package org.springframework.support;

import org.springframework.content.commons.annotations.Content;
import org.springframework.content.commons.annotations.ContentId;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
//@Content
public class TestEntity {
    @Id @GeneratedValue(strategy = GenerationType.AUTO) private long id;
    @ContentId private String contentId;
}
