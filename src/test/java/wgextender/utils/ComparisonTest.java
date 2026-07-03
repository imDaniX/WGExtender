package wgextender.utils;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Comparator;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class ComparisonTest {
    private static Stream<Arguments> ofMagicData() {
        return Stream.of(
                Arguments.of(-1, Comparison.BELOW),
                Arguments.of(-100, Comparison.BELOW),
                Arguments.of(0, Comparison.EQUAL),
                Arguments.of(1, Comparison.ABOVE),
                Arguments.of(100, Comparison.ABOVE)
        );
    }

    @ParameterizedTest
    @MethodSource("ofMagicData")
    void ofMagicTest(int magic, Comparison expected) {
        assertEquals(expected, Comparison.ofMagic(magic));
    }

    private static Stream<Arguments> ofComparableData() {
        return Stream.of(
                Arguments.of(5, 10, Comparison.BELOW),
                Arguments.of(10, 5, Comparison.ABOVE),
                Arguments.of(7, 7, Comparison.EQUAL)
        );
    }

    @ParameterizedTest
    @MethodSource("ofComparableData")
    void ofComparableTest(Comparable<Integer> left, Integer right, Comparison expected) {
        assertEquals(expected, Comparison.of(left, right));
        assertTrue(Comparison.is(left, expected, right));
    }

    private static Stream<Arguments> ofComparatorData() {
        return Stream.of(
                Arguments.of("hey", "hello", Comparison.BELOW),
                Arguments.of("hello", "hey", Comparison.ABOVE),
                Arguments.of("test", "code", Comparison.EQUAL)
        );
    }

    @ParameterizedTest
    @MethodSource("ofComparatorData")
    void ofComparatorTest(String left, String right, Comparison expected) {
        Comparator<String> lengthComparator = Comparator.comparingInt(String::length);
        assertEquals(expected, Comparison.of(lengthComparator, left, right));
        assertTrue(Comparison.is(lengthComparator, left, expected, right));
    }
}