package io.typechecked
package alphabetsoup

import shapeless.Lazy
import shapeless.{::, HList}

// TODO rename, now also handles molecules
trait AtomSelector[L, U] {
  def apply(l: L): U
}

object AtomSelector {

  def apply[L, U](implicit atomSelector: AtomSelector[L, U]): AtomSelector[L, U] = atomSelector

  implicit def atomiseThenDelegate[L, LOut, U](
    implicit dg: Atomiser.Aux[L, LOut],
    s: AtomiseOrDefaultSelector[LOut, U]
  ): AtomSelector[L, U] = new AtomSelector[L, U] {
    def apply(t: L): U = s(dg.to(t))
  }

  trait AtomiseOrDefaultSelector[L, U] {
    def apply(l: L): U
  }

  object AtomiseOrDefaultSelector extends LowPriorityAtomiseOrDefaultSelector{

    implicit def fromAtomSelectorFromAtomised[L, U](implicit atomise: AtomSelectorFromAtomised[L, U]): AtomiseOrDefaultSelector[L, U] =
      new AtomiseOrDefaultSelector[L, U] {
        def apply(l: L): U = atomise.apply(l)
    }

    // Worker trait
    trait AtomSelectorFromAtomised[L, U] {
      def apply(l: L): U
    }

    object AtomSelectorFromAtomised {

      def apply[L, U](implicit as: AtomSelectorFromAtomised[L, U]): AtomSelectorFromAtomised[L, U] = as

      implicit def headSelect[H: Atom, T <: HList]: AtomSelectorFromAtomised[H :: T, H] =
        new AtomSelectorFromAtomised[H :: T, H] {
          def apply(l: H :: T): H = l.head
        }

      implicit def recurse[H, T <: HList, U: Atom](implicit st: AtomSelectorFromAtomised[T, U]): AtomSelectorFromAtomised[H :: T, U] =
        new AtomSelectorFromAtomised[H :: T, U] {
          def apply(l: H :: T) = st(l.tail)
        }

      implicit def recurseNested[H <: HList, T <: HList, U: Atom](
        implicit st: Lazy[AtomSelectorFromAtomised[H, U]]
      ): AtomSelectorFromAtomised[H :: T, U] =
        new AtomSelectorFromAtomised[H :: T, U] {
          def apply(l: H :: T) = st.value(l.head)
        }

      implicit def fuzzyHeadSelectMolecule[M[_], A, T <: HList, B](
        implicit molecule: Molecule[M, B],
        mixer: Lazy[Mixer[A, B]]
      ): AtomSelectorFromAtomised[M[A] :: T, M[B]] =
        new AtomSelectorFromAtomised[M[A] :: T, M[B]] {
          def apply(t: M[A] :: T): M[B] = molecule.functor.map(t.head)(mixer.value.mix)
        }

      implicit def recurseTailMolecule[H, T <: HList, M[_], U](
        implicit molecule: Molecule[M, U],
        st: AtomSelectorFromAtomised[T, M[U]]
      ): AtomSelectorFromAtomised[H :: T, M[U]] =
        new AtomSelectorFromAtomised[H :: T, M[U]] {
          def apply(l: H :: T) = st(l.tail)
        }

      implicit def recurseHeadMolecule[H <: HList, T <: HList, U, M[_]](
        implicit molecule: Molecule[M, U],
        st: Lazy[AtomSelectorFromAtomised[H, M[U]]]
      ): AtomSelectorFromAtomised[H :: T, M[U]] =
        new AtomSelectorFromAtomised[H :: T, M[U]] {
          def apply(l: H :: T) = st.value(l.head)
        }
    }
  }

  trait LowPriorityAtomiseOrDefaultSelector {
    implicit def defaultAtomSelector[L, U](implicit defaultAS: Atom.DefaultAtom[U]) = new AtomiseOrDefaultSelector[L, U] {
      def apply(l: L): U = defaultAS.default
    }
  }
}
