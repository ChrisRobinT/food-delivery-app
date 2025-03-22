package com.crt.fujitsu.food_delivery_app;

import org.junit.jupiter.api.BeforeEach;
import com.crt.fujitsu.food_delivery_app.model.WeatherData;
import com.crt.fujitsu.food_delivery_app.repository.WeatherDataRepository;
import com.crt.fujitsu.food_delivery_app.service_logic.DeliveryFeeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class FoodDeliveryAppApplicationTests {

    @Autowired
    private WeatherDataRepository weatherDataRepository;

	@BeforeEach
	void clear() {
		weatherDataRepository.deleteAll();
	}


	// Classic tests
	@Test
	void testCalculateDeliveryTallinnAndCar() {
		String city = "Tallinn";
		String vehicle = "Car";

		WeatherData testWeatherData = new WeatherData();
		testWeatherData.setStationName("Tallinn-Harku");
		testWeatherData.setAirTemperature(5.0); 	// Normal temperature
		testWeatherData.setWindSpeed(5.0);      	// Normal wind speed
		testWeatherData.setPhenomenon("clear");  	// No weather phenomena
		testWeatherData.setObservationTimestamp(LocalDateTime.now());

		weatherDataRepository.save(testWeatherData);

		double expectedFee = 4.0;

		DeliveryFeeService deliveryFeeService = new DeliveryFeeService(weatherDataRepository);

		double actualFee = Double.parseDouble(deliveryFeeService.calculateDeliveryFee(city, vehicle)
				.replace("Total delivery fee is ", "")
				.replace(" €", ""));

		assertEquals(expectedFee, actualFee, "The delivery fee should match the expected value.");
	}

	@Test
	void testCalculateDeliveryFee_NoCitySpecified() {
		DeliveryFeeService deliveryFeeService = new DeliveryFeeService(weatherDataRepository);

		String vehicle = "Car";

		String result = deliveryFeeService.calculateDeliveryFee(null, vehicle);

		assertEquals("No city specified", result);
	}

	@Test
	void testCalculateDeliveryFee_NoVehicleSpecified() {
		DeliveryFeeService deliveryFeeService = new DeliveryFeeService(weatherDataRepository);

		String city = "Tallinn";

		String result = deliveryFeeService.calculateDeliveryFee(city, null);

		assertEquals("No vehicle type specified", result);
	}

	@Test
	void testCalculateDeliveryFee_InvalidVehicle() {
		DeliveryFeeService deliveryFeeService = new DeliveryFeeService(weatherDataRepository);

		String city = "Tallinn";
		String vehicle = "Submarine";

		String result = deliveryFeeService.calculateDeliveryFee(city, vehicle);

		assertEquals("Invalid vehicle type: " + vehicle, result);
	}

	@Test
	void testCalculateDeliveryFee_UnknownCity() {
		DeliveryFeeService deliveryFeeService = new DeliveryFeeService(weatherDataRepository);

		String city = "Tõrva";
		String vehicle = "Car";

		String result = deliveryFeeService.calculateDeliveryFee(city, vehicle);

		assertEquals("Unknown city: " + city, result);
	}

	@Test
	void testCalculateDeliveryFee_WeatherDataNotAvailable() {
		String city = "Tallinn";
		String vehicle = "Car";

		DeliveryFeeService deliveryFeeService = new DeliveryFeeService(weatherDataRepository);

		String result = deliveryFeeService.calculateDeliveryFee(city, vehicle);

		assertEquals("No weather data available for city: " + city, result);
	}

	@Test
	void testCalculateDeliveryFee_RestrictedDueToWeather() {

		String city = "Tallinn";
		String vehicle = "Bike";

		WeatherData testWeatherData = new WeatherData();
		testWeatherData.setStationName("Tallinn-Harku");
		testWeatherData.setAirTemperature(5.0); 	// Normal temperature
		testWeatherData.setWindSpeed(20.0001);  	// Too high wind speed
		testWeatherData.setPhenomenon("Clear"); 	// No weather phenomena
		testWeatherData.setObservationTimestamp(LocalDateTime.now());

		weatherDataRepository.save(testWeatherData);

		DeliveryFeeService deliveryFeeService = new DeliveryFeeService(weatherDataRepository);

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> deliveryFeeService.calculateDeliveryFee(city, vehicle));

		assertEquals("Usage of selected vehicle type is forbidden", exception.getMessage());
	}

	@Test
	void testCalculateDeliveryFee_RestrictedDueToPhenomenon() {

		String city = "Tallinn";
		String vehicle = "Scooter";

		WeatherData testWeatherData = new WeatherData();
		testWeatherData.setStationName("Tallinn-Harku");
		testWeatherData.setAirTemperature(5.0); 	// Normal temperature
		testWeatherData.setWindSpeed(5.0);  		// Too high wind speed
		testWeatherData.setPhenomenon("Glaze"); 	// No weather phenomena
		testWeatherData.setObservationTimestamp(LocalDateTime.now());

		weatherDataRepository.save(testWeatherData);

		DeliveryFeeService deliveryFeeService = new DeliveryFeeService(weatherDataRepository);

		IllegalStateException exception = assertThrows(IllegalStateException.class,
				() -> deliveryFeeService.calculateDeliveryFee(city, vehicle));

		assertEquals("Usage of selected vehicle type is forbidden", exception.getMessage());
	}

	@Test
	void testCalculateDeliveryFee_ExtraFeeCalculation() {
		String city = "Tartu";
		String vehicle = "Scooter";

		WeatherData testWeatherData = new WeatherData();
		testWeatherData.setStationName("Tartu-Tõravere");
		testWeatherData.setAirTemperature(-5.0); 	// Colder temperature
		testWeatherData.setWindSpeed(15.0);     	// Stronger wind speed
		testWeatherData.setPhenomenon("snow");  	// No weather phenomena
		testWeatherData.setObservationTimestamp(LocalDateTime.now());

		weatherDataRepository.save(testWeatherData);

		double baseFee = 3.0;
		double extraFee = 1.5;
		double expectedFee = baseFee + extraFee;

		DeliveryFeeService deliveryFeeService = new DeliveryFeeService(weatherDataRepository);

		double actualFee = Double.parseDouble(deliveryFeeService.calculateDeliveryFee(city, vehicle)
				.replace("Total delivery fee is ", "")
				.replace(" €", ""));

		assertEquals(expectedFee, actualFee, "The delivery fee should match the expected value.");
	}


	// Tests for edge cases
	@Test
	void testCalculateDeliveryFee_NullCityAndVehicle() {
		DeliveryFeeService deliveryFeeService = new DeliveryFeeService(weatherDataRepository);

		String result = deliveryFeeService.calculateDeliveryFee(null, null);

		assertEquals("No city specified", result);
	}

	@Test
	void testCalculateDeliveryFee_EmptyCityAndVehicle() {
		DeliveryFeeService deliveryFeeService = new DeliveryFeeService(weatherDataRepository);

		String result = deliveryFeeService.calculateDeliveryFee("", "");

		assertEquals("No city specified", result);
	}

	@Test
	void testCalculateDeliveryFee_MaxWindSpeedBike() {
		String city = "Tallinn";
		String vehicle = "Bike";

		WeatherData testWeatherData = new WeatherData();
		testWeatherData.setStationName("Tallinn-Harku");
		testWeatherData.setAirTemperature(10.0);
		testWeatherData.setWindSpeed(20.0);		// Max wind speed possible for bike
		testWeatherData.setPhenomenon("clear");
		testWeatherData.setObservationTimestamp(LocalDateTime.now());

		weatherDataRepository.save(testWeatherData);

		DeliveryFeeService deliveryFeeService = new DeliveryFeeService(weatherDataRepository);

		double expectedFee = 3.0 + 0.5;

		double actualFee = Double.parseDouble(deliveryFeeService.calculateDeliveryFee(city, vehicle)
				.replace("Total delivery fee is ", "")
				.replace(" €", ""));

		assertEquals(expectedFee, actualFee);
	}

	@Test
	void testCalculateDeliveryFee_MissingPhenomenon() {
		String city = "Tallinn";
		String vehicle = "Scooter";

		WeatherData testWeatherData = new WeatherData();
		testWeatherData.setStationName("Tallinn-Harku");
		testWeatherData.setAirTemperature(0.0);
		testWeatherData.setWindSpeed(5.0);
		testWeatherData.setPhenomenon(null);	// No weather phenomenon recorded
		testWeatherData.setObservationTimestamp(LocalDateTime.now());

		weatherDataRepository.save(testWeatherData);

		DeliveryFeeService deliveryFeeService = new DeliveryFeeService(weatherDataRepository);

		double expectedFee = 3.5;

		double actualFee = Double.parseDouble(deliveryFeeService.calculateDeliveryFee(city, vehicle)
				.replace("Total delivery fee is ", "")
				.replace(" €", ""));

		assertEquals(expectedFee, actualFee);
	}

	@Test
	void testCalculateDeliveryFee_EmptyRepository() {
		String city = "Tallinn";
		String vehicle = "Car";

		DeliveryFeeService deliveryFeeService = new DeliveryFeeService(weatherDataRepository);

		String result = deliveryFeeService.calculateDeliveryFee(city, vehicle);

		assertEquals("No weather data available for city: " + city, result);
	}
}
