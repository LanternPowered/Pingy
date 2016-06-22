# Pingy [![Build Status](https://travis-ci.org/LanternPowered/Pingy.svg?branch=master)](https://travis-ci.org/LanternPowered/Pingy)

A minimal Minecraft ping server. It is licensed under the [MIT License].

* [Source]
* [Issues]
* [Wiki]

## Prerequisites
* [Java 8]

## Building
__Note:__ If you do not have [Gradle] installed then use ./gradlew for Unix systems or Git Bash and gradlew.bat for Windows systems in place of any 'gradle' command.

In order to build Pingy you simply need to run the `gradle build` command. You can find the compiled JAR file in `./build/libs` labeled similarly to 'pingy-x.x.x-SNAPSHOT.jar'.

## IDE Setup
__Note:__ If you do not have [Gradle] installed then use ./gradlew for Unix systems or Git Bash and gradlew.bat for Windows systems in place of any 'gradle' command.

__For [Eclipse]__
  1. Run `gradle eclipse`
  2. Import Pingy as an existing project (File > Import > General)
  3. Select the root folder for Pingy
  4. Check Pingy when it finishes building and click **Finish**

__For [IntelliJ]__
  1. Run `gradle idea`
  2. Import Pingy as an existing project (File > Open)
  3. Select the root folder for Pingy and click **Ok**

[Eclipse]: https://eclipse.org/
[Gradle]: https://www.gradle.org/
[IntelliJ]: http://www.jetbrains.com/idea/
[Source]: https://github.com/LanternPowered/Pingy
[Java 8]: http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html
[Issues]: https://github.com/LanternPowered/Pingy/Issues
[Wiki]: https://github.com/LanternPowered/Pingy/Wiki
[MIT License]: https://www.tldrlegal.com/license/mit-license
