package io.jdrphillips
package alphabetsoup

import shapeless.<:!<
import shapeless.{::, DepFn1, HList}

trait AtomSelector[L, U] extends DepFn1[L] with Serializable {
  type Out = U
}

object AtomSelector extends LowPriorityAtomSelectorImplicits {

  def apply[L, U](implicit atomSelector: AtomSelector[L, U]): AtomSelector[L, U] = atomSelector

  implicit def headSelect[H: Atom, T <: HList]: AtomSelector[H :: T, H] =
    new AtomSelector[H :: T, H] {
      def apply(l: H :: T): H = l.head
    }

  implicit def recurse[H, T <: HList, U: Atom](implicit st: AtomSelector[T, U]): AtomSelector[H :: T, U] =
    new AtomSelector[H :: T, U] {
      def apply(l: H :: T) = st(l.tail)
    }

  implicit def recurseNested[H <: HList, T <: HList, U: Atom](implicit st: AtomSelector[H, U]): AtomSelector[H :: T, U] =
    new AtomSelector[H :: T, U] {
      def apply(l: H :: T) = st(l.head)
    }
}


trait LowPriorityAtomSelectorImplicits {

  implicit def deepGenericFirst[L, LOut <: HList, U: Atom](
    implicit
    ev: L <:!< HList,
    dg: Atomiser.Aux[L, LOut],
    s: AtomSelector[LOut, U]
  ): AtomSelector[L, U] = new AtomSelector[L, U] {
    def apply(t: L): U = s(dg.to(t))
  }

}
