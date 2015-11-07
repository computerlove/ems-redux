package ems
package storage

import java.net.URI

import ems.security.User
import model._

trait DBStorage {
  def binary: BinaryStorage

  def getEvents(): Vector[Event]

  def getEventsWithSessionCount(implicit user: User): Vector[EventWithSessionCount]

  def getEvent(id: UUID): Option[Event]

  def getEventBySlug(name: String): Option[UUID]

  def saveEvent(event: Event): Either[Throwable, Event]

  def getSlots(eventId: UUID, parent: Option[UUID] = None): Vector[Slot]

  def getSlot(eventId: UUID, id: UUID): Option[Slot]

  def saveSlot(slot: Slot): Either[Throwable, Slot]

  def removeSlot(eventId: UUID, id: UUID): Either[Throwable, Unit]

  def getRooms(eventId: UUID): Vector[Room]

  def getRoom(eventId: UUID, id: UUID): Option[Room]

  def saveRoom(eventId: UUID, room: Room): Either[Throwable, Room]

  def removeRoom(eventId: UUID, id: UUID): Either[Throwable, Unit]

  def getSessions(eventId: UUID)(implicit user: User) : Vector[Session]

  def getSessionsEnriched(eventId: UUID)(implicit user: User) : Vector[EnrichedSession]

  def getSessionEnriched(eventId: UUID, id: UUID)(implicit user: User) : Option[EnrichedSession]

  def getSessionBySlug(eventId: UUID, slug: String)(implicit user: User): Option[UUID]

  def getSession(eventId: UUID, id: UUID)(implicit user: User): Option[Session]

  def saveSession(session: Session): Either[Throwable, Session]

  def publishSessions(eventId: UUID, sessions: Seq[UUID]): Either[Throwable, Unit]

  def getSpeakers(sessionId: UUID): Vector[Speaker]

  def getSpeaker(sessionId: UUID, speakerId: UUID): Option[Speaker]

  def saveSpeaker(sessionId: UUID, speaker: Speaker): Either[Throwable, Speaker]

  def removeSession(sessionId: UUID): Either[Throwable, Unit]

  def updateSpeakerWithPhoto(sessionId: UUID, speakerId: UUID, photo: URI):  Either[Throwable, Unit]

  def removeSpeaker(sessionId: UUID, speakerId: UUID): Either[Throwable, Unit]

  def status(): String

  def shutdown()
}
