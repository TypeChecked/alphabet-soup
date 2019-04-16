// shadow sbt-scalajs' crossProject and CrossType from Scala.js 0.6.x
import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType}

resolvers ++= Seq (
  "Maven Central Server" at "http://repo1.maven.org/maven2",
  "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/",
  "Sonatype OSS Releases"  at "http://oss.sonatype.org/content/repositories/releases/",
  "Sonatype OSS Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/"
)

val paradiseVersion = "2.1.0"

lazy val publishingSettings = Seq (

  credentials += Credentials(Path.userHome / ".sbt" / "sonatype_credential"),

  useGpg := true,
  ThisBuild / organization := "io.typechecked",
  ThisBuild / organizationName := "alphabet-soup",
  ThisBuild / organizationHomepage := Some(url("http://typechecked.io")),

  ThisBuild / scmInfo := Some(
    ScmInfo(
      url("https://github.com/TypeChecked/alphabet-soup"),
      "scm:git@github.com:TypeChecked/alphabet-soup.git"
    )
  ),

  ThisBuild / developers := List(
    Developer(
      id    = "jdrphillips",
      name  = "James Phillips",
      email = "me@jdrphillips.io",
      url   = url("http://jdrphillips.io")
    )
  ),

  ThisBuild / licenses := List("Apache 2" -> new URL("http://www.apache.org/licenses/LICENSE-2.0.txt")),
  ThisBuild / homepage := Some(url("https://github.com/TypeChecked/alphabet-soup")),

  // Remove all additional repository other than Maven Central from POM
  ThisBuild / pomIncludeRepository := { _ => false },
  ThisBuild / publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value) Some("snapshots" at nexus + "content/repositories/snapshots")
    else Some("releases" at nexus + "service/local/staging/deploy/maven2")
  },
  ThisBuild / publishMavenStyle := true,

  // sbt-release plugin settings
  releaseUseGlobalVersion := true,
  releasePublishArtifactsAction := PgpKeys.publishSigned.value,

)

lazy val commonSettings = Seq(
  scalaVersion := "2.12.8",
  organization := "io.typechecked",
  addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.9"),
  addCompilerPlugin("org.scalamacros" % "paradise" % paradiseVersion cross CrossVersion.full),

  libraryDependencies ++= Seq(
    "com.chuusai" %%% "shapeless" % "2.3.3",
    "org.typelevel" %%% "cats-core" % "1.6.0",
    "org.scalactic" %%% "scalactic" % "3.0.5",
    "org.scalatest" %%% "scalatest" % "3.0.5" % "test",
  ),
  scalacOptions := Seq(
    "-deprecation",                      // Emit warning and location for usages of deprecated APIs.
    "-encoding", "utf-8",                // Specify character encoding used by source files.
    "-explaintypes",                     // Explain type errors in more detail.
    "-feature",                          // Emit warning and location for usages of features that should be imported explicitly.
    "-language:existentials",            // Existential types (besides wildcard types) can be written and inferred
    "-language:experimental.macros",     // Allow macro definition (besides implementation and application)
    "-language:higherKinds",             // Allow higher-kinded types
    "-language:implicitConversions",     // Allow definition of implicit functions called views
    "-unchecked",                        // Enable additional warnings where generated code depends on assumptions.
    "-Xcheckinit",                       // Wrap field accessors to throw an exception on uninitialized access.
    "-Xfuture",                          // Turn on future language features.
    "-Xlint:adapted-args",               // Warn if an argument list is modified to match the receiver.
    "-Xlint:by-name-right-associative",  // By-name parameter of right associative operator.
    "-Xlint:constant",                   // Evaluation of a constant arithmetic expression results in an error.
    "-Xlint:delayedinit-select",         // Selecting member of DelayedInit.
    "-Xlint:doc-detached",               // A Scaladoc comment appears to be detached from its element.
    "-Xlint:inaccessible",               // Warn about inaccessible types in method signatures.
    "-Xlint:infer-any",                  // Warn when a type argument is inferred to be `Any`.
    "-Xlint:missing-interpolator",       // A string literal appears to be missing an interpolator id.
    "-Xlint:nullary-override",           // Warn when non-nullary `def f()' overrides nullary `def f'.
    "-Xlint:option-implicit",            // Option.apply used implicit view.
    "-Xlint:package-object-classes",     // Class or object defined in package object.
    "-Xlint:poly-implicit-overload",     // Parameterized overloaded implicit methods are not visible as view bounds.
    "-Xlint:private-shadow",             // A private field (or class parameter) shadows a superclass field.
    "-Xlint:stars-align",                // Pattern sequence wildcard must align with sequence component.
    "-Xlint:unsound-match",              // Pattern match may not be typesafe.
    "-Yno-adapted-args",                 // Do not adapt an argument list (either by inserting () or creating a tuple) to match the receiver.
    "-Ypartial-unification",             // Enable partial unification in type constructor inference
    "-Ywarn-extra-implicit",             // Warn when more than one implicit parameter section is defined.
    "-Ywarn-inaccessible",               // Warn about inaccessible types in method signatures.
    "-Ywarn-infer-any",                  // Warn when a type argument is inferred to be `Any`.
    "-Ywarn-nullary-override",           // Warn when non-nullary `def f()' overrides nullary `def f'.
    "-Ywarn-unused:patvars",             // Warn if a variable bound in a pattern is unused.
    "-language:postfixOps"
  )
)

val macros = crossProject(JVMPlatform, JSPlatform).crossType(CrossType.Pure).in(file("alphabet-soup-macros")).settings(
  name := "alphabet-soup-macros",
  commonSettings ++ publishingSettings
)

// TODO: Put back in root directory?
val alphabetSoup = crossProject(JVMPlatform, JSPlatform).crossType(CrossType.Pure).in(file("alphabet-soup")).settings(
  name := "alphabet-soup",
  commonSettings ++ publishingSettings
)

lazy val macroJVM = macros.jvm
lazy val alphabetSoupJVM = alphabetSoup.jvm.dependsOn(macroJVM)

lazy val macroJS = macros.js
lazy val alphabetSoupJS = alphabetSoup.js.dependsOn(macroJS)

lazy val alphabetSoupParent = project.in(file(".")).aggregate(macroJVM, macroJS, alphabetSoupJVM, alphabetSoupJS).settings(
  name := "alphabet-soup-parent",
  commonSettings,
  PgpKeys.publishSigned := {},
  PgpKeys.publishLocalSigned := {},
  publishLocal := {},
  publishArtifact in Compile := false,
  publish := {}
)
