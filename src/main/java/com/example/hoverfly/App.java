package com.example.hoverfly;

import io.specto.hoverfly.junit.rule.HoverflyRule;
import org.junit.ClassRule;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

import static io.specto.hoverfly.junit.core.SimulationSource.dsl;
import static io.specto.hoverfly.junit.dsl.HoverflyDsl.service;
import static io.specto.hoverfly.junit.dsl.HttpBodyConverter.jsonWithSingleQuotes;
import static io.specto.hoverfly.junit.dsl.ResponseCreators.*;

public class App {

    private static final RestTemplate restTemplate = new RestTemplate();

    public static void main(String[] args) {
        test();

    }

    public static void test() {
        System.out.println("Hello Hoverfly");

        // When
        URI uri = UriComponentsBuilder.fromHttpUrl("http://www.other-anotherservice.com")
                .path("/api/bookings")
                .queryParam("class", "business", "premium")
                .queryParam("destination", "new york")
                .build()
                .toUri();

        final ResponseEntity<String> getBookingResponse = restTemplate.getForEntity(uri, String.class);

        System.out.println(getBookingResponse.getStatusCode());

    }

    @ClassRule
    public static HoverflyRule hoverflyRule = HoverflyRule.inSimulationMode(dsl(
            service("www.my-test.com")

                    .post("/api/bookings").body("{\"flightId\": \"1\"}")
                    .willReturn(created("http://localhost/api/bookings/1"))

                    .get("/api/bookings/1")
                    .willReturn(success().body(jsonWithSingleQuotes(
                            "{'bookingId':'1','origin':'London','destination':'Singapore','time':'2011-09-01T12:30','_links':{'self':{'href':'http://localhost/api/bookings/1'}}}"
                    ))),

            service("www.other-anotherservice.com")

                    .put("/api/bookings/1")
                    .body("{\"flightId\": \"1\", \"class\": \"PREMIUM\"}")
                    .willReturn(success())

                    .delete("/api/bookings/1")
                    .willReturn(noContent())

                    .get("/api/bookings")
                    .queryParam("class", "business", "premium")
                    .queryParam("destination", "new york")
                    .willReturn(success("{\"bookingId\":\"2\",\"origin\":\"London\",\"destination\":\"New York\",\"class\":\"BUSINESS\",\"time\":\"2011-09-01T12:30\",\"_links\":{\"self\":{\"href\":\"http://localhost/api/bookings/2\"}}}", "application/json"))

                    .patch("/api/bookings/1").body("{\"class\": \"BUSINESS\"}")
                    .willReturn(noContent())

    )).printSimulationData();
}
