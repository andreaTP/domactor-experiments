enablePlugins(ScalaJSPlugin)

name := "todo"

scalaVersion := "2.11.8"
scalacOptions := Seq("-feature", "-language:_", "-deprecation")

resolvers += Resolver.sonatypeRepo("releases")

libraryDependencies ++= Seq(
  "eu.unicredit" %%% "akkajsactor" % "0.2.0",
  //"org.scala-js" %%% "scalajs-dom" % "0.9.0",
  "com.lihaoyi" %%% "scalatags" % "0.6.0"
)

jsDependencies +=
  "org.webjars.bower" % "diff-dom" % "2.0.2" / "diffDOM.js"

persistLauncher in Compile := true

scalaJSStage in Global := FastOptStage

scalaJSUseRhino in Global := false

skip in packageJSDependencies := false
