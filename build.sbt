name := "Polaris"

version := "0.1"

scalaVersion := "2.13.6"

Compile / guardrailTasks := List(
  ScalaServer(file("polaris.yaml"), pkg="org.opendcgrid.app.polaris"),
  ScalaClient(file("polaris.yaml"), pkg="org.opendcgrid.app.pclient"),
)

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor"        % "2.6.15",
  "com.typesafe.akka" %% "akka-stream"       % "2.6.15",
  "com.typesafe.akka" %% "akka-http"         % "10.2.4",
  "com.typesafe.akka" %% "akka-http-testkit" % "10.2.4",
  "io.circe"          %% "circe-core"        % "0.14.1",
  "io.circe"          %% "circe-generic"     % "0.14.1",
  "io.circe"          %% "circe-parser"      % "0.14.1",
  //  "javax.xml.bind"    %  "jaxb-api"          % "2.3.1",
  "org.scalatest"     %% "scalatest"         % "3.2.9" % Test,
  "org.typelevel"     %% "cats-core"         % "2.6.1",
)