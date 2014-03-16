/**
 * Copyright 2012-2014 greencheek.org (www.greencheek.org)
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
/**
 * This file was taken and modified from the Gatling tool source code:
 * http://gatling-tool.org/
 */
package org.greencheek.app.compilation.scenario

import java.lang.reflect.Modifier

import scala.tools.nsc.interpreter.AbstractFileClassLoader
import scala.tools.nsc.io.{ Directory, File, Path }
import scala.tools.nsc.io.Path.string2path
import scala.tools.nsc.io.PlainFile


import grizzled.slf4j.Logging
import org.greencheek.app.compilation.zinc.ZincCompiler
import org.greencheek.jms.yankeedo.structure.scenario.ScenarioContainer


object ScenarioClassLoader extends Logging {

	def fromSourcesDirectory(sourceDirectory: Directory): ScenarioClassLoader = {

		// Compile the classes
		val classesDir = ZincCompiler(sourceDirectory)

		// Load the compiled classes
		val byteCodeDir = PlainFile.fromPath(classesDir)
		val classLoader = new AbstractFileClassLoader(byteCodeDir, getClass.getClassLoader)

		new FileSystemBackedScenarioClassLoader(classLoader, classesDir)
	}

	def fromClasspathBinariesDirectory(binariesDirectory: Directory) = new FileSystemBackedScenarioClassLoader(getClass.getClassLoader, binariesDirectory)
}

abstract class ScenarioClassLoader {

	def simulationClasses(requestedClassName: Option[String]): List[Class[ScenarioContainer]]

	protected def isSimulationClass(clazz: Class[_]): Boolean = classOf[ScenarioContainer].isAssignableFrom(clazz) && !clazz.isInterface && !Modifier.isAbstract(clazz.getModifiers)
}

class FileSystemBackedScenarioClassLoader(classLoader: ClassLoader, binaryDir: Directory) extends ScenarioClassLoader with Logging {

	private def pathToClassName(path: Path, root: Path): String = (path.parent / path.stripExtension)
		.toString
		.stripPrefix(root + File.separator)
		.replace(File.separator, ".")

	def simulationClasses(requestedClassName: Option[String]): List[Class[ScenarioContainer]] = {

		val classNames = requestedClassName match {
      case None =>  {
        binaryDir
          .deepFiles
          .collect { case file if (file.hasExtension("class")) => pathToClassName(file, binaryDir) }
          .toList
      }
      case Some(clazz : String) => {
        clazz :: binaryDir
          .deepFiles
          .collect { case file if (file.hasExtension("class")) => pathToClassName(file, binaryDir) }
          .toList
      }
    }

//		val classes = classNames
//			.map(try { classLoader.loadClass } catch { case e:Throwable => { null } } finally {})
//			.collect { case clazz if (isSimulationClass(clazz)) => clazz.asInstanceOf[Class[ScenarioContainer]] }

    val foundClasses = for { x <- classNames
      if(canLoadClass(x))
    } yield classLoader.loadClass(x)

    val classes = foundClasses.collect { case clazz if (isSimulationClass(clazz)) => clazz.asInstanceOf[Class[ScenarioContainer]] }

		requestedClassName.map { requestedClassName =>
			if (!classes.map(_.getName).contains(requestedClassName)) println("Simulation class '" + requestedClassName + "' could not be found.")
		}

		classes
	}


  private def canLoadClass(clazz : String) : Boolean = {
    try {
      classLoader.loadClass(clazz)
      true
    }
    catch {
      case e:Throwable => {
        warn("Unable to load class: " + clazz)
      }
      false
    } finally {}
  }
}
