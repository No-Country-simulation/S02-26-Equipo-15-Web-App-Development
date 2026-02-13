package com.nocountry.api.dto;

import java.util.List;

public record PagedResponse<T>(List<T> items, int limit, int offset) {
}
