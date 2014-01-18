package jetty

import org.eclipse.jetty.server.Server
import scala.util.Properties
import org.eclipse.jetty.server.session.HashSessionIdManager
import org.eclipse.jetty.server.handler.HandlerCollection
import org.eclipse.jetty.webapp.WebAppContext
import java.io.File

object Jetty extends App {
  val home = new File(Properties.envOrElse("APP_HOME", ".")).getAbsoluteFile

  val server = new Server(Properties.propOrElse("jetty.port", "8081").toInt)

  server.setStopAtShutdown(true)
  server.setSessionIdManager(new HashSessionIdManager())
  server.setHandler({
    val collection = new HandlerCollection()
    collection.addHandler(new WebAppContext(
      new File(home, "webapps/server.war").toString,
      "/server"
    ))
    collection.addHandler(new WebAppContext(
      new File(home, "webapps/admin.war").toString,
      "/admin"
    ))
    collection.getHandlers.foreach(h => h.asInstanceOf[WebAppContext].setParentLoaderPriority(false))
    collection
  })

  server.start()
}
