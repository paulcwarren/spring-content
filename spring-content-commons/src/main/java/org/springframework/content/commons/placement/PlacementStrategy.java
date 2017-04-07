package org.springframework.content.commons.placement;

public interface PlacementStrategy<T> {
	
	String getLocation(T contentId);
	
}
