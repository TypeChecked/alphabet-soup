package io.typechecked
package alphabetsoup

import shapeless.Lazy
import shapeless.{::, HList}

/**
 * A trait that extracts a value U (atom or molecule) from a type L, provided the type L
 * has been atomised first.
 *
 * If no such value exists, it does not compile
 */
trait SelectFromAtomised[L, U] {
  def apply(l: L): U
}

// TODO: Atom/Molecule paths could be unified or split more sensibly
object SelectFromAtomised {

  def apply[L, U](implicit as: SelectFromAtomised[L, U]): SelectFromAtomised[L, U] = as

  implicit def headAtomSelect[H: Atom, T <: HList]: SelectFromAtomised[H :: T, H] =
    new SelectFromAtomised[H :: T, H] {
      def apply(l: H :: T): H = l.head
    }

  implicit def recurse[H, T <: HList, U: Atom](implicit st: SelectFromAtomised[T, U]): SelectFromAtomised[H :: T, U] =
    new SelectFromAtomised[H :: T, U] {
      def apply(l: H :: T) = st(l.tail)
    }

  implicit def recurseNested[H <: HList, T <: HList, U: Atom](
    implicit st: Lazy[SelectFromAtomised[H, U]]
  ): SelectFromAtomised[H :: T, U] =
    new SelectFromAtomised[H :: T, U] {
      def apply(l: H :: T) = st.value(l.head)
    }

  implicit def fuzzyHeadSelectMolecule[M[_], A, T <: HList, B](
    implicit molecule: Molecule[M, B],
    mixer: Lazy[Mixer[A, B]]
  ): SelectFromAtomised[M[A] :: T, M[B]] =
    new SelectFromAtomised[M[A] :: T, M[B]] {
      def apply(t: M[A] :: T): M[B] = molecule.functor.map(t.head)(mixer.value.mix)
    }

  implicit def recurseTailMolecule[H, T <: HList, M[_], U](
    implicit molecule: Molecule[M, U],
    st: SelectFromAtomised[T, M[U]]
  ): SelectFromAtomised[H :: T, M[U]] =
    new SelectFromAtomised[H :: T, M[U]] {
      def apply(l: H :: T) = st(l.tail)
    }

  implicit def recurseHeadMolecule[H <: HList, T <: HList, U, M[_]](
    implicit molecule: Molecule[M, U],
    st: Lazy[SelectFromAtomised[H, M[U]]]
  ): SelectFromAtomised[H :: T, M[U]] =
    new SelectFromAtomised[H :: T, M[U]] {
      def apply(l: H :: T) = st.value(l.head)
    }

}
