package pl.touk.nussknacker.ui.process.marshall

import argonaut.{Parse, PrettyParams}
import org.scalatest.{FlatSpec, Matchers}
import pl.touk.nussknacker.ui.api.helpers.TestProcessingTypes
import pl.touk.nussknacker.restmodel.displayedgraph.displayablenode.{NodeAdditionalFields, ProcessAdditionalFields}

class UiProcessMarshallerSpec extends FlatSpec with Matchers {

  val someProcessDescription = "process description"
  val someNodeDescription = "single node description"
  val processWithAdditionalFields =
    s"""
       |{
       |    "metaData" : { "id": "custom", "typeSpecificData": {"type": "StreamMetaData", "parallelism" : 2}, "additionalFields": { "description": "$someProcessDescription"} },
       |    "exceptionHandlerRef" : { "parameters" : [ { "name": "errorsTopic", "expression": { "language": "spel", "expression": "error.topic" }}]},
       |    "nodes" : [
       |        {
       |            "type" : "Source",
       |            "id" : "start",
       |            "ref" : { "typ": "kafka-transaction", "parameters": [ { "name": "topic", "expression": { "language": "spel", "expression": "in.topic" }}]},
       |            "additionalFields": { "description": "$someNodeDescription"}
       |        }
       |    ],"additionalBranches":[]
       |}
      """.stripMargin

  val processWithoutAdditionalFields =
    s"""
       |{
       |    "metaData" : { "id": "custom", "typeSpecificData": {"type": "StreamMetaData", "parallelism" : 2}},
       |    "exceptionHandlerRef" : { "parameters" : [ { "name": "errorsTopic", "expression": { "language": "spel", "expression": "error.topic" }}]},
       |    "nodes" : [
       |        {
       |            "type" : "Source",
       |            "id" : "start",
       |            "ref" : { "typ": "kafka-transaction", "parameters": [ { "name": "topic", "expression": { "language": "spel", "expression": "in.topic" }}]}
       |        }
       |    ]
       |}
      """.stripMargin


  it should "unmarshall to displayable process properly" in {
    val displayableProcess = ProcessConverter.toDisplayableOrDie(processWithAdditionalFields, TestProcessingTypes.Streaming)

    val processDescription = displayableProcess.properties.additionalFields.flatMap(_.asInstanceOf[ProcessAdditionalFields].description)
    val nodeDescription = displayableProcess.nodes.head.additionalFields.flatMap(_.asInstanceOf[NodeAdditionalFields].description)
    processDescription shouldBe Some(someProcessDescription)
    nodeDescription shouldBe Some(someNodeDescription)
  }

  it should "marshall and unmarshall process" in {
    val baseProcess = processWithAdditionalFields
    val displayableProcess = ProcessConverter.toDisplayableOrDie(baseProcess, TestProcessingTypes.Streaming)
    val canonical = ProcessConverter.fromDisplayable(displayableProcess)

    val processAfterMarshallAndUnmarshall = UiProcessMarshaller.toJson(canonical, PrettyParams.nospace)

    Parse.parse(processAfterMarshallAndUnmarshall) shouldBe Parse.parse(baseProcess)
  }

  it should "unmarshall json without additional fields" in {
    val displayableProcess = ProcessConverter.toDisplayableOrDie(processWithoutAdditionalFields, TestProcessingTypes.Streaming)

    displayableProcess.id shouldBe "custom"
    displayableProcess.nodes.head.additionalFields shouldBe None
    displayableProcess.properties.additionalFields shouldBe None
  }
}
