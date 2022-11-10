package tourGuide;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import gpsUtil.location.Location;
import org.javamoney.moneta.Money;
import org.junit.Test;

import gpsUtil.GpsUtil;
import gpsUtil.location.VisitedLocation;
import rewardCentral.RewardCentral;
import tourGuide.dto.NearbyAttractionDTO;
import tourGuide.helper.InternalTestHelper;
import tourGuide.model.UserPreferences;
import tourGuide.service.RewardsService;
import tourGuide.service.TourGuideService;
import tourGuide.model.User;
import tripPricer.Provider;

import static org.junit.Assert.*;

public class TestTourGuideService {

	@Test
	public void getUserLocation() {
		GpsUtil gpsUtil = new GpsUtil();
		RewardsService rewardsService = new RewardsService(gpsUtil, new RewardCentral());
		InternalTestHelper.setInternalUserNumber(0);
		TourGuideService tourGuideService = new TourGuideService(gpsUtil, rewardsService);
		
		User user = new User(UUID.randomUUID(), "jon", "000", "jon@tourGuide.com");
		VisitedLocation visitedLocation = tourGuideService.trackUserLocation(user);
		tourGuideService.stopTrackingUsersAndCompleteTasks();
		assertEquals(visitedLocation.userId, user.getUserId());
	}
	
	@Test
	public void addUser() {
		GpsUtil gpsUtil = new GpsUtil();
		RewardsService rewardsService = new RewardsService(gpsUtil, new RewardCentral());
		InternalTestHelper.setInternalUserNumber(0);
		TourGuideService tourGuideService = new TourGuideService(gpsUtil, rewardsService);
		
		User user = new User(UUID.randomUUID(), "jon", "000", "jon@tourGuide.com");
		User user2 = new User(UUID.randomUUID(), "jon2", "000", "jon2@tourGuide.com");

		tourGuideService.addUser(user);
		tourGuideService.addUser(user2);
		
		User retrivedUser = tourGuideService.getUser(user.getUserName());
		User retrivedUser2 = tourGuideService.getUser(user2.getUserName());

		tourGuideService.stopTrackingUsersAndCompleteTasks();
		
		assertEquals(user, retrivedUser);
		assertEquals(user2, retrivedUser2);
	}
	
	@Test
	public void getAllUsers() {
		GpsUtil gpsUtil = new GpsUtil();
		RewardsService rewardsService = new RewardsService(gpsUtil, new RewardCentral());
		InternalTestHelper.setInternalUserNumber(0);
		TourGuideService tourGuideService = new TourGuideService(gpsUtil, rewardsService);
		
		User user = new User(UUID.randomUUID(), "jon", "000", "jon@tourGuide.com");
		User user2 = new User(UUID.randomUUID(), "jon2", "000", "jon2@tourGuide.com");

		tourGuideService.addUser(user);
		tourGuideService.addUser(user2);
		
		List<User> allUsers = tourGuideService.getAllUsers();

		tourGuideService.stopTrackingUsersAndCompleteTasks();
		
		assertTrue(allUsers.contains(user));
		assertTrue(allUsers.contains(user2));
	}

	@Test
	public void trackUser() {
		GpsUtil gpsUtil = new GpsUtil();
		RewardsService rewardsService = new RewardsService(gpsUtil, new RewardCentral());
		InternalTestHelper.setInternalUserNumber(0);
		TourGuideService tourGuideService = new TourGuideService(gpsUtil, rewardsService);
		
		User user = new User(UUID.randomUUID(), "jon", "000", "jon@tourGuide.com");
		VisitedLocation visitedLocation = tourGuideService.trackUserLocation(user);

		tourGuideService.stopTrackingUsersAndCompleteTasks();
		
		assertEquals(user.getUserId(), visitedLocation.userId);
	}

	@Test
	public void getNearbyAttractions() {
		GpsUtil gpsUtil = new GpsUtil();
		RewardsService rewardsService = new RewardsService(gpsUtil, new RewardCentral());
		InternalTestHelper.setInternalUserNumber(0);
		TourGuideService tourGuideService = new TourGuideService(gpsUtil, rewardsService);
		
		User user = new User(UUID.randomUUID(), "jon", "000", "jon@tourGuide.com");
		VisitedLocation visitedLocation = tourGuideService.trackUserLocation(user);
		user.addToVisitedLocations(visitedLocation);
		tourGuideService.addUser(user);

		List<NearbyAttractionDTO> attractions = tourGuideService.getNearByAttractions(user.getUserName());

		tourGuideService.stopTrackingUsersAndCompleteTasks();
		assertEquals(5, attractions.size());
	}

	@Test
	public void getTripDeals() {
		GpsUtil gpsUtil = new GpsUtil();
		RewardsService rewardsService = new RewardsService(gpsUtil, new RewardCentral());
		InternalTestHelper.setInternalUserNumber(0);
		TourGuideService tourGuideService = new TourGuideService(gpsUtil, rewardsService);
		
		User user = new User(UUID.randomUUID(), "jon", "000", "jon@tourGuide.com");

		List<Provider> providers = tourGuideService.getTripDeals(user);

		tourGuideService.stopTrackingUsersAndCompleteTasks();
		assertEquals(5, providers.size());
	}

	@Test
	public void getAllCurrentLocations() {
		GpsUtil gpsUtil = new GpsUtil();
		RewardsService rewardsService = new RewardsService(gpsUtil, new RewardCentral());
		InternalTestHelper.setInternalUserNumber(5);
		TourGuideService tourGuideService = new TourGuideService(gpsUtil, rewardsService);

		Map<String, Location> locations = tourGuideService.getAllCurrentLocations();

		tourGuideService.stopTrackingUsersAndCompleteTasks();
		assertEquals(5, locations.size());
	}

	@Test
	public void setUserPreferences() {
		GpsUtil gpsUtil = new GpsUtil();
		RewardsService rewardsService = new RewardsService(gpsUtil, new RewardCentral());
		InternalTestHelper.setInternalUserNumber(0);
		TourGuideService tourGuideService = new TourGuideService(gpsUtil, rewardsService);

		User user = new User(UUID.randomUUID(), "jon", "000", "jon@tourGuide.com");

		UserPreferences preferencesNotChanged = tourGuideService.setUserPreferences(user,
				null, null, null, null, null, null, null,null);
		UserPreferences preferencesChanged = tourGuideService.setUserPreferences(user,
				"EUR", 100, 2.50, 50.50, 2, 3, 2,1);

		assertNotNull(preferencesNotChanged.getHighPricePoint());
		assertNotNull(preferencesNotChanged.getLowerPricePoint());
		assertEquals(100, preferencesChanged.getAttractionProximity());
		assertEquals(Money.of(2.50, "EUR"), preferencesChanged.getLowerPricePoint());
		assertEquals(Money.of(50.50, "EUR"), preferencesChanged.getHighPricePoint());
		assertEquals(2, preferencesChanged.getTripDuration());
		assertEquals(3, preferencesChanged.getTicketQuantity());
		assertEquals(2, preferencesChanged.getNumberOfAdults());
		assertEquals(1, preferencesChanged.getNumberOfChildren());

	}
	
	
}
