package internal.org.springframework.content.fragments;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.fragments.ContentStoreAware;
import org.springframework.content.commons.renditions.Renderable;
import org.springframework.content.commons.renditions.RenditionProvider;
import org.springframework.content.commons.renditions.RenditionService;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.repository.StoreInvoker;
import org.springframework.content.commons.utils.BeanUtils;
import org.springframework.util.MimeType;

public class RenderableImpl implements Renderable, RenditionService, ContentStoreAware {

	private static final Log LOGGER = LogFactory.getLog(RenderableImpl.class);

	private List<RenditionProvider> providers = new ArrayList<RenditionProvider>();
	private ContentStore contentStore;
	private Class<?> domainClass;
	private Class<?> idClass;
	private StoreInvoker storeInvoker;

	public RenderableImpl() {
	}

	@Override
	public void setDomainClass(Class<?> domainClass) {
		this.domainClass = domainClass;
	}

	@Override
	public void setIdClass(Class<?> idClass) {
		this.idClass = idClass;
	}

	@Override
    public void setContentStore(ContentStore store) {
		this.contentStore = store;
	}

	@Autowired(required = false)
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
	public InputStream convert(String fromMimeType, InputStream fromInputSource,
			String toMimeType) {
		for (RenditionProvider provider : providers) {
			if (MimeType.valueOf(fromMimeType)
					.includes(MimeType.valueOf(provider.consumes()))) {
				for (String produce : provider.produces()) {
					if (MimeType.valueOf(toMimeType)
							.includes(MimeType.valueOf(produce))) {
						return provider.convert(fromInputSource, toMimeType);
					}
				}
			}
		}
		return null;
	}

	@Override
	public InputStream getRendition(Object entity, String mimeType) {
		String fromMimeType = null;
		fromMimeType = (String) BeanUtils.getFieldWithAnnotation(entity, org.springframework.content.commons.annotations.MimeType.class);
		if (fromMimeType == null) {
			return null;
		}

		if (this.canConvert(fromMimeType, mimeType)) {
			InputStream content = null;
			try {
				content = contentStore.getContent(entity);
				if (content != null) {
					return this.convert(fromMimeType, content, mimeType);
				}
			}
			catch (Exception e) {
				LOGGER.error(String.format("Failed to get rendition from %s to %s", fromMimeType, mimeType), e);
			}
		}
		return null;
	}

    @Override
    public boolean hasRendition(Object entity, String mimeType) {

        String fromMimeType = null;
        fromMimeType = (String) BeanUtils.getFieldWithAnnotation(entity, org.springframework.content.commons.annotations.MimeType.class);
        if (fromMimeType == null) {
            return false;
        }

        return this.canConvert(fromMimeType, mimeType);
    }
}
