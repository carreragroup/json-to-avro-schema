package io.carrera.jsontoavroschema

import io.lemonlabs.uri.Uri
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers._

class TranspilerSpec extends AnyFlatSpec {

  it should "create a record" in {
    val root =
      JsonSchema.empty.copy(
        id = schemaUri,
        desc = Some("a description"),
        properties = Map("title" -> JsonSchema.empty.copy(desc = Some("a title"), types = Seq(JsonSchemaString))),
        required = Seq("title")
      )

    val Right(avroSchema) = Transpiler.transpile(root, None)

    val expectedRecord =
      AvroRecord(
        "schema",
        None,
        Some("a description"),
        Seq(AvroField("title", Some("a title"), AvroString, None, None))
      )

    avroSchema should be(expectedRecord)
  }

  it should "include namespace in root schema" in {
    val root = JsonSchema.empty.copy(id = schemaUri)
    val Right(avroSchema) = Transpiler.transpile(root, Some("com.example"))
    val expectedRecord = AvroRecord("schema", Some("com.example"), None, Seq())
    avroSchema should be(expectedRecord)
  }

  it should "transpile numbers to doubles" in {
    val root =
      JsonSchema.empty.copy(
        id = schemaUri,
        properties = Map("maximum" -> JsonSchema.empty.copy(types = Seq(JsonSchemaNumber))),
        required = Seq("maximum")
      )

    val Right(avroSchema) = Transpiler.transpile(root, None)

    val expectedRecord =
      AvroRecord("schema", None, None, Seq(AvroField("maximum", None, AvroDouble, None, None)))
    avroSchema should be(expectedRecord)
  }

  it should "transpile integers to longs" in {
    val root =
      JsonSchema.empty.copy(
        id = schemaUri,
        properties = Map("length" -> JsonSchema.empty.copy(types = Seq(JsonSchemaInteger))),
        required = Seq("length")
      )

    val Right(avroSchema) = Transpiler.transpile(root, None)

    val expectedRecord =
      AvroRecord("schema", None, None, Seq(AvroField("length", None, AvroLong, None, None)))
    avroSchema should be(expectedRecord)
  }

  it should "transpile optional values" in {
    val root =
      JsonSchema.empty.copy(
        id = schemaUri,
        properties = Map("length" -> JsonSchema.empty.copy(types = Seq(JsonSchemaInteger))),
      )

    val Right(avroSchema) = Transpiler.transpile(root, None)

    val expectedRecord =
      AvroRecord("schema", None, None, Seq(AvroField("length", None, AvroUnion(Seq(AvroNull, AvroLong)), Some(ujson.Null), None)))
    avroSchema should be(expectedRecord)
  }

  it should "transpile booleans to booleans" in {
    val root = JsonSchema.empty.copy(
      id = schemaUri,
      properties = Map("uniqueItems" -> JsonSchema.empty.copy(types = Seq(JsonSchemaBool))),
      required = Seq("uniqueItems")
    )
    val Right(avroSchema) = Transpiler.transpile(root, None)
    val expectedRecord =
      AvroRecord("schema", None, None, Seq(AvroField("uniqueItems", None, AvroBool, None, None)))

    avroSchema should be(expectedRecord)
  }

  it should "transpile nulls to nulls" in {
    val root = JsonSchema.empty.copy(
      id = schemaUri,
      properties = Map("applesauce" -> JsonSchema.empty.copy(types = Seq(JsonSchemaNull))),
      required = Seq("applesauce")
    )
    val Right(avroSchema) = Transpiler.transpile(root, None)
    val expectedRecord =
      AvroRecord("schema", None, None, Seq(AvroField("applesauce", None, AvroNull, None, None)))

    avroSchema should be(expectedRecord)
  }

  it should "transpile empty schema to bytes" in {
    val root = JsonSchema.empty.copy(
      id = schemaUri,
      properties = Map("const" -> JsonSchema.empty),
      required = Seq("const")
    )
    val Right(avroSchema) = Transpiler.transpile(root, None)
    val expectedRecord =
      AvroRecord("schema", None, None, Seq(AvroField("const", None, AvroBytes, None, None)))

    avroSchema should be(expectedRecord)
  }

  it should "transpile arrays with types" in {
    /*
     "stringArray": {
       "type": "array",
       "items": { "type": "string" }
     }
     */

    val root = JsonSchema.empty.copy(
      id = schemaUri,
      properties = Map("someList" ->
        JsonSchema.empty.copy(
          types = Seq(JsonSchemaArray),
          items = Seq(JsonSchema.empty.copy(types = Seq(JsonSchemaString)))
        )
      ),
      required = Seq("someList")
    )
    val Right(avroSchema) = Transpiler.transpile(root, None)
    val expectedRecord =
      AvroRecord("schema", None, None, Seq(AvroField("someList", None, AvroArray(AvroString), None, None)))

    avroSchema should be(expectedRecord)
  }

  it should "transpile arrays of any (empty schema) to arrays of bytes" in {
    /*
      "examples": {
        "type": "array",
        "items": {}
      },
     */

    val root = JsonSchema.empty.copy(
      id = schemaUri,
      properties = Map("examples" ->
        JsonSchema.empty.copy(
          types = Seq(JsonSchemaArray),
          items = Seq(JsonSchema.empty)
        )
      ),
      required = Seq("examples")
    )
    val Right(avroSchema) = Transpiler.transpile(root, None)
    val expectedRecord =
      AvroRecord("schema", None, None, Seq(AvroField("examples", None, AvroArray(AvroBytes), None, None)))

    avroSchema should be(expectedRecord)
  }

  it should "transpile arrays of any to arrays of bytes" in {
    /*
      "someList: {
        "type": "array"
       }
     */
    val root = JsonSchema.empty.copy(
      id = schemaUri,
      properties = Map("someList" -> JsonSchema.empty.copy(types = Seq(JsonSchemaArray))),
      required = Seq("someList")
    )
    val Right(avroSchema) = Transpiler.transpile(root, None)
    val expectedRecord =
      AvroRecord("schema", None, None, Seq(AvroField("someList", None, AvroArray(AvroBytes), None, None)))
    avroSchema should be(expectedRecord)
  }

  it should "transpile objects to map when properties is not present" in {
    /*
      "someObj": {
        "type": "object"
        "additionalProperties": { "type": "string" }
       }
     */
    val root = JsonSchema.empty.copy(
      id = schemaUri,
      properties = Map("someObj" -> JsonSchema.empty.copy(
        types = Seq(JsonSchemaObject),
        additionalProperties = Some(JsonSchema.empty.copy(types = Seq(JsonSchemaString)))
      )),
      required = Seq("someObj")
    )
    val Right(avroSchema) = Transpiler.transpile(root, None)
    val expectedRecord =
      AvroRecord("schema", None, None, Seq(AvroField("someObj", None, AvroMap(AvroString), None, None)))
    avroSchema should be(expectedRecord)
  }

  it should "transpile objects to records when an properties is present" in {
    /*
     "someObj": {
       "$id": "SomeObj",
       "type: "object",
       "properties": {
          "Inner": { "type": "integer" }
       }
     }
     */
    val root = JsonSchema.empty.copy(
      id = schemaUri,
      properties = Map("someObj" -> JsonSchema.empty.copy(
        id = Uri.parseOption("SomeObj"),
        types = Seq(JsonSchemaObject),
        properties = Map("Inner" -> JsonSchema.empty.copy(types = Seq(JsonSchemaInteger))),
        required = Seq("Inner")
      )),
      required = Seq("someObj")
    )
    val Right(avroSchema) = Transpiler.transpile(root, None)
    val innerRecord = AvroRecord("SomeObj", None, None, Seq(AvroField("Inner", None, AvroLong, None, None)))
    val expectedRecord =
      AvroRecord("schema", None, None, Seq(AvroField("someObj", None, innerRecord, None, None)))
    avroSchema should be(expectedRecord)
  }

  it should "transpile string enums" in {
    val root = JsonSchema.empty.copy(
      id = schemaUri,
      properties = Map("someProp" -> JsonSchema.empty.copy(
          `enum` = Seq(ujson.Str("a"), ujson.Str("b"))
        )
      ),
      required = Seq("someProp")
    )
    val Right(avro) = Transpiler.transpile(root, None)

    val expectedRecord =
      AvroRecord("schema", None, None,
        Seq(AvroField("someProp", None, AvroEnum("somePropEnum", Seq("a","b")), None, None))
      )

    avro should be(expectedRecord)
  }

  it should "fail if json enum values are not strings" in {
    /*
     * We should really find a different avro type to transpile to instead.
     * See issue: https://github.com/carreragroup/json-to-avro-schema/issues/22
     */
    val root = JsonSchema.empty.copy(
      id = schemaUri,
      properties = Map("someProp" -> JsonSchema.empty.copy(
        `enum` = Seq(ujson.Str("a"), ujson.Bool(false))
        )
      ),
      required = Seq("someProp")
    )
    val Left(err) = Transpiler.transpile(root, None)
    err.message should be("Unimplemented: non-string enums aren't supported yet at someProp. Value: false")
  }

  it should "transpile type unions" in {
    val root = JsonSchema.empty.copy(
      id = schemaUri,
      properties = Map("unionType" -> JsonSchema.empty.copy(types = Seq(JsonSchemaBool, JsonSchemaString))),
      required = Seq("unionType")
    )
    val Right(avroSchema) = Transpiler.transpile(root, None)
    val expectedRecord =
      AvroRecord("schema", None, None, Seq(AvroField("unionType", None, AvroUnion(Seq(AvroBool, AvroString)), None, None)))

    avroSchema should be(expectedRecord)
  }

  it should "transpile a record without an id" in {
    val root = JsonSchema.empty.copy(
      id = schemaUri,
      properties = Map(
        "A" -> JsonSchema.empty.copy(
          properties = Map(
            "name" -> JsonSchema.empty.copy(types = Seq(JsonSchemaString)),
            "index" -> JsonSchema.empty.copy(types = Seq(JsonSchemaInteger))
          ),
          required = Seq("name", "index")
        )
      ),
      required = Seq("A")
    )
    val Right(avro) = Transpiler.transpile(root, None)

    val expected =
      AvroRecord("schema", None, None,
        Seq(
          AvroField("A", None,
            AvroRecord("A", None, None,
              Seq(
                AvroField("name", None, AvroString, None, None),
                AvroField("index", None, AvroLong, None, None)
              )
            ), None, None
          ),
        )
      )

    avro should be(expected)
  }

  it should "resolve reference to a record" in {
    val root = JsonSchema.empty.copy(
      id = schemaUri,
      properties = Map(
        "A" -> JsonSchema.empty.copy(
          properties = Map(
            "name" -> JsonSchema.empty.copy(types = Seq(JsonSchemaString)),
            "index" -> JsonSchema.empty.copy(types = Seq(JsonSchemaInteger))
          ),
          required = Seq("name", "index")
        ),
        "B" -> JsonSchema.empty.copy(
          ref = Uri.parseOption("#/properties/A")
        )
      ),
      required = Seq("A","B")
    )
    val Right(avro) = Transpiler.transpile(root, None)

    val expected =
      AvroRecord("schema", None, None,
        Seq(
          AvroField("A", None,
            AvroRecord("A", None, None,
              Seq(
                AvroField("name", None, AvroString, None, None),
                AvroField("index", None, AvroLong, None, None)
              )
            ), None, None
          ),
          AvroField("B", None, AvroRef("A"), None, None)
        )
      )

    avro should be(expected)
  }

  it should "resolve reference id of a record if available" in {
    val root = JsonSchema.empty.copy(
      id = schemaUri,
      properties = Map(
        "A" -> JsonSchema.empty.copy(
          id = Uri.parseOption("AwesomeSchema"),
          properties = Map(
            "name" -> JsonSchema.empty.copy(types = Seq(JsonSchemaString)),
            "index" -> JsonSchema.empty.copy(types = Seq(JsonSchemaInteger))
          ),
          required = Seq("name", "index")
        ),
        "B" -> JsonSchema.empty.copy(
          ref = Uri.parseOption("#/properties/A")
        )
      ),
      required = Seq("A","B")
    )

    val Right(avro) = Transpiler.transpile(root, None)

    avro.fields.filter(f => f.name == "B").head.`type` should be(AvroRef("AwesomeSchema"))
  }

  it should "inline first definition reference" in {
    val root = JsonSchema.empty.copy(
      id = schemaUri,
      definitions = Map("A" -> JsonSchema.empty.copy(
        types = Seq(JsonSchemaInteger)
      )),
      properties = Map("B" -> JsonSchema.empty.copy(
        ref = Uri.parseOption("#/definitions/A")
      )),
      required = Seq("B")
    )

    val Right(avro) = Transpiler.transpile(root, None)

    val expected = AvroRecord("A", None, None, Seq(
      AvroField("value", None, AvroLong, None, None)
    ))
    avro.fields.filter(f => f.name == "B").head.`type` should be(expected)
  }

  it should "reference subsequent definition references by name" in {
    val root = JsonSchema.empty.copy(
      id = schemaUri,
      definitions = Map("A" -> JsonSchema.empty.copy(
        types = Seq(JsonSchemaInteger)
      )),
      properties = Map("B" -> JsonSchema.empty.copy(
          ref = Uri.parseOption("#/definitions/A")
        ),
        "C" -> JsonSchema.empty.copy(
          ref = Uri.parseOption("#/definitions/A")
        )
      ),
      required = Seq("B","C")
    )

    val Right(avro) = Transpiler.transpile(root, None)

    val expected = AvroRef("A")
    avro.fields.filter(f => f.name == "C").head.`type` should be(expected)
  }

  it should "handle definitions with properties" in {
    val root = JsonSchema.empty.copy(
      id = schemaUri,
      definitions = Map(
        "A" -> JsonSchema.empty.copy(
          properties = Map(
            "name" -> JsonSchema.empty.copy(types = Seq(JsonSchemaString)),
            "index" -> JsonSchema.empty.copy(types = Seq(JsonSchemaInteger))
          ),
          required = Seq("name", "index")
        )
      ),
      properties = Map(
        "B" -> JsonSchema.empty.copy(
          ref = Uri.parseOption("#/definitions/A")
        )
      ),
      required = Seq("B")
    )

    val Right(avro) = Transpiler.transpile(root, None)

    val expected = AvroRecord(
      "A", None, None,
      Seq(
        AvroField("name", None, AvroString, None, None),
        AvroField("index", None, AvroLong, None, None)
      )
    )
    avro.fields.filter(f => f.name == "B").head.`type` should be(expected)
  }

  it should "successfully transpile if a definition is not referenced" in {
    //there was a bug where we would try to index into the fields
    //at index -1 if a definition was never referenced

    val root = JsonSchema.empty.copy(
      id = schemaUri,
      definitions = Map("A" -> JsonSchema.empty.copy(
          types = Seq(JsonSchemaInteger)
        ),
        "B" -> JsonSchema.empty.copy(
          types = Seq(JsonSchemaBool)
        )
      ),
      properties = Map("C" -> JsonSchema.empty.copy(
        ref = Uri.parseOption("#/definitions/A")
      )),
      required = Seq("C")
    )

    val Right(_) = Transpiler.transpile(root, None)
  }

  private def schemaUri =
    Uri.parseOption("http://json-schema.org/draft-06/schema#")
}
