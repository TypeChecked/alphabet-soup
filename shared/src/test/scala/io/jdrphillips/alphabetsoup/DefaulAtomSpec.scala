package io.typechecked
package alphabetsoup

import org.scalatest.FlatSpec
import org.scalatest.Matchers
import shapeless.::
import shapeless.HNil
import shapeless.test.illTyped

class DefaultAtomSpec extends FlatSpec with Matchers {

	"Mixer" should "work with a supplied default" in {
	   
	    case class Source(a: Int)
	    case class Target(a: Int, b: String)

	    implicit val default: Atom.DefaultAtom[String] = Atom.DefaultAtom("default")

	    val mixer = Mixer[Source, Target]
	    
	    val source = Source(1)

	    mixer.mix(source) shouldBe Target(1, "default")

	  }
	  it should "return types from left ignoring default" in {

	  	case class Source(a: String)
	  	case class Target(a: String)

	  	implicit val default: Atom.DefaultAtom[String] = Atom.DefaultAtom("default")

	  	val mixer = Mixer[Source, Target]

	  	mixer.mix(Source("source")) shouldBe Target("source")
	  }
	  it should "work with multiple supplied defaults" in {
	   
	    case class Source(a: Int)
	    case class Target(a: Int, b: String, c: Boolean, d: Long)

	    implicit val defaultString: Atom.DefaultAtom[String] = Atom.DefaultAtom("default")
	    implicit val defaultBoolean: Atom.DefaultAtom[Boolean] = Atom.DefaultAtom(true)
	    implicit val defaultLong: Atom.DefaultAtom[Long] = Atom.DefaultAtom(10L)

	    val mixer = Mixer[Source, Target]
	    
	    val source = Source(1)

	    mixer.mix(source) shouldBe Target(1, "default", true, 10L)

	  }
	  it should "not work with molecules containing no internal structure" in {
	    case class Source(a: Int)
	    case class Target(a: Int, b: List[String], c: Unit)

	    implicit val default: Atom.DefaultAtom[String] = Atom.DefaultAtom("default")
	    
	    illTyped("Mixer[Source, Target]")

	  }  
	  
	  it should "work with molecules" in {
	    case class Source(a: Int, b: List[Tuple1[Long]])
	    case class Target(a: Int, b: List[(Long,String)])

	    implicit val default: Atom.DefaultAtom[String] = Atom.DefaultAtom("default")
	    
	    val mixer = Mixer[Source, Target]

	    mixer.mix(Source(1, List(Tuple1(10L)))) shouldBe Target(1, List((10L,"default")))
	  }
	  
	  it should "override molecule behaviour with default" in {
	    case class Source(a: Int)
	    case class Target(a: Int, b: List[String], c: Unit)

	    implicit val default: Atom.DefaultAtom[List[String]] = Atom.DefaultAtom(List("default"))
	    
	    val mixer = Mixer[Source, Target]

	    mixer.mix(Source(1)) shouldBe Target(1, List("default"), ())
	  }

	  it should "work when it finds a HNil before traversing entire structure" in {

	  	case class Source(a : Int)

	  	implicit val default: Atom.DefaultAtom[String] = Atom.DefaultAtom("default")

	  	AtomSelector[(Int :: HNil) :: (String :: HNil) :: HNil, String].apply((1 :: HNil) :: ("Me!" :: HNil) :: HNil) shouldBe "Me!"
	  	
	  }

	  it should "always work with HNil in this particular position" in {

	  	implicit val default: Atom.DefaultAtom[String] = Atom.DefaultAtom("default")

	  	AtomSelector[Int :: (String :: HNil) :: HNil, String].apply(1 :: ("Me!" :: HNil) :: HNil) shouldBe "Me!"
	  	
	  }

	 "MixerBuilder" should "take precedence over supplied defaults" in {
	 		case class Source(a: Int)
	 		case class Target(a: Int, b: String)

	 		implicit val default: Atom.DefaultAtom[String] = Atom.DefaultAtom("Don't use me!!!")

	 		val mixer = Mixer.from[Source].to[Target].withDefault("I AM THE REAL DEFAULT").build

	 		mixer.mix(Source(1)) shouldBe Target(1, "I AM THE REAL DEFAULT")


	 }

}