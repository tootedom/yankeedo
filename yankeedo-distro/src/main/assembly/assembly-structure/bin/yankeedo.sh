#!/bin/sh
#
# Copyright 2012-2013 greencheek.org (www.greencheek.org)
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# 		http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

OLDDIR=`pwd`
BIN_DIR=`dirname $0`
cd ${BIN_DIR}/.. && DEFAULT_YANKEEDO_HOME=`pwd` && cd ${OLDDIR}

YANKEEDO_HOME=${YANKEEDO_HOME:=${DEFAULT_YANKEEDO_HOME}}
YANKEEDO_CONF=${YANKEEDO_CONF:=$YANKEEDO_HOME/conf}
YANKEEDO_DATA=${YANKEEDO_DATA:=$YANKEEDO_HOME/data-files}

export YANKEEDO_HOME YANKEEDO_CONF

echo "YANKEEDO_HOME is set to ${YANKEEDO_HOME}"

JAVA_OPTS="-server -XX:+UseThreadPriorities -XX:ThreadPriorityPolicy=42 -Xms512M -Xmx512M -Xmn100M -Xss2M -XX:+HeapDumpOnOutOfMemoryError -XX:+AggressiveOpts -XX:+OptimizeStringConcat -XX:+UseFastAccessorMethods -XX:+UseParNewGC -XX:+UseConcMarkSweepGC -XX:+CMSParallelRemarkEnabled -XX:+CMSClassUnloadingEnabled -XX:SurvivorRatio=8 -XX:MaxTenuringThreshold=1 -XX:CMSInitiatingOccupancyFraction=75 -XX:+UseCMSInitiatingOccupancyOnly ${JAVA_OPTS} -Dlogback.configurationFile=${YANKEEDO_CONF}/logback.xml"

CLASSPATH="$YANKEEDO_HOME/lib/*:${YANKEEDO_HOME}/scenario-lib/*:$YANKEEDO_DATA:$YANKEEDO_CONF:${JAVA_CLASSPATH}"

java $JAVA_OPTS -cp $CLASSPATH org.greencheek.yankeedo.app.Yankeedo $@
