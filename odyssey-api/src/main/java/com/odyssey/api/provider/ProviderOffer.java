package com.odyssey.api.provider;

import java.math.BigDecimal;

public interface ProviderOffer {

    String provider();

    String externalId();

    BigDecimal price();

    String currency();
}