package io.jdrphillips
package alphabetsoup

import shapeless.HNil

trait Atom[T]

object Atom {

  def apply[T]: Atom[T] = new Atom[T] {}

  implicit val hnilAtom: Atom[HNil] = new Atom[HNil] {}
  implicit val stringAtom: Atom[String] = new Atom[String] {}
  implicit val charAtom: Atom[Char] = new Atom[Char] {}
  implicit val booleanAtom: Atom[Boolean] = new Atom[Boolean] {}
  implicit val intAtom: Atom[Int] = new Atom[Int] {}
  implicit val floatAtom: Atom[Float] = new Atom[Float] {}
  implicit val doubleAtom: Atom[Double] = new Atom[Double] {}
  implicit val longAtom: Atom[Long] = new Atom[Long] {}
  implicit val unitAtom: Atom[Unit] = new Atom[Unit] {}
}
