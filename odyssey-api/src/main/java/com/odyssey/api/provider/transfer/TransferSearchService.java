package com.odyssey.api.provider.transfer;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TransferSearchService {

    private final List<TransferProvider> providers;

    public TransferSearchService(List<TransferProvider> providers) {
        this.providers = providers;
    }

    public List<TransferOffer> search(TransferSearchRequest request) {

        return providers.stream()
                .flatMap(provider -> provider.search(request).stream())
                .toList();
    }
}