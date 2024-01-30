package org.springframework.data.rest.extensions.entitycontent;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.InvalidPropertyException;
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

import java.beans.PropertyDescriptor;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
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

        BeanWrapper wrapper = new BeanWrapperImpl(clazz);
        Map<String, Object> paramsMap = new HashMap<>();
        Enumeration<String> paramNames = ((ServletServerHttpRequest)inputMessage).getServletRequest().getParameterNames();
        while (paramNames.hasMoreElements()) {
            String paramName = paramNames.nextElement();

            PropertyDescriptor descriptor = null;
            try {
                descriptor = wrapper.getPropertyDescriptor(paramName);
            } catch (InvalidPropertyException ipe) {
                descriptor = findJsonProperty(clazz, paramName, wrapper);
            }

            if (descriptor != null) {
                if (descriptor.getPropertyType().isArray() || Collection.class.isAssignableFrom(descriptor.getPropertyType())) {
                    paramsMap.put(paramName, ((ServletServerHttpRequest) inputMessage).getServletRequest().getParameterValues(paramName));
                } else {
                    paramsMap.put(paramName, ((ServletServerHttpRequest) inputMessage).getServletRequest().getParameter(paramName));
                }
            }
        }

        HttpHeaders headers = new HttpHeaders(inputMessage.getHeaders());
        headers.setContentType(MediaType.APPLICATION_JSON);

        return this.converter.read(clazz, new HttpInputMessage() {
            @Override
            public InputStream getBody() throws IOException {
                ObjectMapper mapper = new ObjectMapper();
                String convertedFormData = mapper.writeValueAsString(paramsMap);
                return new ByteArrayInputStream(convertedFormData.getBytes());
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

    private PropertyDescriptor findJsonProperty(Class<?> clazz, String propertyName, BeanWrapper beanWrapper) {
        // Iterate through all properties and find those with @JsonProperty annotation
        for (PropertyDescriptor propertyDescriptor : beanWrapper.getPropertyDescriptors()) {
            String name = propertyDescriptor.getName();
            Field field = findField(clazz, name);

            if (field != null && field.isAnnotationPresent(JsonProperty.class)) {
                JsonProperty jsonPropertyAnnotation = field.getAnnotation(JsonProperty.class);
                String jsonPropertyName = jsonPropertyAnnotation.value();
                if (jsonPropertyName.equals(propertyName)) {
                    return propertyDescriptor;
                }
            }
        }
        return null;
    }

    private static Field findField(Class<?> clazz, String propertyName) {
        try {
            return clazz.getDeclaredField(propertyName);
        } catch (NoSuchFieldException e) {
            return null;
        }
    }
}
