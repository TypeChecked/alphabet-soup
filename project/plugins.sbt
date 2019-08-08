resolvers ++= Seq("Maven Central Server" at "http://repo1.maven.org/maven2")

addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.11")
addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.28")
addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "0.6.0")
addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.1.2")
addSbtPlugin("com.47deg" % "sbt-microsites" % "0.9.2")
addSbtPlugin("org.scalameta" % "sbt-mdoc" % "1.3.1" )
