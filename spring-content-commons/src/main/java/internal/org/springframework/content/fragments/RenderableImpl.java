package internal.org.springframework.content.fragments;

import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.fragments.ContentStoreAware;
import org.springframework.content.commons.mappingcontext.ContentProperty;
import org.springframework.content.commons.mappingcontext.MappingContext;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.renditions.Renderable;
import org.springframework.content.commons.renditions.RenditionProvider;
import org.springframework.content.commons.renditions.RenditionService;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.utils.BeanUtils;
import org.springframework.core.io.Resource;

import internal.org.springframework.content.commons.renditions.RenditionServiceImpl;

public class RenderableImpl implements Renderable, ContentStoreAware {

    private static final Log LOGGER = LogFactory.getLog(RenderableImpl.class);

    private org.springframework.content.commons.store.ContentStore store;
	private ContentStore<Object, Serializable> contentStore;

    private MappingContext mappingContext;

    private RenditionService renditionService = null;

    private List<RenditionProvider> providers = new ArrayList<>();

    public RenderableImpl() {
       this.mappingContext = new MappingContext("/", ".");
	}

	@Autowired(required=false)
    public void setRenditionProviders(RenditionProvider... providers) {
        for (RenditionProvider provider : providers) {
            this.providers.add(provider);
        }
    }

	@Override
	public void setDomainClass(Class<?> domainClass) {
	}

	@Override
	public void setIdClass(Class<?> idClass) {
	}

	@Override
    public void setContentStore(ContentStore store) {
		this.contentStore = store;
	}

    @Override
    public void setContentStore(org.springframework.content.commons.store.ContentStore store) {
        this.store = store;
    }

    public RenditionService getRenditionService() {
	    if (this.renditionService == null) {
	        this.renditionService = new RenditionServiceImpl(providers.toArray(new RenditionProvider[0]));
	    }
	    return this.renditionService;
	}

	@Autowired(required = false)
	public void setRenditionService(RenditionService renditionService) {
	    this.renditionService = renditionService;
	}

	@Override
	public InputStream getRendition(Object entity, String mimeType) {
		String fromMimeType = null;
		fromMimeType = (String) BeanUtils.getFieldWithAnnotation(entity, org.springframework.content.commons.annotations.MimeType.class);
		if (fromMimeType == null) {
			return null;
		}

		if (this.getRenditionService().canConvert(fromMimeType, mimeType)) {
			InputStream content = null;
			try {
                if (store != null) {
                    content = store.getContent(entity);
                } else if (contentStore != null) {
                    content = contentStore.getContent(entity);
                }
				if (content != null) {
					return this.getRenditionService().convert(fromMimeType, content, mimeType);
				}
			}
			catch (Exception e) {
				LOGGER.error(String.format("Failed to get rendition from %s to %s", fromMimeType, mimeType), e);
			}
		}
		return null;
	}

    @Override
    public InputStream getRendition(Object entity, PropertyPath propertyPath, String mimeType) {

        Object fromMimeType = null;

        ContentProperty property = this.mappingContext.getContentProperty(entity.getClass(), propertyPath.getName());
        if (property == null) {
            return null;
        }

        fromMimeType = property.getMimeType(entity);

        if (fromMimeType == null) {
            return null;
        }

        if (this.getRenditionService().canConvert(fromMimeType.toString(), mimeType)) {
            try {
                Resource r = null;
                if (store != null) {
                    r = store.getResource(entity, propertyPath);
                } else if (contentStore != null) {
                    r = contentStore.getResource(entity, propertyPath);
                }
                if (r != null) {
                    try (InputStream content = r.getInputStream()) {
                        if (content != null) {
                            return this.getRenditionService().convert(fromMimeType.toString(), content, mimeType);
                        }
                    }
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

        return this.getRenditionService().canConvert(fromMimeType, mimeType);
    }

    @Override
    public boolean hasRendition(Object entity, PropertyPath propertyPath, String mimeType) {

        ContentProperty property = this.mappingContext.getContentProperty(entity.getClass(), propertyPath.getName());
        // todo: property == null

        Object fromMimeType = null;
        fromMimeType = property.getMimeType(entity);
        if (fromMimeType == null) {
            return false;
        }

        return this.getRenditionService().canConvert(fromMimeType.toString(), mimeType);
    }
}
