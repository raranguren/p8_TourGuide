package tourGuide.dto;

import gpsUtil.location.Attraction;
import gpsUtil.location.Location;

public class NearbyAttractionDTO {

    // Name of Tourist attraction,
    // Tourist attractions lat/long,
    // The user's location lat/long,
    // The distance in miles between the user's location and each of the attractions.
    // The reward points for visiting each Attraction.
    //    Note: Attraction reward points can be gathered from RewardsCentral

    String attractionName;
    Location attractionLocation;
    Location userLocation;
    double distanceInMiles;
    int rewardPoints;

    public NearbyAttractionDTO(Attraction attraction, Location userLocation, double distanceInMiles, int rewardPoints) {
        this.attractionName = attraction.attractionName;
        this.attractionLocation = attraction; // downcast
        this.userLocation = userLocation;
        this.distanceInMiles = distanceInMiles;
        this.rewardPoints = rewardPoints;
    }
}
