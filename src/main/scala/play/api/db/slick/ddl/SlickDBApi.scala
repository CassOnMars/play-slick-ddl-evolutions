package play.api.db.slick.ddl

import play.api.db.slick.SlickApi
import play.api.db.DBApi
import play.api.Configuration
import play.api.Application
import play.api.db.slick.ddl.internal.DBApiAdapter
import play.api.db.slick.DatabaseConfigProvider
import com.google.inject.Injector

object SlickDBApi {
  def apply(slickApi: SlickApi, configuration: Configuration, application: Application, dbConfigProvider: DatabaseConfigProvider): DBApi = new DBApiAdapter(slickApi, configuration, application, dbConfigProvider)
}
