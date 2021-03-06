package utest

/**
* Test suite for all the assertions that uTest comes bundled with.
*
* I use Predef.assert and manual try-catch-asserts throughout this suite,
* since it is the thing that is meant to be *testing* all the fancy uTest
* asserts, we can't assume they work.
*/
object AssertsTests extends TestSuite{
  def tests = TestSuite{
    'assert{
      'success{
        def f(x: Boolean) = x
        assert(f(true))
        "success!"
      }
      'failure{
        val (e, logged, cause) = try {
          val x = 1
          val y = "2"
          assert(
            x > 0,
            x.toString == y
          )

          Predef.assert(false)
          ???
        } catch { case e @ AssertionError(_, logged, cause) =>
          (e, logged, cause)
        }
        val expected = Seq(LoggedValue("x", "Int", 1), LoggedValue("y", "String", "2"))
        * - Predef.assert(
          cause == null,
          "cause should be null for boolean failure"
        )

        * - Predef.assert(
          logged == expected,
          "Logging didn't capture the locals properly " + logged
        )

        * - Predef.assert(
          e.toString.contains("y: String = 2") && e.toString.contains("x: Int = 1"),
          "Logging doesn't display local values properly " + e.toString
        )

        * - Predef.assert(
          e.toString.contains("x.toString == y"),
          "Message didnt contain source text " + e.toString
        )
      }
      'failureWithException{
        val x = 1L
        val y = 0l
        try {
          assert(x / y == 10)
          Predef.assert(false)
        } catch {case e @ AssertionError(src, logged, cause) =>
          Predef.assert(cause.isInstanceOf[ArithmeticException])
          Predef.assert(cause.getMessage == "/ by zero")
          e.getMessage
        }
      }

      'tracingOnFailure{
        try {
          val a = "i am cow"
          val b = 31337
          val c = 98
          assert(a + b == c.toString)
        } catch { case e: AssertionError =>
          e.getMessage.contains("i am cow")
          e.getMessage.contains("31337")
          e.getMessage.contains("98")
        }
      }
    }

    'intercept{
      'success{
        val e = intercept[MatchError]{
          (0: Any) match { case _: String => }
        }
        Predef.assert(e.toString.contains("MatchError"))
        e.toString
      }
      'failureWrongException{
        try {
          val x = 1
          val y = 2.0
          intercept[NumberFormatException]{
            (x: Any) match { case _: String => y + 1 }
          }
          Predef.assert(false) // error wasn't thrown???
        } catch { case e: AssertionError =>
          Predef.assert(e.msg.contains("(x: Any) match { case _: String => y + 1 }"))
          // This is subtle: only `x` should be logged as an interesting value, for
          // `y` was not evaluated at all and could not have played a part in the
          // throwing of the exception
          Predef.assert(e.captured == Seq(LoggedValue("x", "Int", 1)))
          Predef.assert(e.cause.isInstanceOf[MatchError])
          e.msg
        }
      }
      'failureNoThrow{
        try{
          val x = 1
          val y = 2.0
          intercept[NullPointerException]{
            123 + x + y
          }
        }catch {case e: AssertionError =>
          Predef.assert(e.msg.contains("123 + x + y"))
          Predef.assert(e.captured == Seq(LoggedValue("x", "Int", 1), LoggedValue("y", "Double", 2.0)))
          e.msg
        }
      }
    }
    'assertMatch{
      'success{
        val thing = Seq(1, 2, 3)
        assertMatch(thing){case Seq(1, _, 3) =>}
        ()
      }

      'failure{
        try {
          val x = 1
          val iAmCow = Seq("2.0")
          assertMatch(Seq(x, iAmCow, 3)){case Seq(1, 2) =>}
          Predef.assert(false)
        } catch{ case e: utest.AssertionError =>

          Predef.assert(e.captured == Seq(
            LoggedValue("x", "Int", 1), LoggedValue("iAmCow", "Seq[String]", Seq("2.0")))
          )
          Predef.assert(e.msg.contains("assertMatch(Seq(x, iAmCow, 3)){case Seq(1, 2) =>}"))

          e.getMessage
        }
      }

      'failureWithException{
        try {
          val a = 1L
          val b = 2
          assertMatch(Seq(a / 0, 3, b)){case Seq(1, 2) =>}
          Predef.assert(false)
        } catch{ case e: utest.AssertionError =>
          Predef.assert(e.captured == Seq(LoggedValue("a", "Long", 1)))
          Predef.assert(e.cause.isInstanceOf[ArithmeticException])
          Predef.assert(e.msg.contains("assertMatch(Seq(a / 0, 3, b)){case Seq(1, 2) =>}"))
          e.getMessage
        }
      }
    }
    'compileError{
      'success {
        // Make sure that on successfully catching a compilation
        // error, the error it reports is in the correct place for
        // a variety of inputs
        val qq = "\"" * 3
        * - compileError("1 + abc").check(
          """
        * - compileError("1 + abc").check(
                              ^
          """,
          "not found: value abc"
        )
        * - compileError(""" 1 + abc""").check(
          s"""
        * - compileError($qq 1 + abc$qq).check(
                                 ^
          """,
          "not found: value abc"
        )
        * - compileError("""
            1 + abc
        """).check(
          """
            1 + abc
                ^
          """,
          "not found: value abc"
        )
        * - compileError("""



            1 + abc


        """).check(
          """
            1 + abc
                ^
          """,
          "not found: value abc"
        )
        * - compileError("true * false").check(
          """
        * - compileError("true * false").check(
                               ^
          """,
          "value * is not a member of Boolean"
        )
        // need to work around inability to use """ in string

        * - compileError(""" true * false""").check(
          s"""
        * - compileError($qq true * false$qq).check(
                                  ^
          """,
          "value * is not a member of Boolean"
        )
        * - compileError("ab ( cd }").check(
          """""",
          "')' expected but '}' found."

        )
      }

      'failure{
        // Use compileError to check itself to verify that when it
        // doesn't throw an error, it actually does (super meta!)
        * - compileError("""
            compileError("1 + 1").check(
              ""
            )
          """).check(
          """
            compileError("1 + 1").check(
                        ^
          """,
          "compileError check failed to have a compilation error"
        )
        * - compileError("""
            val x = 0
            compileError("x + x").check(
            ""
          )
          """).check(
          """
            compileError("x + x").check(
                        ^
          """,
          "compileError check failed to have a compilation error"
        )
        * - compileError("""
            compileError("1" * 2).check(
              ""
            )
        """).check(
          """
            compileError("1" * 2).check(
                             ^
          """,
          "You can only have literal strings in compileError"
        )
      }
    }
  }
}

