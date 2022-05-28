import org.apache.ivy.core.module.descriptor.ExcludeRule
val scala3Version = "3.1.2"

lazy val root = project
  .in(file("."))
  .settings(
    assembly / mainClass := Some("Main"),
    name := "distributer",
    version := "0.1.0",
    assemblyMergeStrategy in assembly := {
     case PathList("META-INF", xs @ _*) => MergeStrategy.discard
     case x => MergeStrategy.first
    },

    scalaVersion := scala3Version,

    libraryDependencies ++= Seq(
      ("com.wavesplatform" % "wavesj" % "1.2.4"),
      "io.monix" %% "monix" % "3.4.1"
    )
  )
