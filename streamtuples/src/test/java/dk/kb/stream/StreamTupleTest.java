package dk.kb.stream;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @noinspection WeakerAccess, Convert2MethodRef, ArraysAsListWithZeroOrOneArgument
 */
public class StreamTupleTest {
    @Test
    public void areLeftAndRightAsExpected() {
        String s1 = "s1";
        String s2 = "s2";
        StreamTuple<String, String> streamTuple = new StreamTuple<>(s1, s2);
        assertEquals(s1, streamTuple.left());
        assertEquals(s2, streamTuple.right());
    }

    @Test
    public void isSimpleStreamCollectionWorking() {
        Map<String, String> m = Stream.of("1", "2", "3")
                .map(StreamTuple::create)
                .collect(toMap(t -> t.left(), t -> t.right()));

        Map<String, String> expected = new TreeMap<>();
        expected.put("1", "1");
        expected.put("2", "2");
        expected.put("3", "3");

        assertThat(m, is(expected));
    }

    @Test  // junit 5
    public void simpleMapUpdateOperationUsingStreamTupleForEach() {
        Map<Integer, String> map = new HashMap<>();
        map.put(1, "1.");
        map.put(2, "2.");

        Map result = map.keySet().stream()
                .map(StreamTuple::create) // (1, 1), (2, 2)
                // lookup value for key
                .map(st -> st.map(key -> map.get(key))) // (1, "1."), (2, "2.")
                // only process those who are interesting
                .filter(st -> st.filter(s -> s.startsWith("1"))) // (1, "1.")
                // manipulate value, no notion of key
                .map(st -> st.map(s -> s + " OK"))  // (1, "1. OK")
                // store value back for key
                .map(st -> st.map((key, s) -> map.put(key, s))) // (1, "1.") (put returns old value)
                .collect(toMap(st -> st.left(), st -> st.right()));

        Map<Integer, String> expectedResult = new HashMap<>();
        expectedResult.put(1, "1.");
        assertThat(result, is(expectedResult));

        Map<Integer, String> expectedMap = new HashMap<>();
        expectedMap.put(1, "1. OK");
        expectedMap.put(2, "2.");
        assertThat(map, is(expectedMap));
    }

    @Test
    public void canWeMapBetweenTypes() {
        Map<String, String> m = Stream.of("1", "2", "3")
                .map(StreamTuple::create)
                .map(t -> t.map(v -> Integer.valueOf(v) * 2))
                .map(t -> t.map(v -> "doubled is " + v))
                .collect(toMap(t -> t.left(), t -> t.right()));

        Map<String, String> expected = new TreeMap<>();
        expected.put("1", "doubled is 2");
        expected.put("2", "doubled is 4");
        expected.put("3", "doubled is 6");

        assertThat(m, is(expected));
    }

    @Test
    public void canWeMapBetweenTypesUsingBothLeftAndRight() {
        Map<String, String> m = Stream.of("1", "2", "3")
                .map(StreamTuple::create)
                .map(t -> t.map(v -> Integer.valueOf(v) * 2))
                .map(t -> t.map((id, v) -> id + "*2=" + v))
                .collect(toMap(t -> t.left(), t -> t.right()));

        Map<String, String> expected = new TreeMap<>();
        expected.put("1", "1*2=2");
        expected.put("2", "2*2=4");
        expected.put("3", "3*2=6");

        assertThat(m, is(expected));
    }

    @Test
    public void twoArgumentConstructorAndTypeChangesFilteringValues() {
        // Change value type several times.
        Map<Integer, String> m = Stream.of(1, 2, 3)
                .map(id -> new StreamTuple<>(id, Math.PI * id))
                .filter(t -> t.filter((id, v) -> id > 1 && v < 7))
                .map(t -> t.map(r -> Double.toString(r).substring(0, 4)))
                .collect(toMap(t -> t.left(), t -> t.right()));

        Map<Integer, String> expected = new TreeMap<>();
        expected.put(2, "6.28");

        assertThat(m, is(expected));
    }

    @Test
    public void filterOutRightsLargerThanTwo() {
        Map<Integer, Integer> m = Stream.of(1, 2, 3)
                .map(StreamTuple::create)
                .filter(t -> t.filter(r -> r < 2))
                .collect(toMap(t -> t.left(), t -> t.right()));

        Map<Integer, Integer> expected = new TreeMap<>();
        expected.put(1, 1);

        assertThat(m, is(expected));
    }

    @Test
    public void flatMap_doubleOddRights() {
        Map<Integer, Integer> m = Stream.of(1, 2, 3)
                .map(StreamTuple::create)
                // get odd ones and multiply them by two
                .flatMap(t -> t.flatMap(r -> r % 2 == 1 ? Stream.of(r * 2) : Stream.of()))
                .collect(toMap(t -> t.left(), t -> t.right()));

        Map<Integer, Integer> expected = new TreeMap<>();
        expected.put(1, 2);
        expected.put(3, 6);

        assertThat(m, is(expected));
    }

    @Test
    public void flatMap_doubleOddRightsTripleEvenRights() {
        Map<Integer, List<Integer>> m = Stream.of(1, 2, 3)
                .map(StreamTuple::create)
                .flatMap(t -> t.flatMap((l, r) -> l % 2 == 1 ? Stream.of(r * 2) : Stream.of(r, r * 2, r * 3)))
                .collect(groupingBy(t -> t.left(), mapping(t -> t.right(), toList()))); // multi-valued map

        Map<Integer, List<Integer>> expected = new TreeMap<>();
        expected.put(1, Arrays.asList(2));
        expected.put(2, Arrays.asList(2, 4, 6));
        expected.put(3, Arrays.asList(6));

        assertThat(m, is(expected));
    }

    @Test
    public void sorted_noArgument() {
        List<String> l = Stream.of(
                new StreamTuple<>(1, "z"),
                new StreamTuple<>(2, "b"),
                new StreamTuple<>(3, "a")
                )
                .sorted()
                .map(st -> st.right())
                .collect(toList());

        assertThat(l, is(Arrays.asList("a", "b", "z")));

    }
}
