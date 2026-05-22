package com.notifyengine.cucumber.steps;

import com.notifyengine.cucumber.ScenarioContext;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.http.ContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

public class NotificationSteps {

    private static final String VALID_API_KEY = "notify-engine-local-test-key";

    @Autowired
    private ScenarioContext context;

    @Value("${local.server.port}")
    private int port;

    @Given("the API is available")
    public void theApiIsAvailable() {
        // port is injected — no setup needed
    }

    @When("I create a notification with:")
    public void iCreateANotificationWithValidKey(DataTable table) {
        Map<String, String> body = table.asMap();
        context.setResponse(
            given()
                .baseUri("http://localhost:" + port)
                .header("X-API-Key", VALID_API_KEY)
                .contentType(ContentType.JSON)
                .body(body)
            .when()
                .post("/api/v1/notifications")
        );
    }

    @When("I create a notification without an API key with:")
    public void iCreateANotificationWithoutApiKey(DataTable table) {
        Map<String, String> body = table.asMap();
        context.setResponse(
            given()
                .baseUri("http://localhost:" + port)
                .contentType(ContentType.JSON)
                .body(body)
            .when()
                .post("/api/v1/notifications")
        );
    }

    @When("I create a notification with API key {string} and:")
    public void iCreateANotificationWithSpecificKey(String apiKey, DataTable table) {
        Map<String, String> body = table.asMap();
        context.setResponse(
            given()
                .baseUri("http://localhost:" + port)
                .header("X-API-Key", apiKey)
                .contentType(ContentType.JSON)
                .body(body)
            .when()
                .post("/api/v1/notifications")
        );
    }

    @Then("the response status is {int}")
    public void theResponseStatusIs(int expectedStatus) {
        context.getResponse()
            .then()
            .statusCode(expectedStatus);
    }

    @Then("the response contains field {string} with value {string}")
    public void theResponseContainsFieldWithValue(String field, String value) {
        context.getResponse()
            .then()
            .body(field, equalTo(value));
    }

    @Then("the response contains a non-null {string}")
    public void theResponseContainsNonNull(String field) {
        context.getResponse()
            .then()
            .body(field, notNullValue());
    }
}
