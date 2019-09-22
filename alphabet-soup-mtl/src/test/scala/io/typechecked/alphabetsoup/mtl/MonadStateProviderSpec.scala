package io.typechecked
package alphabetsoup
package mtl

import cats.Id
import cats.Monad
import cats.mtl.MonadState
import org.scalatest._
import shapeless.{::, HNil}

class MonadStateProviderSpec extends FlatSpec with Matchers {

  type F[X] = Id[X]
  type S = (Int, (String, Boolean), Char)

  implicit val ms = new MonadState[F, S] {

    var value: S = (19, ("hello", true), 'f')

    val monad: Monad[F] = implicitly[Monad[Id]]
    def get: F[S] = value
    def set(s: S): F[Unit] = value = s
    def inspect[A](f: S => A): F[A] = f(value)
    def modify(f: S => S): F[Unit] = value = f(value)

  }

  "MonadStateProvider" should "be provided for sub groupings of constituent types" in {

    // implicits pulled in from package object
    implicitly[MonadState[Id, (String, Boolean, Int)]]
    implicitly[MonadState[Id, (Char, Int)]]

  }

  it should "get, set, inspect and modify correctly" in {

    val ms2 = implicitly[MonadState[Id, (Char, Int)]]

    ms2.set('p' -> 999)
    ms.get shouldBe ((999, "hello" -> true, 'p'))

    ms2.inspect(_._1) shouldBe 'p'
    ms.get shouldBe ((999, "hello" -> true, 'p'))

    ms2.modify { case (_, int) => ('M', int / 111) }
    ms.get shouldBe ((9, "hello" -> true, 'M'))

  }

}
