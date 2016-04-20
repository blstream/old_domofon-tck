package domofon.tck

import akka.http.scaladsl.model.StatusCodes
import domofon.tck.BaseTckTest.ContactCreationResult

trait RemoveContactItemTest extends BaseTckTest {

  describe("DELETE /contacts/{id}") {

    it("When contact doesn't exist 404 is returned") {

      Delete(s"/contacts/$nonExistentUuid") ~> authorizeWithSecret(nonExistentUuid) ~~> {
        status shouldBe StatusCodes.NotFound
      }
    }

    it("Remove contact when one exists") {
      val ContactCreationResult(uuid, secret) = postContactRequest()

      Delete(s"/contacts/$uuid") ~> authorizeWithSecret(secret) ~~> {
        status shouldBe StatusCodes.OK
      }
    }
  }
}

