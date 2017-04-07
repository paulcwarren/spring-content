package internal.org.springframework.content.commons.placement;

import java.util.UUID;

import org.springframework.content.commons.placement.PlacementStrategy;
import org.springframework.util.Assert;

public class UUIDPlacementStrategy implements PlacementStrategy<UUID> {

	public UUIDPlacementStrategy() {
	}

	@Override
	public String getLocation(UUID contentId) {
		Assert.notNull(contentId);
		String loc = contentId.toString();
		loc = loc.replaceAll("-","/");
		return String.format("/%s", loc);
	}

}
