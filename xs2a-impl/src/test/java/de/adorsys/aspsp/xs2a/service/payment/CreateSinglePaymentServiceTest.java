/*
 * Copyright 2018-2018 adorsys GmbH & Co KG
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.adorsys.aspsp.xs2a.service.payment;

import de.adorsys.aspsp.xs2a.domain.*;
import de.adorsys.aspsp.xs2a.domain.account.Xs2aAccountReference;
import de.adorsys.aspsp.xs2a.domain.consent.Xs2aPisConsent;
import de.adorsys.aspsp.xs2a.domain.pis.*;
import de.adorsys.aspsp.xs2a.service.authorization.AuthorisationMethodService;
import de.adorsys.aspsp.xs2a.service.authorization.pis.PisScaAuthorisationService;
import de.adorsys.aspsp.xs2a.service.consent.PisConsentService;
import de.adorsys.psd2.xs2a.core.profile.PaymentProduct;
import de.adorsys.psd2.xs2a.core.profile.PaymentType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Currency;

import static de.adorsys.aspsp.xs2a.domain.Xs2aTransactionStatus.RCVD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CreateSinglePaymentServiceTest {
    private final Currency EUR_CURRENCY = Currency.getInstance("EUR");
    private static final String CONSENT_ID = "d6cb50e5-bb88-4bbf-a5c1-42ee1ed1df2c";
    private static final String PAYMENT_ID = "12345";
    private static final String IBAN = "DE123456789";
    private final TppInfo TPP_INFO = buildTppInfo();

    @InjectMocks
    private CreateSinglePaymentService createSinglePaymentService;
    @Mock
    private ScaPaymentService scaPaymentService;
    @Mock
    private PisConsentService pisConsentService;
    @Mock
    private PisScaAuthorisationService pisScaAuthorisationService;
    @Mock
    private AuthorisationMethodService authorisationMethodService;

    @Before
    public void init() {
        when(scaPaymentService.createSinglePayment(buildSinglePayment(), TPP_INFO, PaymentProduct.SEPA, buildXs2aPisConsent())).thenReturn(buildSinglePaymentInitiationResponse());
    }

    @Test
    public void success_initiate_single_payment() {
        //When
        ResponseObject<SinglePaymentInitiationResponse> actualResponse = createSinglePaymentService.createPayment(buildSinglePayment(), buildPaymentInitiationParameters(), buildTppInfo(), buildXs2aPisConsent());

        //Then
        assertThat(actualResponse.hasError()).isFalse();
        assertThat(actualResponse.getBody().getPaymentId()).isEqualTo(PAYMENT_ID);
        assertThat(actualResponse.getBody().getTransactionStatus()).isEqualTo(RCVD);
    }

    private SinglePayment buildSinglePayment() {
        SinglePayment payment = new SinglePayment();
        Xs2aAmount amount = buildXs2aAmount();
        payment.setInstructedAmount(amount);
        payment.setDebtorAccount(buildReference());
        payment.setCreditorAccount(buildReference());
        payment.setTransactionStatus(Xs2aTransactionStatus.RCVD);
        return payment;
    }

    private Xs2aAmount buildXs2aAmount() {
        Xs2aAmount amount = new Xs2aAmount();
        amount.setCurrency(EUR_CURRENCY);
        amount.setAmount("100");
        return amount;
    }

    private Xs2aAccountReference buildReference() {
        Xs2aAccountReference reference = new Xs2aAccountReference();
        reference.setIban(IBAN);
        reference.setCurrency(EUR_CURRENCY);
        return reference;
    }

    private Xs2aPisConsent buildXs2aPisConsent() {
        return new Xs2aPisConsent(CONSENT_ID);
    }

    private PaymentInitiationParameters buildPaymentInitiationParameters() {
        PaymentInitiationParameters parameters = new PaymentInitiationParameters();
        parameters.setPaymentProduct(PaymentProduct.SEPA);
        parameters.setPaymentType(PaymentType.SINGLE);
        return parameters;
    }

    private SinglePaymentInitiationResponse buildSinglePaymentInitiationResponse() {
        SinglePaymentInitiationResponse response = new SinglePaymentInitiationResponse();
        response.setPaymentId(PAYMENT_ID);
        response.setTransactionStatus(Xs2aTransactionStatus.RCVD);
        response.setPisConsentId(CONSENT_ID);
        return response;
    }

    private TppInfo buildTppInfo() {
        TppInfo tppInfo = new TppInfo();
        tppInfo.setAuthorisationNumber("registrationNumber");
        tppInfo.setTppName("tppName");
        tppInfo.setTppRoles(Collections.singletonList(Xs2aTppRole.PISP));
        tppInfo.setAuthorityId("authorityId");
        return tppInfo;
    }
}