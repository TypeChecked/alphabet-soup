package io.typechecked
package alphabetsoup

import shapeless.::
import shapeless.=:!=
import shapeless.HList
import shapeless.HNil
import shapeless.Lazy

// This is the version code should use
trait Mixer[A, B] {
  def mix(a: A): B
}

object Mixer {

  def apply[A, B](implicit m: MixerImpl[A, B]): Mixer[A, B] = fromMixerImpl(m)

  implicit def materialise[A, B](implicit m: MixerImpl[A, B]): Mixer[A, B] = fromMixerImpl(m)

  def from[From]: MixerBuilder[From] = new MixerBuilder[From] {}

  trait MixerBuilder[From] {

    def to[To]: MixerBuilderCanComplete[HNil, To] = MixerBuilderCanComplete[HNil, To](HNil)

    case class MixerBuilderCanComplete[Defaults <: HList, To](defaults: Defaults) {

      def withDefault[F](t: F): MixerBuilderCanComplete[F :: Defaults :: HNil, To] =
        MixerBuilderCanComplete[F :: Defaults :: HNil, To](t :: this.defaults :: HNil)

      def mix(from: From)(implicit m: MixerImpl[From :: Defaults :: HNil, To]): To =
        m.mix(from :: defaults :: HNil)

      def build(implicit m: MixerImpl[From :: Defaults :: HNil, To]): Mixer[From, To] =
        new Mixer[From, To] {
          def mix(a: From): To = {
            m.mix(a :: defaults :: HNil)
          }
        }

    }

  }

  private def fromMixerImpl[A, B](m: MixerImpl[A, B]): Mixer[A, B] = new Mixer[A, B] {
    def mix(a: A): B = m.mix(a)
  }
}

// This exists to separate the concerns of implicit searches and Mixer building and defaults, which
// is presented in object Mixer. Code should not use this.
trait MixerImpl[A, B] {
  def mix(a: A): B
}

object MixerImpl {

  def apply[A, B](implicit m: MixerImpl[A, B]): MixerImpl[A, B] = m

  implicit def mixerImplEquality[A]: MixerImpl[A, A] = new MixerImpl[A, A] {
    def mix(a: A): A = a
  }

  implicit def atomiseThenImpl[A, AOut, B, BOut <: HList](
    implicit ev: A =:!= B,
    atomiserB: Atomiser.Aux[B, BOut],
    atomiserA: Atomiser.Aux[A, AOut],
    m: Lazy[MixerImplFromAtomised[AOut, BOut]]
  ): MixerImpl[A, B] = new MixerImpl[A, B] {
    def mix(a: A): B = atomiserB.from(m.value.mix(atomiserA.to(a)))
  }

  implicit def bIsAtomRecurse[A, B](
    implicit atom: Atom[B],
    s: AtomSelector[A, B]
  ): MixerImpl[A, B] = new MixerImpl[A, B] {
    def mix(a: A): B = s(a)
  }

  implicit def bIsMoleculeRecurse[A, M[_], B](
    implicit molecule: Molecule[M, B],
    s: AtomSelector[A, M[B]]
  ): MixerImpl[A, M[B]] = new MixerImpl[A, M[B]] {
    def mix(a: A): M[B] = s(a)
  }

}

// A mixer that assumes the both sides are atomised
trait MixerImplFromAtomised[A, B] {
  def mix(a: A): B
}

object MixerImplFromAtomised extends LowPriorityMFAImplicits1 {

  import AtomSelector.AtomiseOrDefaultSelector

  def apply[A, B](implicit m: MixerImplFromAtomised[A, B]): MixerImplFromAtomised[A, B] = m

  // Anything can satisfy HNil
  implicit def hnilCase[A]: MixerImplFromAtomised[A, HNil] = new MixerImplFromAtomised[A, HNil] {
    def mix(a: A): HNil = HNil
  }

  implicit def bHeadIsAtomRecurse[A, BH, BT <: HList](
    implicit atom: Atom[BH],
    s: AtomiseOrDefaultSelector[A, BH],
    m2: MixerImplFromAtomised[A, BT]
  ): MixerImplFromAtomised[A, BH :: BT] = new MixerImplFromAtomised[A, BH :: BT] {
    def mix(a: A): BH :: BT = s(a) :: m2.mix(a)
  }

  implicit def bHeadIsMoleculeRecurse[A, M[_], BH, BT <: HList](
    implicit molecule: Molecule[M, BH],
    s: AtomiseOrDefaultSelector[A, M[BH]],
    m2: MixerImplFromAtomised[A, BT]
  ): MixerImplFromAtomised[A, M[BH] :: BT] = new MixerImplFromAtomised[A, M[BH] :: BT] {
    def mix(a: A): M[BH] :: BT = s(a) :: m2.mix(a)
  }

}

trait LowPriorityMFAImplicits1 {

  implicit def bHeadIsHListRecurse[A, BT <: HList, BHH, BHT <: HList](
    implicit m1: Lazy[MixerImplFromAtomised[A, BHH :: BHT]],
    m2: MixerImplFromAtomised[A, BT]
  ): MixerImplFromAtomised[A, (BHH :: BHT) :: BT] = new MixerImplFromAtomised[A, (BHH :: BHT) :: BT] {
    def mix(a: A): (BHH :: BHT) :: BT = m1.value.mix(a) :: m2.mix(a)
  }

}
