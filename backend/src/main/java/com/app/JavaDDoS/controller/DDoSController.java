package com.app.JavaDDoS.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.MediaType;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.*;

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

    private final List<Map.Entry<String, String>> countryPairs = new ArrayList<>();

    public DDoSController() {
        continentCountries.forEach((continent, countries) -> {
            for (String country : countries) {
                countryPairs.add(Map.entry(continent, country));
            }
        });
    }

    @GetMapping("/simulate-ddos")
    public List<Map<String, String>> simulateDDoS() {
        Collections.shuffle(countryPairs);
        List<Map.Entry<String, String>> selected = countryPairs.subList(0, Math.min(50, countryPairs.size()));

        List<Map<String, String>> attacks = new ArrayList<>();
        for (Map.Entry<String, String> entry : selected) {
            attacks.add(generateAttack(entry));
        }
        return attacks;
    }

    @GetMapping(value = "/simulate-ddos/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<List<Map<String, String>>> streamDDoS() {
        return Flux.interval(Duration.ofSeconds(5)).map(i -> {
            Collections.shuffle(countryPairs);
            List<Map<String, String>> batch = new ArrayList<>();
            for (int j = 0; j < 5; j++) {
                Map.Entry<String, String> entry = countryPairs.get(j);
                batch.add(generateAttack(entry));
            }
            return batch;
        });
    }

    private Map<String, String> generateAttack(Map.Entry<String, String> entry) {
        Map<String, String> attack = new HashMap<>();
        attack.put("continent", entry.getKey());
        attack.put("country", entry.getValue());
        attack.put("ip", randomIP());
        attack.put("protocol", randomProtocol());
        attack.put("timestamp", new Date().toString());
        return attack;
    }

    private String randomIP() {
        return rand.nextInt(256) + "." + rand.nextInt(256) + "." + rand.nextInt(256) + "." + rand.nextInt(256);
    }

    private String randomProtocol() {
        return protocols.get(rand.nextInt(protocols.size()));
    }
}