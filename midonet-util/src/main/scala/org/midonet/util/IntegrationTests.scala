/*
 * Copyright 2015 Midokura SARL
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
package org.midonet.util

import java.nio.ByteBuffer

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future, Promise}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success, Try}

import rx.Observer

import org.midonet.util.reactivex.AwaitableObserver

object IntegrationTests {
    val OK = "ok"

    object UnexpectedResultException extends RuntimeException
    object TestPrepareException extends RuntimeException

    type Test = (String, Future[Any])
    type TestSuite = Seq[Test]
    type LazyTest = () => Test
    type LazyTestSuite = Seq[() => Test]

    type Report = Seq[(String, Try[String])]

    def printReport(r: Report) = r.foldLeft(true) {
        case (status, result) => if (status) printTest(result) else false
    }

    def printTest(info: (String, Try[String])) = info match {
        case (desc, Success(msg)) =>
            Console println Console.GREEN + "[o] " + desc + Console.RESET
            true
        case (desc, Failure(ex)) =>
            Console println Console.RED + "[x] " + desc + Console.RESET
            ex.printStackTrace
            false
    }

    def runSuite(ts: TestSuite): Report = toReport(ts)

    def toReport(ts: TestSuite): Stream[(String, Try[String])] = ts match {
        case Nil => Stream.empty
        case (d,t) :: tail =>
            val r = Try(Await.result(t, 2 seconds)) map { case _ => "passed" }
            (d,r) #:: (if (r.isSuccess) toReport(tail) else Stream.empty)
    }

    def runLazySuite(ts: LazyTestSuite): Report = toReportLazy(ts)

    def toReportLazy(ts: LazyTestSuite): Stream[(String, Try[String])] =
        ts match {
            case Nil => Stream.empty
            case h :: tail =>
                val (d, test) = h()
                val r = Try(Await.result(test, 2.seconds)).map(_ => "passed")
                (d, r) #:: (if (r.isSuccess) toReportLazy(tail) else Stream.empty)
        }

    object TestObserver {
        def apply[T](condition: T => Boolean)
                    (implicit promise: Promise[String] = Promise[String]()) =
            new TestObserver[T] with AwaitableObserver[T] {
                override var check = condition
            }
    }

    abstract class TestObserver[T](implicit promise: Promise[String])
        extends Observer[T] {
        var check: T => Boolean

        def test: Future[String] = promise.future
        override def onCompleted(): Unit = { promise.trySuccess(OK) }
        override def onError(t: Throwable): Unit = { promise.tryFailure(t) }
        override def onNext(resource: T): Unit = try {
            if (!check(resource)) {
                promise.tryFailure(UnexpectedResultException)
            }
        } catch {
            case t: Throwable => promise.failure(t)
        }
    }

    implicit
    def closureToTestObserver[T](closure: T => Boolean)
                                (implicit p: Promise[String]): TestObserver[T] =
        TestObserver.apply(closure)
}