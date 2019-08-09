package io.typechecked
package alphabetsoup

import org.scalatest._
import shapeless.{::, HNil}
import shapeless.test.illTyped

// TODO: nested structure tests
class SelectFromAtomisedSpec extends FlatSpec with Matchers {

  "SelectFromAtomised" should "select an atomic value from an atomised structure" in {
    type L = Int :: String :: HNil
    type U = String

    val l = 17 :: "twine" :: HNil

    SelectFromAtomised[L, U].apply(l) shouldBe "twine"
  }

  it should "select a molecule from an atomised structure" in {
    type L = Int :: List[String] :: HNil
    type U = List[String]

    val l = 17 :: List("twine", "yarn") :: HNil

    SelectFromAtomised[L, U].apply(l) shouldBe List("twine", "yarn")
  }

  it should "fuzzily select a molecule from an atomised structure" in {
    case class A(str: String)
    case class B(str: String)

    type L = Int :: List[A] :: HNil
    type U = List[B]

    val l = 17 :: List(A("twine"), A("yarn")) :: HNil

    SelectFromAtomised[L, U].apply(l) shouldBe List(B("twine"), B("yarn"))
  }

  it should "replace an atomic value from an atomised structure" in {
    type L = Int :: String :: HNil
    type U = String

    val l = 17 :: "twine" :: HNil

    SelectFromAtomised[L, U].replace("thread", l) shouldBe 17 :: "thread" :: HNil
  }

  it should "replace a molecule from an atomised structure" in {
    type L = Int :: List[String] :: HNil
    type U = List[String]

    val l = 17 :: List("twine", "yarn") :: HNil

    SelectFromAtomised[L, U].replace(List("thread"), l) shouldBe 17 :: List("thread") :: HNil
  }

  it should "NOT fuzzily replace a molecule from an atomised structure if the internal types cannot be mixed to one another" in {

    // Fuzziness does NOT work with replacing molecules

    // A and B are not isomorphic; we have Mixer[A, B] but not Mixer[B, A]
    case class A(str: String, int: Int)
    case class B(str: String)

    type L = Int :: List[A] :: HNil
    type U = List[B]

    val l = 17 :: List(A("twine", 0), A("yarn", 2)) :: HNil

    SelectFromAtomised[L, U].replace(List(B("thread")), l) shouldBe l
  }

  it should "fuzzily replace a molecule from an atomised structure if the internal types are isomorphic" in {

    // Fuzziness DOES work with replacing molecules if the internal types are isomorphic

    case class A(str: String)
    case class B(str: String)

    type L = Int :: List[A] :: HNil
    type U = List[B]

    val l = 17 :: List(A("twine"), A("yarn")) :: HNil

    SelectFromAtomised[L, U].replace(List(B("thread")), l) shouldBe 17 :: List(A("thread")) :: HNil
  }

  it should "not compile for an atomic value from a non-atomised structure" in {
    case class L(int: Int, str: String)
    type U = String

    illTyped("SelectFromAtomised[L, U]", ".*could not find implicit value.*SelectFromAtomised.*")
  }

  it should "not compile for a molecule from a non-atomised structure" in {
    case class L(int: Int, str: List[String])
    type U = List[String]

    illTyped("SelectFromAtomised[L, U]", ".*could not find implicit value.*SelectFromAtomised.*")
  }

  it should "not compile for fuzzily selecting a molecule from a non-atomised structure" in {

    case class A(str: String)
    case class B(str: String)

    case class L(int: Int, str: List[A])
    type U = List[B]

    illTyped("SelectFromAtomised[L, U]", ".*could not find implicit value.*SelectFromAtomised.*")
  }

  it should "not compile if the type is not present" in {
    type L = Int :: String :: HNil
    type U = Boolean

    illTyped("SelectFromAtomised[L, U]", ".*could not find implicit value.*SelectFromAtomised.*")
  }

  it should "not compile if the type is not present even if there is a default present" in {
    type L = Int :: String :: HNil
    type U = Boolean

    implicit val default: Atom.DefaultAtom[U] = Atom.DefaultAtom[U](false)

    illTyped("SelectFromAtomised[L, U]", ".*could not find implicit value.*SelectFromAtomised.*")
  }
}
