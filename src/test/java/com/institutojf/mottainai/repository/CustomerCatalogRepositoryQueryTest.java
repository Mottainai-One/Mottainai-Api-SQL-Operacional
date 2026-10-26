package com.institutojf.mottainai.repository;

import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomerCatalogRepositoryQueryTest {

    @Test
    void promotionListAndDetailRequireApprovedCurrentPromotionsWithAvailableProducts() {
        String list = query(PromotionRepository.class, "findCustomerPromotions").value();
        String detail = query(PromotionRepository.class, "findCustomerPromotion").value();

        for (String sql : List.of(list, detail)) {
            String normalized = normalize(sql);
            assertContains(normalized,
                "p.status = :status",
                "p.active = true",
                "p.deletedat is null",
                "p.store.active = true",
                "p.store.deletedat is null",
                "p.startsat <= :now",
                "p.endsat > :now",
                "exists (select i.id from promotionitem i where i.promotion = p and i.deletedat is null and i.quantityavailable > 0",
                "i.quantityavailable > 0",
                "i.product.active = true",
                "i.product.deletedat is null");
        }

        assertContains(normalize(list),
            "(:storeid = 0 or p.store.id = :storeid)",
            "(:query = '' or lower(p.name) like lower(concat('%', :query, '%')))");
    }

    @Test
    void generalStoreSearchIncludesStoresWithoutCoordinatesButGeoSearchDoesNot() {
        String general = normalize(query(RetailStoreRepository.class, "findCustomerStores").value());
        assertContains(general,
            "s.active = true",
            "s.deletedat is null",
            "(:query = '' or lower(s.name) like lower(concat('%', :query, '%')))");
        assertFalse(general.contains("s.latitude is not null"));

        Query nearby = query(RetailStoreRepository.class, "findCustomerStoresNear");
        assertTrue(nearby.nativeQuery());
        assertContains(normalize(nearby.value()),
            "s.active = true",
            "s.deleted_at is null",
            "s.latitude is not null",
            "s.longitude is not null",
            "6371.0 * acos(",
            ":radiuskm");
        assertContains(normalize(nearby.countQuery()),
            "s.active = true",
            "s.deleted_at is null",
            "s.latitude is not null",
            "s.longitude is not null",
            "6371.0 * acos(",
            ":radiuskm");
    }

    @Test
    void customerPromotionItemsExcludeUnavailableOrInactiveProducts() {
        for (String methodName : List.of("findAvailableCustomerItems", "findAvailableCustomerItemsForPromotions")) {
            String jpql = normalize(query(PromotionItemRepository.class, methodName).value());
            assertContains(jpql,
                "i.quantityavailable > 0",
                "i.product.active = true",
                "i.product.deletedat is null");
        }
    }

    private static Query query(Class<?> repository, String methodName) {
        Method method = Arrays.stream(repository.getDeclaredMethods())
            .filter(candidate -> candidate.getName().equals(methodName))
            .findFirst()
            .orElseThrow();
        return method.getAnnotation(Query.class);
    }

    private static String normalize(String sql) {
        return sql.replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private static void assertContains(String query, String... fragments) {
        for (String fragment : fragments) {
            assertTrue(query.contains(fragment), () -> "Missing query condition: " + fragment + " in " + query);
        }
    }
}
