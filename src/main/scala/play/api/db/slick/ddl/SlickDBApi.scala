package play.api.db.slick.ddl

import play.api.db.slick.SlickApi
import play.api.db.DBApi
import play.api.Configuration
import play.api.Application
import play.api.db.slick.ddl.internal.DBApiAdapter

object SlickDBApi {
  def apply(slickApi: SlickApi, configuration: Configuration, application: Application): DBApi = new DBApiAdapter(slickApi, configuration, application)
}
