package dk.ravnand.streamtuples;

import java.util.Arrays;
import java.util.stream.Stream;

public class StreamTuples {
    /**
     * Suitable for StreamTuples::of.  Both left and right are set to the value passed in.  This is a good start if you have
     * a streamtuples of keys which you need later.
     *
     * @param item the item to be placed in both {@code left} and {@code right}
     * @return StreamTuple with item in both left and right.
     */

    public static <I> StreamTuple<I, I> of(I item) {
        return new StreamTuple<>(item, item);
    }

    /**
     * Create stream of streamtuples of the items.  Convenient shortcut for the Stream.of(...).map(StreamTuples::of)
     * snippet.
     *
     * @param items what to return StreamTuples for.
     * @param <I> type of item
     * @return stream of streamtuples for items.
     */
    public static <I> Stream<StreamTuple<I, I>> streamOf(I... items) {
        return Arrays.stream(items).map(StreamTuples::of);
    }
}
