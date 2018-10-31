name := "forex"
version := "1.0.0"

scalaVersion := "2.12.7"

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding",
  "UTF-8",
  "-feature",
  "-language:existentials",
  "-language:higherKinds",
  "-language:experimental.macros",
  "-language:implicitConversions"
)

resolvers +=
  "Sonatype OSS Snapshots".at("https://oss.sonatype.org/content/repositories/snapshots")

libraryDependencies ++= Seq(
  "com.github.pureconfig"      %% "pureconfig"                     % "0.9.2",
  "com.softwaremill.quicklens" %% "quicklens"                      % "1.4.11",
  "com.typesafe.akka"          %% "akka-actor"                     % "2.5.17",
  "com.typesafe.akka"          %% "akka-http"                      % "10.1.5",
  "de.heikoseeberger"          %% "akka-http-circe"                % "1.22.0",
  "io.circe"                   %% "circe-core"                     % "0.10.0",
  "io.circe"                   %% "circe-generic"                  % "0.10.0",
  "io.circe"                   %% "circe-generic-extras"           % "0.10.0",
  "io.circe"                   %% "circe-java8"                    % "0.10.0",
  "io.circe"                   %% "circe-jawn"                     % "0.10.0",
  "io.circe"                   %% "circe-optics"                   % "0.10.0",
  "org.typelevel"              %% "cats-core"                      % "1.4.0",
  "org.zalando"                %% "grafter"                        % "2.6.1",
  "ch.qos.logback"             % "logback-classic"                 % "1.2.3",
  "co.fs2"                     %% "fs2-core"                       % "1.0.0",
  "co.fs2"                     %% "fs2-reactive-streams"           % "1.0.0",
  "com.typesafe.scala-logging" %% "scala-logging"                  % "3.9.0",
  "com.softwaremill.sttp"      %% "core"                           % "1.3.9",
  "com.softwaremill.sttp"      %% "circe"                          % "1.3.9",
  "com.beachape"               %% "enumeratum"                     % "1.5.13",
  "com.typesafe.akka"          %% "akka-stream"                    % "2.5.16",
  "com.typesafe.akka"          %% "akka-slf4j"                     % "2.5.16",
  "com.github.cb372"           %% "scalacache-core"                % "0.26.0",
  "com.github.cb372"           %% "scalacache-cats-effect"         % "0.26.0",
  "com.github.cb372"           %% "scalacache-circe"               % "0.26.0",
  "com.github.cb372"           %% "scalacache-cache2k"             % "0.26.0",
  "com.softwaremill.sttp"      %% "async-http-client-backend-cats" % "1.3.9",
  "org.scalatest"              %% "scalatest"                      % "3.0.5" % Test,
  compilerPlugin(("org.scalamacros" %% "paradise" % "2.1.1").cross(CrossVersion.full))
)

cancelable in Global := true
fork in run := true

maxErrors := 5
triggeredMessage := Watched.clearWhenTriggered
