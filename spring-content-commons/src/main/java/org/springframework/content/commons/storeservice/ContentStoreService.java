package org.springframework.content.commons.storeservice;

public interface ContentStoreService {
	
	static final StoreFilter MATCH_ALL = new StoreFilter() {
		@Override
		public boolean matches(ContentStoreInfo info) {
			return true;
		}
	};
	
	public ContentStoreInfo[] getStores(Class<?> storeType);

	public ContentStoreInfo[] getStores(Class<?> storeType, StoreFilter filter);

	@Deprecated
	public ContentStoreInfo[] getContentStores();
}
