package tourGuide;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import gpsUtil.GpsUtil;
import rewardCentral.RewardCentral;

@Configuration
public class TourGuideConfiguration {

	// Values in miles for the rewards calculations
	public static final double STATUTE_MILES_PER_NAUTICAL_MILE = 1.15077945;

	public static final int DEFAULT_PROXIMITY_BUFFER = 10;
	public static final int ATTRACTION_PROXIMITY_RANGE = 200;

	// Test mode
	public static final boolean IS_TEST_MODE_ENABLED = true;

	// Beans to inject external libraries in services

	@Bean
	public GpsUtil getGpsUtil() {
		return new GpsUtil();
	}

	@Bean
	public RewardCentral getRewardCentral() {
		return new RewardCentral();
	}

}
