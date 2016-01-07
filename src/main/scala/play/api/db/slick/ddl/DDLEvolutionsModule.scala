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
import com.google.inject.Injector
import slick.profile._

class DDLEvolutionsModule(environment: Environment, configuration: Configuration) extends AbstractModule {
  def configure() = {
    bind(classOf[DBApi]).to(classOf[DBApiAdapter]).in(classOf[Singleton])
  }
}

/*
 * Helper to provide Slick implementation of DBApi.
 */
trait SlickDDLEvolutionsComponents {
  def api: SlickApi
  def configuration: Configuration
  def application: Application
  def dbConfigProvider: DatabaseConfigProvider

  lazy val dbApi: DBApi = SlickDBApi(api, configuration, application, dbConfigProvider)
}
