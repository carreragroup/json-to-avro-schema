package io.carrera.jsontoavroschema

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers._

class AvroWriterSpec extends AnyFlatSpec {
  it should "create json object" in {
    val schema =
        AvroRecord(
          "Element",
          None,
          Some("a description"),
          Seq(AvroField("id", Some("an id"), AvroString(), None, None))
        )

    val expected = ujson.Obj(
      "type" -> "record",
      "name" -> "Element",
      "doc" -> "a description",
      "fields" -> ujson.Arr(
        ujson.Obj(
          "name" -> "id",
          "doc" -> "an id",
          "type" -> "string",
        )
      ),
    )

    val Right(records) = AvroWriter.toJson(schema)
    records should be(expected)
  }
}