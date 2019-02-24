package io.jdrphillips
package alphabetsoup

import shapeless.HList
import shapeless.::
import shapeless.HNil
import shapeless.Lazy
import shapeless.ops.hlist.IsHCons
import shapeless.test.illTyped

// Existence of this proves A can be mixed into B
trait Mixer[A, B] {
  def mix(a: A): B
}

object Mixer extends LowPriorityMixerImplicits1 {

  def apply[A, B](implicit m: Mixer[A, B]): Mixer[A, B] = m

  // If we are dealing with an atom, we can mix it into itself
  implicit def atomicCase[A: Atom]: Mixer[A, A] = new Mixer[A, A] {
    def mix(a: A): A = a
  }

}

trait LowPriorityMixerImplicits1 extends LowPriorityMixerImplicits2 {

  // Anything can satisfy HNil
  implicit def hnilCase[A]: Mixer[A, HNil] = new Mixer[A, HNil] {
    def mix(a: A): HNil = HNil
  }

  // Atomise B, and if it is an HList split it and recurse on head and tail
  implicit def bHListRecurse[A, B, BOut <: HList, BH, BT <: HList](
    implicit atomiser: Atomiser.Aux[B, BOut],
    hcons: IsHCons.Aux[BOut, BH, BT],
    m1: Lazy[Mixer[A, BH]],
    m2: Mixer[A, BT]
  ): Mixer[A, B] = new Mixer[A, B] {
    def mix(a: A): B = atomiser.from(hcons.cons(m1.value.mix(a), m2.mix(a)))
  }

}

trait LowPriorityMixerImplicits2 {

  // If B is an atom, select the value from A
  implicit def bAtomicRecurse[A, AOut <: HList, B](
    implicit atom: Atom[B],
    s: AtomSelector[A, B]
  ): Mixer[A, B] = new Mixer[A, B] {
    def mix(a: A): B = s(a)
  }

}

object MixerTests {

  case class Single(s: String)
  case class A1(i: Int, s: String)
  case class A2(s2: String, i2: Int)
  case class B(b: Boolean, s: String)
  case class N1(a1: A1, a2: A2)
  case class N2(a1: A1, b: B)

  case class Age(i: Int)
  case class FirstName(value: String)
  case class LastName(value: String)
  case class Address1(value: String)
  case class Address2(value: String)
  case class City(value: String)
  case class Postcode(value: String)

  case class Person(firstName: FirstName, lastName: LastName, age: Age)
  case class Address(a1: Address1, a2: Address2, c: City, p: Postcode)
  case class AllInfo(p: Person, a: Address)

  case class FullName(f: FirstName, l: LastName)
  case class PersonAndPostcode(f: FullName, p: Postcode)
  case class PersonAndPostcodeAndAddress(pp: PersonAndPostcode, a1: Address1, a2: Address2, c: City)

  // Extremely complex
  {
    implicit val a1: Atom[Age] = Atom[Age]
    implicit val a2: Atom[FirstName] = Atom[FirstName]
    implicit val a3: Atom[LastName] = Atom[LastName]
    implicit val a4: Atom[Address1] = Atom[Address1]
    implicit val a5: Atom[Address2] = Atom[Address2]
    implicit val a6: Atom[City] = Atom[City]
    implicit val a7: Atom[Postcode] = Atom[Postcode]
//    Mixer[PersonAndPostcodeAndAddress, AllInfo]
    Mixer[AllInfo, PersonAndPostcodeAndAddress]
  }

  // Nested
  Mixer[(A1, A2), (A1, A2)]
  Mixer[(A1, A2), A2 :: A1 :: HNil]
  // Constucts A2 all by itself
  Mixer[(Int, A1, String), A2 :: A1 :: HNil]
  // Constructs both A1 and A2
  Mixer[(Int, String), A2 :: A1 :: HNil]
  // Selects the left-most and outer-most int to fill A1,A2
  Mixer[(Int, Int, String), A2 :: A1 :: HNil]
  Mixer[N1, A1 :: A2 :: HNil]
  Mixer[(A2, Boolean :: HNil), N2]
  Mixer[(Int, Boolean :: String :: HNil), N2]

  // Longer + order swapping
  Mixer[Int :: String :: HNil, String :: Int :: HNil]
  Mixer[Int :: String :: HNil, Int :: String :: HNil]
  Mixer[Int :: String :: HNil, (String, Int)]
  illTyped("Mixer[Int :: HNil, (String, Int)]")
  Mixer[Int :: String :: HNil, A1]
  Mixer[Int :: String :: HNil, A2]
  Mixer[(String, Int), A1]
  Mixer[A2, (Int, String)]
  Mixer[A1, A2]
  Mixer[A2, A1]

  // Simple single transformation case
  Mixer[Tuple1[String], String :: HNil]
  Mixer[Tuple1[String], Single]
  Mixer[String :: HNil, Tuple1[String]]
  Mixer[String :: HNil, Single]
  Mixer[Single, String :: HNil]
  Mixer[Single, Tuple1[String]]
  illTyped("Mixer[Single, Tuple1[Int]]")

  // Simple equality
  Mixer[String, String]
  illTyped("""Mixer[Int, String]""")
  Mixer[String :: HNil, String :: HNil]
  Mixer[Tuple1[String], Tuple1[String]]
  Mixer[A1, A1]

}
