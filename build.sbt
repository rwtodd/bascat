import Dependencies._

ThisBuild / scalaVersion := "2.13.3"
ThisBuild / version      := "1.0"
ThisBuild / organization := "org.rwtodd.bascat"

lazy val root = (project in file("."))
	.settings(
		scalacOptions ++= Seq("-target:11", "-deprecation"),
		name := "bascat"
	)

// vim: filetype=sbt:noet:tabstop=4:autoindent
