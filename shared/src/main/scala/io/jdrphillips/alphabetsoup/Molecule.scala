package io.jdrphillips
package alphabetsoup

import cats.Functor
import cats.instances.list.catsStdInstancesForList
import cats.instances.option.catsStdInstancesForOption

trait Molecule[M[_], A] {
  def functor: Functor[M]
}

object Molecule {

  def apply[M[_], A](implicit f: Functor[M]): Molecule[M, A] = new Molecule[M, A] { val functor = f }

  implicit def listMolecule[A]: Molecule[List, A] = Molecule[List, A]
  implicit def optionMolecule[A]: Molecule[Option, A] = Molecule[Option, A]

}
