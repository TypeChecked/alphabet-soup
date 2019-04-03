package io.typechecked.alphabetsoup

sealed trait Transmute[A, B] {

  def convert: A => B

}

object Transmute {

  def apply[A: Atom, B: Atom](f: A => B): Transmute[A, B] =
    new Transmute[A, B] {

     def convert: A => B = f

  }
}

