package io.jdrphillips.alphabetsoup.product

import shapeless.{DepFn1, HList, ::}

trait NestedSelector[L <: HList, U] extends DepFn1[L] with Serializable {
  type Out = U
}

object NestedSelector {

  def apply[L <: HList, U](implicit nestedSelector: NestedSelector[L, U]): NestedSelector[L, U] = nestedSelector

  implicit def headSelect[H, T <: HList]: NestedSelector[H :: T, H] =
    new NestedSelector[H :: T, H] {
      def apply(l: H :: T): H = l.head
    }

  implicit def recurse[H, T <: HList, U]
  (implicit st: NestedSelector[T, U]): NestedSelector[H :: T, U] =
    new NestedSelector[H :: T, U] {
      def apply(l: H :: T) = st(l.tail)
    }

  implicit def recurseNested[H <: HList, T <: HList, U]
  (implicit st: NestedSelector[H, U]): NestedSelector[H :: T, U] =
    new NestedSelector[H :: T, U] {
      def apply(l: H :: T) = st(l.head)
    }
}
