package com.crt.fujitsu.food_delivery_app.repository;

import com.crt.fujitsu.food_delivery_app.model.WeatherData;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/* Repository interface for managing WeatherData entities in the database.
   Provides methods to perform CRUD operations and queries.
 */
public interface WeatherDataRepository extends JpaRepository<WeatherData, Long> {
    Optional<WeatherData> findTopByStationNameOrderByObservationTimestampDesc(String stationName);
}

    /*  Just for clarity:

        findTopBy: Spring Data returns only the first result.
        StationName: Filters the results by the given station name.
        OrderByObservationTimestampDesc: Orders the results by the observation timestamp in descending order
                                         (most recent record comes first)
    */
