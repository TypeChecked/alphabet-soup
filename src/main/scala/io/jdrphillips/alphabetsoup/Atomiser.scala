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
    implicit deepGenH: Lazy[Atomiser.Aux[H, HOut]],
    deepGenT: Atomiser.Aux[T, TOut],
  ): Atomiser.Aux[H :: T, HOut :: TOut] = new Atomiser[H :: T] {
    type Repr = HOut :: TOut
    def to(t: H :: T): HOut :: TOut = deepGenH.value.to(t.head) :: deepGenT.to(t.tail)
    def from(r: HOut :: TOut): H :: T = deepGenH.value.from(r.head) :: deepGenT.from(r.tail)
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
  implicit def genericCase[T, GenOut, DeepGenOut <: HList](
    implicit gen: Generic.Aux[T, GenOut],
    deepGen: Lazy[Atomiser.Aux[GenOut, DeepGenOut]]
  ): Atomiser.Aux[T, DeepGenOut] = new Atomiser[T] {
    type Repr = DeepGenOut
    def to(t: T): DeepGenOut = deepGen.value.to(gen.to(t))
    def from(r: DeepGenOut): T = gen.from(deepGen.value.from(r))
  }

}
