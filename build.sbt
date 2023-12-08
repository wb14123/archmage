
import xerial.sbt.Sonatype.autoImport.sonatypePublishToBundle

ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "2.13.12"
ThisBuild / organization := "me.binwang.archmage"

ThisBuild / publishTo := sonatypePublishToBundle.value
ThisBuild / sonatypeCredentialHost := "s01.oss.sonatype.org"
ThisBuild / sonatypeRepository := "https://s01.oss.sonatype.org/service/local"
ThisBuild / licenses := Seq("AGPL" -> url("https://github.com/wb14123/archmage/blob/master/LICENSE"))
ThisBuild / homepage := Some(url("https://github.com/wb14123/archmage"))
ThisBuild / scmInfo := Some(ScmInfo(
  url("https://github.com/wb14123/archmage"),
  "scm:https://github.com/wb14123/archmage.git"
))
ThisBuild / developers := List(
  Developer(id="wb14123", name="Bin Wang", email="bin.wang@mail.binwang.me", url=url("https://www.binwang.me"))
)


lazy val root = (project in file("."))
  .aggregate(core, coretest)
  .settings(
    name := "archmage"
  )

lazy val core = (project in file("core"))
  .settings(
    name := "core",
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-reflect" % "2.13.12",
      "co.fs2" %% "fs2-core" % "3.9.3",
    )
  )

lazy val coretest = (project in file("coretest"))
  .settings(
    name := "core-test"
  ) dependsOn core

ThisBuild / scalacOptions += "-Ymacro-debug-lite"