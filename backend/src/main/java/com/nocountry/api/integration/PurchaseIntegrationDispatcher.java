package com.nocountry.api.integration;

import com.nocountry.api.integration.ga4.Ga4MeasurementProtocolService;
import com.nocountry.api.integration.meta.MetaCapiService;
import com.nocountry.api.integration.pipedrive.PipedriveService;
import org.springframework.stereotype.Service;

@Service
public class PurchaseIntegrationDispatcher {

    private final MetaCapiService metaCapiService;
    private final Ga4MeasurementProtocolService ga4MeasurementProtocolService;
    private final PipedriveService pipedriveService;

    public PurchaseIntegrationDispatcher(
            MetaCapiService metaCapiService,
            Ga4MeasurementProtocolService ga4MeasurementProtocolService,
            PipedriveService pipedriveService
    ) {
        this.metaCapiService = metaCapiService;
        this.ga4MeasurementProtocolService = ga4MeasurementProtocolService;
        this.pipedriveService = pipedriveService;
    }

    public void dispatchPurchase(PurchaseIntegrationPayload payload) {
        metaCapiService.sendPurchase(payload);
        ga4MeasurementProtocolService.sendPurchase(payload);
        pipedriveService.sendPurchase(payload);
    }
}
