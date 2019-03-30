package io.typechecked
package alphabetsoup

/**
 * Tries to use SelectFromAtomised to produce a value U from L. If it fails, tries to find a default atom
 * of type U
 *
 * This assumes L is atomised.
 */
trait SelectOrDefault[L, U] {
  def apply(l: L): U
}

object SelectOrDefault extends LowPrioritySelectOrDefault {

  implicit def trySelectFromAtomised[L, U](implicit selector: SelectFromAtomised[L, U]): SelectOrDefault[L, U] =
    new SelectOrDefault[L, U] {
      def apply(l: L): U = selector(l)
  }

}

trait LowPrioritySelectOrDefault {
  implicit def tryDefaultSelect[L, U](implicit defaultAS: Atom.DefaultAtom[U]): SelectOrDefault[L, U] =
    new SelectOrDefault[L, U] {
      def apply(l: L): U = defaultAS.default
    }
}
