import sbt.KeyRanks.ATask
import sbt.Keys.scalaVersion

ThisBuild / scalaVersion     := "2.12.7"
ThisBuild / version          := "2.0.x-SNAPSHOT"
ThisBuild / organization     := "io.guanaco.alerta"
ThisBuild / organizationName := "Guanaco"


val commonSettings = Seq(
)

def withCommonSettings(ss : sbt.Def.SettingsDefinition*) : Seq[sbt.Def.SettingsDefinition] = {
  commonSettings ++ ss
}

lazy val root = (project in file("."))
  .enablePlugins(ScalafmtPlugin, JavaAppPackaging, SbtOsgi)
  .settings(withCommonSettings(
    name := "alerta",
    libraryDependencies ++= Dependencies.tests
  ):_*)
  .aggregate(api, features, impl, test, util)


lazy val api = (project in file("api"))
  .enablePlugins(SbtOsgi)
  .settings(withCommonSettings(
    name := "api",
    description := "Alerta public API",
    libraryDependencies ++= Dependencies.api,
    osgiSettings,
    OsgiKeys.additionalHeaders := Map(
      "Bundle-Name" -> "Guanaco :: Alerta :: API",
    )
  ):_*)

val packageXml = taskKey[File]("Produces an xml artifact.").withRank(ATask)

lazy val features = (project in file("features"))
  .settings(withCommonSettings(
    name := "features",

    // disable .jar publishing
    publishArtifact in (Compile, packageBin) := false,
    publishArtifact in (Compile, packageDoc) := false,
    publishArtifact in (Compile, packageSrc) := false,
    packageXml := {
      val artifact: File = file("features/src/main/resources/features.xml")
      artifact
    },
    addArtifact( Artifact("features", "features", "xml"), packageXml ).settings
  ):_*)

lazy val impl = (project in file("impl"))
  .enablePlugins(SbtOsgi)
  .settings(withCommonSettings(
    name := "impl",
    packageName := "Guanaco :: Alerta :: Impl",
    description := "Camel routes from MQ to Alerta API",
    libraryDependencies ++= Dependencies.impl,
    parallelExecution in Test := false,
    osgiSettings,
    OsgiKeys.additionalHeaders := Map(
      "Bundle-Name" -> "Guanaco :: Alerta :: Implementation",
      //"Bundle-Blueprint" -> "OSGI-INF/blueprint/alerta-blueprint.xml"
    )
  ):_*)
  .dependsOn(api)

lazy val test = (project in file("test"))
  .settings(withCommonSettings(
    name := "test",
    description := "Utilities for unit testing your own alerta projects",
    libraryDependencies ++= Dependencies.test
  ):_*)
  .dependsOn(api, impl)

lazy val util = (project in file("util"))
  .settings(withCommonSettings(
    name := "util",
    libraryDependencies ++= Dependencies.util,
    parallelExecution in Test := false
  ):_*)
  .dependsOn(api, test)

scalacOptions += "-feature"
testOptions in Test := Seq(Tests.Argument(TestFrameworks.JUnit, "-a"))

fork in run := true
