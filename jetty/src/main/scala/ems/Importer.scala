package ems

import java.io.{BufferedReader, FileReader, File}
import java.util.Locale
import net.liftweb.json.{DefaultFormats, JsonParser}
import no.java.ems.storage.{MongoSetting, MongoDBStorage}
import no.java.ems.{URIAttachment, StreamingAttachment, Attachment}
import no.java.ems.model._
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import scala.util.Properties
import java.net.URI
import net.liftweb.json.JsonAST.{JString, JArray}


object ImportMain extends App {

  Importer.execute(new File(args.headOption.getOrElse("/tmp/ems")))
}

object Importer {
  object storage extends MongoDBStorage {
    val MongoSetting(db) = Properties.envOrNone("MONGOLAB_URI")
  }

  implicit val Formats = DefaultFormats

  val isoDF = ISODateTimeFormat.basicDateTimeNoMillis


  def execute(baseDir: File = new File("/tmp/ems")) {
    events(new File(baseDir, "events.json")).foreach(e => {
      val written = storage.importEntity[Event](e)
      println("Wrote " + e.name + " to DB with id " + written.id.get)
      val id = written.id.getOrElse("")
      val eventDir = new File(baseDir, id)
      if (new File(eventDir, "sessions.json").exists()) {
        sessions(new File(eventDir, "sessions.json")).foreach(s => {
          val written = storage.importEntity[Session](s)
          println("Wrote " + s.abs.title + " to DB with id " + written.id.get)
        })
      }
    })
    storage.shutdown()
  }

  def events(file: File) = {
    val parsed = JsonParser.parse(new BufferedReader(new FileReader(file)))
    (parsed \ "events").children.map(c =>
      Event(
        (c \ "id").extractOpt[String],
        (c \ "name").extract[String],
        (c \ "start").extractOpt[String].map(isoDF.parseDateTime(_)).getOrElse(new DateTime(0L)),
        (c \ "end").extractOpt[String].map(isoDF.parseDateTime(_)).getOrElse(new DateTime(1L)),
        (c \ "venue").extract[String],
        (c \ "rooms").children.map(o => {
          Room(
            (o \ "id").extractOpt[String],
            (o \ "name").extract[String]
          )
        }),
        (c \ "timeslots").children.map(o => {
          Slot(
            (o \ "id").extractOpt[String],
            (o \ "start").extractOpt[String].map(isoDF.parseDateTime(_)).getOrElse(new DateTime(0L)),
            (o \ "end").extractOpt[String].map(isoDF.parseDateTime(_)).getOrElse(new DateTime(1L))
          )
        })
      )
    )
  }

  def sessions(file: File) = {
    val parsed = JsonParser.parse(new BufferedReader(new FileReader(file)))
    (parsed \ "sessions").children.map(c => {
      val eventId = (c \ "eventId").extract[String]
      val event = storage.getEvent(eventId)
      val room = event.flatMap(_.rooms.find(_.id == (c \ "room").extractOpt[String]))
      val slot = event.flatMap(_.slots.find(_.id == (c \ "slot").extractOpt[String]))

      Session(
        (c \ "id").extractOpt[String],
        (c \ "eventId").extract[String],
        room,
        slot,
        Abstract(
          Option((c \ "title").extract[String]).getOrElse("No title"),
          (c \ "summary").extractOpt[String],
          (c \ "body").extractOpt[String],
          (c \ "audience").extractOpt[String],
          (c \ "outline").extractOpt[String],
          (c \ "equipment").extractOpt[String],
          (c \ "lang").extractOpt[String].map(l => new Locale(l)).getOrElse(new Locale("no")),
          (c \ "level").extractOpt[String].map(Level(_)).getOrElse(Level.Beginner),
          (c \ "format").extractOpt[String].map(Format(_)).getOrElse(Format.Presentation),
          (c \ "speakers").children.map(s =>
            Speaker(
            (s \ "id").extract[String],
            (s \ "name").extract[String],
            (s \ "email").extract[String],
            (s \ "bio").extractOpt[String], {
              val JArray(tags) = (s \ "tags")
              tags.collect{case JString(t) => Tag(t)}.toSet[Tag]
            },
            (s \ "photo").extractOpt[String].map(f => {
              val file = new File(f)
              storage.saveAttachment(StreamingAttachment(file))
            })
            ))
          ),
        (c \ "state").extractOpt[String].map(State(_)).getOrElse(State.Pending),
        (c \ "published").extractOrElse(false),
        (c \ "attachments").children.map(f => {
          val file = new File(f.values.toString)
          val att: Attachment with Entity = storage.saveAttachment(StreamingAttachment(file))
          URIAttachment(
            URI.create(att.id.get),
            att.name,
            att.size,
            att.mediaType
          )
        }),
        (c \ "tags").extractOpt[String].map(_.split(",").map(Tag(_)).toSet[Tag]).getOrElse(Set.empty),
        (c \ "keywords").extractOpt[String].map(_.split(",").map(Keyword(_)).toSet[Keyword]).getOrElse(Set.empty)
      )}
    )
  }
}