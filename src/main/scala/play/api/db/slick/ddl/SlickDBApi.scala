package play.api.db.slick.ddl

import play.api.db.slick.SlickApi
import play.api.db.DBApi
import play.api.db.slick.ddl.internal.DBApiAdapter

object SlickDBApi {
  def apply(slickApi: SlickApi): DBApi = new DBApiAdapter(slickApi)
}
