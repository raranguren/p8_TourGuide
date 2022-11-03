package tourGuide.service;

import java.util.*;
import java.util.concurrent.*;

import org.springframework.stereotype.Service;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;
import rewardCentral.RewardCentral;
import tourGuide.model.User;
import tourGuide.model.UserReward;

@Service
public class RewardsService {
    private static final double STATUTE_MILES_PER_NAUTICAL_MILE = 1.15077945;

	// proximity in miles
    private int defaultProximityBuffer = 10;
	private int proximityBuffer = defaultProximityBuffer;
	private int attractionProximityRange = 200;
	private final GpsUtil gpsUtil;
	private final RewardCentral rewardsCentral;

	public RewardsService(GpsUtil gpsUtil, RewardCentral rewardCentral) {
		this.gpsUtil = gpsUtil;
		this.rewardsCentral = rewardCentral;
	}

	public void setProximityBuffer(int proximityBuffer) {
		this.proximityBuffer = proximityBuffer;
	}

	public void setDefaultProximityBuffer() {
		proximityBuffer = defaultProximityBuffer;
	}

	public void calculateRewards(User user) {
		List<VisitedLocation> userLocations = user.getVisitedLocations();
		List<Attraction> attractions = gpsUtil.getAttractions();

		Collection<Callable<UserReward>> tasks = new ArrayList<>();

		for (Attraction attraction : attractions) {
			for (VisitedLocation visitedLocation : userLocations) {
				if (nearAttraction(visitedLocation, attraction)) {
					tasks.add(() ->
							getRewardPoints(attraction, user)
									.thenApplyAsync((points) -> new UserReward(visitedLocation, attraction, points))
									.get()
					);
					break; // if an attraction is visited then there is no need to loop through visited locations
				}
			}
		}

		try {
			List<Future<UserReward>> rewardFutures = Executors
					.newCachedThreadPool()
					.invokeAll(tasks);
			for (Future<UserReward> future : rewardFutures) {
				user.addUserReward(future.get()); // this setter verifies if the reward exists
			}
		} catch (InterruptedException | ExecutionException e) {
			throw new RuntimeException(e);
		}

	}

	public boolean isWithinAttractionProximity(Attraction attraction, Location location) {
		return getDistance(attraction, location) > attractionProximityRange ? false : true;
	}

	private boolean nearAttraction(VisitedLocation visitedLocation, Attraction attraction) {
		return getDistance(attraction, visitedLocation.location) > proximityBuffer ? false : true;
	}

	CompletableFuture<Integer> getRewardPoints(Attraction attraction, User user) {
		return CompletableFuture.supplyAsync( () ->
			rewardsCentral.getAttractionRewardPoints(attraction.attractionId, user.getUserId())
		);
	}

	public double getDistance(Location loc1, Location loc2) {
        double lat1 = Math.toRadians(loc1.latitude);
        double lon1 = Math.toRadians(loc1.longitude);
        double lat2 = Math.toRadians(loc2.latitude);
        double lon2 = Math.toRadians(loc2.longitude);

        double angle = Math.acos(Math.sin(lat1) * Math.sin(lat2)
                               + Math.cos(lat1) * Math.cos(lat2) * Math.cos(lon1 - lon2));

        double nauticalMiles = 60 * Math.toDegrees(angle);
        double statuteMiles = STATUTE_MILES_PER_NAUTICAL_MILE * nauticalMiles;
        return statuteMiles;
	}

}
