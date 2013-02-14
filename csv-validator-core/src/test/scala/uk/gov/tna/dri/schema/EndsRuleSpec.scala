package uk.gov.tna.dri.schema

import org.specs2.mutable.Specification
import uk.gov.tna.dri.metadata.{Cell, Row}
import scalaz.{Failure, Success}

/**
 * User: Jim Collins
 * Date: 2/13/13
 */
class EndsRuleSpec extends Specification {

  val endsRule = EndsRule(LiteralTypeProvider("myhello world today today"))

  "EndsRule with a string literal behaviour" should  {
    "succeed if EndsRule ends with value" in {
      endsRule.evaluate(0, Row(List(Cell("hello world today today")), 1), Schema(1, List(ColumnDefinition("column1")))) must be_==(Success(true))
    }

    "fail if the value does not end with" in {
      val endsRule = EndsRule(LiteralTypeProvider("my hello world today"))

      endsRule.evaluate(0, Row(List(Cell("my hello world")), 1), Schema(1, List(ColumnDefinition("column1")))) must beLike {
        case Failure(msgs) => msgs.head mustEqual "ends: my hello world today fails for line: 1, column: column1, value: my hello world"
      }
    }
  }
}