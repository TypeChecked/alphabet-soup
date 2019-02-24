package io.jdrphillips
package alphabetsoup
package product

import org.scalatest._
import shapeless.{::, HNil}

trait Evidence[T]
object Evidence {
  implicit val intEvidence: Evidence[Int] = new Evidence[Int] {}
  implicit val hnilEvidence: Evidence[HNil] = new Evidence[HNil] {}
}

class DeepGenericSpec extends FlatSpec with Matchers {

  "DeepGeneric" should "work on Unit" in {
    val g = DeepGeneric[Unit]
    (g.to(()): Unit) shouldBe (HNil)
    (g.from(HNil): Unit) shouldBe (())
  }

  it should "work on HNil" in {
    val g = DeepGeneric[HNil]
    (g.to(HNil): HNil) shouldBe HNil
    (g.from(HNil): HNil) shouldBe HNil
  }

  it should "work on simple hlists" in {
    type H = Int :: String :: Boolean :: HNil
    val hlist: H = 5 :: "hello" :: true :: HNil

    DeepGeneric[Int :: HNil]
    val gen = DeepGeneric[H]
    (gen.to(hlist): H) shouldBe hlist
    (gen.from(hlist): H) shouldBe hlist
  }

  it should "work on complex hlists" in {
    type H = Int :: (String :: Boolean :: HNil) :: (Float :: Double :: HNil) :: Boolean :: HNil
    val hlist: H = 5 :: ("hello" :: true :: HNil) :: (3.4f :: 3.9 :: HNil) :: true :: HNil
    val gen = DeepGeneric[H]
    (gen.to(hlist): H) shouldBe hlist
    (gen.from(hlist): H) shouldBe hlist
  }

  it should "work with small tuples" in {
    type T = (String, Boolean)
    type H = String :: Boolean :: HNil
    val gen = DeepGeneric[T]
    val tuple: T = ("hello", true)
    val hlist: H = "hello" :: true :: HNil

    (gen.to(tuple): H) shouldBe hlist
    (gen.from(hlist): T) shouldBe tuple
  }

  it should "work with a larger flat tuple" in {
    type T = (Int, String, Boolean)
    type H = Int :: String :: Boolean :: HNil
    val gen = DeepGeneric[T]
    val tuple: T = (5, "hello", true)
    val hlist: H = 5 :: "hello" :: true :: HNil

    (gen.to(tuple): H) shouldBe hlist
    (gen.from(hlist): T) shouldBe tuple
  }

  it should "work on flat case classes" in {
    case class C(i: Int, s: String, b: Boolean)
    type H = Int :: String :: Boolean :: HNil
    val gen = DeepGeneric[C]
    val value: C = C(5, "hello", true)
    val hlist: H = 5 :: "hello" :: true :: HNil
    (gen.to(value): H) shouldBe hlist
    (gen.from(hlist): C) shouldBe value
  }

  it should "work for simply nested tuples recursively" in {
    type T = ((Int, Float), String, Boolean)
    type H = (Int :: Float :: HNil) :: String :: Boolean :: HNil
    val gen = DeepGeneric[T]
    val tuple: T = ((5, 4.5f), "hello", true)
    val hlist: H = (5 :: 4.5f :: HNil) :: "hello" :: true :: HNil

    (gen.to(tuple): H) shouldBe hlist
    (gen.from(hlist): T) shouldBe tuple
  }

  it should "work for simply nested case classes recursively" in {
    case class N(i: Int, f: Char)
    case class T(n: N, s: String, b: Boolean)

    type H = (Int :: Char :: HNil) :: String :: Boolean :: HNil

    val gen = DeepGeneric[T]
    val value: T = T(N(5, 'g'), "hello", true)
    val hlist: H = (5 :: 'g' :: HNil) :: "hello" :: true :: HNil

    (gen.to(value): H) shouldBe hlist
    (gen.from(hlist): T) shouldBe value
  }

  it should "work for complex nested tuples recursively" in {
    type T = ((Int, Float), (String, (Boolean, Double)))
    type H = (Int :: Float :: HNil) :: (String :: (Boolean :: Double :: HNil) :: HNil) :: HNil
    val gen = DeepGeneric[T]
    val tuple: T = ((5, 4.5f), ("hello", (true, 10.0)))
    val hlist: H = (5 :: 4.5f :: HNil) :: ("hello" :: (true :: 10.0 :: HNil) :: HNil) :: HNil

    (gen.to(tuple): H) shouldBe hlist
    (gen.from(hlist): T) shouldBe tuple
  }

  it should "work for complex nested case classes recursively" in {
    case class N(i: Int, f: Float)
    case class M(d: Double, n: N)
    case class P(b: Boolean, l: Long)
    case class T(n: N, p: P, m: M, s: String)

    type H = (Int :: Float :: HNil) :: (Boolean :: Long :: HNil) :: (Double :: (Int :: Float :: HNil) :: HNil) :: String :: HNil

    val gen = DeepGeneric[T]
    val value: T = T(N(1, 1.4f), P(true, 3l), M(2.3, N(10, 10.4f)), "hello")
    val hlist: H = (1 :: 1.4f :: HNil) :: (true :: 3l :: HNil) :: (2.3 :: (10 :: 10.4f :: HNil) :: HNil) :: "hello" :: HNil

    (gen.to(value): H) shouldBe hlist
    (gen.from(hlist): T) shouldBe value
  }


  it should "work for an hlist inside a tuple inside a cas class" in {
    case class T(t: (Double, String :: HNil))

    type H = (Double :: (String :: HNil) :: HNil) :: HNil

    val gen = DeepGeneric[T]
    val value: T = T((5.0, "hello" :: HNil))
    val hlist: H = (5.0 :: ("hello" :: HNil) :: HNil) :: HNil

    (gen.to(value): H) shouldBe hlist
    (gen.from(hlist): T) shouldBe value
  }

  it should "work for a simple case class at the head of an HList" in {
    case class N(t: String)

    type T = N :: Int :: HNil
    type H = (String :: HNil) :: Int :: HNil

    val gen = DeepGeneric[T]
    val value: T = N("hello") :: 11 :: HNil
    val hlist: H = ("hello" :: HNil) :: 11 :: HNil

    (gen.to(value): H) shouldBe hlist
    (gen.from(hlist): T) shouldBe value
  }

  it should "work for a simple case class at the head of a tuple" in {
    case class N(t: String)

    type T = (N, Int)
    type H = (String :: HNil) :: Int :: HNil

    val gen = DeepGeneric[T]
    val value: T = N("hello") -> 11
    val hlist: H = ("hello" :: HNil) :: 11 :: HNil

    (gen.to(value): H) shouldBe hlist
    (gen.from(hlist): T) shouldBe value
  }

  it should "work for a tuple at the head of an hlist at the head of an hlist" in {
    type T = ((String, Boolean):: HNil) :: Int :: HNil
    type H = ((String :: Boolean :: HNil) :: HNil) :: Int :: HNil

    val gen = DeepGeneric[T]
    val value: T = (("hello" -> true) :: HNil) :: 11 :: HNil
    val hlist: H = (("hello" :: true :: HNil) :: HNil) :: 11 :: HNil

    (gen.to(value): H) shouldBe hlist
    (gen.from(hlist): T) shouldBe value
  }

  it should "work for a case class containing only a tuple" in {

    case class N(t: (String, Boolean))
    type H = (String :: Boolean :: HNil) :: HNil

    val gen = DeepGeneric[N]
    val value = N("hello" -> true)
    val hlist = ("hello" :: true :: HNil) :: HNil

    (gen.to(value): H) shouldBe hlist
    (gen.from(hlist): N) shouldBe value
  }

  it should "work for a tuple1 in a case class at the head of a tuple" in {
    case class N(t: Tuple1[String])

    type T = (N, Int)
    type H = ((String :: HNil) :: HNil) :: Int :: HNil

    val gen = DeepGeneric[T]
    val value: T = N(Tuple1("hello")) -> 11
    val hlist: H = (("hello" :: HNil) :: HNil) :: 11 :: HNil

    (gen.to(value): H) shouldBe hlist
    (gen.from(hlist): T) shouldBe value
  }

  it should "work for an hlist inside a tuple inside a cas class inside a tuple" in {
    case class N(t: (Double, String :: HNil))

    type T = (N, Int)
    type H = ((Double :: (String :: HNil) :: HNil) :: HNil) :: Int :: HNil

    val gen = DeepGeneric[T]
    val value: T = (N((5.0, "hello" :: HNil)), 11)
    val hlist: H = ((5.0 :: ("hello" :: HNil) :: HNil) :: HNil) :: 11 :: HNil

    (gen.to(value): H) shouldBe hlist
    (gen.from(hlist): T) shouldBe value
  }

  it should "work for an HList inside a tuple inside a case class inside a tuple inside an HList" in {
    case class N(t: (Double, String :: HNil))

    type T = Boolean :: Long :: (N, Int) :: HNil
    type H = Boolean :: Long :: (((Double :: (String :: HNil) :: HNil) :: HNil) :: Int :: HNil) :: HNil

    val gen = DeepGeneric[T]
    val value: T = true :: 10l :: (N((5.0, "hello" :: HNil)), 11) :: HNil
    val hlist: H = true :: 10l :: (((5.0 :: ("hello" :: HNil) :: HNil) :: HNil) :: 11 :: HNil) :: HNil

    (gen.to(value): H) shouldBe hlist
    (gen.from(hlist): T) shouldBe value
  }

  it should "work for a complex mix of nested hlists, case classes and tuples" in {

    case class N(t: (Double, String :: HNil))
    case class C(
      t: (Int, String),
      d: Double,
      h: Boolean :: Long :: (N, Int) :: HNil
    )

    type H =
      (Int :: String :: HNil) ::
      Double ::
      (Boolean :: Long :: (((Double :: (String :: HNil) :: HNil) :: HNil) :: Int :: HNil) :: HNil) ::
      HNil

    val gen = DeepGeneric[C]
    val value = C((5, "hello"), 10.9, true :: 9l :: (N((10.12, "hello2" :: HNil)), 4) :: HNil)
    // This commented out behaviour is correct
    val hlist = (5 :: "hello" :: HNil) :: 10.9 :: (true :: 9l :: (((10.12 :: ("hello2" :: HNil) :: HNil) :: HNil) :: 4 :: HNil) :: HNil) :: HNil

    (gen.to(value): H) shouldBe hlist
    (gen.from(hlist): C) shouldBe value

  }

}
