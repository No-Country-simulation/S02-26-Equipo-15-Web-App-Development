package com.nocountry.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class TrackRequest {

    @JsonProperty("eventId")
    @Pattern(regexp = "^[0-9a-fA-F-]{36}$", message = "must be a valid UUID")
    private String eventId;

    @NotBlank(message = "is required")
    @Pattern(regexp = "^(landing_view|click_cta|begin_checkout|purchase)$",
            message = "must be one of: landing_view, click_cta, begin_checkout, purchase")
    @JsonProperty("eventType")
    private String eventType;

    @JsonProperty("utm_source")
    private String utmSource;

    @JsonProperty("utm_medium")
    private String utmMedium;

    @JsonProperty("utm_campaign")
    private String utmCampaign;

    @JsonProperty("utm_term")
    private String utmTerm;

    @JsonProperty("utm_content")
    private String utmContent;

    @JsonProperty("gclid")
    private String gclid;

    @JsonProperty("fbclid")
    private String fbclid;

    @JsonProperty("landing_path")
    private String landingPath;

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getUtmSource() {
        return utmSource;
    }

    public void setUtmSource(String utmSource) {
        this.utmSource = utmSource;
    }

    public String getUtmMedium() {
        return utmMedium;
    }

    public void setUtmMedium(String utmMedium) {
        this.utmMedium = utmMedium;
    }

    public String getUtmCampaign() {
        return utmCampaign;
    }

    public void setUtmCampaign(String utmCampaign) {
        this.utmCampaign = utmCampaign;
    }

    public String getUtmTerm() {
        return utmTerm;
    }

    public void setUtmTerm(String utmTerm) {
        this.utmTerm = utmTerm;
    }

    public String getUtmContent() {
        return utmContent;
    }

    public void setUtmContent(String utmContent) {
        this.utmContent = utmContent;
    }

    public String getGclid() {
        return gclid;
    }

    public void setGclid(String gclid) {
        this.gclid = gclid;
    }

    public String getFbclid() {
        return fbclid;
    }

    public void setFbclid(String fbclid) {
        this.fbclid = fbclid;
    }

    public String getLandingPath() {
        return landingPath;
    }

    public void setLandingPath(String landingPath) {
        this.landingPath = landingPath;
    }
}
