package com.crt.fujitsu.food_delivery_app.service_logic;
import org.springframework.stereotype.Service;
import com.crt.fujitsu.food_delivery_app.repository.WeatherDataRepository;
import com.crt.fujitsu.food_delivery_app.model.WeatherData;
import java.util.Optional;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;

@Service
public class DeliveryFeeService {
    private final WeatherDataRepository weatherDataRepository;
    private final Map<String, String> cityToStationMap;
    private final List<String> validVehicles = Arrays.asList("Car", "Bike", "Scooter");

    public DeliveryFeeService(WeatherDataRepository weatherDataRepository) {
        this.weatherDataRepository = weatherDataRepository;

        cityToStationMap = new HashMap<>();
        cityToStationMap.put("Tallinn", "Tallinn-Harku");
        cityToStationMap.put("Tartu", "Tartu-Tõravere");
        cityToStationMap.put("Pärnu", "Pärnu");
    }

    /* Calculates the delivery fee for the given city and vehicle type based on weather conditions.
       The method retrieves the most recent weather data for the city and computes the base fee
       and additional fees or restrictions depending on the weather parameters. If there is an error
       or something is not specified but should be, returns an error message.
       - The parameter "city" the name of the city for which the delivery fee is to be calculated
         (so in our case "Tallinn", "Tartu", and "Pärnu")
       - The parameter "vehicle" the type of vehicle used for the delivery
         (so in our case "Scooter", "Bike", and "Car")
    */
    public String calculateDeliveryFee(String city, String vehicle) {
        if (city == null || city.trim().isEmpty()) {
            return "No city specified";
        }

        if (vehicle == null || vehicle.trim().isEmpty()) {
            return "No vehicle type specified";
        }

        if (!validVehicles.contains(vehicle)) {
            return "Invalid vehicle type: " + vehicle;
        }

        String stationName = cityToStationMap.get(city);
        if (stationName == null) {
            return "Unknown city: " + city;
        }

        Optional<WeatherData> weatherDataOpt = weatherDataRepository.findTopByStationNameOrderByObservationTimestampDesc(stationName);
        if (weatherDataOpt.isEmpty()) {
            return "No weather data available for city: " + city;
        }

        WeatherData weatherData = weatherDataOpt.get();

        double baseFee = getBaseFee(city, vehicle);

        double extraFee = getExtraFee(vehicle, weatherData);

        return "Total delivery fee is " + (baseFee + extraFee) + " €";
    }


    // Helper functions
    private static double getBaseFee(String city, String vehicle) {
        Map<String, Double> baseFees = new HashMap<>();
        baseFees.put("Tallinn,Scooter", 3.5);
        baseFees.put("Tallinn,Bike", 3.0);
        baseFees.put("Tallinn,Car", 4.0);
        baseFees.put("Tartu,Scooter", 3.0);
        baseFees.put("Tartu,Bike", 2.5);
        baseFees.put("Tartu,Car", 3.5);
        baseFees.put("Pärnu,Scooter", 2.5);
        baseFees.put("Pärnu,Bike", 2.0);
        baseFees.put("Pärnu,Car", 3.0);

        String key = city + "," + vehicle;
        return baseFees.getOrDefault(key, 0.0);
    }

    private double getExtraFee(String vehicle, WeatherData weatherData) {
        double extraFee = 0.0;
        if (vehicle.equalsIgnoreCase("Scooter") || vehicle.equalsIgnoreCase("Bike")) {
            if (isSevereWeather(weatherData)) {
                throw new IllegalStateException("Usage of selected vehicle type is forbidden");
            }
            extraFee += getTempFee(weatherData.getAirTemperature());
            extraFee += getPhenomenonFee(weatherData.getPhenomenon());
        }
        if (vehicle.equalsIgnoreCase("Bike") && weatherData.getWindSpeed() != null) {
            extraFee += getWindSpeedFee(weatherData.getWindSpeed());
        }
        return extraFee;
    }

    private boolean isSevereWeather(WeatherData weatherData) {
        if (weatherData.getPhenomenon() != null) {
            String phenomenon = weatherData.getPhenomenon().toLowerCase();
            return phenomenon.contains("glaze") || phenomenon.contains("thunder") || phenomenon.contains("hail");
        }
        return false;
    }

    private double getWindSpeedFee(Double windSpeed) {
        if (windSpeed > 20.0)
            throw new IllegalArgumentException("Usage of selected vehicle type is forbidden");
        else if (windSpeed >= 10.0) return 0.5;
        return 0.0;
    }

    private double getTempFee(Double temp) {
        if (temp == null) return 0.0;
        if (temp < -10) return 1.0;
        else if (temp >= -10 && temp < 0) return 0.5;
        return 0.0;
    }

    private double getPhenomenonFee(String phenomenon) {
        if (phenomenon == null) return 0.0;
        phenomenon = phenomenon.toLowerCase();
        if (phenomenon.contains("snow") || phenomenon.contains("sleet")) return 1.0;
        else if (phenomenon.contains("rain")) return 0.5;
        return 0.0;
    }
}