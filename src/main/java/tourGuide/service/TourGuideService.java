package tourGuide.service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;
import tourGuide.dto.NearbyAttractionDTO;
import tourGuide.helper.InternalTestHelper;
import tourGuide.tracker.Tracker;
import tourGuide.model.User;
import tourGuide.model.UserReward;
import tripPricer.Provider;
import tripPricer.TripPricer;

import static java.lang.Math.min;
import static tourGuide.TourGuideConfiguration.IS_TEST_MODE_ENABLED;

@Service
public class TourGuideService {
    private final Logger logger = LoggerFactory.getLogger(TourGuideService.class);
    private final GpsUtil gpsUtil;
    private final RewardsService rewardsService;
    private final TripPricer tripPricer = new TripPricer();
    public final Tracker tracker;

    private final ExecutorService executorService = Executors.newFixedThreadPool(100);

    public TourGuideService(GpsUtil gpsUtil, RewardsService rewardsService) {
        this.gpsUtil = gpsUtil;
        Locale.setDefault(Locale.US); // needed for GpsUtil to function
        this.rewardsService = rewardsService;

        if (IS_TEST_MODE_ENABLED) {
            logger.info("TestMode enabled");
            logger.debug("Initializing users");
            initializeInternalUsers();
            logger.debug("Finished initializing users");
        }
        tracker = new Tracker(this);
        addShutDownHook();
    }

    public List<UserReward> getUserRewards(User user) {
        return user.getUserRewards();
    }

    public VisitedLocation getUserLocation(User user) {
        return (user.getVisitedLocations().size() > 0) ?
                user.getLastVisitedLocation() :
                trackUserLocation(user);
    }

    public User getUser(String userName) {
        return internalUserMap.get(userName);
    }

    public List<User> getAllUsers() {
        return new ArrayList<>(internalUserMap.values());
    }

    public void addUser(User user) {
        if (!internalUserMap.containsKey(user.getUserName())) {
            internalUserMap.put(user.getUserName(), user);
        }
    }

    public List<Provider> getTripDeals(User user) {
        // TODO use TripPricerTask, executor at class level
        int cumulatativeRewardPoints = user.getUserRewards().stream().mapToInt(UserReward::getRewardPoints).sum();
        List<Provider> providers = tripPricer.getPrice(tripPricerApiKey, user.getUserId(), user.getUserPreferences().getNumberOfAdults(),
                user.getUserPreferences().getNumberOfChildren(), user.getUserPreferences().getTripDuration(), cumulatativeRewardPoints);
        user.setTripDeals(providers);
        return providers;
    }

    public VisitedLocation trackUserLocation(User user) {
        // TODO use completable future, and executor at Class level
        VisitedLocation visitedLocation = gpsUtil.getUserLocation(user.getUserId());
        user.addToVisitedLocations(visitedLocation);
        rewardsService.calculateRewards(user);
        return visitedLocation;
    }

    public List<NearbyAttractionDTO> getNearByAttractions(String userName) {
        User user = getUser(userName);
        Location location = getUserLocation(user).location;
        List<NearbyAttractionDTO> nearbyAttractions = new ArrayList<>();

        // Sort attractions by their distance to the user's location
        Map<Attraction, Double> distances = gpsUtil.getAttractions().stream().parallel()
                .collect(Collectors.toMap(
                        attraction -> attraction,
                        attraction -> rewardsService.getDistance(location, attraction)
                ));
        List<Attraction> attractionsSortedByDistanceAsc = distances.keySet().stream().parallel()
                .sorted(Comparator.comparingDouble(distances::get))
                .collect(Collectors.toList());

        // Return DTO for the 5 closest attractions
        int numberOfAttractions = min(5, distances.size());
        for (int i=0; i<numberOfAttractions; i++) {
            Attraction attraction = attractionsSortedByDistanceAsc.get(i);
            nearbyAttractions.add(new NearbyAttractionDTO(
                    attraction,
                    location,
                    distances.get(attraction),
                    rewardsService.getRewardPoints(attraction, user)));
        }
        return nearbyAttractions;
    }

    public void shutdown() {
        tracker.stopTracking();
        executorService.shutdown();
    }

    private void addShutDownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
    }

    /**********************************************************************************
     * Methods Below: For Internal Testing
     **********************************************************************************/
    private static final String tripPricerApiKey = "test-server-api-key";
    // Database connection will be used for external users, but for testing purposes internal users are provided and stored in memory
    private final Map<String, User> internalUserMap = new HashMap<>();

    private void initializeInternalUsers() {
        IntStream.range(0, InternalTestHelper.getInternalUserNumber()).forEach(i -> {
            String userName = "internalUser" + i;
            String phone = "000";
            String email = userName + "@tourGuide.com";
            User user = new User(UUID.randomUUID(), userName, phone, email);
            generateUserLocationHistory(user);

            internalUserMap.put(userName, user);
        });
        logger.debug("Created " + InternalTestHelper.getInternalUserNumber() + " internal test users.");
    }

    private void generateUserLocationHistory(User user) {
        for (int i = 0; i<4; i++) {
            user.addToVisitedLocations(new VisitedLocation(
                    user.getUserId(),
                    new Location(generateRandomLatitude(), generateRandomLongitude()),
                    getRandomTime()));
        }
    }

    private double generateRandomLongitude() {
        double leftLimit = -180;
        double rightLimit = 180;
        return leftLimit + new Random().nextDouble() * (rightLimit - leftLimit);
    }

    private double generateRandomLatitude() {
        double leftLimit = -85.05112878;
        double rightLimit = 85.05112878;
        return leftLimit + new Random().nextDouble() * (rightLimit - leftLimit);
    }

    private Date getRandomTime() {
        LocalDateTime localDateTime = LocalDateTime.now().minusDays(new Random().nextInt(30));
        return Date.from(localDateTime.toInstant(ZoneOffset.UTC));
    }

}
