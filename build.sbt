import sbt.KeyRanks.ATask

lazy val scala212 = "2.12.7"
lazy val scala211 = "2.11.7"
lazy val supportedScalaVersions = List(scala212, scala211)

ThisBuild / scalaVersion           := scala212
ThisBuild / version                := "2.0.2"
ThisBuild / organization           := "io.guanaco.alerta"
ThisBuild / organizationName       := "Guanaco"

val commonSettings = Seq(
  publishMavenStyle      := true,
  bintrayCredentialsFile := Path.userHome / ".bintray" / ".credentials",
  bintrayOrganization    := Some("guanaco-io"),
  bintrayRepository      := "maven",
  bintrayOmitLicense     := true
)

lazy val root = (project in file("."))
  .enablePlugins(ScalafmtPlugin, JavaAppPackaging, SbtOsgi)
  .settings(commonSettings)
  .settings(
    name := "alerta",
    libraryDependencies ++= Dependencies.tests,
    crossScalaVersions := Nil,
    publish / skip := true
  )
  .aggregate(api, features, impl, test, util)


lazy val api = (project in file("api"))
  .enablePlugins(SbtOsgi)
  .settings(commonSettings)
  .settings(
    name := "api",
    description := "Alerta public API",
    libraryDependencies ++= Dependencies.api,
    crossScalaVersions := supportedScalaVersions,
    osgiSettings,
    OsgiKeys.additionalHeaders := Map(
      "Bundle-Name" -> "Guanaco :: Alerta :: API",
    ),
  )

val packageXml = taskKey[File]("Produces an xml artifact.").withRank(ATask)

lazy val features = (project in file("features"))
  .settings(commonSettings)
  .settings(
    name := "features",
    crossScalaVersions := supportedScalaVersions,

    // disable .jar publishing
    publishArtifact in (Compile, packageBin) := false,
    publishArtifact in (Compile, packageDoc) := false,
    publishArtifact in (Compile, packageSrc) := false,
    packageXml := {
      val artifact: File = file("features/src/main/resources/features.xml")
      artifact
    },
    addArtifact( Artifact("features", "features", "xml"), packageXml ).settings
  )

lazy val impl = (project in file("impl"))
  .enablePlugins(SbtOsgi)
  .settings(commonSettings)
  .settings(
    name := "impl",
    description := "Camel routes from MQ to Alerta API",
    crossScalaVersions := supportedScalaVersions,
    libraryDependencies ++= Dependencies.impl,
    parallelExecution in Test := false,
    osgiSettings,
    OsgiKeys.additionalHeaders := Map(
      "Bundle-Name" -> "Guanaco :: Alerta :: Implementation",
      //"Bundle-Blueprint" -> "OSGI-INF/blueprint/alerta-blueprint.xml"
    )
  )
  .dependsOn(api)

lazy val test = (project in file("test"))
  .settings(commonSettings)
  .settings(
    name := "test",
    description := "Utilities for unit testing your own alerta projects",
    libraryDependencies ++= Dependencies.test,
    crossScalaVersions := supportedScalaVersions
  )
  .dependsOn(api, impl)

lazy val util = (project in file("util"))
  .settings(commonSettings)
  .settings(
    name := "util",
    libraryDependencies ++= Dependencies.util,
    parallelExecution in Test := false,
    crossScalaVersions := supportedScalaVersions
  )
  .dependsOn(api, test)

scalacOptions += "-feature"
testOptions in Test := Seq(Tests.Argument(TestFrameworks.JUnit, "-a"))

fork in run := true
