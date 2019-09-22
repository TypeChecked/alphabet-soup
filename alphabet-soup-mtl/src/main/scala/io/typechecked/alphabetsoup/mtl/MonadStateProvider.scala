package io.typechecked
package alphabetsoup
package mtl

import cats.Monad
import cats.mtl.MonadState
import shapeless.=:!=

trait MonadStateProvider {

  implicit def projectMonadState[M[_], X, Y](
    implicit ms: MonadState[M, X],
    ev: X =:!= Y,
    m: Mixer[X, Y]
  ): MonadState[M, Y] = new MonadState[M, Y] {

    val monad: Monad[M] = ms.monad

    def get: M[Y] = monad.map(ms.get)(m.mix)
    def set(y: Y): M[Unit] = ms.modify(x => m.inject(y, x))
    def inspect[T](f: Y => T): M[T] = ms.inspect(x => f(m.mix(x)))
    def modify(f: Y => Y): M[Unit] = ms.modify(x => m.modify(f)(x))

  }

}

object MonadStateProvider extends MonadStateProvider
