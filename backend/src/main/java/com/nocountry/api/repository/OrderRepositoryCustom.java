package com.nocountry.api.repository;

import com.nocountry.api.entity.OrderRecord;

public interface OrderRepositoryCustom {

    OrderRecord upsert(OrderRecord candidate);
}
