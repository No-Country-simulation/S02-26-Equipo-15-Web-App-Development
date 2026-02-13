package com.nocountry.api.dto;

import java.util.List;

public record ApiError(String error, String message, List<String> details) {
}
