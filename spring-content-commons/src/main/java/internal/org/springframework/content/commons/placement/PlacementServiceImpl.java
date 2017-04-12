package internal.org.springframework.content.commons.placement;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.placement.PlacementService;
import org.springframework.content.commons.placement.PlacementStrategy;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.util.Assert;

public class PlacementServiceImpl implements PlacementService, InitializingBean {

	@Autowired(required=false)
	private List<PlacementStrategy<?>> plugins = new ArrayList<>();
	
	public PlacementServiceImpl() {
	}
	
	/* package */ PlacementServiceImpl(List<PlacementStrategy<?>> plugins) {
		this.plugins = plugins;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public String getLocation(Object contentId) {
		Assert.notNull(contentId);
		
		for (PlacementStrategy plugin : plugins) {
			try {
				plugin.getClass().getMethod("getLocation", contentId.getClass());
				return plugin.getLocation(contentId);
			} catch (NoSuchMethodException nsme) {
				//ignore, dont care!
			}
		}
		return null;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		AnnotationAwareOrderComparator.sort(plugins);
	}
}
