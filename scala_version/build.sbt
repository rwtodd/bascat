
lazy val commonSettings = Seq(
  organization := "com.waywardcode",
  version := "1.0",
  scalaVersion := "2.11.8"
)


lazy val bascat = (project in file(".")).
  settings(commonSettings: _*).
  settings(
    name := "bascat"
  )
