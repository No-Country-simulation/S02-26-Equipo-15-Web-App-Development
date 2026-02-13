package com.nocountry.api.service;

public record RequestMetadata(String userAgent, String clientIp, String ipHash) {
}
