# Alphabet soup

This library is intended to give a seamless way to manipulate scala structures into one another, mixing the types intelligently as required

For a whistle-stop tour of the underlying algorithm, see [here](https://medium.com/@jdrphillips/alphabet-soup-type-level-transformations-eb60918af35d)

## Installation

Releases are available on Maven. Add the following to your build.sbt:

```
"io.typechecked" %% "alphabet-soup" % "<version>"
```

for the version of your choice. The project is fully tagged and release versions are available to view online.

## Release Process

If you're lucky enough to be able to release this project, do `sbt -mem 6000 release` and it will cross-release each scala version + jvm/js combination. Do not do `+release` - this option does not work.

The 6GB of RAM is necessary for the tests to compile at the same time.

## Concepts

### Mixers

The top-level concept is called `Mixer`. You give it two types, and if the libary can squeeze the first into the second
then it compiles.

At runtime, it turns the supplied value into an instance of the second value.

A quick example:

```scala
case class Pet(name: String, age: Int)

val pet: Pet = Pet("Jeremy", 5)

Mixer[Pet, (Int, String)].mix(pet) == (5, "Jeremy")
```

It works recursively on much larger examples too.

### Atoms

The base element in the library is an `Atom`. `Mixer` stops searching the branch of the tree it's in once it reaches an `Atom` - so they form the
building blocks of the structures you want to manipulate.

The library provides `Atom` evidence for the usual suspects `Int`, `String`, etc.

Making your own is very simple:

```scala
implicit val atomT: Atom[T] = Atom[T]
```

Just make sure it's in scope whenever you create a `Mixer`.

Alternatively an `Atom` can be created by using the macro annotation `@Atomic`.

In order to use the macro you must add the following compiler plugin to your `build.sbt`
```
addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full),
```

Then creating an `Atom` is simple:
```scala
@Atomic case class Foo(a: Int)
@Atomic trait Foo
@Atomic class Foo
```

### Molecules

A `Molecule` is something beyond a boundary that we can't handle at compile time - for example a `List`. The information
of how many items are in the source `List` is not known to us, so we can't do anything without some extra help or structure.

That takes the form of recursing inside the `Molecule`, and treating it as its own closed off world of `Atom`s and sub-`Molecules` to
process. No value from an outer class can make it into a `Molecule`.

A `Molecule` is mixed according to the normal rules:

```scala
trait A
trait B
case class AS(as: List[A])
case class BS(bs: List[B])
```

In the above example, `AS` can be mixed into `BS` if and only if `A` can be mixed into `B`.

List is provided for you in the library, for example. In general any `Functor` can be implemented very simply as a molecule.

## Defaults

There are two ways of creating defaults in alphabet-soup.

The first is to create a `DefaultAtom[T]`:

```scala
case class A(i: Int)
case class B(i: Int, s: String)

implicit val default: DefaultAtom[String] = DefaultAtom("some default string")

val mixer = Mixer[A,B]

mixer.mix(A(1))
```
This produces `B(1, "some default string")`

This can be useful if you want to create default values for types which do not possess much information.

`HNil` and `Unit` have provided `DefaultAtom`s.

The second way of creating defaults often comes in handy when you want to mix a class into a bigger type on a one-time basis.
To do this, you must manually create your `Mixer` using the builder provided, and give it the required defaults. For example:

```scala
case class A(i: Int)
case class B(i: Int, s: String, b: Boolean)

Mixer[A, B]  // Does not compile
val m = Mixer.from[A].to[B].withDefault("").withDefault(true).build  // Does compile. This is an instance of `Mixer[A, B]`
m.mix(A(0)) == B(0, "", true)
```

## Complex example

Here's a more complicated example to show what it can do.

```scala
// You should follow the philosophy of every value is a type!
@Atomic case class FirstName(value: String)
@Atomic case class LastName(value: String)
@Atomic case class Address1(value: String)
@Atomic case class City(value: String)
@Atomic case class Postcode(value: String)
@Atomic case class Title(value: String)
@Atomic case class Gender(value: String)

// This is our data tree
case class Address(a1: Address1, c: City, p: Postcode)
case class Alias(firstName: FirstName, lastName: LastName, isLegal: Boolean)
case class AddressHistory(values: List[Address])

// This is our source data class
case class Source(
  firstName: FirstName,
  lastName: LastName,
  addressHistory: AddressHistory,
  aliases: List[(Title, FirstName, LastName)]
)

// This is what we'll be mapping it into
case class Target(
  name: (FirstName, LastName),
  addresses: List[(Address1, Postcode)],
  aliases: List[Alias],
  gender: Gender
)

// We're going to map an intance of Source into an instance of Target
val source = Source(
  firstName = FirstName("John"),
  lastName = LastName("Johnson"),
  addressHistory = AddressHistory(List(
    Address(Address1("5 John Street"), City("Johnsville"), Postcode("JOHN")),
    Address(Address1("5 Jack Street"), City("Jacksville"), Postcode("JACK"))
  )),
  aliases = List(
    (Title("Mr"), FirstName("Johnny"), LastName("Vegas")),
    (Title("Mr"), FirstName("Jon"), LastName("Snow"))
  )
)

// We need a Mixer

// First attempt:
Mixer[Source, Target]  // Won't compile because we have no 'Gender' in our Source

// Second attempt:
Mixer.from[Source].to[Target].withDefault(Gender("male")).build  // Won't compile because we have no 'isLegal' in our source aliases

// Third attempt:
implicit val submixer = Mixer.from[(Title, FirstName, LastName)].to[Alias].withDefault(true)
val mixer = Mixer.from[Source].to[Target].withDefault(Gender("male")).build

val result = mixer.mix(source)

// This test passes!
result shouldBe Target(
  name = (FirstName("John"), LastName("Johnson")),
  addresses = List(
    (Address1("5 John Street"), Postcode("JOHN")),
    (Address1("5 Jack Street"), Postcode("JACK"))
  ),
  aliases = List(
    Alias(FirstName("Johnny"), LastName("Vegas"), true),
    Alias(FirstName("Johnny"), LastName("Vegas"), true)
  ),
  gender = Gender("male")
)
```

## The future

This library will be extended to:

* handle functional `Atom`s, which contain within them a way to transform one type to another
* have a "strict" version, which will prevent duplicate types in the source of the mixer
