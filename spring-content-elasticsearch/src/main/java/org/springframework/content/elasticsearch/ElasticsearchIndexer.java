package org.springframework.content.elasticsearch;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.StoreEventHandler;
import org.springframework.content.commons.repository.StoreAccessException;
import org.springframework.content.commons.repository.events.AbstractStoreEventListener;
import org.springframework.content.commons.repository.events.AfterSetContentEvent;
import org.springframework.content.commons.repository.events.BeforeUnsetContentEvent;
import org.springframework.content.commons.utils.BeanUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestOperations;

import internal.org.springframework.content.elasticsearch.StreamConverter;

@StoreEventHandler
public class ElasticsearchIndexer extends AbstractStoreEventListener<Object> {

	private RestOperations template;
	private StreamConverter streamConverter;
	public ElasticsearchIndexer(RestOperations template, StreamConverter streamConverter) {
		this.template = template;
		this.streamConverter = streamConverter;
	}
	
	@Override
	protected void onAfterSetContent(AfterSetContentEvent event) {
		String id = BeanUtils.getFieldWithAnnotation(event.getSource(), ContentId.class).toString();
		InputStream stream = event.getStore().getContent(event.getSource());
		byte[] bytes = null;
		try {
			bytes = streamConverter.convert(stream);
		} catch (IOException e) {
			throw new StoreAccessException(String.format("IOException error while converting stream to byte array for content ID:%s", id), e.getCause());
		}
		byte[] encoded = Base64.getEncoder().encode(bytes);
		String content = new String(encoded);
		
		JSONObject request = new JSONObject();
		request.put("original-content", content)
			   .put("contentId", id);
		
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<String> entity = new HttpEntity<String>(request.toString(), headers);

		try {
			ResponseEntity<String> response = template.exchange("http://search-spring-content-cc4bqyhqoiokxrakhfp4s2y3tm.us-east-1.es.amazonaws.com/docs/doc/" + id, HttpMethod.PUT, entity, String.class);
			handleResponse(id, response.getStatusCode());
		} catch (RestClientException rce) {
			throw new StoreAccessException(String.format("Unexpected error attempting to index content for content id %s", id), rce);
		}
	}

	@Override
	protected void onBeforeUnsetContent(BeforeUnsetContentEvent event) {
		String id = BeanUtils.getFieldWithAnnotation(event.getSource(), ContentId.class).toString();
		try {
			ResponseEntity<String> response = template.exchange("http://search-spring-content-cc4bqyhqoiokxrakhfp4s2y3tm.us-east-1.es.amazonaws.com/docs/doc/" + id, HttpMethod.DELETE, null, String.class);
			handleResponse(id, response.getStatusCode());
		} catch (RestClientException rce) {
			throw new StoreAccessException(String.format("Unexpected error attempting to delete index for content id %s", id), rce);
		}
	}

	protected void handleResponse(String id, HttpStatus httpStatus) {
		if(httpStatus.is5xxServerError() || httpStatus.is4xxClientError() ) {
			throw new StoreAccessException(String.format("Indexing error while storing content for contentId %s", id));
		}
	}
}