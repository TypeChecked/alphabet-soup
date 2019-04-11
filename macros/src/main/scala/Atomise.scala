package macros

import scala.annotation.StaticAnnotation
import scala.language.experimental.macros
import scala.reflect.macros.blackbox._

class Atomic extends StaticAnnotation {
  def macroTransform(annottees: Any*) = macro AtomicMacro.impl
}

object AtomicMacro {
  def impl(c: Context)(annottees: c.Expr[Any]*): c.Expr[Any] = {
    import c.universe._

    def mkAtomImplicit(className: TypeName) = {
        q"""implicit val ${TermName(className.toTermName.toString + "atom")}: Atom[$className] = Atom[$className]"""
    }

    def extractClassName(classDecl: ClassDef) = {
      try {
        val q"case class $className(..$_) extends ..$_ { ..$_ }" = classDecl
        className
      } catch {
        case _: MatchError => c.abort(c.enclosingPosition, "@Atmic annotation is only supported on case class")
      }
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

    def modifiedDecl(classDecl: ClassDef, maybeCompDecl: Option[ModuleDef] = None) = {
      val className = extractClassName(classDecl)
      val atomImplicit = mkAtomImplicit(className)
      val companion = modifiedCompanion(maybeCompDecl, atomImplicit, className)

      c.Expr(q"""
        $classDecl
        $companion
      """)
    }

    annottees.map(_.tree).toList match {
        case (classDecl: ClassDef) :: Nil => modifiedDecl(classDecl)
        case (classDecl: ClassDef) :: (compDecl: ModuleDef) :: Nil => modifiedDecl(classDecl, Some(compDecl))
        case _ => c.abort(c.enclosingPosition, "Invalid Annotee")
      }
  }
}
