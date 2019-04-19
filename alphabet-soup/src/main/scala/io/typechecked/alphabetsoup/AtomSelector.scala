package io.typechecked
package alphabetsoup

import shapeless.{::, HList}

// TODO rename, now also handles molecules
trait AtomSelector[L, U] {
  def apply(l: L): U
}

object AtomSelector {

  def apply[L, U](implicit atomSelector: AtomSelector[L, U]): AtomSelector[L, U] = atomSelector

  implicit def atomiseThenDelegate[L, LOut, U](
    implicit atomiser: Atomiser.Aux[L, LOut],
    s: SelectOrDefault[LOut, U]
  ): AtomSelector[L, U] = new AtomSelector[L, U] {
    def apply(t: L): U = s(atomiser.to(t))
  }

}
