package internal.org.springframework.content.commons.storeservice;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.repository.Store;
import org.springframework.content.commons.repository.factory.StoreFactory;
import org.springframework.content.commons.storeservice.ContentStoreInfo;
import org.springframework.content.commons.storeservice.ContentStoreService;
import org.springframework.content.commons.storeservice.StoreFilter;

public class ContentStoreServiceImpl implements ContentStoreService {

	private Set<ContentStoreInfo> contentStoreInfos = new HashSet<>();
	
	public ContentStoreServiceImpl() {
	}

	@SuppressWarnings("unchecked")
	@Autowired(required=false)
	public void setFactories(List<StoreFactory> factories){
		for (StoreFactory factory : factories) {
			if (ContentStore.class.isAssignableFrom(factory.getStoreInterface())) {
				ContentStoreInfo info = new ContentStoreInfoImpl(factory.getStoreInterface(), getDomainObjectClass(factory.getStoreInterface()), (ContentStore<Object,Serializable>)factory.getStore());
				contentStoreInfos.add(info);
			} else {
				ContentStoreInfo info = new ContentStoreInfoImpl(factory.getStoreInterface(), getDomainObjectClass(factory.getStoreInterface()), (Store<Serializable>)factory.getStore());
				contentStoreInfos.add(info);
			}
		}
	}
	
	private Class<?> getDomainObjectClass(Class<?> contentStoreInterface) {
		Type[] genericInterfaces = contentStoreInterface.getGenericInterfaces();
		for (Type genericInterface : genericInterfaces) {
			if (genericInterface instanceof ParameterizedType) {
				if (((ParameterizedType)genericInterface).getRawType().equals(ContentStore.class)) {
					Type t = ((ParameterizedType)genericInterface).getActualTypeArguments()[0];
					return (Class<?>)t;
				}
			}
		}
		return null;
	}

	public Set<ContentStoreInfo> getContentStoreInfos() {
		return contentStoreInfos;
	}

	public void setContentStoreInfos(Set<ContentStoreInfo> contentStoreInfos) {
		this.contentStoreInfos = contentStoreInfos;
	}

	public ContentStoreInfo[] getContentStores() {
		return getStores(ContentStore.class);
	}

	@Override
	public ContentStoreInfo[] getStores(Class<?> storeType) {
		return this.getStores(storeType, MATCH_ALL);
	}
	
	@Override
	public ContentStoreInfo[] getStores(Class<?> storeType, StoreFilter filter) {
		Set<ContentStoreInfo> storeInfos = new HashSet<>();
		for (ContentStoreInfo info : contentStoreInfos) {
			if (info.getImplementation(storeType) != null && filter.matches(info)) {
				storeInfos.add(info);
			}
		}
		return storeInfos.toArray(new ContentStoreInfo[] {});
	}
}
