package tourGuide.service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.*;
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

import static tourGuide.TourGuideConfiguration.IS_TEST_MODE_ENABLED;

@Service
public class TourGuideService {
    private final Logger logger = LoggerFactory.getLogger(TourGuideService.class);
    private final GpsUtil gpsUtil;
    private final RewardsService rewardsService;
    private final TripPricer tripPricer = new TripPricer();
    private final Tracker tracker;
    public final ExecutorService executorService;

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
        executorService = Executors.newFixedThreadPool(600);
        addShutDownHook();
    }

    public List<UserReward> getUserRewards(User user) {
        return user.getUserRewards();
    }

    public VisitedLocation getUserLocation(User user) {
        return user.getLastVisitedLocation()
                .orElse(trackUserLocation(user));
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
        int RewardPointsTotal = 0;
        for (UserReward reward : user.getUserRewards()) {
            RewardPointsTotal += reward.getRewardPoints();
        }
        List<Provider> providers = tripPricer.getPrice(tripPricerApiKey, user.getUserId(), user.getUserPreferences().getNumberOfAdults(),
                user.getUserPreferences().getNumberOfChildren(), user.getUserPreferences().getTripDuration(), RewardPointsTotal);
        user.setTripDeals(providers);
        return providers;
    }

    public VisitedLocation trackUserLocation(User user) {
        VisitedLocation visitedLocation = gpsUtil.getUserLocation(user.getUserId());
        user.addToVisitedLocations(visitedLocation);
        rewardsService.calculateRewards(user);
        return visitedLocation;
    }

    //  Get the closest five tourist attractions to the user - no matter how far away they are.
    //  Return a new JSON object that contains:
    //   - Name of Tourist attraction,
    //   - Tourist attractions lat/long,
    //   - The user's location lat/long,
    //   - The distance in miles between the user's location and each of the attractions.
    //   - The reward points for visiting each Attraction.
    //  Note: Attraction reward points can be gathered from RewardsCentral
    public List<NearbyAttractionDTO> getNearByAttractions(String userName) {
        User user = getUser(userName);
        Location userLocation = getUserLocation(user).location;
        Map<Attraction, Double> distances = new HashMap<>();
        PriorityQueue<Attraction> attractions = new PriorityQueue<>(Comparator.comparingDouble(distances::get));

        for (Attraction attraction : gpsUtil.getAttractions()) {
            distances.put(attraction, rewardsService.getDistance(userLocation, attraction));
            attractions.add(attraction);
        }

        int RESPONSE_SIZE = 5;
        List<Callable<NearbyAttractionDTO>> tasks = new ArrayList<>();
        for (int i = 0; i < RESPONSE_SIZE; i++) {
            Attraction attraction = attractions.poll();
            if (attraction != null) {
                tasks.add(()->{
                    int rewardPoints = rewardsService.getRewardPoints(attraction, user);
                    return new NearbyAttractionDTO(
                            attraction, userLocation, distances.get(attraction), rewardPoints);
                });
            }
        }

        try {
            List<NearbyAttractionDTO> nearbyAttractions = new ArrayList<>();
            for (Future<NearbyAttractionDTO> future : executorService.invokeAll(tasks)) {
                nearbyAttractions.add(future.get());
            }
            return nearbyAttractions;
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, Location> getAllCurrentLocations() {
        Map<String, Location> locations = new ConcurrentHashMap<>();
        for (User user : getAllUsers()) {
            String uuid = user.getUserId().toString();
            Location location = getUserLocation(user).location;
            locations.put(uuid, location);
        }
        return locations;
    }

    public void stopTrackingUsersAndCompleteTasks() {
        tracker.stopTracking();
        logger.debug("Tracker stopped. Completing tasks . . .");
        executorService.shutdown();
        int minutes = 0;
        while (true) {
            try {
                if (executorService.awaitTermination(1, TimeUnit.MINUTES)) break;
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            logger.debug("Completing tasks . . . (elapsed {} minutes)", ++minutes);
        }
    }

    private void addShutDownHook() {
        Runtime.getRuntime().addShutdownHook(
                new Thread(this::stopTrackingUsersAndCompleteTasks, "shutdown hook"));
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
