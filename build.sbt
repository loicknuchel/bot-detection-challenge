val CatsVersion = "2.1.1"
val Fs2Version = "2.4.6"
val CirceVersion = "0.13.0"
val Http4sVersion = "0.21.11"
val AkkaVersion = "2.6.10"
val LogbackVersion = "1.2.3"
val ScalatestVersion = "3.2.2"
val ScalatestPlusVersion = "3.2.2.0"
val RandomDataGeneratorVersion = "2.9"

lazy val root = (project in file("."))
  .settings(
    organization := "fr.loicknuchel",
    name := "botless",
    version := "0.0.1-SNAPSHOT",
    scalaVersion := "2.13.4",
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-core" % CatsVersion,
      "co.fs2" %% "fs2-core" % Fs2Version,
      "co.fs2" %% "fs2-io" % Fs2Version,
      "io.circe" %% "circe-generic" % CirceVersion,
      "org.http4s" %% "http4s-blaze-server" % Http4sVersion,
      "org.http4s" %% "http4s-blaze-client" % Http4sVersion,
      "org.http4s" %% "http4s-circe" % Http4sVersion,
      "org.http4s" %% "http4s-dsl" % Http4sVersion,
      "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
      "ch.qos.logback" % "logback-classic" % LogbackVersion,
      "org.scalatest" %% "scalatest" % ScalatestVersion % Test,
      "org.scalatestplus" %% "scalacheck-1-14" % ScalatestPlusVersion % Test,
      "com.danielasfregola" %% "random-data-generator" % RandomDataGeneratorVersion % Test,
      "com.typesafe.akka" %% "akka-actor-testkit-typed" % AkkaVersion % Test),
    addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.10.3"),
    addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1")
  )

mainClass in(Compile, run) := Some("fr.loicknuchel.botless.Demo")
