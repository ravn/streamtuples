_streamtuples_ is a solution to the "I need the value again I had
earlier in the stream".  This is typically when you have an id, use it
to look something up (replacing the id with the value found) and then
needing _both_ the id and the value to send the result back to
storage.

This is very cumbersome to do in Java as it doesn't have multi-value
return types or pairs/tuples in the language, so the official
recommendation is to write a custom class for each intermediate step
in the stream as needed, but even so the language does not help
autoboxing (for lack of a better word) these, unless you use magic 
somewhere in the proces.

This project offers a different approach.  A tuple holding two values
and helper methods to use the tuple in streams.  Focus has been on
following the rules and help the compiler as much as possible.

An example from an early unit test.  The value passed in the actual
stream is turned into a `StreamTuple<L, R>` which _also_ has a `map` 
method which
returns what the actual `map` method needs to pass a suitable new
`StreamTuple`to the next step in the stream.  



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

StreamTuple also has a `filter()` method which helps the `Stream.filter()` method, 
and  `right()` plus `left()` which returns the two values stored in the stream tuple.


/tra 2018-02-20

