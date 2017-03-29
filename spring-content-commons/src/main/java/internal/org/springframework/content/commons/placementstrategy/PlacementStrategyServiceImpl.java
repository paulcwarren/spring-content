package internal.org.springframework.content.commons.placementstrategy;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.placementstrategy.PlacementStrategy;
import org.springframework.content.commons.placementstrategy.PlacementStrategyService;
import org.springframework.util.Assert;

public class PlacementStrategyServiceImpl implements PlacementStrategyService {

	@Autowired
	private List<PlacementStrategy<?>> plugins;
	
	public PlacementStrategyServiceImpl() {
	}
	
	/* package */ PlacementStrategyServiceImpl(List<PlacementStrategy<?>> plugins) {
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
