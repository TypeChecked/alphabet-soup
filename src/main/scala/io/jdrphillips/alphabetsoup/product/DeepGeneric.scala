package io.jdrphillips
package alphabetsoup
package product

import shapeless.HList
import shapeless.Lazy
import shapeless.{::, Generic}

// T => HList
trait DeepGeneric[T] extends Serializable {
  type Repr
  def to(t : T) : Repr
  def from(r : Repr) : T
}

object DeepGeneric extends LowPriorityDeepGenericImplicits1 {

  type Aux[T, Repr0] = DeepGeneric[T] { type Repr = Repr0 }

  def apply[T](implicit dg: DeepGeneric[T]): DeepGeneric.Aux[T, dg.Repr] = dg

  // Recurse on the head, and then on the tail
  implicit def headFirstSearchCase[H, T <: HList, HOut, TOut <: HList](
    implicit deepGenH: Lazy[DeepGeneric.Aux[H, HOut]],
    deepGenT: DeepGeneric.Aux[T, TOut],
  ): DeepGeneric.Aux[H :: T, HOut :: TOut] = new DeepGeneric[H :: T] {
    type Repr = HOut :: TOut
    def to(t: H :: T): HOut :: TOut = deepGenH.value.to(t.head) :: deepGenT.to(t.tail)
    def from(r: HOut :: TOut): H :: T = deepGenH.value.from(r.head) :: deepGenT.from(r.tail)
  }

  // If we've hit an Atom, we go no deeper
  implicit def atomCase[T](implicit ev: Atom[T]): DeepGeneric.Aux[T, T] =
    new DeepGeneric[T] {
      type Repr = T
      def to(t: T): T = t
      def from(u: T): T = u
    }

}

trait LowPriorityDeepGenericImplicits1 {

  // Turn T into an HList and recurse. Low priority because we should match on T being HList first
  implicit def genericCase[T, GenOut, DeepGenOut <: HList](
    implicit gen: Generic.Aux[T, GenOut],
    deepGen: Lazy[DeepGeneric.Aux[GenOut, DeepGenOut]]
  ): DeepGeneric.Aux[T, DeepGenOut] = new DeepGeneric[T] {
    type Repr = DeepGenOut
    def to(t: T): DeepGenOut = deepGen.value.to(gen.to(t))
    def from(r: DeepGenOut): T = gen.from(deepGen.value.from(r))
  }

}
