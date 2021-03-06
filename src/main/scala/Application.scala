package io.carrera.jsontoavroschema

import scala.io.Source
import scala.util.Using
import Console.{GREEN, RED, RESET}
import scala.annotation.tailrec

object Application extends App {
  private val usage = "Usage: sbt \"run [-n \"com.example\"] inputFile\""

  parseArgs(args.toList) match {
    case Left(msg) =>
      logError(msg)
      log(usage)
    case Right(options) =>
      getInputFilePath(options) match {
        case None =>
          logError("Must specify inputFile")
          log(usage)
        case Some(inputFilePath) =>
          run(inputFilePath, getNamespace(options)) match {
            case Right(output) =>
              logSuccess("success")
              println(ujson.write(output, indent = 2))
            case Left(err) => logError(err.toString)
          }
      }
  }

  def run(inputFilePath: String, namespace: Option[String]) =
    for {
      content <- loadFile(inputFilePath).toEither
      _ = logSuccess("input loaded")
      inputJson = readJson(content)
      jsonSchema <- JsonSchemaParser.parse(inputJson)
      _ = logSuccess("parsed")
      avroSchema <- Transpiler.transpile(jsonSchema.schema, namespace)
      outputJson = AvroWriter.toJson(avroSchema)
    } yield outputJson

  def readJson(content: String) =
    ujson.read(content)

  def loadFile(path: String) =
    Using(Source.fromFile(path))(_.mkString)

  def parseArgs(args: List[String]): Either[String, Map[String,String]] = {
    if (args.isEmpty)
      Left("inputFile is required")
    else
      parseArgs(args, Map())
  }

  @tailrec
  private def parseArgs(args: List[String], accumulator: Map[String, String]): Either[String, Map[String,String]] =
    args match {
      case Nil => Right(accumulator)
      case filePath :: Nil => Right(accumulator + ("inputFile" -> filePath))
      case ("--namespace" | "-n") :: value :: tail => parseArgs(tail, accumulator + ("namespace" -> value))
      case x :: _ => Left (s"unrecognized option: $x")
    }

  private def getInputFilePath(options: Map[String,String]) =
    options.get("inputFile")

  private def getNamespace(options: Map[String,String]) =
    options.get("namespace")

  //Everything goes to the error stream so we can write results to stdout
  private def logError(msg: String): Unit =
    Console.err.println(s"$RESET$RED$msg$RESET")

  private def logSuccess(msg: String): Unit =
    Console.err.println(s"$RESET$GREEN$msg$RESET")

  private def log(msg: String): Unit =
    Console.err.println(s"$RESET$msg$RESET")
}
