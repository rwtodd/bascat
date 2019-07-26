import Dependencies._

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "org.rwtodd",
      scalaVersion := "2.13.0",
      version      := "1.0"
    )),
    name := "bascat",
    // libraryDependencies += argparse
  )
