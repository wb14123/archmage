package me.binwang.archmage.coretest

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import me.binwang.archmage.core.CatsMacroImpl.MethodMeta
import me.binwang.archmage.core.CatsMacros.timedIO

import scala.concurrent.duration.{DurationInt, FiniteDuration}

trait TimeHandler {
  implicit val handleTime: (MethodMeta, FiniteDuration) => IO[Unit] = (method: MethodMeta, time: FiniteDuration) => {
    IO.blocking(println(s"Time used for ${method.name}: $time"))
  }
}


object MethodMetaTester extends TimeHandler {

  def testMethod(b: Boolean, s: String): IO[Unit] = timedIO {
    IO.sleep(2.second)
  }

}


object MethodMetaTest {

  def main(args: Array[String]) = {
    println(MethodMetaTester.testMethod(true, "testArg").unsafeRunSync())
  }

}
