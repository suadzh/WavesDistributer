import org.apache.ivy.core.module.descriptor.ExcludeRule
val scala3Version = "3.1.2"

lazy val root = project
  .in(file("."))
  .enablePlugins(NativeImagePlugin)
  .settings(
    assembly / mainClass := Some("Main"),
    Compile / mainClass := Some("Main"),
    nativeImageOptions += s"-H:ReflectionConfigurationFiles=${target.value / "native-image-configs" / "reflect-config.json"}",
    nativeImageOptions += s"-H:ConfigurationFileDirectories=${target.value / "native-image-configs" }",
    nativeImageOptions +="-H:+JNI",
    nativeImageInstalled := true,
    nativeImageOutput := file("build") / "distributer",
    name := "distributer",
    version := "0.1.1",
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