package io.jdrphillips
package alphabetsoup

import shapeless.HList
import shapeless.Lazy
import shapeless.{::, Generic}

// T => HList or Atom
trait Atomiser[T] extends Serializable {
  type Repr
  def to(t : T) : Repr
  def from(r : Repr) : T
}

object Atomiser extends LowPriorityAtomiserImplicits1 {

  type Aux[T, Repr0] = Atomiser[T] { type Repr = Repr0 }

  def apply[T](implicit dg: Atomiser[T]): Atomiser.Aux[T, dg.Repr] = dg

  // Recurse on the head, and then on the tail
  implicit def headFirstSearchCase[H, T <: HList, HOut, TOut <: HList](
    implicit atomiserH: Lazy[Atomiser.Aux[H, HOut]],
    atomiserT: Atomiser.Aux[T, TOut],
  ): Atomiser.Aux[H :: T, HOut :: TOut] = new Atomiser[H :: T] {
    type Repr = HOut :: TOut
    def to(t: H :: T): HOut :: TOut = atomiserH.value.to(t.head) :: atomiserT.to(t.tail)
    def from(r: HOut :: TOut): H :: T = atomiserH.value.from(r.head) :: atomiserT.from(r.tail)
  }

  // If we've hit an Atom, we go no deeper
  implicit def atomCase[T](implicit ev: Atom[T]): Atomiser.Aux[T, T] =
    new Atomiser[T] {
      type Repr = T
      def to(t: T): T = t
      def from(u: T): T = u
    }

}

trait LowPriorityAtomiserImplicits1 {

  // Turn T into an HList and recurse. Low priority because we should match on T being HList first
  implicit def genericCase[T, GenOut, AtomiserOut <: HList](
    implicit gen: Generic.Aux[T, GenOut],
    atomiser: Lazy[Atomiser.Aux[GenOut, AtomiserOut]]
  ): Atomiser.Aux[T, AtomiserOut] = new Atomiser[T] {
    type Repr = AtomiserOut
    def to(t: T): AtomiserOut = atomiser.value.to(gen.to(t))
    def from(r: AtomiserOut): T = gen.from(atomiser.value.from(r))
  }

}
