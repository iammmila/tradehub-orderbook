Feature: Order Matching Engine
  As a trader, I want my orders to match against existing orders in the book
  so that trades are executed at the best available price.

  Scenario: Full match between a new BUY order and an existing SELL order
    Given the following resting orders exist in the book:
      | id | side | quantity | price | instrument | userId | status |
      | 10 | SELL | 100      | 150.0 | AAPL       | 2      | NEW    |
    When a new BUY order arrives:
      | id | side | quantity | price | instrument | userId | status |
      | 20 | BUY  | 100      | 150.0 | AAPL       | 1      | NEW    |
    Then the BUY order should be "FILLED" with 0 remaining quantity
    And the SELL order 10 should be "FILLED" with 0 remaining quantity
    And a trade event should be published for 100 shares at 150.0