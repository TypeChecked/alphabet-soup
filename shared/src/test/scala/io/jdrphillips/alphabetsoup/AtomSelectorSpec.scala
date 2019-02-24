package io.jdrphillips
package alphabetsoup

import org.scalatest.FlatSpec
import org.scalatest.Matchers
import shapeless.::
import shapeless.HNil
import shapeless.test.illTyped

class AtomSelectorSpec extends FlatSpec with Matchers {

  case class Pair(i: Int, s: String)
  case class T(p: Pair, b: Boolean)

  "AtomSelector" should "not work on atoms" in {
    illTyped("AtomSelector[Int, Int]")
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

}
