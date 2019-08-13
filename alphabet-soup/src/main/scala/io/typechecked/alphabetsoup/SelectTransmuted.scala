package io.typechecked.alphabetsoup

import shapeless.{::, HList}

trait SelectTransmuted[L, U] {
  def apply(l: L): U
}

object SelectTransmuted extends LowPrioritySelectAndTransmute {

  implicit def tryHeadAtom[L <: HList, T: Atom, U](implicit transmute: Transmute[T, U]): SelectTransmuted[T :: L, U] =
    new SelectTransmuted[T :: L, U] {
      def apply(l: T :: L): U = transmute.convert(l.head)
    }

  implicit def tryHeadMolecule[L <: HList, M[_], T, U](
    implicit molecule: Molecule[M, T],
    transmute: Transmute[M[T], U]
  ): SelectTransmuted[M[T] :: L, U] =
    new SelectTransmuted[M[T] :: L, U] {
      override def apply(l: M[T] :: L): U = transmute.convert(l.head)
    }

  implicit def recurseNested[L <: HList, T <: HList, U](implicit transmutation: SelectTransmuted[T, U]): SelectTransmuted[T :: L, U] =
    new SelectTransmuted[T :: L, U] {
      def apply(l: T :: L): U = transmutation(l.head)
    }
}

trait LowPrioritySelectAndTransmute {

  implicit def recurseTail[L <: HList, T, U](implicit transmutation: SelectTransmuted[L, U]): SelectTransmuted[T :: L, U] =
    new SelectTransmuted[T :: L, U] {
      def apply(l: T :: L): U = transmutation(l.tail)
    }

}
