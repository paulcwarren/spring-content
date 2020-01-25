package org.springframework.support;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import org.springframework.content.commons.annotations.ContentId;

@Entity
public class TestEntity {
    @Id @GeneratedValue(strategy = GenerationType.AUTO) private long id;
    @ContentId private String contentId;
}
