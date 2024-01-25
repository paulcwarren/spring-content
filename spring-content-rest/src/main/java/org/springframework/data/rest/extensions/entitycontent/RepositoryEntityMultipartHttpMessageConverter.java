package org.springframework.data.rest.extensions.entitycontent;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.rest.webmvc.PersistentEntityResource;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.http.server.ServletServerHttpRequest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.*;

public class RepositoryEntityMultipartHttpMessageConverter implements HttpMessageConverter<Object> {

    private HttpMessageConverter converter = null;

    public RepositoryEntityMultipartHttpMessageConverter(List<HttpMessageConverter<?>> messageConverters) {
        for (HttpMessageConverter converter : messageConverters) {
            if (converter.canRead(RepresentationModel.class, MediaType.APPLICATION_JSON)) {
                this.converter = converter;
                break;
            }
        }
        if (this.converter == null) {
            throw new IllegalStateException("unable to resolve persistent entity resource message converter");
        }
    }

    @Override
    public boolean canRead(Class clazz, MediaType mediaType) {
        return PersistentEntityResource.class.equals(clazz) && MediaType.MULTIPART_FORM_DATA.equals(mediaType);
    }

    @Override
    public boolean canWrite(Class clazz, MediaType mediaType) {
        return false;
    }

    @Override
    public List<MediaType> getSupportedMediaTypes() {
        return Collections.singletonList(MediaType.MULTIPART_FORM_DATA);
    }

    @Override
    public List<MediaType> getSupportedMediaTypes(Class clazz) {
        return HttpMessageConverter.super.getSupportedMediaTypes(clazz);
    }

    @Override
    public Object read(Class clazz, HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
        if (inputMessage instanceof ServletServerHttpRequest == false) {
            return null;
        }

        Map<String, String> paramsMap = new HashMap<>();
        Enumeration<String> paramNames = ((ServletServerHttpRequest)inputMessage).getServletRequest().getParameterNames();
        while (paramNames.hasMoreElements()) {
            String paramName = paramNames.nextElement();
            String paramValue = ((ServletServerHttpRequest)inputMessage).getServletRequest().getParameter(paramName);
            paramsMap.put(paramName, paramValue);
        }

        HttpHeaders headers = new HttpHeaders(inputMessage.getHeaders());
        headers.setContentType(MediaType.APPLICATION_JSON);

        return this.converter.read(clazz, new HttpInputMessage() {
            @Override
            public InputStream getBody() throws IOException {
                return new ByteArrayInputStream(new ObjectMapper().writeValueAsString(paramsMap).getBytes());
            }

            @Override
            public HttpHeaders getHeaders() {
                return headers;
            }
        });
    }

    @Override
    public void write(Object o, MediaType contentType, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
        throw new UnsupportedEncodingException();
    }
}
