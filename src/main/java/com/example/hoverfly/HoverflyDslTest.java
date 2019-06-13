package com.example.hoverfly;

import io.specto.hoverfly.junit.rule.HoverflyRule;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URISyntaxException;

import static io.specto.hoverfly.junit.core.SimulationSource.dsl;
import static io.specto.hoverfly.junit.dsl.HoverflyDsl.service;
import static io.specto.hoverfly.junit.dsl.HttpBodyConverter.jsonWithSingleQuotes;
import static io.specto.hoverfly.junit.dsl.ResponseCreators.*;
import static org.junit.Assert.assertEquals;
import static org.springframework.http.MediaType.APPLICATION_JSON;

public class HoverflyDslTest {

    private final RestTemplate restTemplate = new RestTemplate();

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

    @Test
    public void shouldBeAbleToAmendABookingUsingHoverfly() throws URISyntaxException {
        // Given
        final RequestEntity<String> bookFlightRequest = RequestEntity
                .put(new URI("http://www.other-anotherservice.com/api/bookings/1"))
                .contentType(APPLICATION_JSON)
                .body("{\"flightId\": \"1\", \"class\": \"PREMIUM\"}");

        // When
        final ResponseEntity<String> bookFlightResponse = restTemplate.exchange(bookFlightRequest, String.class);

        // Then
        assertEquals(bookFlightResponse.getStatusCode(), HttpStatus.OK);
    }

    @Test
    public void shouldBeAbleToDeleteBookingUsingHoverfly() throws Exception {
        // Given
        final RequestEntity<Void> bookFlightRequest = RequestEntity.delete(new URI("http://www.other-anotherservice.com/api/bookings/1")).build();

        // When
        final ResponseEntity<Void> bookFlightResponse = restTemplate.exchange(bookFlightRequest, Void.class);

        // Then
        assertEquals(bookFlightResponse.getStatusCode(), HttpStatus.NO_CONTENT);

    }

    @Test
    public void shouldBeAbleToQueryBookingsUsingHoverfly() {
        // When
        URI uri = UriComponentsBuilder.fromHttpUrl("http://www.other-anotherservice.com")
                .path("/api/bookings")
                .queryParam("class", "business", "premium")
                .queryParam("destination", "new york")
                .build()
                .toUri();
        final ResponseEntity<String> getBookingResponse = restTemplate.getForEntity(uri, String.class);

        // Then
        assertEquals(getBookingResponse.getStatusCode(), HttpStatus.OK);
        assertEquals(getBookingResponse.getBody(), "{" +
                "\"bookingId\":\"2\"," +
                "\"origin\":\"London\"," +
                "\"destination\":\"New York\"," +
                "\"class\":\"BUSINESS\"," +
                "\"time\":\"2011-09-01T12:30\"," +
                "\"_links\":{\"self\":{\"href\":\"http://localhost/api/bookings/2\"}}" +
                "}");

    }

//    @Test
//    public void shouldBeAbleToPatchBookingsUsingHoverfly() throws Exception {
//        // Given
//        final RequestEntity<String> bookFlightRequest = RequestEntity.patch(new URI("http://www.other-anotherservice.com/api/bookings/1"))
//                .contentType(APPLICATION_JSON)
//                .body("{\"class\": \"BUSINESS\"}");
//
//        // When Apache HttpClient is required for making PATCH request using RestTemplate
//        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
//
//        restTemplate.setRequestFactory(requestFactory);
//
//        final ResponseEntity<Void> bookFlightResponse = restTemplate.exchange(bookFlightRequest, Void.class);
//
//        // Then
//        assertEquals(bookFlightResponse.getStatusCode(), HttpStatus.NO_CONTENT);
//    }


}