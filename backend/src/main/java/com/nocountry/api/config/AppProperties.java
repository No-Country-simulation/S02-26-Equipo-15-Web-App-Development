package com.nocountry.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final Tracking tracking = new Tracking();
    private final Stripe stripe = new Stripe();
    private final Integrations integrations = new Integrations();
    private final Meta meta = new Meta();
    private final Ga4 ga4 = new Ga4();
    private final Pipedrive pipedrive = new Pipedrive();
    private final Cors cors = new Cors();
    private final Http http = new Http();

    public Tracking getTracking() {
        return tracking;
    }

    public Stripe getStripe() {
        return stripe;
    }

    public Integrations getIntegrations() {
        return integrations;
    }

    public Meta getMeta() {
        return meta;
    }

    public Ga4 getGa4() {
        return ga4;
    }

    public Pipedrive getPipedrive() {
        return pipedrive;
    }

    public Cors getCors() {
        return cors;
    }

    public Http getHttp() {
        return http;
    }

    public static class Tracking {
        private boolean enabled = true;
        private int rateLimitCapacity = 30;
        private int rateLimitRefillPerSecond = 10;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getRateLimitCapacity() {
            return rateLimitCapacity;
        }

        public void setRateLimitCapacity(int rateLimitCapacity) {
            this.rateLimitCapacity = rateLimitCapacity;
        }

        public int getRateLimitRefillPerSecond() {
            return rateLimitRefillPerSecond;
        }

        public void setRateLimitRefillPerSecond(int rateLimitRefillPerSecond) {
            this.rateLimitRefillPerSecond = rateLimitRefillPerSecond;
        }
    }

    public static class Stripe {
        private String webhookSecret = "";

        public String getWebhookSecret() {
            return webhookSecret;
        }

        public void setWebhookSecret(String webhookSecret) {
            this.webhookSecret = webhookSecret;
        }
    }

    public static class Integrations {
        private boolean metaCapiEnabled = false;
        private boolean ga4MpEnabled = false;
        private boolean pipedriveEnabled = false;

        public boolean isMetaCapiEnabled() {
            return metaCapiEnabled;
        }

        public void setMetaCapiEnabled(boolean metaCapiEnabled) {
            this.metaCapiEnabled = metaCapiEnabled;
        }

        public boolean isGa4MpEnabled() {
            return ga4MpEnabled;
        }

        public void setGa4MpEnabled(boolean ga4MpEnabled) {
            this.ga4MpEnabled = ga4MpEnabled;
        }

        public boolean isPipedriveEnabled() {
            return pipedriveEnabled;
        }

        public void setPipedriveEnabled(boolean pipedriveEnabled) {
            this.pipedriveEnabled = pipedriveEnabled;
        }
    }

    public static class Meta {
        private String pixelId = "";
        private String accessToken = "";

        public String getPixelId() {
            return pixelId;
        }

        public void setPixelId(String pixelId) {
            this.pixelId = pixelId;
        }

        public String getAccessToken() {
            return accessToken;
        }

        public void setAccessToken(String accessToken) {
            this.accessToken = accessToken;
        }
    }

    public static class Ga4 {
        private String measurementId = "";
        private String apiSecret = "";

        public String getMeasurementId() {
            return measurementId;
        }

        public void setMeasurementId(String measurementId) {
            this.measurementId = measurementId;
        }

        public String getApiSecret() {
            return apiSecret;
        }

        public void setApiSecret(String apiSecret) {
            this.apiSecret = apiSecret;
        }
    }

    public static class Pipedrive {
        private String apiToken = "";

        public String getApiToken() {
            return apiToken;
        }

        public void setApiToken(String apiToken) {
            this.apiToken = apiToken;
        }
    }

    public static class Cors {
        private List<String> allowedOrigins = new ArrayList<>();

        public List<String> getAllowedOrigins() {
            return allowedOrigins;
        }

        public void setAllowedOrigins(List<String> allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }
    }

    public static class Http {
        private int connectTimeoutMs = 2500;
        private int readTimeoutMs = 3500;

        public int getConnectTimeoutMs() {
            return connectTimeoutMs;
        }

        public void setConnectTimeoutMs(int connectTimeoutMs) {
            this.connectTimeoutMs = connectTimeoutMs;
        }

        public int getReadTimeoutMs() {
            return readTimeoutMs;
        }

        public void setReadTimeoutMs(int readTimeoutMs) {
            this.readTimeoutMs = readTimeoutMs;
        }
    }
}
