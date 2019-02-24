package io.jdrphillips.alphabetsoup.product

import shapeless.<:!<
import shapeless.HNil
import shapeless.test.illTyped
import shapeless.{::, DepFn1, HList}

trait NestedSelector[L, U] extends DepFn1[L] with Serializable {
  type Out = U
}

object NestedSelector extends LP {

  def apply[L, U](implicit nestedSelector: NestedSelector[L, U]): NestedSelector[L, U] = nestedSelector

  implicit def headSelect[H: Atom, T <: HList]: NestedSelector[H :: T, H] =
    new NestedSelector[H :: T, H] {
      def apply(l: H :: T): H = l.head
    }

  implicit def recurse[H, T <: HList, U: Atom](implicit st: NestedSelector[T, U]): NestedSelector[H :: T, U] =
    new NestedSelector[H :: T, U] {
      def apply(l: H :: T) = st(l.tail)
    }

  implicit def recurseNested[H <: HList, T <: HList, U: Atom](implicit st: NestedSelector[H, U]): NestedSelector[H :: T, U] =
    new NestedSelector[H :: T, U] {
      def apply(l: H :: T) = st(l.head)
    }
}


trait LP {

  implicit def deepGenericFirst[L, LOut <: HList, U: Atom](
    implicit
    ev: L <:!< HList,
    dg: DeepGeneric.Aux[L, LOut],
    s: NestedSelector[LOut, U]
  ): NestedSelector[L, U] = new NestedSelector[L, U] {
    def apply(t: L): U = s(dg.to(t))
  }

}
