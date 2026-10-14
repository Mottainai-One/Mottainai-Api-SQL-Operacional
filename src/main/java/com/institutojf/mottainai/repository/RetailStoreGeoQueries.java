package com.institutojf.mottainai.repository;

final class RetailStoreGeoQueries {
    static final String FIND_NEARBY_STORES = """
        select s.*
        from mottainai.retail_store s
        where s.active = true and s.deleted_at is null
            and (:query = '' or lower(s.name) like concat('%', lower(:query), '%'))
            and s.latitude is not null and s.longitude is not null
            and 6371.0 * acos(least(1.0, greatest(-1.0,
                cos(radians(:latitude)) * cos(radians(s.latitude))
                * cos(radians(s.longitude) - radians(:longitude))
                + sin(radians(:latitude)) * sin(radians(s.latitude))))) <= :radiusKm
        order by s.name asc, s.store_id asc
        """;

    static final String COUNT_NEARBY_STORES = """
        select count(*)
        from mottainai.retail_store s
        where s.active = true and s.deleted_at is null
            and (:query = '' or lower(s.name) like concat('%', lower(:query), '%'))
            and s.latitude is not null and s.longitude is not null
            and 6371.0 * acos(least(1.0, greatest(-1.0,
                cos(radians(:latitude)) * cos(radians(s.latitude))
                * cos(radians(s.longitude) - radians(:longitude))
                + sin(radians(:latitude)) * sin(radians(s.latitude))))) <= :radiusKm
        """;

    private RetailStoreGeoQueries() {
    }
}
