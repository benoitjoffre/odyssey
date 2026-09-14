package com.odyssey.api.trip;
import java.math.BigDecimal;

public record UpdateAssistanceFeeRequest(
    BigDecimal assistanceFee
) {}
