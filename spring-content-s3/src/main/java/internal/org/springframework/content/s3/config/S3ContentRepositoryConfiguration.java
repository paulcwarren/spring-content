package internal.org.springframework.content.s3.config;

import java.util.UUID;

import org.springframework.content.commons.placementstrategy.PlacementStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import internal.org.springframework.content.commons.placementstrategy.UUIDPlacementStrategy;

@Configuration
public class S3ContentRepositoryConfiguration {

	@Bean
	public PlacementStrategy<UUID> uuidPlacement() {
		return new UUIDPlacementStrategy();
	}
	
}
