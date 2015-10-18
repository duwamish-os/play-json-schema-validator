package com.eclipsesource.schema.internal

import play.api.data.mapping._
import play.api.data.validation.ValidationError
import play.api.libs.json._

object Results {

  def merge(va1: VA[JsValue], va2: VA[JsValue]): VA[JsValue] = {
    (va1, va2) match {
      case (Success(_), f@Failure(err)) => f
      case (f@Failure(_), Success(_)) => f
      case (f1@Failure(errs1), f2@Failure(errs2)) => Failure(errs1 ++ errs2)
      case (Success(obj1@JsObject(_)), Success(obj2@JsObject(_))) => Success(obj1 ++ obj2)
      case (Success(JsArray(values1)), Success(JsArray(values2))) => Success(JsArray(values1 ++ values2))
      case (s@Success(json), Success(_)) => s
    }
  }

  def aggregateAsObject(validatedProps: Seq[(String, VA[JsValue])], context: Context): VA[JsValue] = {
    validatedProps.foldLeft[VA[JsValue]](Success(Json.obj()))((va, result) => (va, result._2) match {
      case (Success(_), f@Failure(err)) => f
      case (f@Failure(_), Success(_)) => f
      case (f1@Failure(errs1), f2@Failure(errs2)) => Failure(errs1 ++ errs2)
      case (Success(obj@JsObject(fields)), Success(s2)) => Success(JsObject(obj.fields :+ (result._1, s2)))
    })
  }

  def mergeAsObject(valid: Seq[(String, JsValue)], invalid: Seq[VA[JsValue]]): VA[JsValue] = {
    if (invalid.nonEmpty) {
      invalid.collect { case f@Failure(_) => f }.reduceLeft((f1, f2) => Failure.merge(f1, f2))
    } else {
      Success(JsObject(valid))
    }
  }

  def success(prop: (String, JsValue)): PropertyValidationResult = {
    prop._1 -> Success(prop._2)
  }

  // TODO: replace keyword type
  def failureWithPath(msg: String, schemaPath: String, instancePath: String, instance: JsValue): VA[JsValue] = {
    Failure(Seq(Path \ instancePath ->
      Seq(ValidationError(msg,
        Json.obj(
          "schemaPath" -> schemaPath,
          "instancePath" -> instancePath,
          "value" -> instance
        )
      ))
    ))
  }

}
