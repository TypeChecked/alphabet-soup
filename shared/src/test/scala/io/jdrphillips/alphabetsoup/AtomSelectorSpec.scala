package io.typechecked
package alphabetsoup

import org.scalatest._
import shapeless.{::, Generic, HNil}
import shapeless.test.illTyped

class AtomSelectorSpec extends FlatSpec with Matchers {

  case class Pair(i: Int, s: String)
  case class T(p: Pair, b: Boolean)

  "AtomSelector" should "not work on atoms" in {

    illTyped("AtomSelector[Int, Int]")
  }

  it should "not extract types that aren't there" in {
    illTyped("AtomSelector[Int :: String :: HNil, Boolean]")
  }

  it should "work on simple hlists" in {
    val list = 5 :: "hello" :: HNil
    AtomSelector[Int :: String :: HNil, Int].apply(list) shouldBe 5
    AtomSelector[Int :: String :: HNil, String].apply(list) shouldBe "hello"
  }

  it should "work on simple tuples" in {
    val tuple = (5, "hello")
    AtomSelector[(Int, String), Int].apply(tuple) shouldBe 5
    AtomSelector[(Int, String), String].apply(tuple) shouldBe "hello"
  }

  it should "work on case classes" in {
    val t = T(Pair(5, "hello"), true)
    AtomSelector[T, Int].apply(t) shouldBe 5
    AtomSelector[T, String].apply(t) shouldBe "hello"
    AtomSelector[T, Boolean].apply(t) shouldBe true
  }

  it should "work on case classes with an atom specified" in {
    implicit val pairAtom: Atom[Pair] = Atom[Pair]
    val t = T(Pair(5, "hello"), true)
    illTyped("AtomSelector[T, Int]")
    illTyped("AtomSelector[T, String]")
    AtomSelector[T, Boolean].apply(t) shouldBe true
    AtomSelector[T, Pair].apply(t) shouldBe Pair(5, "hello")
  }

  it should "work on (A, B) :: (C, D) :: HNil" in {
    implicit val pairAtom: Atom[Pair] = Atom[Pair]
    type T = (Int, String) :: (Char, Boolean) :: HNil
    val t = (1, "one") :: ('1', true) :: HNil
    AtomSelector[T, Int].apply(t) shouldBe 1
    AtomSelector[T, String].apply(t) shouldBe "one"
    AtomSelector[T, Char].apply(t) shouldBe '1'
    AtomSelector[T, Boolean].apply(t) shouldBe true
  }

  it should "select a molecule as-is given a mixer" in {

    case class A(b: Boolean, s: String)
    case class A2(s: String, b: Boolean)
    case class B(l: List[A])

    // TODO: Generate identity mixer automatically
    implicit val submixer: Mixer[A, A] = Mixer[A, A]

    val b = B(List(A(true, "1"), A(false, "2")))

    // A can be mixed to A2, so we can select a molecule of A2 from B
    AtomSelector[B, List[A]].apply(b) shouldBe b.l
  }

  it should "be able to select molecules that are mixable rather than precise matches, given a relevant mixer" in {

    case class A(b: Boolean, s: String)
    case class A2(s: String, b: Boolean)
    case class B(l: List[A])

    implicit val submixer: Mixer[A, A2] = Mixer[A, A2]

    val b = B(List(A(true, "1"), A(false, "2")))

    // A can be mixed to A2, so we can select a molecule of A2 from B
    AtomSelector[B, List[A2]].apply(b) shouldBe List(A2("1", true), A2("2", false))
  }

  it should "be able to handle reading a molecule from a tuple" in {
    AtomSelector[(Int, List[(String, Boolean)]), List[String]].apply(5 -> List("0" -> true)) shouldBe List("0")
  }

  it should "not allow data from outside the boundary of the molecule into the created molecule structure given a relevant mixer" in {
    case class A(b: Boolean, s: String)
    case class A2(s: String, b: Boolean)
    case class B(a: A, l: List[A])

    implicit val submixer: Mixer[A, A2] = Mixer[A, A2]

    val b = B(A(true, "DANGER"), List(A(true, "1"), A(false, "2")))

    // The initial 'A' value in b is NOT used in the construction of our molecule List[A2]
    AtomSelector[B, List[A2]].apply(b) shouldBe List(A2("1", true), A2("2", false))
  }

  it should "not atom select when no default is supplied" in {
    case class A(a: Int)
    illTyped("AtomSelector[A, String].apply(A(7))")
  }

  it should "atom select when a default is supplied" in {
    case class A(a: Int)

    implicit val default: Atom.DefaultAtom[String] = Atom.DefaultAtom("default")

    AtomSelector[A, String].apply(A(7)) shouldBe "default"

  }

  it should "not select when a default of wrong type is supplied" in {
    case class A(a: Int)

    implicit val default: Atom.DefaultAtom[Int] = Atom.DefaultAtom(5)

    illTyped("AtomSelector[A, String].apply(A(7))")

  }
  
  it should "atom select when a default is supplied for a complex type" in {

    case class A(a: Int)

    type Complex = (String, Int, (Boolean, Long))
    val complex = ("", 5, (false, 10L))

    implicit val default: Atom.DefaultAtom[Complex] = Atom.DefaultAtom(complex)

    AtomSelector[A, Complex].apply(A(7)) shouldBe complex

  }

  it should "always find DefaultAtom[HNil] and DefaultAtom[Unit]" in {

    case class A(i: Int)

    AtomSelector[A, HNil].apply(A(7)) shouldBe HNil
    
    AtomSelector[A, Unit].apply(A(7)) shouldBe (())

    illTyped("AtomSelector[A, (HNil, Unit)].apply(A(7))")

  }

  it should "not find a tuple of (HNil, Unit)" in {
    case class A(i: Int)

    illTyped("AtomSelector[A, (HNil, Unit)].apply(A(7))")

  } 

  it should "accept DefaultAtom as an Atom" in {
    trait A
    illTyped("implicitly[Atom[A]]")
    val _ = {
      implicit val default: Atom.DefaultAtom[A] = Atom.DefaultAtom[A](new A{})
      "implicitly[Atom[A]]" should compile
    }
  }



}
