package play.api.db.slick.ddl

import javax.inject.Singleton
import play.api.Configuration
import play.api.Environment
import play.api.Application
import play.api.Mode
import play.api.Play
import play.api.db.DBApi
import play.api.db.slick.SlickApi
import play.api.db.slick.ddl.internal.DBApiAdapter
import play.api.db.slick._
import play.api.inject.Binding
import com.google.inject.AbstractModule
import com.google.inject.name.Names
import slick.profile._

object DDLEvolutionsModule {
  final val configKey = "slick"
  final val CreatedBy = "# --- Created by "
}

class DDLEvolutionsModule(app: Application, configuration: Configuration, val dbConfigProvider: DatabaseConfigProvider) extends AbstractModule with HasDatabaseConfigProvider[SqlProfile] {
  def configure() = {
    val conf = configuration.getConfig(DDLEvolutionsModule.configKey)
    conf.foreach { conf =>
      conf.keys.foreach { key =>
        val packageNames = conf.getString(key).getOrElse(throw conf.reportError(key, "Expected key " + key + " but could not get its values!", None)).split(",").toSet
        if (app.mode != Mode.Prod) {
          val evolutionsEnabled = !"disabled".equals(app.configuration.getString("evolutionplugin"))
          if (evolutionsEnabled) {
            val evolutions = app.getFile("conf/evolutions/" + key + "/1.sql");
            val evolutionsFile = scala.io.Source.fromFile(evolutions)
            if (!evolutions.exists() || evolutionsFile.mkString.startsWith(DDLEvolutionsModule.CreatedBy)) {
              evolutionsFile.close()
              try {
                evolutionScript(key, packageNames)(app).foreach { evolutionScript =>
                  new java.io.File("conf/evolutions/" + key).mkdir()
                  
                  java.nio.file.Files.write(java.nio.file.Paths.get("conf/evolutions" + key + "/1.sql"), evolutionScript.getBytes(java.nio.charset.StandardCharsets.UTF_8))
                }
              } catch {
                case e: SlickDDLException => throw conf.reportError(key, e.message, Some(e))
              }
            }
          }
        }
      }
    }

    bind(classOf[DBApi]).to(classOf[DBApiAdapter]).asEagerSingleton
  }

  def evolutionScript(driverName: String, names: Set[String])(app: Application): Option[String] = {
    import driver.api._

    val ddls = TableScanner.reflectAllDDLMethods(names, driver, app.classloader)

    val delimiter = ";" //TODO: figure this out by asking the db or have a configuration setting?

    if (ddls.nonEmpty) {
      val ddl = ddls
          .toSeq.sortBy(a => a.createStatements.mkString ++ a.dropStatements.mkString) //sort to avoid generating different schemas
          .reduceLeft((a, b) => a.asInstanceOf[driver.SchemaDescription] ++ b.asInstanceOf[driver.SchemaDescription])

      Some(DDLEvolutionsModule.CreatedBy + "Slick DDL\n" +
        "# To stop Slick DDL generation, remove this comment and start using Evolutions\n" +
        "\n" +
        "# --- !Ups\n\n" +
        ddl.createStatements.mkString("", s"$delimiter\n", s"$delimiter\n") +
        "\n" +
        "# --- !Downs\n\n" +
        ddl.dropStatements.mkString("", s"$delimiter\n", s"$delimiter\n") +
        "\n")
    } else None
  }
}

/*
 * Helper to provide Slick implementation of DBApi.
 */
trait SlickDDLEvolutionsComponents {
  def api: SlickApi

  lazy val dbApi: DBApi = SlickDBApi(api)
}
