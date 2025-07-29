

scalaVersion := "2.13.12"

scalacOptions ++= Seq(
  "-feature",
  "-language:reflectiveCalls",
)

libraryDependencies ++= Seq("org.chipsalliance" %% "chisel" % "6.5.0")
addCompilerPlugin("org.chipsalliance" % "chisel-plugin" % "6.5.0" cross CrossVersion.full)

libraryDependencies ++= Seq(
    "org.scalatest" %% "scalatest" % "3.2.+" % "test"
  )
