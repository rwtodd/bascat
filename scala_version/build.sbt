
lazy val commonSettings = Seq(
  organization := "com.waywardcode",
  version := "1.0",
  scalaVersion := "2.12.0-RC1"
)


lazy val bascat = (project in file(".")).
  settings(commonSettings: _*).
  settings(
    name := "bascat"
  )
