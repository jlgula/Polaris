name := "Polaris"

version := "0.1"

scalaVersion := "2.13.6"

scalacOptions += "-deprecation"

Compile / guardrailTasks := List(
  ScalaServer(file("polaris.yaml"), pkg="org.opendcgrid.app.polaris.server"),
  ScalaClient(file("polaris.yaml"), pkg="org.opendcgrid.app.polaris.client"),
  ScalaServer(file("notification.yaml"), pkg="org.opendcgrid.app.polaris.client"),
  ScalaClient(file("notification.yaml"), pkg="org.opendcgrid.app.polaris.server"),
)

val AkkaVersion = "2.6.15"
val AkkaHTTPVersion = "10.2.4"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor"        % AkkaVersion,
  "com.typesafe.akka" %% "akka-actor-typed"  % AkkaVersion,
  "com.typesafe.akka" %% "akka-stream"       % AkkaVersion,
  "com.typesafe.akka" %% "akka-testkit"      % AkkaVersion % Test,
  "com.typesafe.akka" %% "akka-actor-testkit-typed" % AkkaVersion % Test,
  "com.typesafe.akka" %% "akka-http"         % AkkaHTTPVersion,
  "com.typesafe.akka" %% "akka-http-testkit" % AkkaHTTPVersion,
  "io.circe"          %% "circe-core"        % "0.14.1",
  "io.circe"          %% "circe-generic"     % "0.14.1",
  "io.circe"          %% "circe-parser"      % "0.14.1",
  //  "javax.xml.bind"    %  "jaxb-api"          % "2.3.1",
  "org.scalatest"     %% "scalatest"         % "3.2.9" % Test,
  "org.typelevel"     %% "cats-core"         % "2.6.1",
  "org.slf4j"         % "slf4j-simple"       % "1.7.30",
  //"ch.qos.logback"    % "logback-classic"   % "1.2.5"
 )