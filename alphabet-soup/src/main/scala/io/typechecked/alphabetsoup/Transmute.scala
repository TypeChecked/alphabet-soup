package io.typechecked.alphabetsoup

sealed trait Transmute[A, B] {

  def convert: A => B

}

object Transmute {

  def apply[A: Atom, B: Atom](f: A => B): Transmute[A, B] =
    new Transmute[A, B] {
      def convert: A => B = f
  }

  def molecular[M[_], A, B: Atom](f: M[A] => B)(implicit ma: Molecule[M, A]): Transmute[M[A], B] = new Transmute[M[A], B] {
    def convert: M[A] => B = f
  }

  def transmuteF[M[_], A, B](f: A => B)(implicit ma: Molecule[M, A], mb: Molecule[M, B]): Transmute[M[A], M[B]] =
    new Transmute[M[A], M[B]] {
      def convert: M[A] => M[B] = ma.functor.map(_)(f)
    }

  def transmuteK[M[_], N[_], A, B](f: M[A] => N[B])(implicit ma: Molecule[M, A], nb: Molecule[N, B]): Transmute[M[A], N[B]] =
    new Transmute[M[A], N[B]] {
      def convert: M[A] => N[B] = f
    }
}

