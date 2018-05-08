package internal.org.springframework.content.commons.utils;

import java.util.Arrays;
import java.util.List;

import org.springframework.cache.interceptor.SimpleKey;
import org.springframework.content.commons.io.DefaultMediaResource;

public class CacheKey {
	public String name;
	public String key;
	public String mime;

	public CacheKey(String name, String key, String mime) {
		super();
		this.name = name;
		this.key = key;
		this.mime = mime;
	}

	public static Object getKey(Object... pms) {
		List<Object> p = Arrays.asList(pms);

		String key = "";

		// p0 - is
		if (p.get(0) instanceof DefaultMediaResource) {
			key += ((DefaultMediaResource) p.get(0)).getName();
		} else {
			return new SimpleKey(pms);
		}

		// p1 - mime

		key += p.get(1);

		return new CacheKey(((DefaultMediaResource) p.get(0)).getName(), key.replace("/", ""), p.get(1).toString());
	}
}
