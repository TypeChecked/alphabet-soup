package io.jdrphillips
package alphabetsoup
package product

import shapeless.<:!<
import shapeless.HList
import shapeless.HNil
import shapeless.Lazy
import shapeless.{::, Generic}

// T => HList
trait DeepGeneric[T] extends Serializable {
  /** The generic representation type for {T}, which will be composed of {Coproduct} and {HList} types  */
  type Repr

  /** Convert an instance of the concrete type to the generic value representation */
  def to(t : T) : Repr

  /** Convert an instance of the generic representation to an instance of the concrete type */
  def from(r : Repr) : T
}

// TODO: simplify implicits a little
object DeepGeneric extends LowPriorityDeepGenericImplicits1 {

  type Aux[T, Repr0] = DeepGeneric[T] { type Repr = Repr0 }

  def apply[T](implicit dg: DeepGeneric[T]): DeepGeneric.Aux[T, dg.Repr] = dg


  implicit def headFirstSearchCase[H, T <: HList, HOut, TOut <: HList](
    implicit
    deepGenH: Lazy[DeepGeneric.Aux[H, HOut]],
    deepGenT: Lazy[DeepGeneric.Aux[T, TOut]],
  ): DeepGeneric.Aux[H :: T, HOut :: TOut] = new DeepGeneric[H :: T] {

    type Repr = HOut :: TOut

    def to(t: H :: T): HOut :: TOut = deepGenH.value.to(t.head) :: deepGenT.value.to(t.tail)
    def from(r: HOut :: TOut): H :: T = deepGenH.value.from(r.head) :: deepGenT.value.from(r.tail)

  }

}

trait LowPriorityDeepGenericImplicits1 extends LowPriorityDeepGenericImplicits2 {

  // If we've hit an Atom, we go no deeper
  implicit def atomCase[T](implicit ev: Atom[T]): DeepGeneric.Aux[T, T] =
    new DeepGeneric[T] {
      type Repr = T
      def to(t: T): T = t
      def from(u: T): T = u
    }

}

trait LowPriorityDeepGenericImplicits2 {

  // Turn T into an HList and try again
  implicit def genericFirstCase[T <: Product, GenOut, DeepGenOut <: HList](
    implicit gen: Generic.Aux[T, GenOut],
    // Generic should NEVER return HNil
    evTest: GenOut <:!< HNil,
    deepGen: Lazy[DeepGeneric.Aux[GenOut, DeepGenOut]]
  ): DeepGeneric.Aux[T, DeepGenOut] = new DeepGeneric[T] {
    type Repr = DeepGenOut
    def to(t: T): DeepGenOut = deepGen.value.to(gen.to(t))
    def from(r: DeepGenOut): T = gen.from(deepGen.value.from(r))
  }

}
