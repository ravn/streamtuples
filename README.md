# tl;dr StreamTuples didn't turn out well - use Java records!

## Backstory

I have always liked Haskell - one of the mainstream functional languages popular in academia - for many reasons.
Functional languages are characterized with that you create small functions taking input and generating output, and
chain them together to process data structures. This is what Java has tried to take most of with the Streams introduced
in Java 8, and which I spent quite some time trying to do more than just the "invert the loop over a Collection" basic
usage.

Unfortunately I found rather quickly that there were some things that was harder to do with Streams than I expected
because Java does not embrace the tuple (handling multiple independent values toegether).

There are good reasons for this. The Guava library designers considered it and rejected
it (https://github.com/google/guava/wiki/IdeaGraveyard#comgooglecommonbase) and Oracle have advocated creating named
classes for this, and said in JEPS 395 _"A central aspect of Java's design philosophy is that names matter. Classes and
their members have meaningful names, while tuples and tuple components do not. That is, a Person record class with
components firstName and lastName is clearer and safer than an anonymous tuple of two strings."_ as part of the
rationale for designing records instead.

I found however that in expressing the way I thought as code, I had a need for being able to save a value at one point
in the stream, in order to be able to use it later, even after mapping/filtering into something else. The Stream API
does not allow you to add new things so you only have the items _inside_ the stream to work with.

After some consideration I got the idea back in 2018 what about a helper class that not only could handle more than one
value, but also knew it lived inside a stream and only there and could provide the appropriate helper methods? Perhaps
that could that be a reasonable compromise?

I decided to try it out with this library on real code to see how it was to work with.

It turned out that my usecases only needed to save a single value, so that was what was implemented. I also used the
opportunity to write unit tests with the new language features in Java 10+ - that worked very well, and is highly
recommended.

In very short, it did not turn out to my satisfaction. It became very verbose and rather hard to read and I didn't get
the expressiveness that I was looking for.

My conclusion was that the "Java can only return _one_ thing from a method" could not be easily worked around with the
Stream API being closed for extension, and I put it away.

## Java 16+ records

It turns out that Oracle has some very long term projects running for improving the Java language, but that due to the
attention to not changing an API after introduction (was in the genes for Sun who created Java) it may take quite some
time before things become part of the official Java languages. Java 8 was a watershed release which was the last one
where almost all old programs could compile and run - I guess this makes it the Python2 equivalent in that it will live
extremely long for those who can afford paid maintenance - and all the new goodies started coming in in Java 9 and
onwards.

**Records** has been underway for quite some time, but will go out of preview in Java 16 coming in 2021. The full JEPS
is at https://openjdk.java.net/jeps/395, but in brief:

* Records are very well suited for immutable Data Transfer Objects (objects just with data, and no code).
* All the getter/setter/hashCode/equals scaffolding is generated automatically.
* Records have a very concise syntax so most turn out to be one-line definitions which can be placed right next to where
  they are used.

The sample from the JEPS:

```java
List<Merchant> findTopMerchants(List<Merchant> merchants,int month){
        // Local record
        record MerchantSales(Merchant merchant,double sales){}

        return merchants.stream()
        .map(merchant->new MerchantSales(merchant,computeSales(merchant,month)))
        .sorted((m1,m2)->Double.compare(m2.sales(),m1.sales()))
        .map(MerchantSales::merchant)
        .collect(toList());
        }

```

which computes the sales for a given month for a merchant, put both together in a MerchantSales record, and sorts by it,
and then extracts the merchant again and collects in a list.

For StreamTuples this would be a create on the merchant saving it into a `StreamTuple<Mechant, Merchant>`, then map the
value to the sales for the month giving a `StreamTuple<Mechant, Double>`, and sort by that, and then use `left()` to get
the merchant back and then collect.

The major difference in the line of thought is that instead of calculating things in `map()` calls after the `create()`,
records lend to saving all the derived values in the record, and calculating them as parameters to the constructor call.
This is not quite as flexible as what I had in mind, but close enough to be very useful.

So my conclusion again:

> **Do _not_ use StreamTuples, upgrade to Java 16 and use `record`**

Note: Records are also available as preview in Java 14 and 15, but only use those if you cannot upgrade. Next Long Term
Support of Java as of 2020-12-27 is Java 17 scheduled for autum 2021. There will be many useful new things - like the
new packager - so this is a good reason to upgrade your sources a bit ahead of time.

/ravn 2020-12-27

# Original project description.

Note: Requires Java 10+ to compile, Java 8+ to use.

_streamtuples_ is a solution to the "I need the value again I had earlier in the stream". This is typically when you
have an id, use it to look something up (replacing the id with the value found) and then needing _both_ the id and the
value to send the result back to storage. In most functional languages this is typically done by returning a pair of
values (or more). Such a pair is called a _tuple_
and is typically written like `(a, b) = method(...)`.

This is not possible in Java 8 as there are no multi-value return types or pairs/tuples in the language, so the official
recommendation is to write a custom class for each intermediate step in the stream as needed, but even so the language
does not help with autoboxing (for lack of a better word) these, unless you use magic somewhere in the process.

This project offers a different approach: A tuple holding two values and helper methods to use the tuple in streams.
Focus has been on following the rules and help the compiler as much as possible.

## TODO## :  Graphic showing  what is going on.

An example from an early unit test. The really important bit is the
`(left, right) -> left + "*2=" + right)` snippet inside a `map(...)`
which is a two-argument lambda expression where the value saved earlier and the item currently in the stream are both
used.

Details: The value passed in the actual stream is turned into a
`StreamTuple<L, R>` which _also_ has a `map` method which returns what the actual `map` method needs to pass a suitable
new `StreamTuple`to the next step in the stream.

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
`Stream.filter()` method, and `right()` plus `left()` which returns the two values stored in the stream tuple.


COMPILATION & DEPLOYMENT
---
Usage:

* Java 8 or later. Module is "dk.ravnand.streamtuples".

Required for building:

* Java 10+

Create new version for Maven Central:

1. `mvn clean install`
1. `git status` (must report "Your branch is up-to-date with origin/X")
1. `mvn -f streamtuples release:prepare -DpushChanges=false -Dresume=false`
1. `git checkout streamtuples-X.X`
1. `mvn -f streamtuples clean deploy`
1. `git checkout master`
1. `git push --tags && git push`

*If anything fails, clean and start over* (possibly by discarding this clone and creating a new one). ONLY push when
everything is in perfect order!

Note this is different from the typical way of doing this, because I want to have better control of the actual deploy
process. Maven is badly overengineered there.

Only the library itself is intended to go to Maven Central. It is a Multi Release Jar with a (for now) manually compiled
module-info.class file.

/tra 2018-04-05

Moved from previous employer domain "dk.kb.\*" to new personal domain "dk.ravnand.\*".

/ravn 2018-04-25
  
