package com.odyssey.api.provider.transfer;

import java.util.List;

public interface TransferProvider {

    String getName();

    List<TransferOffer> search(TransferSearchRequest request);
}