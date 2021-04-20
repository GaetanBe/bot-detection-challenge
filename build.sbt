
name := "bot-detection-challenge"

version := "0.1"

scalaVersion := "2.13.5"

idePackagePrefix := Some("com.gaetan.bervet")

val circeVersion: String = "0.12.3"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "com.typesafe.akka" %% "akka-http" % "10.2.4",
  "com.typesafe.akka" %% "akka-stream" % "2.6.14",
  "de.heikoseeberger" %% "akka-http-circe" % "1.36.0",

  "org.scalatest" %% "scalatest" % "3.2.7" % Test
)

scalacOptions ++= Seq(
  "-target:jvm-1.8",
  "-deprecation",
  "-encoding", "UTF-8",
  "-feature", // More verbose warnings
  "-Xlint",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Ywarn-unused",
)

// Define the Dockerfile
enablePlugins(DockerPlugin)

docker / imageNames := Seq(
  ImageName(
    namespace = Some(organization.value),
    repository = name.value,
    tag = Some("v" + version.value)
  )
)

docker / dockerfile := {
  val jarFile: File = Keys.`package`.in(Compile, packageBin).value
  val classpath: Classpath = (Compile / managedClasspath).value
  val libs: String = "/app/libs"
  val jarTargetFolder: String = "/app/" + jarFile.name

  new Dockerfile {
    // Use a base image that contains Java
    from(image = "openjdk:8u171-jre-alpine3.8")

    // Copy all dependencies to 'libs' in the staging directory
    classpath.files.foreach { dependencyFile =>
      val targetFilepath: File = file(libs) / dependencyFile.name
      stageFile(dependencyFile, targetFilepath)
    }
    // Add the libs dir
    copyRaw(libs, libs)

    // Add the generated jar file
    copy(jarFile, jarTargetFolder)
    // The classpath is the 'libs' dir and the produced jar file
    val classpathString: String = s"$libs/*:$jarTargetFolder"

    val javaCommand: String = Seq(
      "java",
      /* Flags "UnlockExperimentalVMOptions" and "UseCGroupMemoryLimitForHeap" are used to
         tell the JVM to be aware of Docker memory limits in the absence of setting a maximum Java heap via -Xmx.
         Cf https://blogs.oracle.com/java-platform-group/java-se-support-for-docker-cpu-and-memory-limits
      */
      "-XX:+UnlockExperimentalVMOptions",
      "-XX:+UseCGroupMemoryLimitForHeap",
      /* -XX:MaxRAMFraction=n specifies that the JVM will use a maximum of 1/n of total RAM as its heap size.
          The default is set to n=4, so with 600MB RAM the JVM will use 1/4*600 = 150M as the maximum heap size.
          Because the JVM also accounts for other overhead, setting n=1 will configure
          a maximum heap size of slightly less than 600MB.
       */
      "-XX:MaxRAMFraction=2",
      // Display JVM Settings
      "-XshowSettings:vm",
      "-cp",
      classpathString,
      "com.gaetan.bervet.BotDetection"
    ).mkString(" ")

    /* Set the command to start the application using the main class.

       NB: CMD [ "echo", "$HOME" ] will not do variable substitution on $HOME.
       We need to write CMD [ "sh", "-c", "echo $HOME" ] to do variable substitution.
       Cf https://docs.docker.com/engine/reference/builder/#cmd
     */
    cmd(
      args = "sh",
      "-c",
      javaCommand
    )
  }
}
