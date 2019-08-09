package io.typechecked
package alphabetsoup

import shapeless.HList

/**
 * Tries to use SelectFromAtomised to produce a value U from L. If it fails, tries to find a default atom
 * of type U
 *
 * This assumes L is atomised.
 *
 * ### Replace behaviour
 *
 * If we can select U from L, replace acts as it does for SelectFromAtomised
 *
 * If we can not select U from L and rely on an implicit default, we replace nothing and return our L unchanged
 */
trait SelectOrTransmuteOrDefault[L, U] {
  def apply(l: L): U
  def replace(u: U, l: L): L
}

object SelectOrTransmuteOrDefault extends LowPrioritySelectOrTransmuteOrDefault {

  def apply[L, U](implicit s: SelectOrTransmuteOrDefault[L, U]): SelectOrTransmuteOrDefault[L, U] = s

  implicit def trySelectFromAtomised[L, U](implicit selector: SelectFromAtomised[L, U]): SelectOrTransmuteOrDefault[L, U] =
    new SelectOrTransmuteOrDefault[L, U] {
      def apply(l: L): U = selector(l)
      def replace(u: U, l: L): L = selector.replace(u, l)
    }

}

trait LowPrioritySelectOrTransmuteOrDefault extends LowLowPrioritySelectOrTransmuteOrDefault {

  implicit def tryTransmute[L, U](implicit transmute: SelectAndTransmute[L, U]): SelectOrTransmuteOrDefault[L, U] =
    new SelectOrTransmuteOrDefault[L, U] {
      def apply(l: L): U = transmute(l)
      def replace(u: U, l: L): L = l
    }

}

trait LowLowPrioritySelectOrTransmuteOrDefault {
  implicit def tryDefaultSelect[L, U](implicit defaultAS: Atom.DefaultAtom[U]): SelectOrTransmuteOrDefault[L, U] =
    new SelectOrTransmuteOrDefault[L, U] {
      def apply(l: L): U = defaultAS.default
      def replace(u: U, l: L): L = l
    }
}
