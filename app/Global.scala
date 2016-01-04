import controllers.com.bjond.persistence.MongoService
import play.api._
import reactivemongo.api.MongoDriver
import reactivemongo.api.collections.bson.BSONCollection

/**
  * Created by bcflynn on 1/4/16.
  */
object Global extends GlobalSettings {

  val mongo = new MongoService()
  implicit val context = play.api.libs.concurrent.Execution.Implicits.defaultContext

  override def onStart(app: Application) {
    Logger.info("Application start...")
    setupMongo
  }

  override def onStop(app: Application) {
    Logger.info("Application shutdown...")
  }

  def setupMongo: Unit = {
    Logger.info("Setting up MongoDB")
    val driver = new MongoDriver
    val connection = driver.connection(List("localhost"))
    val db = connection("bjond_github_adapter")
    db[BSONCollection]("configurations").create(true)
  }
}
