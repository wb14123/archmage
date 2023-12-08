package me.binwang.archmage.core


import cats.effect.{Clock, IO}
import me.binwang.archmage.core.CatsMacroImpl.MethodMeta

import scala.annotation.{compileTimeOnly, tailrec}
import scala.concurrent.duration.FiniteDuration
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

object CatsMacroImpl {

  case class MethodMeta(
    name: String,
    args: Map[String, Any],
  )

  private def findOwningMethod(c: blackbox.Context)(sym: c.Symbol): Option[c.Symbol] = {


    @tailrec
    def go(sym: c.Symbol): Option[c.Symbol] = {
      if (sym == c.universe.NoSymbol) {
        None
      } else if (sym.isMethod) {
        Option(sym)
      } else {
        go(sym.owner)
      }
    }

    go(sym)
  }


  @compileTimeOnly("Enable macro paradise plugin to expand macro annotations or add scalac flag -Ymacro-annotations.")
  def insertIO[B: c.WeakTypeTag, T: c.WeakTypeTag](c: blackbox.Context)(
      before: c.Expr[MethodMeta => IO[B]], block: c.Expr[IO[T]],
      after: c.Expr[(B, T, MethodMeta) => IO[_]]): c.Expr[IO[T]] = {

    import c.universe.reify

    val name = functionNameImpl(c)
    val args = argumentsMapImpl(c)

    reify {
      val methodMeta = MethodMeta(
        name = name.splice,
        args = args.splice,
      )
      for {
        beforeResult <- before.splice.apply(methodMeta)
        result <- block.splice
        _ <- after.splice.apply(beforeResult, result, methodMeta)
      } yield result
    }
  }


  @compileTimeOnly("Enable macro paradise plugin to expand macro annotations or add scalac flag -Ymacro-annotations.")
  def insertStream[B: c.WeakTypeTag, T: c.WeakTypeTag](c: blackbox.Context)(
    before: c.Expr[MethodMeta => IO[B]], block: c.Expr[fs2.Stream[IO, T]],
    after: c.Expr[(B, MethodMeta) => IO[_]]): c.Expr[fs2.Stream[IO, T]] = {

    import c.universe.reify

    val name = functionNameImpl(c)
    val args = argumentsMapImpl(c)

    reify {
      val methodMeta = MethodMeta(
        name = name.splice,
        args = args.splice,
      )

      fs2.Stream.eval(before.splice.apply(methodMeta)).flatMap { beforeResult =>
        block.splice.map(Right(_)) ++ fs2.Stream.eval(after.splice.apply(beforeResult, methodMeta).map(Left(_)))
      }.filter(_.isRight).map(_.toOption.get)
    }
  }

  @compileTimeOnly("Enable macro paradise plugin to expand macro annotations or add scalac flag -Ymacro-annotations.")
  def timedIO[T: c.WeakTypeTag](c: blackbox.Context)(block: c.Expr[IO[T]])
      (handleTime: c.Expr[(MethodMeta, FiniteDuration) => IO[Unit]]): c.Expr[IO[T]] = {
    import c.universe.reify
    val before = reify { (_: MethodMeta) => Clock[IO].monotonic }
    val after = reify {(time: FiniteDuration, _: T, methodMeta: MethodMeta) => Clock[IO].monotonic.flatMap { afterTime =>
      val diff = afterTime - time
      handleTime.splice.apply(methodMeta, diff)
    }}
    insertIO[FiniteDuration, T](c)(before, block, after)
  }

  @compileTimeOnly("Enable macro paradise plugin to expand macro annotations or add scalac flag -Ymacro-annotations.")
  def timedStream[T: c.WeakTypeTag](c: blackbox.Context)(block: c.Expr[fs2.Stream[IO, T]])
      (handleTime: c.Expr[(MethodMeta, FiniteDuration) => IO[Unit]]): c.Expr[fs2.Stream[IO, T]] = {
    import c.universe.reify
    val before = reify { (_: MethodMeta) => Clock[IO].monotonic }
    val after = reify { (time: FiniteDuration, methodMeta: MethodMeta) =>
      Clock[IO].monotonic.flatMap { afterTime =>
        val diff = afterTime - time
        handleTime.splice.apply(methodMeta, diff)
      }
    }
    insertStream[FiniteDuration, T](c)(before, block, after)
  }


  @compileTimeOnly("Enable macro paradise plugin to expand macro annotations or add scalac flag -Ymacro-annotations.")
  private def functionNameImpl(c: blackbox.Context): c.Expr[String] = {
    findOwningMethod(c)(c.internal.enclosingOwner)
      .map(owner => c.Expr(c.parse(s""""${owner.fullName}"""")))
      .getOrElse(c.abort(c.enclosingPosition, "arguments can be used only inside function."))
  }

  @compileTimeOnly("Enable macro paradise plugin to expand macro annotations or add scalac flag -Ymacro-annotations.")
  private def argumentsMapImpl(c: blackbox.Context): c.Expr[Map[String, Any]] = {

    findOwningMethod(c)(c.internal.enclosingOwner)
      .map(owner => {
        val argsStr = owner.asMethod.paramLists.headOption
          .getOrElse(Nil)
          .map(s => s""""${s.name}" -> ${s.name}""")
          .mkString(", ")

        c.Expr(c.parse(s"""Map($argsStr)"""))
      })
      .getOrElse(c.abort(c.enclosingPosition, "arguments can be used only inside function."))
  }
}

object CatsMacros {

  def timed[T](block: IO[T])
      (implicit handleTime: (MethodMeta, FiniteDuration) => IO[Unit]): IO[T]= macro CatsMacroImpl.timedIO[T]

  def timed[T](block: fs2.Stream[IO, T])
      (implicit handleTime: (MethodMeta, FiniteDuration) => IO[Unit]): fs2.Stream[IO, T] = macro CatsMacroImpl.timedStream[T]

}
