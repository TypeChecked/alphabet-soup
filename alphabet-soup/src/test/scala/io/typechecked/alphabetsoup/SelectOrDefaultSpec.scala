package io.typechecked
package alphabetsoup

import org.scalatest._
import shapeless.{::, HNil}
import shapeless.test.illTyped

import Atom.DefaultAtom

// TODO: nested structure tests
class SelectOrDefaultSpec extends FlatSpec with Matchers {

  "SelectOrDefault" should "select a value from an atomised structure even with a default atom present" in {
    type L = Int :: String :: HNil
    type U = String

    val l = 17 :: "twine" :: HNil

    implicit val defaultU: DefaultAtom[U] = DefaultAtom[U]("default")

    SelectOrDefault[L, U].apply(l) shouldBe "twine"
  }

  it should "replace a value in an atomised structure even with a default atom present" in {
    type L = Int :: String :: HNil
    type U = String

    val l = 17 :: "twine" :: HNil

    implicit val defaultU: DefaultAtom[U] = DefaultAtom[U]("default")

    SelectOrDefault[L, U].replace("yarn", l) shouldBe 17 :: "yarn" :: HNil
  }

  it should "select a default from an atomised structure, if there is a default present but the type is not in the structure" in {
    type L = Int :: String :: HNil
    type U = Boolean

    val l = 17 :: "twine" :: HNil

    implicit val defaultU: DefaultAtom[U] = DefaultAtom[U](false)

    SelectOrDefault[L, U].apply(l) shouldBe false
  }

  it should "replace nothing if there is a default present for a type not in the original structure" in {
    type L = Int :: String :: HNil
    type U = Boolean

    val l = 17 :: "twine" :: HNil

    implicit val defaultU: DefaultAtom[U] = DefaultAtom[U](false)

    SelectOrDefault[L, U].replace(true, l) shouldBe l
  }

  it should "not compile if the type we want is not present and there is no default atom" in {
    type L = Int :: String :: HNil
    type U = Boolean

    illTyped("SelectOrDefault[L, U]", ".*could not find implicit value.*SelectOrDefault.*")
  }

}
