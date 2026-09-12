package com.odyssey.api.provider.transfer;
import org.springframework.stereotype.Component;
import java.util.List;
@Component
public class FakeTransferProvider implements TransferProvider {

    @Override
    public String getName() {
        return "FAKE_TRANSFER";
    }

    @Override
    public List<TransferOffer> search(TransferSearchRequest request) {
        TransferOffer offer1 = new TransferOffer(getName(), "1", "Sedan", new java.math.BigDecimal("100.00"), "USD", 4);
        TransferOffer offer2 = new TransferOffer(getName(), "2", "SUV", new java.math.BigDecimal("150.00"), "USD", 4);
       
        return List.of(offer1, offer2);
    }
}
