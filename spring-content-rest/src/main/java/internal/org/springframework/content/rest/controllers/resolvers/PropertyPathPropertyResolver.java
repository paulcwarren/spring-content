package internal.org.springframework.content.rest.controllers.resolvers;

import java.beans.PropertyDescriptor;
import java.util.Map;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.storeservice.StoreInfo;
import org.springframework.content.commons.storeservice.Stores;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.repository.support.Repositories;
import org.springframework.http.HttpMethod;

import internal.org.springframework.content.rest.controllers.ResourceNotFoundException;

public class PropertyPathPropertyResolver {

    private HttpMethod method;
    private Repositories repositories;
    private Stores stores;
    private StoreInfo storeInfo;

    public PropertyPathPropertyResolver(HttpMethod method, Repositories repositories, Stores stores, StoreInfo storeInfo) {
        this.method = method;
        this.repositories = repositories;
        this.stores = stores;
        this.storeInfo = storeInfo;
    }

    public PropertyResolver.PropertySpec resolve(Object domainObj, Map<String,String> variables) {

        String contentProperty = variables.get("property");
        String contentPropertyId = variables.get("contentId");

        String path = contentProperty;
        if (contentPropertyId != null) {
            path = path + "." + contentPropertyId;
        }

//
//
//        PersistentEntity<?, ?> entity = repositories.getPersistentEntity(domainObj.getClass());
//        if (null == entity) {
//            throw new ResourceNotFoundException();
//        }
//
//        PersistentProperty<?> property = getContentPropertyDefinition(entity, contentProperty);
//        Class<?> propertyClass = property.getActualType();
//
//        if (ContentPropertyUtils.isPrimitiveContentPropertyClass(propertyClass)) {
//
//            return new PropertySpec(storeInfo, domainObj, domainObj, false);
//        }

        // get or create property value
        BeanWrapper wrapper = new BeanWrapperImpl(domainObj);
        PropertyDescriptor descriptor = wrapper.getPropertyDescriptor(path);
        Object propVal = wrapper.getPropertyValue(path);  //accessor.getProperty(property);

        if (propVal == null) {

//            if (!PersistentEntityUtils.isPropertyMultiValued(property)) {
//                propVal = instantiate(propertyClass);
//                accessor.setProperty(property, propVal);
                  propVal = instantiate(descriptor.getPropertyType());
                  wrapper.setPropertyValue(path, propVal);
//            }
//            else {
//                    if (property.isArray()) {
//                        Object member = instantiate(propertyClass);
//                        try {
//                            member = StoreRestController.save(repositories, member);
//                        }
//                        catch (HttpRequestMethodNotSupportedException e) {
//                            e.printStackTrace();
//                        }
//
//                        Object newArray = Array.newInstance(propertyClass, 1);
//                        Array.set(newArray, 0, member);
//                        accessor.setProperty(property, newArray);
//                        propVal = member;
//                    } else if (property.isCollectionLike()) {
//                        Object member = instantiate(propertyClass);
//                        @SuppressWarnings("unchecked")
//                        Collection<Object> contentCollection = (Collection<Object>) accessor.getProperty(property);
//                        contentCollection.add(member);
//                        propVal = member;
//                    }
//            }
        } else {
            if (isCollectionElementRequest(contentPropertyId)) {
//                if (property.isArray()) {
//                        Object componentValue = null;
//                        for (Object content : (Object[]) propVal) {
//                            if (BeanUtils.hasFieldWithAnnotation(content, ContentId.class) &&
//                                BeanUtils.getFieldWithAnnotation(content, ContentId.class) != null) {
//                                String candidateId = BeanUtils.getFieldWithAnnotation(content, ContentId.class).toString();
//                                if (candidateId.equals(contentPropertyId)) {
//                                    componentValue = content;
//                                    break;
//                                }
//                            }
//                        }
//                        propVal = componentValue;
//                } else if (property.isCollectionLike()) {
//                        Object componentValue = null;
//                        for (Object content : (Collection<?>) propVal) {
//                            if (BeanUtils.hasFieldWithAnnotation(content, ContentId.class) && BeanUtils
//                                    .getFieldWithAnnotation(content, ContentId.class) != null) {
//                                String candidateId = BeanUtils.getFieldWithAnnotation(content, ContentId.class)
//                                        .toString();
//                                if (candidateId.equals(contentPropertyId)) {
//                                    componentValue = content;
//                                    break;
//                                }
//                            }
//                        }
//                        propVal = componentValue;
//                }
//            } else if (isCollectionRequest(contentPropertyId) &&
//                    (PersistentEntityUtils.isPropertyMultiValued(property) &&
//                            (method.equals(HttpMethod.GET) || method.equals(HttpMethod.DELETE)))) {
//                throw new MethodNotAllowedException("GET", null);
            } else if (isCollectionRequest(contentPropertyId) ) {
//                if (property.isArray()) {
//                        Object member = instantiate(propertyClass);
//                        Object newArray = Array.newInstance(propertyClass, Array.getLength(propVal) + 1);
//                        System.arraycopy(propVal, 0, newArray, 0, Array.getLength(propVal));
//                        Array.set(newArray, Array.getLength(propVal), member);
//                        accessor.setProperty(property, newArray);
//                        propVal = member;
//                }
//                else if (property.isCollectionLike()) {
//                    Object member = instantiate(propertyClass);
//                    @SuppressWarnings("unchecked")
//                    Collection<Object> contentCollection = (Collection<Object>) accessor.getProperty(property);
//                    contentCollection.add(member);
//                    propVal = member;
//                }
            }
        }

        // get property store
//        StoreInfo info = stores.getStore(ContentStore.class, Stores.withDomainClass(propertyClass));
//        if (info == null) {
//            throw new IllegalStateException(String.format("Store for property %s not found", property.getName()));
//        }

        boolean embeddedProperty = false;
//        if (PersistentEntityUtils.isPropertyMultiValued(property) || propVal.getClass().getAnnotation(Embeddable.class) != null) {
//            embeddedProperty = true;
//        }

        return new PropertyResolver.PropertySpec(this.storeInfo /* info */, domainObj, propVal, /*embeddedProperty*/ true);
    }

    private Object instantiate(Class<?> clazz) {

        Object newObject = null;
        try {
            newObject = clazz.newInstance();
        }
        catch (InstantiationException e) {
            e.printStackTrace();
        }
        catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return newObject;
    }

    public boolean isCollectionRequest(String contentPropertyId) {
        return contentPropertyId == null;
    }

    public boolean isCollectionElementRequest(String contentPropertyId) {
        return contentPropertyId != null;
    }

    public PersistentProperty<?> getContentPropertyDefinition(PersistentEntity<?, ?> persistentEntity, String contentProperty) {
        PersistentProperty<?> prop = persistentEntity.getPersistentProperty(contentProperty);

        if (prop == null) {
            for (PersistentProperty<?> candidate : persistentEntity.getPersistentProperties(ContentId.class)) {
                if (candidate.getName().contains(contentProperty)) {
                    prop = candidate;
                }
            }
        }

        if (null == prop) {
            throw new ResourceNotFoundException();
        }

        return prop;
    }
}
