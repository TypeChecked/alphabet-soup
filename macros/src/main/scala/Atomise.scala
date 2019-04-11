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

    val result = {
      annottees.map(_.tree).toList match {
        case q"case class $className(..$fields){ ..$body }" :: Nil => {
          q""" 
             case class $className(..$fields){ ..$body }
             implicit val ${className.toTermName + "_atom"}: Atom[$className] = Atom[$className]
          """
        }
        case _ => c.abort(c.enclosingPosition, "Annotation @Atomic can be used only with case classes")
      }
    }
    c.Expr[Any](result)
  }
}