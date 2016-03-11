package controllers

import reactivemongo.api._
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.api.commands.WriteResult
import reactivemongo.bson.{BSONDocumentReader, BSONDocumentWriter, BSONDocument}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import reactivemongo.bson.Producer.nameValue2Producer

/**
  * Created by bcflynn on 12/30/15.
  */
class MongoService {

  implicit object GroupConfigurationWriter extends BSONDocumentWriter[GroupConfiguration] {
    def write(config: GroupConfiguration): BSONDocument = BSONDocument(
      "groupid" -> config.groupid,
      "gitHubAPIKey" -> config.gitHubAPIKey,
      "username" -> config.username,
      "password" -> config.password)
  }

  implicit object GroupConfigurationReader extends BSONDocumentReader[GroupConfiguration] {
    def read(doc: BSONDocument): GroupConfiguration = {
      GroupConfiguration(
        doc.getAs[String]("groupid").get,
        doc.getAs[String]("gitHubAPIKey").get,
        doc.getAs[String]("username").get,
        doc.getAs[String]("password").get)
    }
  }
  
  implicit object UserConfigurationWriter extends BSONDocumentWriter[UserConfiguration] {
    def write(config: UserConfiguration): BSONDocument = BSONDocument(
      "groupid" -> config.groupid,
      "userid" -> config.userid,
      "gitHubUsername" -> config.gitHubUsername)
  }

  implicit object UserConfigurationReader extends BSONDocumentReader[UserConfiguration] {
    def read(doc: BSONDocument): UserConfiguration = {
      UserConfiguration(
        doc.getAs[String]("groupid").get,
        doc.getAs[String]("userid").get,
        doc.getAs[String]("gitHubUsername").get)
    }
  }

  implicit object BjondRegistrationWriter extends BSONDocumentWriter[BjondRegistration] {
    def write(registration: BjondRegistration): BSONDocument = BSONDocument (
      "id" -> registration.id,
      "url" -> registration.url)
  }

  implicit object BjondRegistrationReader extends BSONDocumentReader[BjondRegistration] {
    def read(doc: BSONDocument): BjondRegistration = {
      BjondRegistration(
        doc.getAs[String]("id").get,
        doc.getAs[String]("url").get
      )
    }
  }

  def connect() : DefaultDB = {
    val driver = new MongoDriver
    val connection = driver.connection(List("localhost"))
    connection("bjond_github_adapter")
  }

  def insertGroupConfig(groupid: String, config: GroupConfiguration): Future[WriteResult] = {
    val db = connect()
    val configuration = config.copy(groupid, config.gitHubAPIKey, config.username, config.password)
    val selector = BSONDocument("groupid" -> groupid)
    db[BSONCollection]("configurations").update(selector, configuration, upsert = true)
  }

  def getGroupConfiguration(groupid: String): Future[Option[GroupConfiguration]] = {
    val db = connect()
    val query = BSONDocument("groupid" -> groupid)
    db[BSONCollection]("configurations").find(query).one[GroupConfiguration]
  }
  
  def insertUserConfig(groupid: String, userid: String, config: UserConfiguration): Future[WriteResult] = {
    val db = connect()
    val configuration = config.copy(groupid, userid, config.gitHubUsername)
    val selector = BSONDocument("groupid" -> groupid, "userid" -> userid)
    db[BSONCollection]("user_configurations").update(selector, configuration, upsert = true)
  }

  def getUserConfiguration(groupid: String, userid: String): Future[Option[UserConfiguration]] = {
    val db = connect()
    val query = BSONDocument("groupid" -> groupid, "userid" -> userid)
    db[BSONCollection]("user_configurations").find(query).one[UserConfiguration]
  }

  def insertGroupEndpoint(groupid: String, url: String): Future[WriteResult] = {
    val db = connect()
    val endpoint = new BjondRegistration(groupid, url)
    val selector = BSONDocument("id" -> groupid)
    db[BSONCollection]("endpoints").update(selector, endpoint, upsert = true)
  }

  def getGroupEndpoint(groupid: String) : Future[Option[BjondRegistration]] = {
    val db = connect()
    val query = BSONDocument("id" -> groupid)
    db[BSONCollection]("endpoints").find(query).one[BjondRegistration]
  }

}
