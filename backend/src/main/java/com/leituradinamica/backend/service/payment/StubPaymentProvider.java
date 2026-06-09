package com.leituradinamica.backend.service.payment;

import com.leituradinamica.backend.domain.entity.Subscription;
import org.springframework.stereotype.Component;

@Component
public class StubPaymentProvider implements PaymentProvider {

    @Override
    public String providerName() {
        return "stub";
    }

    @Override
    public String createCheckout(Subscription subscription) {
        return "https://payments.local/checkout/" + subscription.getId();
    }
}