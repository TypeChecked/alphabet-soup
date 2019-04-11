package io.typechecked.alphabetsoup
import macros._
import org.scalatest._
import shapeless.test.illTyped

class AtomMacroSpec extends FlatSpec with Matchers {

	"@Atomic" should "create implicit Atom for an empty case class" in {

		@Atomic case class Foo()

  	implicitly[Atom[Foo]]
	}
	it should "create an implicit Atom for a case class with a single field" in {
		@Atomic case class Foo(bar: String)

  	implicitly[Atom[Foo]]
	}
	it should "create an implicit Atom for a case class with a multiple fields" in {
		@Atomic case class Foo(bar: String, baz: Int, bam: Float)

  	implicitly[Atom[Foo]] 
	}
	it should "create an implicit Atom for a case class with a multiple fields and function defintions inside" in {
		@Atomic case class Foo(bar: String, baz: Int, bam: Boolean) {
			def getBar: String = bar
			def getBaz: Int = baz
		}

  	implicitly[Atom[Foo]] 

  	val foo = Foo("bar", 7, true)

  	foo.getBar shouldBe "bar"
  	foo.getBaz shouldBe 7
  	foo.bam shouldBe true
	}
	it should "place implicit inside companion object if one already exists" in {
		@Atomic case class Foo()

		object Foo {
			val iAmHere: String = "Yeah Baby!"
		}

		implicitly[Atom[Foo]]

		Foo.iAmHere shouldBe "Yeah Baby!"

	}
	it should "place implicit inside companion object if one already exists for a case class with multiple fields" in {
		@Atomic case class Foo(bar: String, baz: Int, bam: Boolean) {

			def giveMeBam: Boolean = bam
		}

		object Foo {
			val iAmHere: String = "Yeah Baby!"
		}

		implicitly[Atom[Foo]]

		val foo = Foo("plumbus", 42, false)

		Foo.iAmHere shouldBe "Yeah Baby!"
		foo.bar shouldBe "plumbus"
		foo.baz shouldBe 42
		foo.giveMeBam shouldBe false

	}
	it should "blow up with ambiguous implicits if an Atom is already defined" in {
		@Atomic case class Foo()

		object Foo {
			implicit val atom: Atom[Foo] = Atom[Foo]
		}

		illTyped{"implicitly[Atom[Foo]]"}
	}
	it should "blow up if something is defined inside companion object with same name as generated one" in {
		illTyped{"""
			@Atomic case class Foo()

			object Foo {
				val Fooatom = "I am the real Fooatom!!!"
			}
		"""}
	}
}
