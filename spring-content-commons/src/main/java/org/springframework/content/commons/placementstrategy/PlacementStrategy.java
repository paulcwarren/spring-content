package org.springframework.content.commons.placementstrategy;

public interface PlacementStrategy<T> {
	
	String getLocation(T contentId);
	
}
