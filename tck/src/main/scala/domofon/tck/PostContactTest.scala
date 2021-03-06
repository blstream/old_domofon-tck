package domofon.tck

import java.time.LocalDate

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import domofon.tck.DomofonMarshalling._
import domofon.tck.entities._
import org.scalatest.prop.TableDrivenPropertyChecks._
import spray.json._

trait PostContactTest extends BaseTckTest {

  describe("POST /contacts") {

    it("Discards wrong request type body") {
      Post("/contacts", "") ~~> {
        status shouldBe StatusCodes.UnsupportedMediaType
      }
    }

    it("Discards request as Json array") {
      Post("/contacts", JsArray()) ~~> {
        status shouldBe StatusCodes.BadRequest
      }
    }

    it("Discards empty json object") {
      Post("/contacts", JsObject()) ~~> {
        status shouldBe StatusCodes.BadRequest
      }
    }

    it("Discards json with illegal string values") {
      val validContactRequest = contactRequest().toJson.asJsObject
      val stringFields = Seq("name", "company", "notifyEmail", "phone", "adminEmail")
      val illegalStringValues = Seq(JsNumber(42), JsBoolean(false), JsObject.empty)
      val cartesianProduct = for {
        field <- stringFields
        value <- illegalStringValues
      } yield (field, value)
      val invalidParameters = Table(("field", "value"), cartesianProduct: _*)
      forAll(invalidParameters) { (field, value) =>
        val invalidContactRequest = JsObject(validContactRequest.fields.updated(field, value))
        Post("/contacts", invalidContactRequest.toJson) ~~> {
          status shouldBe StatusCodes.BadRequest
        }
      }
    }

    it("Discards json with illegal date values") {
      import DomofonMarshalling._

      val validContactRequest = contactRequest().toJson.asJsObject
      val values = Seq(
        (JsString("2000.01.1"), JsString("2000-01")),
        (JsString("02/29/2007"), JsNull)
      )

      val dateFields = Table(("fromDate", "tillDate"), values: _*)
      forAll(dateFields) { (from, till) =>
        val invalidContactRequest = JsObject(validContactRequest.fields ++ Map("fromDate" -> from, "tillDate" -> till))
        Post("/contacts", invalidContactRequest.toJson) ~~> {
          status shouldBe StatusCodes.BadRequest
        }
      }

    }

    it("Rejects when only one date is set or they don't respect relation (fromDate <= tillDate)") {
      import DomofonMarshalling._

      val validContactRequest = contactRequest().toJson.asJsObject
      val values = Seq(
        (None, Some(LocalDate.now().toString)),
        (Some(LocalDate.now().toString), None),
        (Some("2012-12-12"), Some("2012-11-11"))
      ).map(x => (x._1.toJson, x._2.toJson))

      val dateFields = Table(("fromDate", "tillDate"), values: _*)
      forAll(dateFields) { (from, till) =>
        val invalidContactRequest = JsObject(validContactRequest.fields ++ Map("fromDate" -> from, "tillDate" -> till))
        Post("/contacts", invalidContactRequest.toJson) ~~> {
          status shouldBe StatusCodes.UnprocessableEntity
        }
      }

    }

    it("Validates it works for no dates, or when dates respect (fromDate <= tillDate)") {
      import DomofonMarshalling._

      val values = Seq[(Option[LocalDate], Option[LocalDate])](
        (None, None),
        (Some(LocalDate.now.plusDays(2)), Some(LocalDate.now.plusDays(2))),
        (Some(LocalDate.now.plusDays(2)), Some(LocalDate.now.plusDays(3)))
      )

      val dateFields = Table(("fromDate", "tillDate"), values: _*)
      forAll(dateFields) { (from, till) =>
        val req = contactRequest(fromDate = from, tillDate = till)
        Post("/contacts", req.toJson) ~~> {
          status shouldBe StatusCodes.OK
        }
      }

    }

    it("Rejects Contact with wrong Category ID as text/plain") {
      Post("/contacts", contactRequest(category = nonExistentUuid).toJson) ~> acceptPlain ~~> {
        status shouldBe StatusCodes.UnprocessableEntity
      }
    }

    it("Rejects Contact with wrong Category ID as application/json") {
      Post("/contacts", contactRequest(category = nonExistentUuid).toJson) ~> acceptJson ~~> {
        status shouldBe StatusCodes.UnprocessableEntity
      }
    }

    it("Accepts proper Contact entity, returns text/plain entity id") {
      Post("/contacts", contactRequest().toJson) ~> acceptPlain ~~> {
        status shouldBe StatusCodes.OK
        responseAs[String] should not be 'empty
      }
    }

    it("Accepts proper Contact entity, returns application/json with entity id") {
      Post("/contacts", contactRequest().toJson) ~> acceptJson ~~> {
        status shouldBe StatusCodes.OK
        responseAs[EntityCreated].id shouldBe an[EntityID]
      }
    }

    it("Accepts proper Contact entity, returns application/json with entity id and secret") {
      Post("/contacts", contactRequest().toJson) ~> acceptJson ~~> {
        status shouldBe StatusCodes.OK
        val created = responseAs[EntityCreatedWithSecret]
        created.id shouldBe a[EntityID]
        created.secret shouldBe a[Token]
      }
    }

    val requiredFields = Set("name", "notifyEmail")
    for (field <- requiredFields) {
      it(s"Fails when required field '${field}' is missing as application/json") {
        val cr = contactRequest()
        val json = JsObject(cr.toJson.asJsObject.fields - field)
        Post("/contacts", json) ~> acceptJson ~~> {
          status shouldBe StatusCodes.BadRequest
          responseAs[ValidationFieldsError].fields should contain(field)
        }
      }
    }

    it(s"When failing it notifies about all missing fields as application/json") {
      val cr = contactRequest()
      val json = JsObject(cr.toJson.asJsObject.fields -- requiredFields)
      Post("/contacts", json) ~> acceptJson ~~> {
        status shouldBe StatusCodes.BadRequest
        val r = requiredFields -- responseAs[ValidationFieldsError].fields
        r shouldBe empty
      }
    }

    for (field <- requiredFields) {
      it(s"Fails when required field '${field}' is missing as text/plain") {
        val cr = contactRequest()
        val json = JsObject(cr.toJson.asJsObject.fields - field)
        Post("/contacts", json) ~> acceptPlain ~~> {
          status shouldBe StatusCodes.BadRequest
          responseAs[String] should include(field)
        }
      }
    }

    it(s"When failing it notifies about all missing fields text/plain") {
      val cr = contactRequest()
      val json = JsObject(cr.toJson.asJsObject.fields -- requiredFields)
      Post("/contacts", json) ~> acceptPlain ~~> {
        status shouldBe StatusCodes.BadRequest
        val r = responseAs[String]
        requiredFields.foreach {
          field =>
            r should include(field)
        }
      }
    }

  }
}
