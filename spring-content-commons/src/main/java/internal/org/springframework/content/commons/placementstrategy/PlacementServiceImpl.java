package internal.org.springframework.content.commons.placementstrategy;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.placementstrategy.PlacementStrategy;
import org.springframework.content.commons.placementstrategy.PlacementService;
import org.springframework.util.Assert;

public class PlacementServiceImpl implements PlacementService {

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
}
