package io.typechecked
package alphabetsoup

import shapeless.::
import shapeless.=:!=
import shapeless.HList
import shapeless.HNil
import shapeless.Lazy
import shapeless.LowPriority

// This is the version application code should use
trait Mixer[A, B] {
  def mix(a: A): B
  def inject(b: B, a: A): A
  def modify(f: B => B)(a: A): A = inject(f(mix(a)), a)
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

      def build(implicit m: MixerImpl[From :: Defaults :: HNil, To], r: MixerImpl[(To, From), From]): Mixer[From, To] =
        new Mixer[From, To] {
          def mix(a: From): To = m.mix(a :: defaults :: HNil)
          def inject(b: To, a: From): From = r.mix(b -> a)
        }

    }

  }

  private def fromMixerImpl[A, B](m: MixerImpl[A, B]): Mixer[A, B] =
    new Mixer[A, B] {
      def mix(a: A): B = m.mix(a)
      def inject(b: B, a: A): A = m.inject(b, a)
    }
}

/**
 * Mixes A into B, atomising them both as a first step
 *
 * Allows you to inject an instance of B into an instance of A, replacing all values in A whose types
 * have a corresponding value in B
 *
 * MixerImplFromAtomised[A, B].inject(b, a) is the same as MixerImplFromAtomised[(B, A), A].mix(b -> a)
 * but more efficient due to reusing the same internal structures
 */
trait MixerImpl[A, B] {
  def mix(a: A): B
  def inject(b: B, a: A): A
}

object MixerImpl {

  def apply[A, B](implicit m: MixerImpl[A, B]): MixerImpl[A, B] = m

  implicit def mixerImplEquality[A]: MixerImpl[A, A] = new MixerImpl[A, A] {
    def mix(a: A): A = a
    def inject(b: A, a: A): A = b
  }

  // Do not put bound `BOut <: HList`
  // It does not behave as expected and inference causes issues downstream
  // Reply on boutIsHList check instead
  implicit def atomiseThenImpl[A, AOut, B, BOut](
    implicit ev: A =:!= B,
    atomiserB: Atomiser.Aux[B, BOut],
    boutIsHList: BOut <:< HList,
    atomiserA: Atomiser.Aux[A, AOut],
    m: Lazy[MixerImplFromAtomised[AOut, BOut]]
  ): MixerImpl[A, B] = new MixerImpl[A, B] {
    def mix(a: A): B = atomiserB.from(m.value.mix(atomiserA.to(a)))
    def inject(b: B, a: A): A = {
      val bAtoms = atomiserB.to(b)
      val aAtoms = atomiserA.to(a)
      val injected = m.value.inject(bAtoms, aAtoms)
      atomiserA.from(injected)
    }
  }

  implicit def bIsAtomRecurse[A, B](
    implicit atom: Atom[B],
    s: AtomSelector[A, B]
  ): MixerImpl[A, B] = new MixerImpl[A, B] {
    def mix(a: A): B = s(a)
    def inject(b: B, a: A): A = s.replace(b, a)
  }

  implicit def bIsMoleculeRecurse[A, M[_], B](
    implicit lp: LowPriority,
    molecule: Molecule[M, B],
    s: AtomSelector[A, M[B]]
  ): MixerImpl[A, M[B]] = new MixerImpl[A, M[B]] {
    def mix(a: A): M[B] = s(a)
    def inject(b: M[B], a: A): A = s.replace(b, a)
  }

}

/**
 * Mixes A into B, assuming both A and B have been atomised
 *
 * Allows you to inject an instance of B into an instance of A, replacing all values in A whose types
 * have a corresponding value in B
 *
 * MixerImplFromAtomised[A, B].inject(b, a) is the same as MixerImplFromAtomised[(B, A), A].mix(b -> a)
 * but more efficient due to reusing the same internal structures
 */
trait MixerImplFromAtomised[A, B] {
  def mix(a: A): B
  def inject(b: B, a: A): A
}

object MixerImplFromAtomised extends LowPriorityMFAImplicits1 {

  def apply[A, B](implicit m: MixerImplFromAtomised[A, B]): MixerImplFromAtomised[A, B] = m

  // Anything can satisfy HNil
  implicit def hnilCase[A]: MixerImplFromAtomised[A, HNil] = new MixerImplFromAtomised[A, HNil] {
    def mix(a: A): HNil = HNil
    def inject(b: HNil, a: A): A = a
  }

  implicit def bHeadIsAtomRecurse[A, BH, BT <: HList](
    implicit atom: Atom[BH],
    s: SelectOrDefaultOrTransmute[A, BH],
    m2: MixerImplFromAtomised[A, BT]
  ): MixerImplFromAtomised[A, BH :: BT] = new MixerImplFromAtomised[A, BH :: BT] {
    def mix(a: A): BH :: BT = s(a) :: m2.mix(a)
    def inject(b: BH :: BT, a: A): A = {
      val tailInjected = m2.inject(b.tail, a)
      s.replace(b.head, tailInjected)
    }
  }

  implicit def bHeadIsMoleculeRecurse[A, M[_], BH, BT <: HList](
    implicit lp: LowPriority,
    molecule: Molecule[M, BH],
    s: SelectOrDefaultOrTransmute[A, M[BH]],
    m2: MixerImplFromAtomised[A, BT]
  ): MixerImplFromAtomised[A, M[BH] :: BT] = new MixerImplFromAtomised[A, M[BH] :: BT] {
    def mix(a: A): M[BH] :: BT = s(a) :: m2.mix(a)
    def inject(b: M[BH] :: BT, a: A): A = {
      val tailInjected = m2.inject(b.tail, a)
      s.replace(b.head, tailInjected)
    }
  }

}

trait LowPriorityMFAImplicits1 {

  implicit def bHeadIsHListRecurse[A, BT <: HList, BHH, BHT <: HList](
    implicit m1: Lazy[MixerImplFromAtomised[A, BHH :: BHT]],
    m2: MixerImplFromAtomised[A, BT]
  ): MixerImplFromAtomised[A, (BHH :: BHT) :: BT] = new MixerImplFromAtomised[A, (BHH :: BHT) :: BT] {
    def mix(a: A): (BHH :: BHT) :: BT = m1.value.mix(a) :: m2.mix(a)
    def inject(b: (BHH :: BHT) :: BT, a: A): A = {
      val tailInjected = m2.inject(b.tail, a)
      m1.value.inject(b.head, tailInjected)
    }
  }

}
