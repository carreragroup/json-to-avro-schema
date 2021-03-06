package io.carrera.jsontoavroschema

import AvroOrder.AvroOrder

import scala.util.chaining.scalaUtilChainingOps

sealed trait AvroType

case object AvroString extends AvroType
case object AvroDouble extends AvroType
case object AvroBool extends AvroType
case object AvroNull extends AvroType
case object AvroLong extends AvroType
case object AvroBytes extends AvroType
case class AvroArray(items: AvroType) extends AvroType
case class AvroMap(values: AvroType) extends AvroType

case class AvroEnum(name: String, symbols: Seq[String]) extends AvroType
case class AvroUnion(types: Seq[AvroType]) extends AvroType {
  def flatten: AvroUnion =
    types
      .foldLeft(Seq[AvroType]()) {
        case (acc, AvroUnion(types)) => acc ++ types
        case (acc, curr) => acc :+ curr
      }.pipe(AvroUnion)
}

case class AvroField(name: String, doc: Option[String], `type`: AvroType, default: Option[ujson.Value], order: Option[AvroOrder])
case class AvroRecord(name: String, namespace: Option[String], doc: Option[String], fields: Seq[AvroField]) extends AvroType

/** represents a reference to an avro record type */
case class AvroRef(name: String) extends AvroType