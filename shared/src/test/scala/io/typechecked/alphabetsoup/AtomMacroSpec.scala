package io.typechecked.alphabetsoup
import macros._
import org.scalatest._

class AtomMacroSpec extends FlatSpec with Matchers {

	"@Atomic" should "create implicit Atom for an empty case class" in {

		@Atomic case class MakeMeAnAtom()

  	implicitly[Atom[MakeMeAnAtom]]
	}
	// it should "create an implicit Atom for a case class with a single field" in {
		
	// }
}
