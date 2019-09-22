package io.typechecked
package alphabetsoup
package mtl

import cats.Applicative
import cats.mtl.ApplicativeAsk
import shapeless.=:!=

trait ApplicativeAskProvider {

  implicit def projectApplicativeAsk[M[_], X, Y](
    implicit ax: ApplicativeAsk[M, X],
    ev: X =:!= Y,
    m: Mixer[X, Y]
  ): ApplicativeAsk[M, Y] = new ApplicativeAsk[M, Y] {
    val applicative: Applicative[M] = ax.applicative
    def ask: M[Y] = applicative.map(ax.ask)(m.mix)
    def reader[Z](f: Y => Z): M[Z] = ax.reader[Z](a => f(m.mix(a)))
  }

}

object ApplicativeAskProvider extends ApplicativeAskProvider
