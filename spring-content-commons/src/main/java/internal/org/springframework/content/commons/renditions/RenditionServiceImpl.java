package internal.org.springframework.content.commons.renditions;

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
import org.springframework.content.commons.renditions.RenditionCapability;
import org.springframework.content.commons.renditions.RenditionProvider;
import org.springframework.content.commons.renditions.RenditionService;
import org.springframework.content.commons.repository.StoreExtension;
import org.springframework.content.commons.repository.StoreInvoker;
import org.springframework.content.commons.utils.BeanUtils;
import org.springframework.core.io.Resource;

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
			if (provider.isCapable(fromMimeType, toMimeType).isBetterThan(RenditionCapability.NOT_CAPABLE)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String[] conversions(String fromMimeType) {
		Set<String> conversions = new HashSet<>();
		for (RenditionProvider provider : providers) {
			if (provider.consumes(fromMimeType)) {
				conversions.addAll(Arrays.asList(provider.produces()));
			}
		}
		return conversions.toArray(new String[] {});
	}

	@Override
	public Resource convert(String fromMimeType, Resource fromInputSource, String toMimeType) {
		RenditionProvider provider = getProvider(fromMimeType, toMimeType);
		if (provider != null)
			return provider.convert(fromInputSource, toMimeType);
		return null;
	}

	@Override
	public Set<Method> getMethods() {
		Class<?> clazz = Renderable.class;
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
		fromMimeType = (String) BeanUtils.getFieldWithAnnotation(invocation.getArguments()[0],
				org.springframework.content.commons.annotations.MimeType.class);
		if (fromMimeType == null) {
			return null;
		}
		String toMimeType = (String) invocation.getArguments()[1];

		RenditionProvider pr = this.getProvider(fromMimeType, toMimeType);
		if (pr != null) {
			Resource content = null;
			try {
				content = invoker.invokeGetResource();
				return (Resource) pr.convert(content, toMimeType);
			} catch (Exception e) {
				LOGGER.error(String.format("Failed to get rendition from %s to %s", fromMimeType, toMimeType), e);
			}
		}
		return null;
	}

	public RenditionProvider getProvider(String fromMimeType, String toMimeType) {
		RenditionCapability bestCapability = RenditionCapability.NOT_CAPABLE;
		RenditionProvider bestProvider = null;
		for (RenditionProvider provider : providers) {
			RenditionCapability vote = provider.isCapable(fromMimeType, toMimeType);
			if (vote.isBest())
				return provider; // Return the best provider.
			if (vote.isBetterThan(bestCapability)) {
				bestCapability = vote; // Elect a better provider.
				bestProvider = provider;
			}
		}
		return bestProvider;
	}
}
