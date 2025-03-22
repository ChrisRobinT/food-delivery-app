package com.crt.fujitsu.food_delivery_app.service_logic;

import com.crt.fujitsu.food_delivery_app.model.WeatherData;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.crt.fujitsu.food_delivery_app.repository.WeatherDataRepository;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.springframework.scheduling.annotation.Scheduled;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


/* ApiWeather gets weather data from an Estonian weather website and saves it in the database.
   It reads XML weather data every hour at minute 15, converts it, and stores the
   temperature, wind speed, and phenomenon.
*/
@Service
public class ApiWeather {

    private final WeatherDataRepository weatherDataRepository;
    private final RestTemplate restTemplate;

    /* This constructor initializes the ApiWeather service.
       the parameter weatherDataRepository is used to store weather data in the database.
    */
    public ApiWeather(WeatherDataRepository weatherDataRepository) {
        this.weatherDataRepository = weatherDataRepository;
        this.restTemplate = new RestTemplate();
    }

    /* Used @Scheduled to run every hour at the 15th minute.
       ImportWeatherData() method gets weather information as XML,
       processes it, and saves it to the database.
    */
    @Scheduled(cron = "0 15 * * * *")
    public void importWeatherData() {
        System.out.println("importWeatherData called");
        try {
            String url = "https://www.ilmateenistus.ee/ilma_andmed/xml/observations.php";
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            String xmlData = response.getBody();

            Document document = parseXmlString(xmlData);

            NodeList stationNodes = document.getElementsByTagName("station");

            List<WeatherData> weatherDataList = new ArrayList<>();
            LocalDateTime observationTime = LocalDateTime.now();

            for (int i = 0; i < stationNodes.getLength(); i++) {
                Element station = (Element) stationNodes.item(i);

                String stationName = getElementTextContent(station, "name");
                String wmoCode = getElementTextContent(station, "wmocode");
                Double airTemperature = parseDoubleOrNull(getElementTextContent(station, "airtemperature"));
                Double windSpeed = parseDoubleOrNull(getElementTextContent(station, "windspeed"));
                String phenomenon = getElementTextContent(station, "phenomenon");

                WeatherData data = new WeatherData(
                        stationName,
                        wmoCode,
                        airTemperature,
                        windSpeed,
                        phenomenon,
                        observationTime
                );

                weatherDataList.add(data);
            }

            weatherDataRepository.saveAll(weatherDataList);

        } catch (Exception e) {
            System.err.println("Error importing weather data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private Document parseXmlString(String xmlString) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new InputSource(new StringReader(xmlString)));
    }

    private String getElementTextContent(Element parent, String elementName) {
        NodeList nodeList = parent.getElementsByTagName(elementName);
        if (nodeList.getLength() > 0) {
            return nodeList.item(0).getTextContent();
        }
        return null;
    }

    private Double parseDoubleOrNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}