package internal.org.springframework.content.commons.storeservice;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.repository.Store;
import org.springframework.content.commons.repository.factory.StoreFactory;
import org.springframework.content.commons.storeservice.*;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.util.Assert;

public class StoresImpl implements Stores {

	private Set<StoreInfo> storeInfos = new HashSet<>();
	private StoreResolver resolver;

	public StoresImpl() {
	}

	@SuppressWarnings("unchecked")
	@Autowired(required = false)
	public void setFactories(List<StoreFactory> factories) {
		for (StoreFactory factory : factories) {
			if (ContentStore.class.isAssignableFrom(factory.getStoreInterface())) {
				StoreInfo info = new StoreInfoImpl(
						factory.getStoreInterface(),
						ClassTypeInformation.from(factory.getStoreInterface()).getRequiredSuperTypeInformation(ContentStore.class).getTypeArguments().get(0).getType(),
						(ContentStore<Object, Serializable>) factory.getStore());
				storeInfos.add(info);
			}
			else {
				StoreInfo info = new StoreInfoImpl(
						factory.getStoreInterface(),
						getDomainObjectClass(factory.getStoreInterface()),
						(Store<Serializable>) factory.getStore());
				storeInfos.add(info);
			}
		}
	}

	private Class<?> getDomainObjectClass(Class<?> contentStoreInterface) {
		Type[] genericInterfaces = contentStoreInterface.getGenericInterfaces();
		for (Type genericInterface : genericInterfaces) {
			if (genericInterface instanceof ParameterizedType) {
				if (((ParameterizedType) genericInterface).getRawType()
						.equals(ContentStore.class)) {
					Type t = ((ParameterizedType) genericInterface)
							.getActualTypeArguments()[0];
					return (Class<?>) t;
				}
			}
		}
		return null;
	}

//	public Set<ContentStoreInfo> getContentStoreInfos() {
//		return storeInfos;
//	}
//
//	public void setContentStoreInfos(Set<ContentStoreInfo> contentStoreInfos) {
//		this.storeInfos = contentStoreInfos;
//	}

//	public ContentStoreInfo[] getContentStores() {
//		return (ContentStoreInfo[]) getStores(ContentStore.class);
//	}

	@Override
	public StoreInfo getStore(Class<?> storeType, StoreFilter filter) {
		Assert.notNull(storeType);
		Assert.notNull(filter);

		List<StoreInfo> candidates = new ArrayList<>();
		for (StoreInfo info : storeInfos) {
			if (info.getImplementation(storeType) != null && filter.matches(info)) {
				candidates.add(info);
			}
		}

		if (candidates.size() == 1) {
			return candidates.get(0);
		}

		if (candidates.size() > 1) {
			if (resolver == null) {
				throw new IllegalStateException("unable to resolve store.  Consider adding a StoreResolver");
			}
			return resolver.resolve(candidates.toArray(new StoreInfo[]{}));
		}

		return null;
	}

	@Override
	public StoreInfo[] getStores(Class<?> storeType) {
		return this.getStores(storeType, MATCH_ALL);
	}

	@Override
	public StoreInfo[] getStores(Class<?> storeType, StoreFilter filter) {
		Set<StoreInfo> storeInfos = new HashSet<>();
		for (StoreInfo info : this.storeInfos) {
			if (info.getImplementation(storeType) != null && filter.matches(info)) {
				storeInfos.add(info);
			}
		}
		return storeInfos.toArray(new StoreInfo[] {});
	}
}
