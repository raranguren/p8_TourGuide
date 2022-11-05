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

import static tourGuide.TourGuideConfiguration.*;

@Service
public class RewardsService {

	// proximity in miles
    private final int defaultProximityBuffer = DEFAULT_PROXIMITY_BUFFER;
	private int proximityBuffer = defaultProximityBuffer;
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

		List<Callable<UserReward>> tasks = new ArrayList<>();

		for (Attraction attraction : attractions) {
			for (VisitedLocation visitedLocation : userLocations) {
				if (nearAttraction(visitedLocation, attraction)) {
					tasks.add(() ->  new UserReward(visitedLocation, attraction, getRewardPoints(attraction, user)));
					break; // no need to loop through the rest of locations if the user was found to be near already
				}
			}
		}

		ExecutorService executorService = Executors.newCachedThreadPool();
		try {
			List<Future<UserReward>> rewardFutures = executorService.invokeAll(tasks);
			for (Future<UserReward> future : rewardFutures) {
				user.addUserReward(future.get()); // this setter filters out duplicated rewards
			}
		} catch (InterruptedException | ExecutionException e) {
			throw new RuntimeException(e);
		} finally {
			executorService.shutdown();
		}

	}

	public boolean isWithinAttractionProximity(Attraction attraction, Location location) {
		return !(getDistance(attraction, location) > ATTRACTION_PROXIMITY_RANGE);
	}

	private boolean nearAttraction(VisitedLocation visitedLocation, Attraction attraction) {
		return !(getDistance(attraction, visitedLocation.location) > proximityBuffer);
	}

	private Integer getRewardPoints(Attraction attraction, User user) {
		return rewardsCentral.getAttractionRewardPoints(attraction.attractionId, user.getUserId());
	}

	public double getDistance(Location loc1, Location loc2) {
        double lat1 = Math.toRadians(loc1.latitude);
        double lon1 = Math.toRadians(loc1.longitude);
        double lat2 = Math.toRadians(loc2.latitude);
        double lon2 = Math.toRadians(loc2.longitude);

        double angle = Math.acos(Math.sin(lat1) * Math.sin(lat2)
                               + Math.cos(lat1) * Math.cos(lat2) * Math.cos(lon1 - lon2));

        double nauticalMiles = 60 * Math.toDegrees(angle);
		return STATUTE_MILES_PER_NAUTICAL_MILE * nauticalMiles;
	}

}
