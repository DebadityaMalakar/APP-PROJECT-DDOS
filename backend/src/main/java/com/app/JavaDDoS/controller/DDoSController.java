package com.app.JavaDDoS.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

import org.springframework.http.MediaType;
import reactor.core.publisher.Flux;
import java.time.Duration;



@RestController
public class DDoSController {

    private static final Map<String, List<String>> continentCountries = Map.of(
        "North America", List.of("USA", "Canada", "Mexico", "Cuba", "Guatemala", "Panama", "Honduras"),
        "South America", List.of("Brazil", "Argentina", "Chile", "Colombia", "Peru", "Ecuador", "Uruguay"),
        "Europe", List.of("Germany", "France", "UK", "Italy", "Spain", "Netherlands", "Sweden"),
        "Africa", List.of("Nigeria", "South Africa", "Egypt", "Kenya", "Morocco", "Ghana", "Ethiopia"),
        "Asia", List.of("India", "China", "Japan", "South Korea", "Indonesia", "Pakistan", "Thailand"),
        "Australia", List.of("Australia", "New Zealand", "Fiji"),
        "Antarctica", List.of("McMurdo Station", "Palmer Station")
    );

    private static final List<String> protocols = List.of("HTTP", "HTTPS", "WebSocket");

    private final Random rand = new Random();

    @GetMapping("/simulate-ddos")
    public List<Map<String, String>> simulateDDoS() {
        List<Map<String, String>> attacks = new ArrayList<>();

        // Flatten all countries into a list of (continent, country) pairs
        List<Map.Entry<String, String>> countryPairs = new ArrayList<>();
        continentCountries.forEach((continent, countries) -> {
            for (String country : countries) {
                countryPairs.add(Map.entry(continent, country));
            }
        });

        // Shuffle and pick 50
        Collections.shuffle(countryPairs);
        List<Map.Entry<String, String>> selected = countryPairs.subList(0, Math.min(50, countryPairs.size()));

        for (Map.Entry<String, String> entry : selected) {
            Map<String, String> attack = new HashMap<>();
            attack.put("continent", entry.getKey());
            attack.put("country", entry.getValue());
            attack.put("ip", randomIP());
            attack.put("protocol", randomProtocol());
            attack.put("timestamp", new Date().toString());
            attacks.add(attack);
        }

        return attacks;
    }
    @GetMapping(value = "/simulate-ddos/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Map<String, String>> streamDDoS() {
        List<Map.Entry<String, String>> countryPairs = new ArrayList<>();
        continentCountries.forEach((continent, countries) -> {
            for (String country : countries) {
                countryPairs.add(Map.entry(continent, country));
            }
        });

        return Flux.interval(Duration.ofSeconds(1)).map(i -> {
            Collections.shuffle(countryPairs);
            Map.Entry<String, String> entry = countryPairs.get(0);

            Map<String, String> attack = new HashMap<>();
            attack.put("continent", entry.getKey());
            attack.put("country", entry.getValue());
            attack.put("ip", randomIP());
            attack.put("protocol", randomProtocol());
            attack.put("timestamp", new Date().toString());
            return attack;
        });
    }

    private String randomIP() {
        return rand.nextInt(256) + "." + rand.nextInt(256) + "." + rand.nextInt(256) + "." + rand.nextInt(256);
    }

    private String randomProtocol() {
        return protocols.get(rand.nextInt(protocols.size()));
    }
}