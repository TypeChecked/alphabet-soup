package io.typechecked
package alphabetsoup

import shapeless.HNil

trait Atom[T]

object Atom {

  def apply[T]: Atom[T] = new Atom[T] {}

  implicit val stringAtom: Atom[String] = new Atom[String] {}
  implicit val charAtom: Atom[Char] = new Atom[Char] {}
  implicit val booleanAtom: Atom[Boolean] = new Atom[Boolean] {}
  implicit val intAtom: Atom[Int] = new Atom[Int] {}
  implicit val floatAtom: Atom[Float] = new Atom[Float] {}
  implicit val doubleAtom: Atom[Double] = new Atom[Double] {}
  implicit val longAtom: Atom[Long] = new Atom[Long] {}

  private[typechecked] trait DefaultAtomImpl[T] {
    def asDefaultAtom(value: T): DefaultAtom[T] = new DefaultAtom[T] {
      def default: T = value
    }
  }

  sealed trait DefaultAtom[T] extends Atom[T] {
    def default: T
  }

  object DefaultAtom {
    def apply[T](arg: T)(implicit impl: DefaultAtomImpl[T]): DefaultAtom[T] = impl.asDefaultAtom(arg)
  }

  object DefaultAtomImpl extends LowerPriorityDefaultAtomImpl {

    implicit def forbidMoleculeAmbiguous[T[_], A](implicit ev: Molecule[T, A]): DefaultAtomImpl[T[A]] = new DefaultAtomImpl[T[A]]{}
    implicit def forbidMoleculeAmbiguous1[T[_], A](implicit ev: Molecule[T, A]): DefaultAtomImpl[T[A]] = new DefaultAtomImpl[T[A]]{}


  }
  trait LowerPriorityDefaultAtomImpl{
    implicit def freeDefaultAtomImpl[T]: DefaultAtomImpl[T] = new DefaultAtomImpl[T] {}
  }

  implicit val hnilAtom: DefaultAtom[HNil] = DefaultAtom.apply[HNil](HNil)
  implicit val unitAtom: DefaultAtom[Unit] = DefaultAtom.apply[Unit](())
}
