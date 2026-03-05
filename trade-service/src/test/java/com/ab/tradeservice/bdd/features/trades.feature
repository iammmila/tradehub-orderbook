Feature: Trades

  Scenario: Create a trade and fetch it by instrument
    Given the trades database is empty
    When I create a trade with instrument "BT" exchangeCode "XNAS" price 101.50 quantity 10 buyOrderId 1 sellOrderId 2 buyerUserId 10 sellerUserId 20
    Then there should be 1 trade in the database
    And fetching trades for instrument "BT" should return 1 item

  Scenario: Fetch all trades when instrument is blank
    Given the trades database is empty
    When I create a trade with instrument "BT" exchangeCode "XLON" price 100.00 quantity 5 buyOrderId 11 sellOrderId 12 buyerUserId 101 sellerUserId 201
    And I create a trade with instrument "VOD" exchangeCode "XLON" price 200.00 quantity 7 buyOrderId 21 sellOrderId 22 buyerUserId 102 sellerUserId 202
    Then fetching trades with no instrument filter should return 2 items