package com.ab.orderservice.router;

import com.ab.orderservice.dto.route.RouteEstimateDto;
import com.ab.orderservice.model.enums.OrderSide;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RoutingLogFormatter {

    /**
     * Formats routing logs with a fixed structure.
     * Usage: improves searchability in logs (AUTO_ROUTE / ROUTE_PLAN).
     */
    public String autoRoute(SmartOrderRouter.RouteRequest req, String chosen, String reason, List<VenueQuote> quotes, OrderSide side) {
        String top3 = top3Quotes(quotes, side);
        return "instrument=" + req.instrument()
                + " side=" + req.side()
                + " quantity=" + req.qty()
                + " chosen=" + chosen
                + " reason=" + reason
                + " top3=" + top3;
    }

    /**
     * Formats plan logs using DTO list shown to clients.
     * Usage: aligns server logs with API response.
     */
    public String routePlan(SmartOrderRouter.RouteRequest req, String chosen, String reason, List<RouteEstimateDto> ranked) {
        return "instrument=" + req.instrument()
                + " side=" + req.side()
                + " quantity=" + req.qty()
                + " chosen=" + chosen
                + " reason=" + reason
                + " top3=" + top3Plan(ranked);
    }

    /**
     * Extracts top venues for compact logs.
     * Usage: highlights best candidates without dumping full book data.
     */
    private String top3Quotes(List<VenueQuote> quotes, OrderSide side) {
        // Assumes caller provides ranked list or accepts natural order.
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(3, quotes.size()); i++) {
            VenueQuote q = quotes.get(i);
            sb.append(i + 1).append(") ")
                    .append(q.getExchangeCode())
                    .append(" fill=").append(q.getEstimatedFillQty())
                    .append(" eff=").append(q.getEffectivePrice())
                    .append(" vwap=").append(q.getEstimatedExecPx())
                    .append(" | ");
        }
        return sb.toString();
    }

    /**
     * Extracts top venues from API plan output.
     * Usage: keeps ROUTE_PLAN logs compact.
     */
    private String top3Plan(List<RouteEstimateDto> ranked) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(3, ranked.size()); i++) {
            RouteEstimateDto r = ranked.get(i);
            sb.append(i + 1).append(") ")
                    .append(r.getExchange())
                    .append(" fill=").append(r.getFillQuantity())
                    .append(" eff=").append(r.getEffectiveVwap())
                    .append(" vwap=").append(r.getVwap())
                    .append(" | ");
        }
        return sb.toString();
    }
}