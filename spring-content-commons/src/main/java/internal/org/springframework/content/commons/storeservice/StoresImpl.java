package internal.org.springframework.content.commons.storeservice;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.repository.Store;
import org.springframework.content.commons.repository.factory.StoreFactory;
import org.springframework.content.commons.storeservice.StoreFilter;
import org.springframework.content.commons.storeservice.StoreInfo;
import org.springframework.content.commons.storeservice.StoreResolver;
import org.springframework.content.commons.storeservice.Stores;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.util.Assert;

public class StoresImpl implements Stores, InitializingBean {

	private Set<StoreInfo> storeInfos = new HashSet<>();
	private Map<String, StoreResolver> resolvers = new HashMap<>();
    private ListableBeanFactory factory = null;

    public StoresImpl() {
    }

    @Autowired
	public StoresImpl(ListableBeanFactory factory) {

	    this.factory = factory;
	}

    @Override
    public void afterPropertiesSet() {

        String[] names = this.factory.getBeanNamesForType(StoreFactory.class);

        for (String name : names) {
            StoreFactory factory = this.factory.getBean(name, StoreFactory.class);

			if (ContentStore.class.isAssignableFrom(factory.getStoreInterface())) {
				StoreInfo info = new StoreInfoImpl(
						factory.getStoreInterface(),
						ClassTypeInformation.from(factory.getStoreInterface()).getRequiredSuperTypeInformation(ContentStore.class).getTypeArguments().get(0).getType(),
						new StoreSupplier(this.factory, beanNameFromFactoryBeanName(name)));
				storeInfos.add(info);
			}
			else {
				StoreInfo info = new StoreInfoImpl(
						factory.getStoreInterface(),
						getDomainObjectClass(factory.getStoreInterface()),
                        new StoreSupplier(this.factory, beanNameFromFactoryBeanName(name)));
				storeInfos.add(info);
			}
		}
	}

    private String beanNameFromFactoryBeanName(String name) {
        return name.replaceFirst("&", "");
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

	@Override
	public void addStoreResolver(String name, StoreResolver resolver) {
		resolvers.put(name, resolver);
	}

	@Override
	public StoreInfo getStore(Class<?> storeType, StoreFilter filter) {
		Assert.notNull(storeType, "storeType must not be null");
		Assert.notNull(filter, "filter must not be null");

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
			StoreResolver resolver = resolvers.get(filter.name());
			if (resolver == null) {
				throw new IllegalStateException("unable to resolve store.  Consider adding a StoreResolver");
			}
			return resolver.resolve(candidates.toArray(new StoreInfo[]{}));
		}

		return null;
	}

    @Override
    public StoreInfo[] getStores(StoreFilter filter) {
        Set<StoreInfo> storeInfos = new HashSet<>();
        for (StoreInfo info : this.storeInfos) {
            if (filter.matches(info)) {
                storeInfos.add(info);
            }
        }
        return storeInfos.toArray(new StoreInfo[] {});
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

	public static class StoreSupplier implements Supplier<Store<Serializable>>{

	    private final ListableBeanFactory factory;
        private final String storeFactoryBeanName;

        public StoreSupplier(ListableBeanFactory factory, String storeFactoryBeanName) {

	        this.factory = factory;
	        this.storeFactoryBeanName = storeFactoryBeanName;
	    }

        @Override
        public Store<Serializable> get() {

            return factory.getBean(storeFactoryBeanName, Store.class);
        }
	}
}
