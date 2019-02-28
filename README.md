# Alphabet soup

This library is intended to give a seamless way to manipulate scala structures into one another, mixing the types intelligently as required

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

## Complex example

Here's a more complicated example to show what it can do.

```scala
// You should follow the philosophy of every value is a type!

case class Age(i: Int)
case class FirstName(value: String)
case class LastName(value: String)
case class Address1(value: String)
case class Address2(value: String)
case class City(value: String)
case class Postcode(value: String)

// This is our source data tree
case class Person(firstName: FirstName, lastName: LastName, age: Age)
case class Address(a1: Address1, a2: Address2, c: City, p: Postcode)
case class Resident(p: Person, a: Address)

// This is the tree we will squeeze our data into
case class FullName(f: FirstName, l: LastName)
case class PersonAndPostcode(f: FullName, p: Postcode)
case class PersonAndPostcodeAndAddress(pp: PersonAndPostcode, a1: Address1, a2: Address2, c: City)

// In your project, these will be hidden away. We need them otherwise we
// get exciting behaviours. See tests for the example
implicit val a1: Atom[Age] = Atom[Age]
implicit val a2: Atom[FirstName] = Atom[FirstName]
implicit val a3: Atom[LastName] = Atom[LastName]
implicit val a4: Atom[Address1] = Atom[Address1]
implicit val a5: Atom[Address2] = Atom[Address2]
implicit val a6: Atom[City] = Atom[City]
implicit val a7: Atom[Postcode] = Atom[Postcode]

// Here is our source data
val resident = Resident(
  Person(
    FirstName("Boaty"),
    LastName("McBoatface"),
    Age(2)
  ),
  Address(
    Address1("North Pole"),
    Address2("The Arctic"),
    City("Northern Hemisphere"),
    Postcode("N0RT4")
  )
)

// Here is the data we expect to get
val expectedResult: PersonAndPostcodeAndAddress = PersonAndPostcodeAndAddress(
  PersonAndPostcode(
    FullName(
      FirstName("Boaty"),
      LastName("McBoatface")
    ),
    Postcode("N0RT4")
  ),
  Address1("North Pole"),
  Address2("The Arctic"),
  City("Northern Hemisphere")
)

// This test passes. See test file
Mixer[Resident, PersonAndPostcodeAndAddress].mix(allInfo) == expectedResult
```

Note in the above example, `Mixer[PersonAndPostcodeAndAddress, Resident]` would not compile because there is no
`Age` value inside a `PersonAndPostcodeAndAddress` which we can pass over.

If we didn't have the `Atom`s for our types, the program would recurse down to the level of `String` and fill the
entire right hand side with the first `String` it encountered - in this case `"Boaty"`.

## The future

This library will be extended to handle functional `Atom`s, which contain within them a way to transform one type
to another.

It will also acquire a version which works on a "by field name" basis for case classes, rather than by type. One nice
thing about the version above is that it works for essentially any product type: `Tuple`, `HList`, `case class`, etc.
