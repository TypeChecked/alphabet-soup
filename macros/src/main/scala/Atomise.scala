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

    def atomFormatter(className: TypeName) = {
        q"""object ${className.toTermName} { implicit val ${TermName(className.toTermName.toString + "atom")}: Atom[$className] = Atom[$className] }"""
    }

    def extractClassName(classDecl: ClassDef) = {
      try {
        val q"case class $className(..$fields) extends ..$bases { ..$body }" = classDecl
        className
      } catch {
        case _: MatchError => c.abort(c.enclosingPosition, "Annotation is only supported on case class")
      }
    }

    def modifiedClass(classDecl: ClassDef) = {
      val className = extractClassName(classDecl)
      val companion = atomFormatter(className)

      c.Expr(q"""
        $classDecl
        $companion
      """)
    }

    annottees.map(_.tree).toList match {
        case (classDecl: ClassDef) :: Nil => modifiedClass(classDecl)
        case _ => c.abort(c.enclosingPosition, "Invalid Annotee")
      }
  }
}
