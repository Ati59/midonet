/*
 * Copyright 2016 Midokura SARL
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.midonet.midolman.haproxy

import java.io.{File, PrintWriter}

import scala.sys.process._
import org.junit.runner.RunWith
import org.scalatest.concurrent.Eventually
import org.scalatest.junit.JUnitRunner
import org.scalatest.{FeatureSpec, ShouldMatchers}
import org.slf4j.LoggerFactory


@RunWith(classOf[JUnitRunner])
class HaproxyHelperTest extends FeatureSpec
                        with Eventually
                        with ShouldMatchers {

    val log = LoggerFactory.getLogger(classOf[HaproxyHelperTest])

    def verifyIpNetns(name: String, iface: String): Unit = {
        eventually {
            val ns = "ip netns".!!
            ns should include (name)
            val links = s"ip netns exec $name ip link".!!
            links should include (iface)
        }
    }

    def verifyNoIpNetns(name: String, iface: String): Unit = {
        eventually {
            val ns = "ip netns".!!
            ns should not include name
            val links = s"ip link".!!
            links should not include iface
        }
    }

    def verifyHaproxyRunning(name: String): Unit = {
        eventually {
            val pid = s"ip netns pids $name".!!
            val ps = s"ps $pid".!!
            ps should include ("haproxy")
        }
    }

    val haproxyScript = "../midolman/src/lib/midolman/service_containers/haproxy/haproxy-helper"

    val name = "TEST"
    val ifaceName = "iface"
    val mac = "aa:bb:cc:aa:bb:cc"
    val ip = "10.0.0.10"
    val routerIp = "10.0.0.9"
    val haproxyHelper = new HaproxyHelper(haproxyScript)
    val defaultConf =
        s"""global
            |    daemon
            |    user nobody
            |    group daemon
            |    log /dev/log    local0
            |defaults
            |    log     global
            |    timeout connect 5000
            |    timeout client 5000
            |    timeout server 5000
            |frontend A
            |    option tcplog
            |    bind *:8080
            |    default_backend B
            |backend B
            |    server X 10.0.0.1:5000
            |    server Y 10.0.0.2:5000
               """.stripMargin

    feature("creates namespace") {
        scenario("namespace is created") {
            try {
                haproxyHelper.makens(name, ifaceName, mac, ip, routerIp)
                verifyIpNetns(name, ifaceName)
            } finally {
                haproxyHelper.cleanns(name, ifaceName)
                verifyNoIpNetns(name, ifaceName)
            }
        }

        scenario("haproxy starts") {
            val tmpPid = File.createTempFile("haproxy", ".pid")
            val tmpConf = File.createTempFile("haproxy", ".conf")
            val writer = new PrintWriter(tmpConf.getAbsolutePath, "UTF-8")
            writer.println(defaultConf)
            writer.close()
            try {
                haproxyHelper.makens(name, ifaceName, mac, ip, routerIp)
                verifyIpNetns(name, ifaceName)

                haproxyHelper.restartHaproxy(name, tmpConf.getAbsolutePath)
                verifyHaproxyRunning(name)
            } finally {
                haproxyHelper.cleanns(name, ifaceName)
                verifyNoIpNetns(name, ifaceName)
            }
        }

        scenario("cleaning non-existent namespace is a no-op") {
            verifyNoIpNetns(name, ifaceName)
            haproxyHelper.cleanns(name, ifaceName)
            verifyNoIpNetns(name, ifaceName)
        }
    }
}
