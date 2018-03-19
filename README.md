[![Javadocs](http://javadoc.io/badge/dk.kb.stream/streamtuples.svg)](http://javadoc.io/doc/dk.kb.stream/streamtuples)


_streamtuples_ is a solution to the "I need the value again I had
earlier in the stream".  This is typically when you have an id, use it
to look something up (replacing the id with the value found) and then
needing _both_ the id and the value to send the result back to
storage.  In most functional languages this is typically done by
returning a pair of values (or more).  Such a pair is called a _tuple_
and is typically written like `(a, b) = method(...)`.

This is not possible in Java 8 as there are no multi-value return
types or pairs/tuples in the language, so the official recommendation
is to write a custom class for each intermediate step in the stream as
needed, but even so the language does not help with autoboxing (for
lack of a better word) these, unless you use magic somewhere in the
process.

This project offers a different approach: A tuple holding two values
and helper methods to use the tuple in streams.  Focus has been on
following the rules and help the compiler as much as possible.

##TODO## :  Graphic showing  what is going on.

An example from an early unit test.  The really important bit is the
`(left, right) -> left + "*2=" + right)` snippet inside a `map(...)`
which is a two-argument lambda expression where the value saved
earlier and the item currently in the stream are both used.

Details: The value passed in the actual stream is turned into a
`StreamTuple<L, R>` which _also_ has a `map` method which returns what
the actual `map` method needs to pass a suitable new `StreamTuple`to
the next step in the stream.



    @Test
    public void map1() {
        Map<String, String> m = Stream.of("1", "2", "3")
                .map(StreamTuple::create)
                .map(t -> t.map(right -> Integer.valueOf(right) * 2))
                .map(t -> t.map((left, right) -> left + "*2=" + right))
                .collect(toMap(t -> t.left(), t -> t.right()));

        Map<String, String> expected = new TreeMap<>();
        expected.put("1", "1*2=2");
        expected.put("2", "2*2=4");
        expected.put("3", "3*2=6");

        assertThat(m, is(expected));
    }

StreamTuple also has a `filter()` method which helps the
`Stream.filter()` method, and `right()` plus `left()` which returns
the two values stored in the stream tuple.


COMPILATION & DEPLOYMENT
---

Required:
* Java 9 (for tests, library is compiled for Java 8+)

Create new version:
1. `mvn clean install`
1. `git status` (must report "Your branch is up-to-date with origin/X")
1. `mvn -f streamtuples release:prepare -DpushChanges=false -Dresume=false`
1. `git checkout streamtuples-X.X`
1. `mvn -f streamtuples clean deploy`
1. `git checkout master`
1. `git push --tags && git push`

If any fails, clean and start over (possibly by discarding this clone
and creating a new one).  ONLY push when everything is in perfect
order!  Note this is different from the typical way of doing this,
because I want to have better control of the deploy process.  Maven is
badly overengineered there.

Only the library itself is intended to go to Maven Central. 

/tra 2018-03-19
