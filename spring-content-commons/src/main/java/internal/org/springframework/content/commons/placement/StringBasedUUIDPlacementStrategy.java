package internal.org.springframework.content.commons.placement;

import java.util.UUID;

import org.springframework.content.commons.placement.PlacementStrategy;
import org.springframework.util.Assert;

public class StringBasedUUIDPlacementStrategy implements PlacementStrategy<String> {

	@Override
	public String getLocation(String contentId) {
		Assert.notNull(contentId);
		return new UUIDPlacementStrategy().getLocation(UUID.fromString(contentId));
	}

}
