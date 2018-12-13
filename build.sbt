import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType}

name := "stock"

ThisBuild / scalaVersion := "2.12.7"

lazy val backend = (project in file("backend"))
  .settings(
    name := "stock",
    libraryDependencies ++= Seq(
      "com.github.pathikrit" %% "better-files" % "3.6.0",
      "io.circe" %% "circe-core" % "0.10.0",
      "io.circe" %% "circe-generic" % "0.10.0",
      "io.circe" %% "circe-parser" % "0.10.0",
      "net.pishen" %% "minitime" % "0.2.0",
      "org.jsoup" % "jsoup" % "1.11.3",
      "org.rogach" %% "scallop" % "3.1.3",
      "org.scalaj" %% "scalaj-http" % "2.4.1"
    )
  )
  .dependsOn(sharedJVM)

lazy val frontend = (project in file("frontend"))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    name := "stock",
    scalaJSUseMainModuleInitializer := true,
    libraryDependencies ++= Seq(
      "com.lihaoyi" %%% "scalatags" % "0.6.7",
      "io.circe" %%% "circe-core" % "0.9.1",
      "io.circe" %%% "circe-generic" % "0.9.1",
      "io.circe" %%% "circe-java8" % "0.9.1",
      "io.circe" %%% "circe-parser" % "0.9.1",
      "net.pishen" %%% "akka-ui" % "0.4.0",
      "org.scala-js" %%% "scalajs-dom" % "0.9.2"
    )
  )
  .dependsOn(sharedJS)

lazy val shared = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("shared"))
lazy val sharedJVM = shared.jvm
lazy val sharedJS = shared.js
