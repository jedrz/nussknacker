package pl.touk.nussknacker.processCounts.influxdb

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import pl.touk.nussknacker.processCounts._

import scala.concurrent.{ExecutionContext, Future}

class InfluxCountsReporter(env: String, config: InfluxReporterConfig) extends CountsReporter {

  private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

  private val influxBaseReporter = new InfluxBaseCountsReporter(env, config)


  override def prepareRawCounts(processId: String, countsRequest: CountsRequest)(implicit ec: ExecutionContext): Future[String => Option[Long]] = countsRequest match {
    case RangeCount(fromDate, toDate) => prepareRangeCounts(processId, fromDate, toDate)
    case ExecutionCount(pointInTime) => queryInflux(processId, None, pointInTime)
  }

  private def prepareRangeCounts(processId: String, fromDate: LocalDateTime, toDate: LocalDateTime)(implicit ec: ExecutionContext): Future[String => Option[Long]] = {

    influxBaseReporter.detectRestarts(processId, fromDate, toDate).flatMap {
      case Nil => queryInflux(processId, Some(fromDate), toDate)
      case dates => Future.failed(CannotFetchCountsError(s"Counts unavailable, as process was restarted/deployed on " +
        s" following dates: ${dates.map(_.format(dateTimeFormatter)).mkString(", ")}"))
    }

  }

  private def queryInflux(processId: String, fromDate: Option[LocalDateTime], toDate: LocalDateTime)(implicit ec: ExecutionContext): Future[String => Option[Long]] = {
    influxBaseReporter.fetchBaseProcessCounts(processId, fromDate, toDate).map(pbc => nodeId => pbc.getCountForNodeId(nodeId))
  }
}
