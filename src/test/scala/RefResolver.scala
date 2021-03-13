package io.carrera.jsontoavroschema

import io.lemonlabs.uri.typesafe.dsl._
import io.lemonlabs.uri.{AbsoluteUrl, RelativeUrl, Uri}

object RefResolver {
  def normalizeIds(root: JsonSchema): Either[ResolutionError, JsonSchema] =
    for {
      baseUri <- root.id.toRight(ResolutionError("$id must be specified in root schema"))
      resolved <- normalizeIds(root, baseUri)
    } yield resolved

  private def normalizeIds(schema: JsonSchema, baseUri: Uri): Either[ResolutionError, JsonSchema] =
    for {
      definitions <- resolveSchemas(schema.definitions, baseUri, schema)
      additionalItems <- resolveSchema(schema.additionalItems, baseUri, schema)
      contains <- resolveSchema(schema.contains, baseUri, schema)
      properties <- resolveSchemas(schema.properties, baseUri, schema)
      patternProps <- resolveSchemas(schema.patternProperties, baseUri, schema)
      additionalProps <- resolveSchema(schema.additionalProperties, baseUri, schema)
      deps <- resolveDependencies(schema.dependencies, baseUri, schema)
      propNames <- resolveSchema(schema.propertyNames, baseUri, schema)
      allOf <- resolveSchemas(schema.allOf, baseUri, schema)
      anyOf <- resolveSchemas(schema.anyOf, baseUri, schema)
      oneOf <- resolveSchemas(schema.oneOf, baseUri, schema)
    } yield schema.copy(
      definitions = definitions,
      additionalItems = additionalItems,
      contains = contains,
      properties = properties,
      patternProperties = patternProps,
      additionalProperties = additionalProps,
      dependencies = deps,
      propertyNames = propNames,
      allOf = allOf,
      anyOf = anyOf,
      oneOf = oneOf,
    )

  private def resolveDependencies(deps: Map[String, Either[Seq[String], JsonSchema]], baseUri: Uri, ctx: JsonSchema): Either[ResolutionError,  Map[String, Either[Seq[String], JsonSchema]]] =
    deps.foldLeft(Right(Map[String, Either[Seq[String], JsonSchema]]()).withLeft[ResolutionError]) { case (acc, (k, v)) =>
      for {
        last <- acc
        dep <- v match {
          case Left(propNames) => Right(Left(propNames))
          case Right(schema) => for {
            resolved <- resolveSchema(schema, baseUri, ctx)
          } yield Right(resolved)
        }
      } yield last + (k -> dep)
    }

  private def resolveSchemas(schemas: IterableOnce[JsonSchema], baseUri: Uri, ctx: JsonSchema) =
    schemas.iterator.foldLeft(Right(Seq[JsonSchema]()).withLeft[ResolutionError]) { case (acc, cur) =>
      for {
        last <- acc
        schema <- resolveSchema(cur, baseUri, ctx)
      } yield last :+ schema
    }

  private def resolveSchemas(schemaMap: Map[String, JsonSchema], baseUri: Uri, ctx: JsonSchema) =
    schemaMap.foldLeft(Right(Map[String, JsonSchema]()).withLeft[ResolutionError]) { case (acc, (k, v)) =>
      for {
        last <- acc
        schema <- resolveSchema(v, baseUri, ctx)
      } yield last + (k -> schema)
    }

  private def resolveSchema(maybeSchema: Option[JsonSchema], baseUri: Uri, ctx: JsonSchema): Either[ResolutionError, Option[JsonSchema]] =
    maybeSchema match {
      case None => Right(None)
      case Some(schema) =>
        resolveSchema(schema, baseUri, ctx).map(Some(_))
    }

  private def resolveSchema(schema: JsonSchema, baseUri: Uri, ctx: JsonSchema): Either[ResolutionError, JsonSchema] =
    for {
      id <- resolveId(schema.id, baseUri)
      cur = schema.copy(id = id)
      resolved <- normalizeIds(cur, id.getOrElse(baseUri))
    } yield resolved

  private def resolveId(maybeId: Option[Uri], baseUri: Uri): Either[ResolutionError, Option[Uri]] =
    maybeId match {
      case None => Right(None)
      case Some(id) =>
        for {
          absoluteId <- combineUris(baseUri, id)
        } yield Some(absoluteId)
    }

  private def combineUris(baseUri: Uri, id: Uri) =
    (baseUri, id) match {
      case (base: AbsoluteUrl, rel: RelativeUrl) => Right(base / rel)
      case unknown => Left(ResolutionError(s"Unimplemented URI combination: $unknown"))
    }
}

final case class ResolutionError(message: String = "", cause: Throwable = None.orNull)
  extends Exception(message, cause)