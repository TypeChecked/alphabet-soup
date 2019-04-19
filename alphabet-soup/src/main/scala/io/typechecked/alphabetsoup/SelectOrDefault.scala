package io.typechecked
package alphabetsoup

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
trait SelectOrDefault[L, U] {
  def apply(l: L): U
  def replace(u: U, l: L): L
}

object SelectOrDefault extends LowPrioritySelectOrDefault {

  def apply[L, U](implicit s: SelectOrDefault[L, U]): SelectOrDefault[L, U] = s

  implicit def trySelectFromAtomised[L, U](implicit selector: SelectFromAtomised[L, U]): SelectOrDefault[L, U] =
    new SelectOrDefault[L, U] {
      def apply(l: L): U = selector(l)
      def replace(u: U, l: L): L = selector.replace(u, l)
    }

}

trait LowPrioritySelectOrDefault {
  implicit def tryDefaultSelect[L, U](implicit defaultAS: Atom.DefaultAtom[U]): SelectOrDefault[L, U] =
    new SelectOrDefault[L, U] {
      def apply(l: L): U = defaultAS.default
      def replace(u: U, l: L): L = l
    }
}
