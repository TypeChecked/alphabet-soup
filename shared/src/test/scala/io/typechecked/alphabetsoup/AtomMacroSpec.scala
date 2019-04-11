package io.typechecked.alphabetsoup
import macros._

class AtomMacroSpec {
  @Atomic case class MakeMeAnAtom()

  implicitly[Atom[MakeMeAnAtom]]
}
