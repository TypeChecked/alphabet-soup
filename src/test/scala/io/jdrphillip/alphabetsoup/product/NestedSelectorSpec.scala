package io.jdrphillips
package alphabetsoup
package product

import org.scalatest.FlatSpec
import org.scalatest.Matchers
import shapeless.::
import shapeless.HNil
import shapeless.test.illTyped

class NestedSelectorSpec extends FlatSpec with Matchers {

  case class Pair(i: Int, s: String)
  case class T(p: Pair, b: Boolean)

  "NestedSelector" should "not work on atoms" in {
    illTyped("NestedSelector[Int, Int]")
  }

  it should "work on simple hlists" in {
    val list = 5 :: "hello" :: HNil
    NestedSelector[Int :: String :: HNil, Int].apply(list) shouldBe 5
    NestedSelector[Int :: String :: HNil, String].apply(list) shouldBe "hello"
  }

  it should "work on simple tuples" in {
    val tuple = (5, "hello")
    NestedSelector[(Int, String), Int].apply(tuple) shouldBe 5
    NestedSelector[(Int, String), String].apply(tuple) shouldBe "hello"
  }

  it should "work on case classes" in {
    val t = T(Pair(5, "hello"), true)
    NestedSelector[T, Int].apply(t) shouldBe 5
    NestedSelector[T, String].apply(t) shouldBe "hello"
    NestedSelector[T, Boolean].apply(t) shouldBe true
  }

  it should "work on case classes with an atom specified" in {
    implicit val pairAtom: Atom[Pair] = Atom[Pair]
    val t = T(Pair(5, "hello"), true)
    illTyped("NestedSelector[T, Int]")
    illTyped("NestedSelector[T, String]")
    NestedSelector[T, Boolean].apply(t) shouldBe true
    NestedSelector[T, Pair].apply(t) shouldBe Pair(5, "hello")
  }

}
