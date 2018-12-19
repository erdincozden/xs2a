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

package de.adorsys.psd2.consent.service;

import de.adorsys.psd2.consent.api.pis.CmsPayment;
import de.adorsys.psd2.consent.api.pis.CmsPaymentResponse;
import de.adorsys.psd2.consent.api.service.PisCommonPaymentService;
import de.adorsys.psd2.consent.domain.PsuData;
import de.adorsys.psd2.consent.domain.TppInfoEntity;
import de.adorsys.psd2.consent.domain.payment.PisAuthorization;
import de.adorsys.psd2.consent.domain.payment.PisCommonPaymentData;
import de.adorsys.psd2.consent.domain.payment.PisPaymentData;
import de.adorsys.psd2.consent.psu.api.CmsPsuPisService;
import de.adorsys.psd2.consent.repository.PisAuthorizationRepository;
import de.adorsys.psd2.consent.repository.PisCommonPaymentDataRepository;
import de.adorsys.psd2.consent.repository.PisPaymentDataRepository;
import de.adorsys.psd2.consent.service.mapper.CmsPsuPisMapper;
import de.adorsys.psd2.consent.service.mapper.PisCommonPaymentMapper;
import de.adorsys.psd2.consent.service.mapper.PsuDataMapper;
import de.adorsys.psd2.xs2a.core.pis.TransactionStatus;
import de.adorsys.psd2.xs2a.core.psu.PsuIdData;
import de.adorsys.psd2.xs2a.core.sca.ScaStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CmsPsuPisServiceInternal implements CmsPsuPisService {
    private final PisPaymentDataRepository pisPaymentDataRepository;
    private final PisCommonPaymentDataRepository pisCommonPaymentDataRepository;
    private final PisAuthorizationRepository pisAuthorizationRepository;
    private final CmsPsuPisMapper cmsPsuPisMapper;
    private final PisCommonPaymentService pisCommonPaymentService;
    private final CommonPaymentDataService commonPaymentDataService;
    private final PsuDataMapper psuDataMapper;
    private final PisCommonPaymentMapper pisCommonPaymentMapper;

    @Override
    @Transactional
    public boolean updatePsuInPayment(@NotNull PsuIdData psuIdData, @NotNull String redirectId) {
        return pisConsentAuthorizationRepository.findByExternalId(redirectId)
                   .map(PisAuthorization::getPaymentData)
                   .map(dta -> updatePsuData(dta, psuIdData))
                   .orElse(false);
    }

    @Override
    public @NotNull Optional<CmsPayment> getPayment(@NotNull PsuIdData psuIdData, @NotNull String paymentId) {
        if (isPsuDataEquals(paymentId, psuIdData)) {
            Optional<List<PisPaymentData>> list = pisPaymentDataRepository.findByPaymentId(paymentId);

            // todo implementation should be changed https://git.adorsys.de/adorsys/xs2a/aspsp-xs2a/issues/534
            if (list.isPresent()) {
                return list
                           .filter(CollectionUtils::isNotEmpty)
                           .map(cmsPsuPisMapper::mapToCmsPayment);
            } else {
                return commonPaymentDataService.getPisCommonPaymentData(paymentId)
                           .map(cmsPsuPisMapper::mapToCmsPayment);
            }
        }

        return Optional.empty();
    }

    @Override
    @Transactional
    public @NotNull Optional<CmsPaymentResponse> checkRedirectAndGetPayment(@NotNull PsuIdData psuIdData, @NotNull String redirectId) {
        Optional<PisAuthorization> optionalAuthorization = pisAuthorizationRepository.findByExternalId(redirectId)
                                                                      .filter(a -> isAuthorisationValidForPsuAndStatus(psuIdData, a));

        if (optionalAuthorization.isPresent()) {
            PisAuthorization authorisation = optionalAuthorization.get();

            if (authorisation.isNotExpired()) {
                return Optional.of(buildCmsPaymentResponse(authorisation));
            }

            changeAuthorisationStatusToFailed(authorisation);
            String tppNokRedirectUri = authorisation.getConsent().getTppInfo().getNokRedirectUri();

            return Optional.of(new CmsPaymentResponse(tppNokRedirectUri));
        }

        return Optional.empty();
    }

    @Override
    @Transactional
    public boolean updateAuthorisationStatus(@NotNull PsuIdData psuIdData, @NotNull String paymentId,
                                             @NotNull String authorisationId, @NotNull ScaStatus status) {

        Optional<PisAuthorization> pisAuthorisation = pisAuthorizationRepository.findByExternalId(authorisationId);

        boolean isValid = pisAuthorisation
                              .map(auth -> auth.getPaymentData().getPaymentId())
                              .map(id -> validateGivenData(id, paymentId, psuIdData))
                              .orElse(false);

        return isValid && updateAuthorisationStatusAndSaveAuthorisation(pisAuthorisation.get(), status);
    }

    @Override
    @Transactional
    public boolean updatePaymentStatus(@NotNull String paymentId, @NotNull TransactionStatus status) {
        Optional<List<PisPaymentData>> list = pisPaymentDataRepository.findByPaymentId(paymentId);

        // todo implementation should be changed https://git.adorsys.de/adorsys/xs2a/aspsp-xs2a/issues/534
        if (list.isPresent()) {
            return updateStatusInPaymentDataList(list.get(), status);
        } else {
            Optional<PisCommonPaymentData> paymentDataOptional = commonPaymentDataService.getPisCommonPaymentData(paymentId);

            return paymentDataOptional.isPresent()
                       && commonPaymentDataService.updateStatusInPaymentData(paymentDataOptional.get(), status);
        }
    }

    private boolean updatePsuData(PisCommonPaymentData commonPayment, PsuIdData psuIdData) {
        PisCommonPaymentData commonPaymentData = enrichPsuData(psuIdData, commonPayment);

        return Optional.ofNullable(pisCommonPaymentDataRepository.save(commonPaymentData))
                   .isPresent();
    }

    private boolean validateGivenData(String realPaymentId, String givenPaymentId, PsuIdData psuIdData) {
        return Optional.of(givenPaymentId)
                   .filter(p -> isPsuDataEquals(givenPaymentId, psuIdData))
                   .map(id -> StringUtils.equals(realPaymentId, id))
                   .orElse(false);
    }

    private PisCommonPaymentData enrichPsuData(PsuIdData psuIdData,  PisCommonPaymentData paymentData) {
        PsuData psuData = psuDataMapper.mapToPsuData(psuIdData);

        if (psuDataMapper.isPsuDataEmpty(psuData)
                || isPsuDataInList(psuData, paymentData)) {
            return paymentData;
        }

        List<PsuData> psuDataList = paymentData.getPsuData();
        psuDataList.add(psuData);
        paymentData.setPsuData(psuDataList);

        return paymentData;
    }

    private boolean isPsuDataInList(PsuData psuData, PisCommonPaymentData paymentData) {
        return paymentData.getPsuData().stream()
                   .anyMatch(psu -> psu.contentEquals(psuData));
    }

    private boolean updateAuthorisationStatusAndSaveAuthorisation(PisAuthorization pisAuthorisation, ScaStatus status) {
        if (pisAuthorisation.getScaStatus().isFinalisedStatus()) {
            return false;
        }
        pisAuthorisation.setScaStatus(status);
        return Optional.ofNullable(pisAuthorizationRepository.save(pisAuthorisation))
                   .isPresent();
    }

    private boolean isPsuDataEquals(String paymentId, PsuIdData psuIdData) {
        return pisCommonPaymentService.getPsuDataListByPaymentId(paymentId)
                   .map(lst -> lst.stream()
                                   .anyMatch(psu-> psu.contentEquals(psuIdData)))
                   .orElse(false);
    }

    private boolean updateStatusInPaymentDataList(List<PisPaymentData> dataList, TransactionStatus givenStatus) {
        for (PisPaymentData pisPaymentData : dataList) {
            if (pisPaymentData.getTransactionStatus().isFinalisedStatus()) {
                return false;
            }
            pisPaymentData.setTransactionStatus(givenStatus);
            pisPaymentDataRepository.save(pisPaymentData);
        }
        return true;
    }

    // do we need it ?
    private Optional<List<PisPaymentData>> getPaymentDataList(String encryptedPaymentId) {
        return pisCommonPaymentService.getDecryptedId(encryptedPaymentId)
                   .flatMap(pisPaymentDataRepository::findByPaymentId);
    }


    private boolean isAuthorisationValidForPsuAndStatus(PsuIdData givenPsuIdData, PisAuthorization authorization) {
        PsuIdData actualPsuIdData = psuDataMapper.mapToPsuIdData(authorization.getPsuData());
        return actualPsuIdData.contentEquals(givenPsuIdData) && !authorization.getScaStatus().isFinalisedStatus();
    }

    private CmsPaymentResponse buildCmsPaymentResponse(PisAuthorization authorisation) {
        PisCommonPaymentData commonPayment = authorisation.getPaymentData();
        CmsPayment payment = cmsPsuPisMapper.mapToCmsPayment(commonPayment.getPayments());
        TppInfoEntity tppInfo = commonPayment.getTppInfo();

        String tppOkRedirectUri = tppInfo.getRedirectUri();
        String tppNokRedirectUri = tppInfo.getNokRedirectUri();

        return new CmsPaymentResponse(
            payment,
            authorisation.getExternalId(),
            tppOkRedirectUri,
            tppNokRedirectUri);
    }

    private void changeAuthorisationStatusToFailed(PisAuthorization authorisation) {
        authorisation.setScaStatus(ScaStatus.FAILED);
        pisAuthorizationRepository.save(authorisation);
    }

    private Optional<PisCommonPaymentData> getPisCommonPaymentByPaymentId(String paymentId) {
        // todo implementation should be changed https://git.adorsys.de/adorsys/xs2a/aspsp-xs2a/issues/534
        Optional<PisCommonPaymentData> commonPaymentOpt = pisPaymentDataRepository.findByPaymentId(paymentId)
                                              .filter(CollectionUtils::isNotEmpty)
                                              .map(list -> list.get(0).getPaymentData());

        if (!commonPaymentOpt.isPresent()) {
            commonPaymentOpt = pisCommonPaymentDataRepository.findByPaymentId(paymentId);
        }

        return commonPaymentOpt;
    }
}
