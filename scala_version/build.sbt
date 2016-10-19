
lazy val commonSettings = Seq(
  organization := "com.waywardcode",
  version := "1.0",
  scalaVersion := "2.12.0-RC2",
  scalacOptions += "-opt:l:classpath" 
)


lazy val bascat = (project in file(".")).
  settings(commonSettings: _*).
  settings(
    name := "bascat"
  )
