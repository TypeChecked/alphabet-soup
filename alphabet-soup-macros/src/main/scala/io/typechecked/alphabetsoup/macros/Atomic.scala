package io.typechecked
package alphabetsoup
package macros

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

    def modifiedCompanion(maybeCompDecl: Option[ModuleDef], atomImplicit: ValDef, className: TypeName) = {
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

    def modifiedDecl(classDecl: ClassDef, name: c.universe.TypeName, maybeCompDecl: Option[ModuleDef] = None) = {
      val className = name
      val atomImplicit = mkAtomImplicit(className)
      val companion = modifiedCompanion(maybeCompDecl, atomImplicit, className)

      c.Expr(q"""
        $classDecl
        $companion
      """)
    }

    annottees.map(_.tree).toList match {
        case (classDecl@ClassDef(_, name, _, _)) :: Nil => modifiedDecl(classDecl, name)
        case (classDecl@ClassDef(_, name, _, _)) :: (compDecl: ModuleDef) :: Nil => modifiedDecl(classDecl, name, Some(compDecl))
        case _ => c.abort(c.enclosingPosition, "Invalid: Can not annotate structure with @Atomic")
      }
  }
}
