package org.prebid.cache.model;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;

import static org.reflections.util.ClasspathHelper.*;

class ContractModelTest {

    @ParameterizedTest
    @MethodSource("modelClassProvider")
    void verifyEqualsAndHashCode(Class<?> clazz) {
        EqualsVerifier.forClass(clazz).verify();
    }

    static Iterable<Class<?>> modelClassProvider() {

        Reflections reflections = new Reflections(
                new ConfigurationBuilder().setScanners(
                        new SubTypesScanner(false)
                )
                        .setUrls(forPackage("org.prebid.cache", staticClassLoader()))
                        .filterInputsBy(new FilterBuilder()
                                .include("org\\.prebid\\.cache\\.model\\..*\\.class")
                                .exclude("org\\.prebid\\.cache\\.model\\..*(Builder|Test)\\.class")
                        )
        );

        return reflections.getSubTypesOf(Object.class);
    }
}