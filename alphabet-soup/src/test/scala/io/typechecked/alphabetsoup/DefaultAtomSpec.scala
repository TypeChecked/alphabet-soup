package io.typechecked
package alphabetsoup

import org.scalatest.FlatSpec
import org.scalatest.Matchers
import shapeless.::
import shapeless.HNil
import shapeless.test.illTyped

class DefaultAtomSpec extends FlatSpec with Matchers {

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

  it should "be able to create default atom if a molecule exists" in {
    "implicitly[Molecule[List, String]]" should compile
    "Atom.DefaultAtom[List[String]] = Atom.DefaultAtom(List.empty[String])" should compile
  }

  "Mixer" should "work with a supplied default" in {
    case class Source(a: Int)
    case class Target(a: Int, b: String)

    implicit val default: Atom.DefaultAtom[String] = Atom.DefaultAtom("default")

    val mixer = Mixer[Source, Target]

    val source = Source(1)

    mixer.mix(source) shouldBe Target(1, "default")

  }
  it should "return types from left ignoring default" in {

    case class Source(a: String)
    case class Target(a: String)

    implicit val default: Atom.DefaultAtom[String] = Atom.DefaultAtom("default")

    val mixer = Mixer[Source, Target]

    mixer.mix(Source("source")) shouldBe Target("source")
  }
  it should "work with multiple supplied defaults" in {

    case class Source(a: Int)
    case class Target(a: Int, b: String, c: Boolean, d: Long)

    implicit val defaultString: Atom.DefaultAtom[String] = Atom.DefaultAtom("default")
    implicit val defaultBoolean: Atom.DefaultAtom[Boolean] = Atom.DefaultAtom(true)
    implicit val defaultLong: Atom.DefaultAtom[Long] = Atom.DefaultAtom(10L)

    val mixer = Mixer[Source, Target]

    val source = Source(1)

    mixer.mix(source) shouldBe Target(1, "default", true, 10L)

  }
  it should "not work with molecules containing no internal structure" in {
    case class Source(a: Int)
    case class Target(a: Int, b: List[String], c: Unit)

    implicit val default: Atom.DefaultAtom[String] = Atom.DefaultAtom("default")

    illTyped("Mixer[Source, Target]")

  }

  it should "work with molecules" in {
    case class Source(a: Int, b: List[Tuple1[Long]])
    case class Target(a: Int, b: List[(Long,String)])

    implicit val default: Atom.DefaultAtom[String] = Atom.DefaultAtom("default")

    val mixer = Mixer[Source, Target]

    mixer.mix(Source(1, List(Tuple1(10L)))) shouldBe Target(1, List((10L,"default")))
  }

  it should "ignore default when atom exists" in {
    case class Source(a: String)
    case class Target(a: String)

    implicit val default: Atom.DefaultAtom[String] = Atom.DefaultAtom("default")

    val mixer = Mixer[Source, Target]

    mixer.mix(Source("Choose me!")) shouldBe Target("Choose me!")
  }

  it should "work when it finds a HNil before traversing entire structure" in {

    case class Source(a : Int)

    implicit val default: Atom.DefaultAtom[String] = Atom.DefaultAtom("default")

    AtomSelector[(Int :: HNil) :: (String :: HNil) :: HNil, String].apply((1 :: HNil) :: ("Me!" :: HNil) :: HNil) shouldBe "Me!"

  }

  it should "always work with HNil in this particular position" in {

    implicit val default: Atom.DefaultAtom[String] = Atom.DefaultAtom("default")

    AtomSelector[Int :: (String :: HNil) :: HNil, String].apply(1 :: ("Me!" :: HNil) :: HNil) shouldBe "Me!"

  }

  "MixerBuilder" should "take precedence over supplied defaults" in {
    case class Source(a: Int)
    case class Target(a: Int, b: String)

    implicit val default: Atom.DefaultAtom[String] = Atom.DefaultAtom("Don't use me!!!")

    val mixer = Mixer.from[Source].to[Target].withDefault("I AM THE REAL DEFAULT").build

    mixer.mix(Source(1)) shouldBe Target(1, "I AM THE REAL DEFAULT")


  }

  it should "work in conjunction with DefaultAtom" in {
    case class Source(a: Int)
    case class Target(a: Int, b: String, c: Long)

    implicit val default: Atom.DefaultAtom[String] = Atom.DefaultAtom("Default Brah")

    val mixer = Mixer.from[Source].to[Target].withDefault(69L).build

    mixer.mix(Source(1)) shouldBe Target(1, "Default Brah", 69L)
  }

}