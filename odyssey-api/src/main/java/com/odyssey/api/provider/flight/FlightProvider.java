package com.odyssey.api.provider.flight;

import java.util.List;

public interface FlightProvider {

    String getName();

    List<FlightOffer> search(FlightSearchRequest request);
}