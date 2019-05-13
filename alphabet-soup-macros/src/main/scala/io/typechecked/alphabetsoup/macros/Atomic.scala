package io.typechecked
package alphabetsoup
package macros

import cats.data.NonEmptyList

import scala.annotation.StaticAnnotation
import scala.language.experimental.macros
import scala.reflect.macros.blackbox._

class Atomic extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro AtomicMacro.impl
}

object AtomicMacro {

  def impl(c: Context)(annottees: c.Expr[Any]*): c.Expr[Any] = {
    import c.universe._

    def mkAtomImplicit(className: TypeName) = {
        q"""implicit val ${TermName(className.toTermName.toString + "atom")}: io.typechecked.alphabetsoup.Atom[$className] = io.typechecked.alphabetsoup.Atom[$className]"""
    }

    def mkAtomImplicitParams(className: TypeName, tParams: List[TypeDef]) = {
      val params = tParams.map(_.name)
      q"""implicit def ${TermName(className.toTermName.toString + "atom")}[..$tParams]:io.typechecked.alphabetsoup.Atom[$className[..$params]] = io.typechecked.alphabetsoup.Atom[$className[..$params]]"""
    }

    def modifiedCompanion(maybeCompDecl: Option[ModuleDef], atomImplicit: Tree, className: TypeName) = {
      maybeCompDecl.fold(q"object ${className.toTermName} { $atomImplicit }"){ compDecl =>
        val q"object $obj extends ..$bases { ..$body }" = compDecl
        q"""
          object $obj extends ..$bases {
            ..$body
            $atomImplicit
          }
        """
      }
    }

    def modifiedDecl(classDecl: ClassDef, name: TypeName, tParams: List[TypeDef], maybeCompDecl: Option[ModuleDef] = None) = {
      val atomImplicit = if (tParams.isEmpty) mkAtomImplicit(name) else mkAtomImplicitParams(name, tParams)
      val companion = modifiedCompanion(maybeCompDecl, atomImplicit, name)

      c.Expr(q"""
        $classDecl
        $companion
      """)
    }

    annottees.map(_.tree).toList match {
        case (classDecl@ClassDef(_, name, tParams, _)) :: Nil => modifiedDecl(classDecl, name, tParams)
        case (classDecl@ClassDef(_, name, tParams, _)) :: (compDecl: ModuleDef) :: Nil => modifiedDecl(classDecl, name, tParams, Some(compDecl))
        case _ => c.abort(c.enclosingPosition, "Invalid: Can not annotate structure with @Atomic")
      }
  }
}
