package com.institutojf.mottainai.repository;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.data.jpa.repository.Query;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductCommercialRepositoryTest {

    @ParameterizedTest
    @MethodSource("projectionQueries")
    void selectsOnlyTheColumnsRequiredByTheProjection(String methodName, Class<?>[] parameterTypes, Class<?> projectionType) throws NoSuchMethodException {
        Query query = ProductCommercialRepository.class.getMethod(methodName, parameterTypes).getAnnotation(Query.class);
        String sql = query.value().toLowerCase(Locale.ROOT);
        List<String> selectedColumns = Arrays.stream(sql.substring("select ".length(), sql.indexOf(" from ")).split(","))
            .map(String::trim)
            .sorted()
            .toList();
        List<String> projectionColumns = Arrays.stream(projectionType.getDeclaredMethods())
            .filter(method -> method.getName().startsWith("get") && method.getParameterCount() == 0)
            .map(Method::getName)
            .map(name -> name.substring(3).replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT))
            .sorted()
            .toList();

        assertTrue(query.nativeQuery());
        assertEquals(projectionColumns, selectedColumns);
    }

    private static Stream<Arguments> projectionQueries() {
        return Stream.of(
                Arguments.of("queryStorePrices", new Class<?>[] { Integer.class }, ProductCommercialRepository.StorePriceProjection.class),
                Arguments.of("queryStorePriceForUpdate", new Class<?>[] { Integer.class, Integer.class }, ProductCommercialRepository.StorePriceProjection.class),
                Arguments.of("queryMasterHistory", new Class<?>[] { Integer.class, LocalDateTime.class, LocalDateTime.class }, ProductCommercialRepository.ProductHistoryProjection.class),
                Arguments.of("queryPriceHistory", new Class<?>[] { Integer.class, LocalDateTime.class, LocalDateTime.class }, ProductCommercialRepository.ProductPriceHistoryProjection.class)
        );
    }

}
