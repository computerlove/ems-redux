package ems.config

import java.io.File
import java.net.URI
import org.constretto._
import Constretto._

case class ServerConfig(binary: File, mongo: String, root: URI)

case class CryptConfig(algorithm: String = "DES", password: String = "changeme")

case class CacheConfig(events: Int = 30, sessions: Int = 30)

object Config {
  private val constretto = {
    Constretto(List(
      inis("classpath:config.ini", "file:/opt/jb/ems-redux/config.ini")
    ))
  }

  val server: ServerConfig = ServerConfig(
    constretto[File]("server.binary"),
    constretto[String]("server.mongo"),
    constretto.get[String]("server.root").map(URI.create(_)).getOrElse(throw new IllegalArgumentException("Missing server root"))
  )
  val crypt: CryptConfig = CryptConfig(
    constretto[String]("crypt.algorithm"), constretto[String]("crypt.password")
  )

  val cache: CacheConfig = CacheConfig(
    constretto[Int]("cache.events"), constretto[Int]("cache.sessions")
  )
}