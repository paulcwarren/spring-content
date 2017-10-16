package internal.org.springframework.content.rest.links;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.InvalidPropertyException;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.storeservice.ContentStoreInfo;
import org.springframework.content.commons.storeservice.ContentStoreService;
import org.springframework.content.commons.utils.BeanUtils;
import org.springframework.data.rest.webmvc.PersistentEntityResource;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.LinkBuilder;
import org.springframework.hateoas.ResourceProcessor;
import org.springframework.hateoas.mvc.BasicLinkBuilder;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

import internal.org.springframework.content.rest.utils.ContentStoreUtils;

/** 
 * Adds content and content collection links to Spring Data REST Entity Resources.
 * 
 * @author warrep
 *
 */
public class ContentLinksResourceProcessor implements ResourceProcessor<PersistentEntityResource> {
	
	private static final Log log = LogFactory.getLog(ContentLinksResourceProcessor.class);
	
	private ContentStoreService stores;
	
	public ContentLinksResourceProcessor(ContentStoreService stores) {
		this.stores = stores;
	}
	
	public PersistentEntityResource process(final PersistentEntityResource resource) {
	
		Object object = resource.getContent();
		if (object == null)
			return resource;

		// entity
	    ContentStoreInfo store = ContentStoreUtils.findContentStore(stores, object.getClass());
		if (store != null) {
			Object id = BeanUtils.getFieldWithAnnotation(object, ContentId.class);
			if (id != null) {
				resource.add(BasicLinkBuilder.linkToCurrentMapping().slash(ContentStoreUtils.storePath(store)).slash(id).withRel(ContentStoreUtils.storePath(store)));
			}
		}
		
		
		List<Field> processed = new ArrayList<>();

		// public fields
		for (Field field : object.getClass().getFields()) {
			processed.add(field);
			handleField(field, resource);
		}
		
		// handle properties
		BeanWrapper wrapper = new BeanWrapperImpl(object);
		for (PropertyDescriptor descriptor : wrapper.getPropertyDescriptors()) {
			Field field = null;
			try {
				field = object.getClass().getDeclaredField(descriptor.getName());
				if (processed.contains(field) == false) {
					handleField(field, resource);
				}
			} catch (NoSuchFieldException nsfe) {
				log.trace(String.format("No field for property %s, ignoring", descriptor.getName()));
			} catch (SecurityException se) {
				log.warn(String.format("Unexpected security error while handling content links for property %s", descriptor.getName()));
			}
		}

		return resource;
	}

	private void handleField(Field field, final PersistentEntityResource resource) {
		
		Class<?> fieldType = field.getType();
		if (fieldType.isArray()) {
			fieldType = fieldType.getComponentType();

			ContentStoreInfo store = ContentStoreUtils.findContentStore(stores, fieldType);
			if (store != null) {
				resource.add(new Link(resource.getLink("self").getHref() + "/" + field.getName(), field.getName()));
			}
		} else if (Collection.class.isAssignableFrom(fieldType)) {
			Type type = field.getGenericType();

		    if (type instanceof ParameterizedType) {

		        ParameterizedType pType = (ParameterizedType)type;
		        Type[] arr = pType.getActualTypeArguments();

		        for (Type tp: arr) {
		            fieldType = (Class<?>)tp;
		        }
				
		        ContentStoreInfo store = ContentStoreUtils.findContentStore(stores, fieldType);
				if (store != null) {
					Object object = resource.getContent();
					BeanWrapper wrapper = new BeanWrapperImpl(object);
					Object value = null;
					try {
						value = wrapper.getPropertyValue(field.getName());
					} catch (InvalidPropertyException ipe) {
						try {
							value = ReflectionUtils.getField(field, object);
						} catch (IllegalStateException ise) {
							log.trace(String.format("Didn't get value for property %s", field.getName()));
						}
					}
					if (value != null) {
						int i=0;
						Iterator iter = ((Collection)value).iterator();
						while (iter.hasNext()) {
							Object o = iter.next();
							if (BeanUtils.hasFieldWithAnnotation(o, ContentId.class)) {
								String cid = BeanUtils.getFieldWithAnnotation(o, ContentId.class).toString();
								resource.add(new Link(resource.getLink("self").getHref() + "/" + field.getName() + "/" + cid, field.getName() /*+ "#" + i*/));
								
								LinkBuilder lb = BasicLinkBuilder.linkToCurrentMapping();
								int j = 0;
							}
							i++;
						}
					}
				}
		    }
		} else {
		    ContentStoreInfo store = ContentStoreUtils.findContentStore(stores, fieldType);
			if (store != null) {
				Object object = resource.getContent();
				BeanWrapper wrapper = new BeanWrapperImpl(object);
				Object value = null;
				try {
					value = wrapper.getPropertyValue(field.getName());
				} catch (InvalidPropertyException ipe) {
					try {
						value = ReflectionUtils.getField(field, object);
					} catch (IllegalStateException ise) {
						log.trace(String.format("Didn't get value for property %s", field.getName()));
					}
				}
				if (value != null) {
					String id = BeanUtils.getFieldWithAnnotation(value, ContentId.class).toString();
					Assert.notNull(id);
					resource.add(new Link(resource.getLink("self").getHref() + "/" + field.getName() + "/" + id, field.getName()));
				}
			}
		}
	}
}
