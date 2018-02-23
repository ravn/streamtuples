package dk.kb.streams;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * <p>
 * One of the major problems with using streams in Java is that the
 * language does not support tuples (multiple return values from a
 * method) so you very frequently find that you need to pass <em>both</em>
 * the original value and the current result to the next step instead
 * of just the current result.  The current Word Of God is that you
 * create a custom class for each intermediate step, which is rather
 * much a pain.  This is an experiment to see if a helper class that
 * knows the original value plus some suitable helper methods can
 * replace these custom classes.  As the JRE does only have two-argument
 * but not three-argument definitions, this is for
 * two-tuples only for now. </p>
 * <p>Due to the way Java works the general idea is that each Stream
 * method, like <code>filter(...)</code> has a corresponding helper
 * method here which returns what the outer method needs to do its
 * job. </p>
 */
public class StreamTuple<L, R> {

    protected final L left;
    protected final R right;

    /**
     * for <code> (l, r) -> new StreamTuple<>(l, r) </code>
     *
     * @param left
     * @param right
     */

    public StreamTuple(L left, R right) {
        this.left = left;
        this.right = right;
    }

    /**
     * Suitable for Stream::create.  Both left and right are set to the value passed in.
     */

    public static <L> StreamTuple<L, L> create(L left) {
        return new StreamTuple<>(left, left);
    }

    /**
     * Gets the left value.
     *
     * @return L left
     */
    public L left() {
        return left;
    }

    /**
     * Gets the right value
     *
     * @return R right
     */
    public R right() {
        return right;
    }

    /**
     * Return new TupleElement object with the given value (which may
     * be a completely different type than the one hold by this
     * object) and the same context.
     *
     * @param right value for new object.
     * @return IdValue with the same id and the new value.
     */
    public <U> StreamTuple<L, U> of(U right) {
        return new StreamTuple<>(left, right);
    }

    /**
     * Apply a given function to the value and return a new IdValue
     * object with the result (and same context).
     *
     * @param f function to apply to the current value to get the new value.
     * @return new SteamTuple with the same id and the result of applying f to current value.
     */
    public <U> StreamTuple<L, U> map(Function<R, U> f) {
        return of(f.apply(right));
    }

    /**
     * Apply a given two-argument function to the context <b>and</b>
     * the current value, and return a new object with the result (and
     * the same context).
     *
     * @param f function to apply to context and value to get new value.
     * @return new IdValue with the same id and the result of applying
     * f to current id and value.
     */
    public <U> StreamTuple<L, U> map(BiFunction<L, R, U> f) {
        return of(f.apply(left, right));
    }

    /**
     * for <pre>.filter(c->c.filter(v -> ...))</pre>
     */
    public boolean filter(Predicate<R> predicate) {
        return predicate.test(right);
    }

    /**
     * for <pre>.flatMap(c->c.flatMap(v -> ....))</pre>
     */

    public <U> Stream<StreamTuple<L, U>> flatMap(Function<R, Stream<U>> f) {
        return f.apply(right).map(this::of);
    }

    /**
     * for <pre>.flatMap(c->c.flatMap((id, v) -> ....))</pre>
     */
    public <U> Stream<StreamTuple<L, U>> flatMap(BiFunction<L, R, Stream<U>> f) {
        return f.apply(left, right).map(this::of);
    }

    /**
     * Default toString() as generated by IntelliJ
     */
    @Override
    public String toString() {
        return "StreamTuple{" +
                "left=" + left +
                ", right=" + right +
                '}';
    }
}
