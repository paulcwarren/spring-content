package org.springframework.content.rest.controllers;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;

public interface ContentService {

    void setContent(InputStream content, MediaType mimeType, String originalFilename, Resource target) throws IOException;

    void unsetContent(Resource resource);
}
