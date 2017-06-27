package org.springframework.content.elasticsearch;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.StoreEventHandler;
import org.springframework.content.commons.repository.events.AbstractStoreEventListener;
import org.springframework.content.commons.repository.events.AfterSetContentEvent;
import org.springframework.content.commons.utils.BeanUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestOperations;

@StoreEventHandler
public class ElasticsearchIndexer extends AbstractStoreEventListener<Object> {

	private RestOperations template;
	
	public ElasticsearchIndexer(RestOperations template) {
		this.template = template;
	}
	
	@Override
	protected void onAfterSetContent(AfterSetContentEvent event) {
		String id = BeanUtils.getFieldWithAnnotation(event.getSource(), ContentId.class).toString();
		InputStream stream = event.getStore().getContent(event.getSource());
		String content = null;
		try {
			byte[] bytes = IOUtils.toByteArray(stream);
			byte[] encoded = Base64.getEncoder().encode(bytes);
			content = new String(encoded);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		JSONObject request = new JSONObject();
		request.put("original-content", content)
			   .put("contentId", id);
		
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<String> entity = new HttpEntity<String>(request.toString(), headers);

		ResponseEntity<String> response = template.exchange("http://search-spring-content-cc4bqyhqoiokxrakhfp4s2y3tm.us-east-1.es.amazonaws.com/docs/doc/1", HttpMethod.PUT, entity, String.class);
	}

}
