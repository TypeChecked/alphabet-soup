package io.typechecked.alphabetsoup

import shapeless.{::, HList}

trait SelectAndTransmute[L, U] {
  def apply(l: L): U
}

object SelectAndTransmute extends LowPrioritySelectAndTransmute {

  implicit def tryHeadAtom[L <: HList, T: Atom, U](implicit transmute: Transmute[T, U]): SelectAndTransmute[T :: L, U] =
    new SelectAndTransmute[T :: L, U] {
      def apply(l: T :: L): U = transmute.convert(l.head)
    }

  implicit def tryHeadMolecule[L <: HList, M[_], T, U](
    implicit molecule: Molecule[M, T],
    transmute: Transmute[M[T], U]
  ): SelectAndTransmute[M[T] :: L, U] =
    new SelectAndTransmute[M[T] :: L, U] {
      override def apply(l: M[T] :: L): U = transmute.convert(l.head)
    }

  implicit def recurseNested[L <: HList, T <: HList, U](implicit transmutation: SelectAndTransmute[T, U]): SelectAndTransmute[T :: L, U] =
    new SelectAndTransmute[T :: L, U] {
      def apply(l: T :: L): U = transmutation(l.head)
    }
}

trait LowPrioritySelectAndTransmute {

  implicit def recurseTail[L <: HList, T, U](implicit transmutation: SelectAndTransmute[L, U]): SelectAndTransmute[T :: L, U] =
    new SelectAndTransmute[T :: L, U] {
      def apply(l: T :: L): U = transmutation(l.tail)
    }

}
