package io.jdrphillips
package alphabetsoup

import shapeless.HList
import shapeless.HNil
import shapeless.Lazy
import shapeless.ops.hlist.IsHCons

// Existence of this proves A can be mixed into B
trait Mixer[A, B] {
  def mix(a: A): B
}

// TODO: Tidy up implicit structure?
object Mixer extends LowPriorityMixerImplicits1 {

  def apply[A, B](implicit m: Mixer[A, B]): Mixer[A, B] = m

  // If we are dealing with an atom, we can mix it into itself
  implicit def atomicCase[A: Atom]: Mixer[A, A] = new Mixer[A, A] {
    def mix(a: A): A = a
  }

}

trait LowPriorityMixerImplicits1 extends LowPriorityMixerImplicits2 {

  // Anything can satisfy HNil
  implicit def hnilCase[A]: Mixer[A, HNil] = new Mixer[A, HNil] {
    def mix(a: A): HNil = HNil
  }

  // Atomise B, and if it is an HList split it
  // If the head is an atom, process it, and recurse on tail
  implicit def bHListRecurse2[A, B, BOut <: HList, BH, BT <: HList](
    implicit atomiser: Atomiser.Aux[B, BOut],
    hcons: IsHCons.Aux[BOut, BH, BT],
    atom: Atom[BH],
    s: AtomSelector[A, BH],
    m2: Mixer[A, BT]
  ): Mixer[A, B] = new Mixer[A, B] {
    def mix(a: A): B = atomiser.from(hcons.cons(s(a), m2.mix(a)))
  }

}

trait LowPriorityMixerImplicits2 extends LowPriorityMixerImplicits3 {
  // Atomise B, and if it is an HList split it and recurse on head and tail after proving head is an HList
  implicit def bHListRecurse[A, B, BOut <: HList, BH<: HList, BT <: HList, BHH, BHT <: HList](
    implicit atomiser: Atomiser.Aux[B, BOut],
    hcons: IsHCons.Aux[BOut, BH, BT],
    hcons2: IsHCons.Aux[BH, BHH, BHT],
    m1: Lazy[Mixer[A, BH]],
    m2: Mixer[A, BT]
  ): Mixer[A, B] = new Mixer[A, B] {
    def mix(a: A): B = atomiser.from(hcons.cons(m1.value.mix(a), m2.mix(a)))
  }
}

trait LowPriorityMixerImplicits3 {

  // If B is an atom, select the value from A
  implicit def bAtomicRecurse[A, B](
    implicit atom: Atom[B],
    s: AtomSelector[A, B]
  ): Mixer[A, B] = new Mixer[A, B] {
    def mix(a: A): B = s(a)
  }

}


