package org.springframework.support;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import org.springframework.content.commons.annotations.ContentId;

@Entity
public class TestEntity {
    @Id @GeneratedValue(strategy = GenerationType.AUTO) private long id;
    @ContentId private String contentId;
}
