package fr.loicknuchel.botless.server.domain

import fr.loicknuchel.botless.server.domain.UrlRequest.ResourceType

/**
 * Holds the requested log url and add easy methods to analyze and extract some parts of iy
 */
final case class UrlRequest(value: String) extends AnyVal {
  def path: UrlPath = UrlPath(value.split("\\?|#").headOption.getOrElse("/"))

  def file: Option[FileName] = path.value.split("/").lastOption.filter(_.contains(".")).map(FileName)

  def fileExtension: Option[FileExtension] = file.flatMap(_.value.split("\\.").lastOption).map(FileExtension)

  def resourceType: ResourceType = fileExtension.map(_.value) match {
    case Some("js") => ResourceType.Asset
    case Some("css") => ResourceType.Asset
    case Some("jpg") => ResourceType.Asset
    case Some("png") => ResourceType.Asset
    case Some("gif") => ResourceType.Asset
    case Some("ico") => ResourceType.Asset
    case Some("php") => ResourceType.WebPage
    case Some("htm") => ResourceType.WebPage
    case Some("html") => ResourceType.WebPage
    case Some("log") => ResourceType.TechnicalFile
    case Some("txt") => ResourceType.TechnicalFile
    case Some("zip") => ResourceType.TechnicalFile
    case Some("rar") => ResourceType.TechnicalFile
    case Some("bz2") => ResourceType.TechnicalFile
    case Some("tar") => ResourceType.TechnicalFile
    case Some("tgz") => ResourceType.TechnicalFile
    case Some("gz") => ResourceType.TechnicalFile
    case Some("7z") => ResourceType.TechnicalFile
    case Some("xml") => ResourceType.TechnicalFile
    case Some("json") => ResourceType.TechnicalFile
    case Some("sql") => ResourceType.TechnicalFile
    case Some("bak") => ResourceType.TechnicalFile
    case Some("old") => ResourceType.TechnicalFile
    case Some(_) => ResourceType.Unknown
    case None => ResourceType.Folder
  }
}

object UrlRequest {

  sealed abstract class ResourceType(val symbol: String)

  object ResourceType {

    case object Folder extends ResourceType("F")

    case object Asset extends ResourceType("A")

    case object WebPage extends ResourceType("P")

    case object TechnicalFile extends ResourceType("T")

    case object Unknown extends ResourceType("U")

  }

}
