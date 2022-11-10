package tourGuide;

import java.util.List;
import java.util.Map;

import gpsUtil.location.Location;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jsoniter.output.JsonStream;

import gpsUtil.location.VisitedLocation;
import tourGuide.model.UserPreferences;
import tourGuide.service.TourGuideService;
import tourGuide.model.User;
import tripPricer.Provider;

@RestController
public class TourGuideController {

	@Autowired
	TourGuideService tourGuideService;
	
    @RequestMapping("/")
    public String index() {
        return "Greetings from TourGuide!";
    }
    
    @RequestMapping("/getLocation") 
    public String getLocation(@RequestParam String userName) {
    	VisitedLocation visitedLocation = tourGuideService.getUserLocation(getUser(userName));
		return JsonStream.serialize(visitedLocation.location);
    }
    
    @RequestMapping("/getNearbyAttractions")
    public String getNearbyAttractions(@RequestParam String userName) {
    	return JsonStream.serialize(tourGuideService.getNearByAttractions(userName));
    }
    
    @RequestMapping("/getRewards") 
    public String getRewards(@RequestParam String userName) {
    	return JsonStream.serialize(tourGuideService.getUserRewards(getUser(userName)));
    }

    // Get a list of every user's most recent location as JSON
    //- Note: does not use gpsUtil to query for their current location,
    //        but rather gathers the user's current location from their stored location history.
    //
    // Return object should be the just a JSON mapping of userId to Locations similar to:
    //     {
    //        "019b04a9-067a-4c76-8817-ee75088c3822": {"longitude":-48.188821,"latitude":74.84371}
    //        ...
    //     }
    @RequestMapping("/getAllCurrentLocations")
    public String getAllCurrentLocations() {
        Map<String, Location> locations = tourGuideService.getAllCurrentLocations();
        return JsonStream.serialize(locations);
    }
    
    @RequestMapping("/getTripDeals")
    public String getTripDeals(@RequestParam String userName) {
    	List<Provider> providers = tourGuideService.getTripDeals(getUser(userName));
    	return JsonStream.serialize(providers);
    }

    @RequestMapping("/setUserPreferences")
    public String setUserPreferences(@RequestParam String userName,
                                 @RequestParam(required = false) String currency,
                                 @RequestParam(required = false) Integer attractionProximity,
                                 @RequestParam(required = false) Double lowerPricePoint,
                                 @RequestParam(required = false) Double highPricePoint,
                                 @RequestParam(required = false) Integer tripDuration,
                                 @RequestParam(required = false) Integer ticketQuantity,
                                 @RequestParam(required = false) Integer numberOfAdults,
                                 @RequestParam(required = false) Integer numberOfChildren) {
        UserPreferences updatedPreferences = tourGuideService.setUserPreferences(
                getUser(userName), currency, attractionProximity, lowerPricePoint, highPricePoint,
                tripDuration, ticketQuantity, numberOfAdults, numberOfChildren);
        return JsonStream.serialize(updatedPreferences);
    }
    
    private User getUser(String userName) {
    	return tourGuideService.getUser(userName);
    }
   

}