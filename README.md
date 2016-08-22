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
  2. Run `gradle genEclipseRunConfigurations`
  3. Import Pingy as an existing project (File > Import > General)
  4. Select the root folder for Pingy
  5. Check Pingy when it finishes building and click **Finish**

__For [IntelliJ]__
  1. Make sure you have the Gradle plugin enabled (File > Settings > Plugins)
  2. Click File > New > Project from Existing Sources > Gradle and select the root folder for Pingy
  3. Select Use customizable gradle wrapper if you do not have Gradle installed.
  4. Once the project is loaded, run `gradle genIntelliJRunConfigurations`
  5. IntelliJ will now ask to reload the project, click **Yes**

[Eclipse]: https://eclipse.org/
[Gradle]: https://www.gradle.org/
[IntelliJ]: http://www.jetbrains.com/idea/
[Source]: https://github.com/LanternPowered/Pingy
[Java 8]: http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html
[Issues]: https://github.com/LanternPowered/Pingy/issues
[Wiki]: https://github.com/LanternPowered/Pingy/wiki
[MIT License]: https://www.tldrlegal.com/license/mit-license
