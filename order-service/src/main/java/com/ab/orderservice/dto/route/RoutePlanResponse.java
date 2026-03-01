package com.ab.orderservice.dto.route;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RoutePlanResponse {
    private String chosenExchange;
    private List<RouteEstimateDto> ranked;
}
