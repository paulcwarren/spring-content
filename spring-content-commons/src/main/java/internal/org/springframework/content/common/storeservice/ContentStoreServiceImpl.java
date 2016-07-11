package internal.org.springframework.content.common.storeservice;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.common.repository.ContentStore;
import org.springframework.content.common.repository.factory.ContentStoreFactory;
import org.springframework.content.common.storeservice.ContentStoreInfo;
import org.springframework.content.common.storeservice.ContentStoreService;

public class ContentStoreServiceImpl implements ContentStoreService {

	private Set<ContentStoreInfo> contentStoreInfos = new HashSet<>();
	
	public ContentStoreServiceImpl() {
	}

	@Autowired
	public void setFactories(List<ContentStoreFactory> factories){
		for (ContentStoreFactory factory : factories) {
			ContentStoreInfo info = new ContentStoreInfoImpl(factory.getContentStoreInterface(), getDomainObjectClass(factory.getContentStoreInterface()), factory.getContentStore());
			contentStoreInfos.add(info);
		}
	}
	
	private Class<?> getDomainObjectClass(Class<? extends ContentStore<Object,Serializable>> contentStoreInterface) {
		Type[] genericInterfaces = contentStoreInterface.getGenericInterfaces();
		for (Type genericInterface : genericInterfaces) {
			if (genericInterface instanceof ParameterizedType) {
				if (((ParameterizedType)genericInterface).getRawType().equals(ContentStore.class)) {
					Type t = ((ParameterizedType)genericInterface).getActualTypeArguments()[0];
					return (Class<?>)t;
				}
			}
		}
		throw new IllegalStateException(String.format("ContentStore %s must specify parameters <T, SID>", contentStoreInterface.getCanonicalName()));
	}

	public Set<ContentStoreInfo> getContentStoreInfos() {
		return contentStoreInfos;
	}

	public void setContentStoreInfos(Set<ContentStoreInfo> contentStoreInfos) {
		this.contentStoreInfos = contentStoreInfos;
	}

	public ContentStoreInfo[] getContentStores() {
		return contentStoreInfos.toArray(new ContentStoreInfo[] {});
	}
}
