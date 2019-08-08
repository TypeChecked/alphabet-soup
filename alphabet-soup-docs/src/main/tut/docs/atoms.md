---
layout: docs
title: Atoms and Atomisation
---

# Atoms and Atomisation

Alphabet soup works by first _atomising_ your data, powered by [shapeless](https://github.com/milessabin/shapeless/)' `Generic`, which
turns some data into `HList`s.

Eg,

* `(Int, String)`
* `Int :: String :: HNil`
* `case class Foo(i: Int, s: String)`

all atomise to `Int :: String :: HNil`.

But alphabet soup transforms the entire data structure into an `HList` recursively, rather than just the top layer as `Generic` does.
A more complex example:

```scala
case class Foo(
  one: (Int, String),
  two: Boolean :: Unit :: HNil
)
```

`Foo` would atomise to

```scala
(Int :: String :: HNil) :: (Boolean :: Unit :: HNil) :: HNil
```

It is these types, in the atomised representation, which are used to fill in `Target` in the `Mixer`.

Any arbitrarily nested combination of case classes, tuples and hlists will be handled.

## Atoms

However we don't _always_ want to atomise through all the layers.

You'll notice above that `Foo` disappeared from our atomised structure. If we _wanted_ a `Foo` in `Target` then it would reappear
for us after mixing, but in the intermediary step the data is free of any concept of `Foo`. The elements in the atomised `HList`
are what the algorithm works on, not the outer class names.

You may have for example the following value class, or similar:

```scala
case class UserId(value: Int) extends AnyVal
```

This would atomise to `Int :: HNil`, which you would almost never want. The `Int` isn't the important part of `UserId` - the fact it's a `UserId` is the important part.
You would never use a `UserId` in place of an `Int`, and neither should alphabet soup.

For this reason, alphabet soup introduces the concept of `Atom`.

`Atom` is a simple typeclass. They act as flags to the compiler, telling it to stop atomising on that branch of data.

_Every type in your data structures need an `Atom`_

They are easy to define:

```scala
@Atomic case class UserId(value: Int) extends AnyVal
```

or alternatively you can create them explicitly:

```scala
case class UserId(value: Int) extends AnyVal
object UserId {
  implicit val atomUserId = Atom[UserId]
}
```

You can also define them for types which you don't control or lack a friendly name:

```scala
implicit val reallyBigAtom = Atom[Map[Int, Map[Byte, String]]]
```

The usual suspects `Atom[Int]`, `Atom[String]`, etc, are defined for you.

### Examples

`(Int, String)` atomises to `Int :: String :: HNil`.

`case class Foo(i: Int, s: String)` atomises to `Int :: String :: HNil`.

`@Atomic case class Foo(i: Int, s: String)` atomises to `Foo`.

And now, with an atomic `Foo`, a `Foo` in our `Source` will only be matched to a `Foo` in our target. The internals,
the `Int` and the `String`, are not exposed or mixed by the algorithm:

```scala

type Source = Foo
type Target = (Int, String)

Mixer[Source, Target]  // Does not compile
```
