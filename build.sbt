import sbt.KeyRanks.ATask
import sbt.file

lazy val scala212 = "2.12.7"
lazy val scala211 = "2.11.7"
lazy val supportedScalaVersions = List(scala212, scala211)

ThisBuild / scalaVersion           := scala212
ThisBuild / version                := "2.0.5"
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
    OsgiKeys.exportPackage := List(OsgiKeys.bundleSymbolicName.value),
    OsgiKeys.privatePackage := Nil,
    OsgiKeys.additionalHeaders := Map(
      "Bundle-Name" -> "Guanaco :: Alerta :: API",
    ),
  )

val packageXml = taskKey[File]("Produces an xml artifact.").withRank(ATask)
val generateFeatures = taskKey[Unit]("Generates the features files.")

lazy val features = (project in file("features"))
  .settings(commonSettings)
  .settings(
    generateFeatures := {
      streams.value.log.info("Generating features.xml files")
      val input = (resourceDirectory in Compile).value / "features.xml"
      val output = file("features") / "target" / "features.xml"
      IO.write(output, IO.read(input).replaceAll("\\$\\{version\\}", version.value))
    },

    publishM2 := (publishM2 dependsOn generateFeatures).value,
    publish := (publish dependsOn generateFeatures).value,

    name := "features",
    crossScalaVersions := supportedScalaVersions,

    // disable .jar publishing
    publishArtifact in (Compile, packageBin) := false,
    publishArtifact in (Compile, packageDoc) := false,
    publishArtifact in (Compile, packageSrc) := false,
    packageXml := file("features") / "target" / "features.xml",
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
    OsgiKeys.importPackage := List("*", "org.apache.activemq.camel.component"),
    OsgiKeys.privatePackage := List(s"${OsgiKeys.bundleSymbolicName.value}.*", "spray.json"),
    OsgiKeys.additionalHeaders := Map(
      "Bundle-Name" -> "Guanaco :: Alerta :: Implementation"
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
