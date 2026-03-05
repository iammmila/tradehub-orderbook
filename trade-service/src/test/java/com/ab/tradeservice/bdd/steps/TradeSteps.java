package com.ab.tradeservice.bdd.steps;

import com.ab.tradeservice.bdd.CucumberSpringConfig;
import com.ab.tradeservice.dto.CreateTradeRequest;
import com.ab.tradeservice.repository.TradeRepository;
import com.ab.tradeservice.service.TradeService;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;

/**
 * Step Definitions:
 * - This class contains Java methods that implement the steps written in Gherkin (.feature files).
 * <p>
 * Important:
 * - DO NOT annotate this class with @Component/@Service.
 * Cucumber creates these glue objects itself, and adding Spring component scanning can cause duplicate bean errors.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
// (each scenario starts clean).
public class TradeSteps {

    @Autowired
    private TradeService tradeService;

    @Autowired
    private TradeRepository tradeRepository;

    /**
     * This step matches:
     * Given the trades database is empty
     * !Purpose:
     * - Ensure known starting state (no trades in DB).
     */
    @Given("the trades database is empty")
    public void the_trades_database_is_empty() {
        tradeRepository.deleteAll();
        assertEquals(0, tradeRepository.count());
    }

    /*
     * Notes:
     * - {string}, {double}, {int}, {long} are Cucumber parameter types.
     * - Cucumber parses values from the feature file and passes them here.
     * - Build a CreateTradeRequest (DTO) and call the real service method.
     */
    @When("I create a trade with instrument {string} exchangeCode {string} price {double} quantity {int} buyOrderId {long} sellOrderId {long} buyerUserId {long} sellerUserId {long}")
    public void i_create_a_trade(
            String instrument,
            String exchangeCode,
            double price,
            int quantity,
            long buyOrderId,
            long sellOrderId,
            long buyerUserId,
            long sellerUserId
    ) {
        CreateTradeRequest req = new CreateTradeRequest();
        req.setInstrument(instrument);
        req.setExchangeCode(exchangeCode);
        req.setPrice(BigDecimal.valueOf(price));
        req.setQuantity((long) quantity);
        req.setBuyOrderId(buyOrderId);
        req.setSellOrderId(sellOrderId);
        req.setBuyerUserId(buyerUserId);
        req.setSellerUserId(sellerUserId);

        tradeService.createTrade(req);
    }

    // - Verify persistence result (DB row count).
    @Then("there should be {int} trade in the database")
    public void there_should_be_n_trade_in_db(int expected) {
        assertEquals(expected, tradeRepository.count());
    }

    /**
     * This step matches:
     * And fetching trades for instrument "BT" should return 1 item
     * Purpose:
     * - Verify  service filtering logic works (getTrades(instrument)).
     */
    @Then("fetching trades for instrument {string} should return {int} item")
    public void fetching_by_instrument_should_return_n(String instrument, int expected) {
        assertEquals(expected, tradeService.getTrades(instrument).size());
    }

    /**
     * This step matches:
     * Then fetching trades with no instrument filter should return 2 items
     * Purpose:
     * - Verify your "null/blank instrument means return all" behavior.
     */
    @Then("fetching trades with no instrument filter should return {int} items")
    public void fetching_with_no_filter_should_return_n(int expected) {
        assertEquals(expected, tradeService.getTrades(null).size());
    }
}