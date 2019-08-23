// shadow sbt-scalajs' crossProject and CrossType from Scala.js 0.6.x
import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType}
import microsites._

resolvers ++= Seq (
  "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/",
  "Sonatype OSS Releases"  at "http://oss.sonatype.org/content/repositories/releases/",
  "Sonatype OSS Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/"
)

lazy val supportedScalaVersions = List("2.12.9", "2.13.0")

lazy val catsVersion = SettingKey[String]("catsVersion")

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
  scalaVersion := "2.12.9",
  organization := "io.typechecked",

  libraryDependencies ++= Seq(
    "com.chuusai" %%% "shapeless" % "2.3.3",
    "org.typelevel" %%% "cats-core" % catsVersion.value,
    "org.scalactic" %%% "scalactic" % "3.0.8",
    "org.scalatest" %%% "scalatest" % "3.0.8" % "test",
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
    "-Xlint:adapted-args",               // Warn if an argument list is modified to match the receiver.
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
    "-Ywarn-extra-implicit",             // Warn when more than one implicit parameter section is defined.
    "-Ywarn-unused:patvars",             // Warn if a variable bound in a pattern is unused.
    "-language:postfixOps",
    "-Ywarn-macros:after"
  ),

  Compile / scalacOptions ++= {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, n)) if n >= 13 => "-Ymacro-annotations" :: Nil
      case _ => "-Ypartial-unification" :: Nil
    }
  },

  libraryDependencies ++= {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, n)) if n >= 13 => Nil
      case _ => compilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full) :: Nil
    }
  },

  catsVersion := (scalaBinaryVersion.value match {
    case "2.12" => "1.6.1"
    case "2.13" => "2.0.0-RC1"
  })
)

import ReleaseTransformations._

val macros = crossProject(JVMPlatform, JSPlatform).crossType(CrossType.Pure).in(file("alphabet-soup-macros")).settings(
  name := "alphabet-soup-macros",
  commonSettings ++ publishingSettings,
  crossScalaVersions := supportedScalaVersions
)

// TODO: Put back in root directory?
val alphabetSoup = crossProject(JVMPlatform, JSPlatform).crossType(CrossType.Pure).in(file("alphabet-soup")).settings(
  name := "alphabet-soup",
  commonSettings ++ publishingSettings,
  crossScalaVersions := supportedScalaVersions
)

lazy val macroJVM = macros.jvm
lazy val alphabetSoupJVM = alphabetSoup.jvm.dependsOn(macroJVM)

lazy val macroJS = macros.js
lazy val alphabetSoupJS = alphabetSoup.js.dependsOn(macroJS)

lazy val docs = project.in(file("alphabet-soup-docs"))
  .settings(
    crossScalaVersions -= "2.13.0",
    skip.in(publish) := true,

    libraryDependencies ++= Seq(
      compilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full),
      "org.typelevel" %%% "cats-mtl-core" % "0.2.3"
    ),

    micrositeCompilingDocsTool := WithMdoc,
    mdocIn := tutSourceDirectory.value,
    micrositeName := "Alphabet Soup",
    micrositeDescription := "Typelevel Transformations",
    micrositeUrl := "http://alphabet-soup.typechecked.io",
    micrositeGithubOwner := "typechecked",
    micrositeGithubRepo := "alphabet-soup",
    micrositeAuthor := "TypeChecked",
    micrositePalette := Map(
      "brand-primary" -> "#DF301D",
      "brand-secondary" -> "#1B1D21",
      "brand-tertiary" -> "#12181f",
      "gray-dark" -> "#49494B",
      "gray" -> "#7B7B7E",
      "gray-light" -> "#E5E5E6",
      "gray-lighter" -> "#F4F3F4",
      "white-color" -> "#FFFFFF"
    ),
    micrositeDocumentationUrl := "/docs",
    micrositeDocumentationLabelDescription := "Documentation",
    micrositeShareOnSocial := false,
    micrositeGitterChannel := false,
    mdocVariables := Map(
      "VERSION" -> version.value
    ),
    excludeFilter in ghpagesCleanSite :=
      new FileFilter{
        def accept(f: File) = (ghpagesRepository.value / "CNAME").getCanonicalPath == f.getCanonicalPath
      } || "versions.html"
  )
  .enablePlugins(MicrositesPlugin, MdocPlugin)
  .dependsOn(alphabetSoupJVM, macroJVM)

lazy val alphabetSoupParent = project.in(file(".")).aggregate(macroJVM, macroJS, alphabetSoupJVM, alphabetSoupJS, docs).settings(
  name := "alphabet-soup-parent",
  commonSettings,

  // crossScalaVersions must be set to Nil on the aggregating project
  crossScalaVersions := Nil,

  publish / skip := true,

  // don't use sbt-release's cross facility
  releaseCrossBuild := false,
  releaseProcess := Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    runClean,
    releaseStepCommandAndRemaining("+test"),
    setReleaseVersion,
    commitReleaseVersion,
    tagRelease,
    releaseStepCommandAndRemaining("+publishSigned"),
    setNextVersion,
    commitNextVersion,
    pushChanges
  ),

  PgpKeys.publishSigned := {},
  PgpKeys.publishLocalSigned := {},
  publishLocal := {},
  publishArtifact in Compile := false,
  publish := {}
)
