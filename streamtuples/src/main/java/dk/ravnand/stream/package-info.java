/**
 * <p>One of the major problems with using streams in Java 8+ is that the
 * language does not support tuples (multiple return values from a method)
 * so you very frequently
 * find that you need to pass both the original value and the
 * current result to the next step instead of just the current result.
 * The current Word Of God is that you
 * create a custom class for each intermediate step, which is rather
 * cumbersome.  This is an experiment to see if a helper class that
 * knows the original value plus some suitable helper methods can
 * replace these custom classes.  As the JRE does only have two-argument
 * but not three-argument definitions, this is for
 * two-tuples only for now. </p>
 * <p>Due to the way Java works the general idea is that each Stream
 * method, like {@code filter(...)} has a corresponding helper
 * method here which returns what the outer method needs to do its
 * job. </p>
 */
package dk.ravnand.stream;
