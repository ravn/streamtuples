package dk.ravnand.streamtuples;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @noinspection WeakerAccess, Convert2MethodRef
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
        var m = Stream.of("1", "2", "3")
                .map(StreamTuple::create)
                .collect(toMap(t -> t.left(), t -> t.right()));

        assertThat(m, is(Map.of("1", "1",
                "2", "2",
                "3", "3")));
    }

    @Test  // junit 5
    public void simpleMapUpdateOperationUsingStreamTupleForEach() {
        var map = new HashMap<>(Map.of(
                1, "1.",
                2, "2."));

        var result = map.keySet().stream()
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

        assertThat(result, is(Map.of(1, "1.")));

        assertThat(map, is(Map.of(
                1, "1. OK",
                2, "2.")));
    }

    @Test
    public void canWeMapBetweenTypes() {
        var m = Stream.of("1", "2", "3")
                .map(StreamTuple::create)
                .map(t -> t.map(v -> Integer.valueOf(v) * 2))
                .map(t -> t.map(v -> "doubled is " + v))
                .collect(toMap(t -> t.left(), t -> t.right()));

        assertThat(m, is(Map.of("1", "doubled is 2",
                "2", "doubled is 4",
                "3", "doubled is 6")));
    }

    @Test
    public void canWeMapBetweenTypesUsingBothLeftAndRight() {
        var m = Stream.of("1", "2", "3")
                .map(StreamTuple::create)
                .map(t -> t.map(v -> Integer.valueOf(v) * 2))
                .map(t -> t.map((id, v) -> id + "*2=" + v))
                .collect(toMap(t -> t.left(), t -> t.right()));

        assertThat(m, is(Map.of("1", "1*2=2",
                "2", "2*2=4",
                "3", "3*2=6")));
    }

    @Test
    public void twoArgumentConstructorAndTypeChangesFilteringValues() {
        // Change value type several times.
        var m = Stream.of(1, 2, 3)
                .map(id -> new StreamTuple<>(id, Math.PI * id))
                .filter(t -> t.filter((id, v) -> id > 1 && v < 7))
                .map(t -> t.map(r -> Double.toString(r).substring(0, 4)))
                .collect(toMap(t -> t.left(), t -> t.right()));

        assertThat(m, is(Map.of(2, "6.28")));
    }

    @Test
    public void filterOutRightsLargerThanTwo() {
        var m = Stream.of(1, 2, 3)
                .map(StreamTuple::create)
                .filter(t -> t.filter(r -> r < 2))
                .collect(toMap(t -> t.left(), t -> t.right()));

        assertThat(m, is(Map.of(1, 1)));
    }

    @Test
    public void flatMap_doubleOddRights() {
        var m = Stream.of(1, 2, 3)
                .map(StreamTuple::create)
                // get odd ones and multiply them by two
                .flatMap(t -> t.flatMap(r -> r % 2 == 1 ? Stream.of(r * 2) : Stream.of()))
                .collect(toMap(t -> t.left(), t -> t.right()));

        assertThat(m, is(Map.of(1, 2,
                3, 6)));
    }

    @Test
    public void flatMap_doubleOddRightsTripleEvenRights() {
        var m = Stream.of(1, 2, 3)
                .map(StreamTuple::create)
                .flatMap(t -> t.flatMap((l, r) -> l % 2 == 1 ? Stream.of(r * 2) : Stream.of(r, r * 2, r * 3)))
                .collect(groupingBy(t -> t.left(), mapping(t -> t.right(), toList()))); // multi-valued map

        assertThat(m, is(Map.of(
                1, List.of(2),
                2, List.of(2, 4, 6),
                3, List.of(6))));
    }

    @Test
    public void peek_oneArgTestExplicitConsumer() {

        var list = new ArrayList<>();
        var m = Stream.of(1, 2, 3)
                .map(StreamTuple::create)
                .map(st -> st.map(r -> r * 2))
                // collect a derived value in separate list.
                .peek(st -> st.peek(r -> list.add("-" + r)))
                .collect(groupingBy(st -> st.left(), mapping(st -> st.right(), toList())));

        assertThat(m, is(Map.of( //
                1, List.of(2), //
                2, List.of(4), //
                3, List.of(6))));

        assertThat(list, is(List.of("-2", "-4", "-6")));

    }

    @Test
    public void peek_oneArgTest() {

        var list = new ArrayList<>();
        var m = Stream.of(1, 2, 3)
                .map(StreamTuple::create)
                .map(st -> st.map(r -> r * 2))
                .peek(st -> st.peek(r -> list.add("-" + r)))
                .collect(groupingBy(st -> st.left(), mapping(st -> st.right(), toList())));

        assertThat(m, is(Map.of( //
                1, List.of(2), //
                2, List.of(4), //
                3, List.of(6))));

        assertThat(list, is(List.of("-2", "-4", "-6")));

    }

    @Test
    public void peek_twoArgTestExplicitBiConsumer() {
        var list = new ArrayList<>();
        var m = Stream.of(1, 2, 3)
                .map(StreamTuple::create)
                .map(st -> st.map(r -> r * 2))
                // collect derived value in list
                .peek(st -> st.peek((l, r) -> list.add(l + "-" + r)))
                .collect(groupingBy(st -> st.left(), mapping(st -> st.right(), toList())));

        assertThat(m, is(Map.of(
                1, List.of(2),
                2, List.of(4),
                3, List.of(6))));

        assertThat(list, is(List.of("1-2", "2-4", "3-6")));
    }

    @Test
    public void peek_twoArgTest() {
        var list = new ArrayList<>();
        var m = Stream.of(1, 2, 3)
                .map(StreamTuple::create)
                .map(st -> st.map(r -> r * 2))
                .peek(st -> st.peek((l, r) -> list.add(l + "-" + r)))
                .collect(groupingBy(st -> st.left(), mapping(st -> st.right(), toList())));

        assertThat(m, is(Map.of(
                1, List.of(2),
                2, List.of(4),
                3, List.of(6))));

        assertThat(list, is(List.of("1-2", "2-4", "3-6")));
    }

    @Test
    public void sorted_noArgument() {
        var l = Stream.of(
                new StreamTuple<>(1, "z"),
                new StreamTuple<>(2, "b"),
                new StreamTuple<>(3, "a")
        )
                .sorted()
                .map(st -> st.right())
                .collect(toList());

        assertThat(l, is(List.of("a", "b", "z")));
    }
}
