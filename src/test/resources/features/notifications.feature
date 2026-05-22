Feature: Notification API

  Background:
    Given the API is available

  Scenario: Create an EMAIL notification with a valid API key
    When I create a notification with:
      | type           | EMAIL                        |
      | recipientEmail | jane@example.com             |
      | recipientPhone | +32499000000                 |
      | recipientName  | Jane Doe                     |
      | subject        | Your verification code       |
      | body           | Your one-time password is 42 |
    Then the response status is 201
    And the response contains field "type" with value "EMAIL"
    And the response contains field "status" with value "PENDING"
    And the response contains field "recipientEmail" with value "jane@example.com"
    And the response contains a non-null "id"

  Scenario: Create an SMS notification with a valid API key
    When I create a notification with:
      | type           | SMS                          |
      | recipientPhone | +32499000000                 |
      | recipientName  | John Doe                     |
      | body           | Your one-time password is 42 |
    Then the response status is 201
    And the response contains field "type" with value "SMS"
    And the response contains field "status" with value "PENDING"
    And the response contains a non-null "id"

  Scenario: Request without an API key is rejected
    When I create a notification without an API key with:
      | type           | EMAIL            |
      | recipientEmail | jane@example.com |
      | recipientPhone | +32499000000     |
      | recipientName  | Jane Doe         |
    Then the response status is 401

  Scenario: Request with an invalid API key is rejected
    When I create a notification with API key "totally-wrong-key" and:
      | type           | EMAIL            |
      | recipientEmail | jane@example.com |
      | recipientPhone | +32499000000     |
      | recipientName  | Jane Doe         |
    Then the response status is 401

  Scenario: Request with missing required type field is rejected
    When I create a notification with:
      | recipientEmail | jane@example.com             |
      | recipientPhone | +32499000000                 |
      | recipientName  | Jane Doe                     |
      | body           | Your one-time password is 42 |
    Then the response status is 400

  Scenario: Request with an unknown channel type is rejected
    When I create a notification with:
      | type           | WEBHOOK                      |
      | recipientEmail | jane@example.com             |
      | recipientName  | Jane Doe                     |
      | body           | Your one-time password is 42 |
    Then the response status is 400
