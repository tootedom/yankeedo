/**
 * Copyright 2012-2013 greencheek.org (www.greencheek.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.greencheek.app.compilation.zinc

import java.io.{ File => JFile }
import java.net.URLClassLoader
import sbt.inc.IncOptions
import scala.tools.nsc.io.{ Directory, Path }
import scala.tools.nsc.io.Path.{ jfile2path, string2path }

import com.typesafe.zinc.{ Compiler, Inputs, Setup }

import grizzled.slf4j.Logging
import xsbti.Logger
import xsbti.api.Compilation
import xsbti.compile.CompileOrder
import scala.Some

import org.greencheek.yankeedo.app.compilation.findjar.{FindMatchingJar, ClassPathOrShadeMatchingJarFinder}
import org.greencheek.yankeedo.app.ConfigConstants._

/**
 * This file was taken and modified from the Gatling tool source code:
 * http://gatling-tool.org/
 */
object ZincCompiler extends Logging {

  def jarFinder : FindMatchingJar =  new ClassPathOrShadeMatchingJarFinder

	def apply(sourceDirectory: Directory): Directory = {

		val classpathURLs = Thread.currentThread.getContextClassLoader.asInstanceOf[URLClassLoader].getURLs

		def simulationInputs(sourceDirectory: Directory, binDir: Path) = {
			val classpath = classpathURLs.map(url => new JFile(url.toURI))
			val sources = sourceDirectory
				.deepFiles
				.toList
				.collect { case file if (file.hasExtension("scala")) => file.jfile }

			def analysisCacheMapEntry(directoryName: String) = (YANKEEDO_HOME / directoryName).jfile -> (binDir / "cache" / directoryName).jfile

      Inputs.inputs(classpath = classpath,
        sources = sources,
        classesDirectory = (binDir / "classes").jfile,
        scalacOptions = Seq("-encoding", "UTF8", "-target:jvm-1.6", "-deprecation", "-feature", "-unchecked", "-language:implicitConversions", "-language:reflectiveCalls", "-language:postfixOps"),
        javacOptions = Nil,
        analysisCache = Some((binDir / "zincCache").jfile),
        analysisCacheMap = Map(analysisCacheMapEntry("bin"), analysisCacheMapEntry("conf"), analysisCacheMapEntry("user-files")), // avoids having GATLING_HOME polluted with a "cache" folder
        forceClean = false,
        javaOnly = false,
        compileOrder = CompileOrder.JavaThenScala,
	incOptions = IncOptions.Default,
        outputRelations = None,
        outputProducts = None,
	mirrorAnalysis = false)
    }

		def setupZincCompiler(): Setup = {


			val scalaCompiler = jarFinder.findJarMatching("""(.*scala-compiler-\d+\.\d+\.\d+.*\.jar)$""")
			val scalaLibrary = jarFinder.findJarMatching( """(.*scala-library-\d+\.\d+\.\d+.*\.jar)$""")
      val scalaReflect = jarFinder.findJarMatching("""(.*scala-reflect-.*\.jar)$""")
			val sbtInterfaceSrc: JFile = new JFile(classOf[Compilation].getProtectionDomain.getCodeSource.getLocation.toURI)
      val compilerInterfaceSrc: JFile = jarFinder.findJarMatching("""(.*compiler-interface-.*-sources.jar)$""")

			Setup.setup(scalaCompiler = scalaCompiler,
				scalaLibrary = scalaLibrary,
        scalaExtra = List(scalaReflect),
				sbtInterface = sbtInterfaceSrc,
				compilerInterfaceSrc = compilerInterfaceSrc,
				javaHomeDir = None)
		}

		// Setup the compiler
		val setup = setupZincCompiler
		val zincLogger = new ZincLogger
		val zincCompiler = Compiler.create(setup, zincLogger)

		val binDir = YANKEEDO_HOME / "target"

		// Define the inputs
		val inputs = simulationInputs(sourceDirectory, binDir)
		Inputs.debug(inputs, zincLogger)

		zincCompiler.compile(inputs)(zincLogger)

		Directory(inputs.classesDirectory)
	}

	class ZincLogger extends Logger {
		def error(arg: xsbti.F0[String]) { logger.error(arg.apply) }
		def warn(arg: xsbti.F0[String]) { logger.warn(arg.apply) }
		def info(arg: xsbti.F0[String]) { logger.info(arg.apply) }
		def debug(arg: xsbti.F0[String]) { logger.debug(arg.apply) }
		def trace(arg: xsbti.F0[Throwable]) { logger.trace(arg.apply) }
	}
}
