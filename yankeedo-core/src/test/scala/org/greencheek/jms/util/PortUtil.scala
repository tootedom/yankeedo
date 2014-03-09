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
package org.greencheek.jms.util

import java.net.ServerSocket

/**
 * Created by dominictootell on 09/03/2014.
 */
object PortUtil {

  def findFreePort: Int = {
    val server: ServerSocket = new ServerSocket(0)
    val port: Int = server.getLocalPort
    server.close
    return port
  }

}
