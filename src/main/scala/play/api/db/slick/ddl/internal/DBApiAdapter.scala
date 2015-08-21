package play.api.db.slick.ddl.internal

import java.sql.Connection

import javax.inject.Inject
import javax.sql.DataSource
import play.api.Logger
import play.api.db.DBApi
import play.api.Mode
import play.api.Application
import play.api.db.{Database => PlayDatabase}
import play.api.db.slick._
import play.api.Configuration
import slick.backend.DatabaseConfig
import slick.driver.JdbcProfile
import slick.profile._
import slick.jdbc.DataSourceJdbcDataSource
import slick.jdbc.HikariCPJdbcDataSource
import play.api.db.slick.ddl.SlickDDLException
import play.api.db.slick.ddl.TableScanner
import com.google.inject.Injector

private[ddl] class DBApiAdapter @Inject() (slickApi: SlickApi, configuration: Configuration, app: Application, dbConfigProvider: DatabaseConfigProvider) extends DBApi with HasDatabaseConfig[SqlProfile] {
  private val logger = Logger(classOf[DBApiAdapter])
  val dbConfig = DatabaseConfigProvider.get[SqlProfile](app)
  configuration.getConfig(DBApiAdapter.configKey).foreach { conf =>
    conf.keys.map(k => k.split('.').head).foreach { db =>
      val dbConf = conf.getConfig(db).get
      dbConf.keys.foreach { key =>
        if (key == "ddls") {
          val packageNames = dbConf.getString(key).getOrElse(throw conf.reportError(key, "Expected key " + key + " but could not get its values!", None)).split(",").toSet
          if (app.mode != Mode.Prod) {
            val evolutionsEnabled = !"disabled".equals(app.configuration.getString("evolutionplugin"))
            if (evolutionsEnabled) {
              val evolutions = app.getFile("conf/evolutions/" + db + "/1.sql");
              val evolutionsFile = scala.io.Source.fromFile(evolutions)
              val existingEvolutions = evolutionsFile.mkString
              if (!evolutions.exists() || existingEvolutions.startsWith(DBApiAdapter.CreatedBy)) {
                evolutionsFile.close()
                try {
                  evolutionScript(key, packageNames, dbConfigProvider)(app).foreach { evolutionScript =>
                    if (existingEvolutions != evolutionScript) {
                      new java.io.File("conf/evolutions/" + db).mkdir()
                      java.nio.file.Files.write(java.nio.file.Paths.get("conf/evolutions/" + db + "/1.sql"), evolutionScript.getBytes(java.nio.charset.StandardCharsets.UTF_8))
                    }
                  }
                } catch {
                  case e: SlickDDLException => throw conf.reportError(key, e.message, Some(e))
                }
              }
            }
          }
        }
      }
    }
  }

  private lazy val databasesByName: Map[DbName, PlayDatabase] = slickApi.dbConfigs[JdbcProfile]().map {
    case (name, dbConfig) => (name, new DBApiAdapter.DatabaseAdapter(name, dbConfig))
  }(collection.breakOut)

  override def databases: Seq[PlayDatabase] = databasesByName.values.toSeq

  def evolutionScript(driverName: String, names: Set[String], dbConfigProvider: DatabaseConfigProvider)(app: Application): Option[String] = {
    import driver.api._

    val ddls = TableScanner.reflectAllDDLMethods(names, driver, app.classloader, dbConfigProvider)

    val delimiter = ";" //TODO: figure this out by asking the db or have a configuration setting?

    if (ddls.nonEmpty) {
      val ddl = ddls
          .toSeq.sortBy(a => a.createStatements.mkString ++ a.dropStatements.mkString) //sort to avoid generating different schemas
          .reduceLeft((a, b) => a.asInstanceOf[driver.SchemaDescription] ++ b.asInstanceOf[driver.SchemaDescription])

      Some(DBApiAdapter.CreatedBy + "Slick DDL\n" +
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

  def database(name: String): PlayDatabase = {
    databasesByName.getOrElse(DbName(name), throw new IllegalArgumentException(s"Could not find database for $name"))
  }

  def shutdown(): Unit = {
    // no-op: shutting down dbs is automatically managed by `slickApi`
    ()
  }
}

private[ddl] object DBApiAdapter {
  final val configKey = "slick.dbs"
  final val CreatedBy = "# --- Created by "

  // I don't really like this adapter as it can be used as a trojan horse. Let's keep things simple for the moment,
  // but in the future we may need to become more defensive and provide custom implementation for `java.sql.Connection`
  // and `java.sql.DataSource` to prevent the ability of closing a database connection or database when using this
  // adapter class.
  private class DatabaseAdapter(_name: DbName, dbConfig: DatabaseConfig[JdbcProfile]) extends PlayDatabase {
    private val logger = Logger(classOf[DatabaseAdapter])
    def name: String = _name.value
    def dataSource: DataSource = {
      dbConfig.db.source match {
        case ds: DataSourceJdbcDataSource => ds.ds
        case hds: HikariCPJdbcDataSource => hds.ds
        case other =>
          logger.error(s"Unexpected data source type ${other.getClass}. Please, file a ticket ${IssueTracker}.")
          throw new UnsupportedOperationException
      }
    }
    def url: String = dbConfig.db.withSession { _.metaData.getURL }
    def getConnection(): Connection = dbConfig.db.source.createConnection()
    def getConnection(autocommit: Boolean): Connection = {
      val conn = getConnection()
      conn.setAutoCommit(autocommit)
      conn
    }
    def withConnection[A](block: Connection => A): A = {
      dbConfig.db.withSession { session =>
        val conn = session.conn
        block(conn)
      }
    }
    def withConnection[A](autocommit: Boolean)(block: Connection => A): A = withConnection { conn =>
      conn.setAutoCommit(autocommit)
      block(conn)
    }
    def withTransaction[A](block: Connection => A): A = {
      dbConfig.db.withTransaction { session =>
        val conn = session.conn
        block(conn)
      }
    }
    def shutdown(): Unit = {
      // no-op. The rationale is that play-slick already takes care of closing the database on application shutdown
      ()
    }
  }
}
