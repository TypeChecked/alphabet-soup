package io.jdrphillips
package alphabetsoup
package product

import shapeless.<:!<
import shapeless.GenericMacros
import shapeless.HList
import shapeless.HNil
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

object DeepGeneric extends LowPriorityDeepGenericImplicits1 {

  type Aux[T, Repr0] = DeepGeneric[T] { type Repr = Repr0 }

  def apply[T](implicit dg: DeepGeneric[T]): DeepGeneric.Aux[T, dg.Repr] = dg

  // Outermost case: Use Generic to get an HList and then search for HList handing implicits
  implicit def initialToHListRecurse[T, GenOut, DeepGenOut <: HList](
    implicit gen: Generic.Aux[T, GenOut],
    deepGen: DeepGeneric.Aux[GenOut, DeepGenOut]
  ): DeepGeneric.Aux[T, DeepGenOut] = new DeepGeneric[T] {
    type Repr = DeepGenOut
    def to(t: T): DeepGenOut = deepGen.to(gen.to(t))
    def from(r: DeepGenOut): T = gen.from(deepGen.from(r))
  }

  implicit def hnilCase: DeepGeneric.Aux[HNil, HNil] = new DeepGeneric[HNil] {
    type Repr = HNil
    def to(h: HNil): HNil = h
    def from(h: HNil): HNil = h
  }

  implicit def headFirstSearchCase[H, T <: HList, HOut <: HList, TOut <: HList](
    implicit deepGenH: DeepGeneric.Aux[H, HOut],
    deepGenT: DeepGeneric.Aux[T, TOut]
  ): DeepGeneric.Aux[H :: T, HOut :: TOut] = new DeepGeneric[H :: T] {

    type Repr = HOut :: TOut

    def to(t: H :: T): HOut :: TOut = deepGenH.to(t.head) :: deepGenT.to(t.tail)
    def from(r: HOut :: TOut): H :: T = deepGenH.from(r.head) :: deepGenT.from(r.tail)

  }

}

trait LowPriorityDeepGenericImplicits1 extends LowPriorityDeepGenericImplicits2 {
//  self: DeepGeneric.type =>
//  implicit def tuple1Case[A]: DeepGeneric.Aux[Tuple1[A], A :: HNil] = new DeepGeneric[Tuple1[A]] {
//    type Repr = A :: HNil
//    def to(h: Tuple1[A]): A :: HNil = h._1 :: HNil
//    def from(h: A :: HNil): Tuple1[A] = Tuple1(h.head)
//  }

}

trait LowPriorityDeepGenericImplicits2 {
//  self: DeepGeneric.type =>

  implicit def noHeadSearchCase[H, T <: HList, TOut <: HList](
    implicit
//    ev2: H <:!< Tuple1[_],
    deepGenT: DeepGeneric.Aux[T, TOut],
//    ev2: H <:!< Product,
  ): DeepGeneric.Aux[H :: T, H :: TOut] = new DeepGeneric[H :: T] {

    type Repr = H :: TOut

    def to(t: H :: T): H :: TOut = t.head :: deepGenT.to(t.tail)
    def from(r: H :: TOut): H :: T = r.head :: deepGenT.from(r.tail)

  }

}
