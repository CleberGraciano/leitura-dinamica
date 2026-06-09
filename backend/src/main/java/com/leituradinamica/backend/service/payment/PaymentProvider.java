package com.leituradinamica.backend.service.payment;

import com.leituradinamica.backend.domain.entity.Subscription;

public interface PaymentProvider {
    String providerName();
    String createCheckout(Subscription subscription);
}