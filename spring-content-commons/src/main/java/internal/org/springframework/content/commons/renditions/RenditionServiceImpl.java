package internal.org.springframework.content.commons.renditions;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.renditions.Renderable;
import org.springframework.content.commons.renditions.RenditionProvider;
import org.springframework.content.commons.renditions.RenditionService;
import org.springframework.content.commons.repository.StoreExtension;
import org.springframework.content.commons.repository.StoreInvoker;
import org.springframework.content.commons.utils.BeanUtils;
import org.springframework.util.MimeType;

public class RenditionServiceImpl implements RenditionService, StoreExtension {
	
	private static final Log LOGGER = LogFactory.getLog(RenditionServiceImpl.class);

	private List<RenditionProvider> providers = new ArrayList<RenditionProvider>();

	public RenditionServiceImpl() {
	}
	
	@Autowired(required=false)
	public void setProviders(RenditionProvider... providers) {
		for (RenditionProvider provider : providers) {
			this.providers.add(provider);
		}
	}

	@Override
    public boolean canConvert(String fromMimeType, String toMimeType) {
		for (RenditionProvider provider : providers) {
			if (MimeType.valueOf(fromMimeType).includes(MimeType.valueOf(provider.consumes()))) {
				for (String produce : provider.produces()) {
					if (MimeType.valueOf(toMimeType).includes(MimeType.valueOf(produce))) {
						return true;
					}
				}
			}
		}
		return false;
	}

	@Override
	public String[] conversions(String fromMimeType) {
		Set<String> conversions = new HashSet<>();
		for (RenditionProvider provider : providers) {
			if (provider.consumes().equals(fromMimeType)) {
				conversions.addAll(Arrays.asList(provider.produces()));
			}
		}
		return conversions.toArray(new String[] {});
	}

	@Override
	public InputStream convert(String fromMimeType, InputStream fromInputSource, String toMimeType) {
		for (RenditionProvider provider : providers) {
			if (MimeType.valueOf(fromMimeType).includes(MimeType.valueOf(provider.consumes()))) {
				for (String produce : provider.produces()) {
					if (MimeType.valueOf(toMimeType).includes(MimeType.valueOf(produce))) {
						return provider.convert(fromInputSource, toMimeType);
					}
				}
			}
		}
		return null;
	}

	@Override
	public Set<Method> getMethods() {
		Class<?> clazz  = Renderable.class;
		Method getRenditionMethod;
		try {
			getRenditionMethod = clazz.getMethod("getRendition", Object.class, String.class);
			Set<Method> methods = Collections.singleton(getRenditionMethod);
			return methods;
		} catch (Exception e) {
			LOGGER.error("Failed to get Renderable.getRendtion method", e);
		}
		return Collections.emptySet();
	}

	@Override
	public Object invoke(MethodInvocation invocation, StoreInvoker invoker) {
		String fromMimeType = null;
		fromMimeType = (String)BeanUtils.getFieldWithAnnotation(invocation.getArguments()[0], org.springframework.content.commons.annotations.MimeType.class);
		if (fromMimeType == null) {
			return null;
		}
		String toMimeType = (String) invocation.getArguments()[1];
		
		if (this.canConvert(fromMimeType, toMimeType)) {
			InputStream content = null;
			try {
				content = invoker.invokeGetContent();
				return (InputStream) this.convert(fromMimeType, content, toMimeType);
			} catch (Exception e) {
				LOGGER.error(String.format("Failed to get rendition from %s to %s", fromMimeType, toMimeType	), e);
			}
		} 
		return null;
	}
}
