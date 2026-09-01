package com.odyssey.api.provider.accommodation;

import java.util.List;

public interface AccommodationProvider {

    String getName();

    List<AccommodationOffer> search(AccommodationSearchRequest request);
}