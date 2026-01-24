package com.ahthek;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class Fire {

    public static void main(String[] args) {
        // Define the API URL
        String apiUrl = "https://jsonplaceholder.typicode.com/posts";

        // Create an HttpClient instance
        HttpClient client = HttpClient.newHttpClient();

        // Build an HttpRequest
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Accept", "application/json") // Request JSON response
                .build();

        try {
            // Send the request and get the response synchronously
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // Check the response status code
            if (response.statusCode() == 200) {
                System.out.println("API Call Successful! Response Body:");
                System.out.println(response.body());
            } else {
                System.err.println("API Call Failed with status code: " + response.statusCode());
            }

        } catch (IOException | InterruptedException e) {
            System.err.println("An error occurred during the API call: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
