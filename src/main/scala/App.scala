import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import scala.concurrent.duration.Duration
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ContentType
import foo.pet._
import foo.definitions.Pet
import io.circe.Json

import java.io.File
import scala.collection.mutable

object App extends App {
  private val pets = mutable.HashMap[String, Option[String]]()

  implicit def actorSystem = ActorSystem()

  val routes = PetResource.routes(new PetHandler {
    // application/x-www-form-urlencoded
    def createPet(respond: PetResource.CreatePetResponse.type)(name: String, status: Option[String]): scala.concurrent.Future[PetResource.CreatePetResponse] = {
      Future.successful(respond.OK(Pet(name=name, status=status)))
    }
    // multipart/form-data
    def createPet(respond: PetResource.CreatePetResponse.type)(name: String, status: Option[String], file: Option[(java.io.File, Option[String], ContentType)]): scala.concurrent.Future[PetResource.CreatePetResponse] = {
      Future.successful(respond.OK(Pet(name=name, status=status)))
    }
    def createPetMapFileField(fieldName: String, fileName: Option[String], contentType: ContentType): File = ???

    // application/json
    def createPet(respond: PetResource.CreatePetResponse.type)(body: Pet): Future[PetResource.CreatePetResponse] = {
      println("createPet")
      pets.put(body.name, body.status)
      Future.successful(respond.OK(body))
    }

    override def updatePet(respond: PetResource.UpdatePetResponse.type)(name: String, body: Option[Pet]): Future[PetResource.UpdatePetResponse] = {
      if (body.isDefined && pets.contains(name)) {
        body.foreach(pet => pets.update(pet.name, pet.status))
        Future.successful(respond.OK(body.get))
      } else Future.successful(respond.BadRequest(Json.obj("message" -> Json.fromString(s"not found: $name"))))
    }

    override def getPets(respond: PetResource.GetPetsResponse.type)(): Future[PetResource.GetPetsResponse] = {
      println("getPets")
      Future.successful(respond.OK(pets.keys.map(key => Pet(key, pets(key))).toVector))
    }
  })

  Await.result(Http().newServerAt("127.0.0.1", 8080).bindFlow(routes), Duration.Inf)
  println("Running at http://localhost:8080!")
}
