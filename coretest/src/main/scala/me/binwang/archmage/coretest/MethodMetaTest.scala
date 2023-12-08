package me.binwang.archmage.coretest

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import me.binwang.archmage.core.CatsMacroImpl.MethodMeta
import me.binwang.archmage.core.CatsMacros.timed

import scala.concurrent.duration.{DurationInt, FiniteDuration}

trait TimeHandler {
  implicit val handleTime: (MethodMeta, FiniteDuration) => IO[Unit] = (method: MethodMeta, time: FiniteDuration) => {
    IO.blocking(println(s"Time used for ${method.name}: $time"))
  }
}


object MethodMetaTester extends TimeHandler {

  def testIO(b: Boolean, s: String): IO[Unit] = timed {
    IO.sleep(2.second)
  }

  def testStream(b: Boolean, s: String): fs2.Stream[IO, Int] = timed {
    fs2.Stream.eval(IO.sleep(1.second).map(_ => 2)).repeatN(5)
  }


}


object MethodMetaTest {

  def main(args: Array[String]) = {
    println(MethodMetaTester.testIO(true, "testArg").unsafeRunSync())
    println(MethodMetaTester.testStream(true, "testArg").compile.toList.unsafeRunSync())
  }

}
