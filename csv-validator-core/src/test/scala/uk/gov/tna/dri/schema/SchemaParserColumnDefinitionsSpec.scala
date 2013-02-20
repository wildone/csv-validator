package uk.gov.tna.dri.schema

import org.specs2.mutable._
import org.specs2.matcher.ParserMatchers
import java.io.StringReader

class SchemaParserColumnDefinitionsSpec extends Specification with ParserMatchers {

  object TestSchemaParser extends SchemaParser

  override val parsers = TestSchemaParser

  import TestSchemaParser._

  "Schema" should {
    val globalDirsOne = List(TotalColumns(1))
    val globalDirsTwo = List(TotalColumns(2))

    "fail if the total number of columns does not match the number of column definitions" in {
      val schema = """@totalColumns 2
                      LastName: regex ("[a]")"""

      parse(new StringReader(schema)) must beLike { case Failure(message, _) => message mustEqual "@totalColumns = 2 but number of columns defined = 1 at line: 1, column: 1" }
    }

    "fail for invalid column identifier" in {
      val schema = """@totalColumns 1
                      Last Name """

      parse(new StringReader(schema)) must beLike { case Failure(message, _) => message mustEqual "`:' expected but `N' found" }
    }

    "succeed for column definition with no rules" in {
      val schema = """@totalColumns 1
                      Name:"""

      parse(new StringReader(schema)) must beLike { case Success(schema, _) => schema mustEqual Schema(globalDirsOne, List(ColumnDefinition("Name"))) }
    }

    "succeed for column definition with single regex rule" in {
      val schema = """@totalColumns 1
                      Age: regex ("[1-9]*")"""

      parse(new StringReader(schema)) must beLike { case Success(Schema(globalDirsOne, List(ColumnDefinition("Age", List(RegexRule(Literal(Some(r)))), _))), _) => r mustEqual "[1-9]*" }
    }

    "fail for more than one column definition on a line" in {
      val schema = """@totalColumns 1
                      LastName: regex ("[a-z]*") Age"""

      parse(new StringReader(schema)) must beLike { case Failure(message, _) => message mustEqual """Column definition contains invalid text""" }
    }

    "fail for extra text after column definition on a line" in {
      val schema = """@totalColumns 3
                      LastName: regex ("[a-z]*")
                      FirstName: dfsdfsdfwe
                      Age:"""

      parse(new StringReader(schema)) must beLike { case Failure(message, _) => message mustEqual "Column definition contains invalid text" }
    }

    "fail when one invalid column reference" in {
      val schema ="""@totalColumns 2
                    |Column1: in($NotAColumn)
                    |Column2:""".stripMargin

      parse(new StringReader(schema)) must beLike {
        case Failure(message, _) => message mustEqual "Column: Column1 has invalid cross reference in($NotAColumn) at line: 2, column: 10"
      }
    }

    "fail when there are two rules and one is invalid" in {
      val schema ="""@totalColumns 2
                    |Column1: in($Column2) in($NotAColumn2)
                    |Column2:""".stripMargin

      parse(new StringReader(schema)) must beLike {
        case Failure(message, _) => message mustEqual "Column: Column1 has invalid cross reference in($NotAColumn2) at line: 2, column: 23"
      }
    }

    "fail when two rules are invalid " in {
      val schema ="""@totalColumns 2
                     Column1: in($NotAColumn1) in($NotAColumn2)
                     Column2:"""

      parse(new StringReader(schema)) must beLike {
        case Failure(message, _) => message mustEqual """Column: Column1 has invalid cross references in($NotAColumn1) at line: 2, column: 31, in($NotAColumn2) at line: 2, column: 48"""
      }
    }

    "fail when two columns have two rules and each has one invalid column" in {
      val schema ="""@totalColumns 2
                    |Column1: in($Column2) in($NotAColumn2)
                    |Column2: in($NotAColumn3) in($Column2)""".stripMargin

      parse(new StringReader(schema)) must beLike {
        case Failure(message, _) => message mustEqual
          """Column: Column1 has invalid cross reference in($NotAColumn2) at line: 2, column: 23
            |Column: Column2 has invalid cross reference in($NotAColumn3) at line: 3, column: 10""".stripMargin
      }
    }

    /*"fail when two columns have two rules and each has one invalid column with diffferent rules" in {
      val schema ="""@TotalColumns 2
                     Column1: is($Column1) is($NotAColumn1)
                     Column2: not($Column2) not($NotAColumn2)
                     Column3: in($Column3) in($NotAColumn3)
                     Column4: starts($Column4) starts($NotAColumn4)
                     Column5: ends($Column5) ends($NotAColumn5)"""

      parse(new StringReader(schema)) must beLike {
        case Failure(message, _) => message mustEqual """@TotalColumns = 2 but number of columns defined = 5
                                                        |Column: Column1 has invalid cross reference is: NotAColumn1
                                                        |Column: Column2 has invalid cross reference not: NotAColumn2
                                                        |Column: Column3 has invalid cross reference in: NotAColumn3
                                                        |Column: Column4 has invalid cross reference starts: NotAColumn4
                                                        |Column: Column5 has invalid cross reference ends: NotAColumn5""".stripMargin
      }
    }*/

    "multi columns with same name is valid" in {
      val schema = """@totalColumns 2
                      Column1:
                      Column1:"""

      parse(new StringReader(schema)) must beLike {
        case Failure(errors, _) => errors mustEqual "Column: Column1 has duplicates on lines 2, 3"
      }
    }

    "succeed if Column1 correctly has InRule that points to Column2" in {
      val schema = """@totalColumns 2
                      Column1: in($Column2)
                      Column2:"""

      parse(new StringReader(schema)) must beLike {
        case Success(schema, _) => schema mustEqual Schema(globalDirsTwo, List(ColumnDefinition("Column1", List(InRule(ColumnReference("Column2")))),
                                                                               ColumnDefinition("Column2")))
      }
    }

    "fail for invalid column cross references" in {
      val schema ="""@totalColumns 2
                    |Age: in($Blah) regex ("[0-9]+")
                    |Country: in($Boo)""".stripMargin

      parse(new StringReader(schema)) must beLike {
        case Failure(message, _) => message mustEqual
          """Column: Age has invalid cross reference in($Blah) at line: 2, column: 6
            |Column: Country has invalid cross reference in($Boo) at line: 3, column: 10""".stripMargin
      }
    }
  }
}