package io.typechecked
package alphabetsoup
package mtl

import cats.Applicative
import cats.Id
import cats.mtl.ApplicativeAsk
import org.scalatest._
import shapeless.{::, HNil}

class ApplicativeAskProviderSpec extends FlatSpec with Matchers {

  "ApplicativeAskProvider" should "give access to an AA for any sub-grouping of existing AA" in {
    implicit val aa: ApplicativeAsk[Id, (String, Boolean, Int)] =
      ApplicativeAsk.const[Id, (String, Boolean, Int)](("hello", true, 5))

    // implicits pulled in from package object
    implicitly[ApplicativeAsk[Id, (String, Boolean, Int)]].ask shouldBe (("hello", true, 5))
    implicitly[ApplicativeAsk[Id, (String, Int)]].ask shouldBe (("hello", 5))
    implicitly[ApplicativeAsk[Id, (Boolean, Int)]].ask shouldBe ((true, 5))
    implicitly[ApplicativeAsk[Id, (Int, Boolean, String)]].ask shouldBe ((5, true, "hello"))
    implicitly[ApplicativeAsk[Id, Int]].ask shouldBe (5)
    implicitly[ApplicativeAsk[Id, Tuple1[Boolean]]].ask shouldBe (Tuple1(true))

  }

}
