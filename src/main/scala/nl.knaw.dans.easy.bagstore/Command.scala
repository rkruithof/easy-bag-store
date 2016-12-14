/**
 * Copyright (C) 2016 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.knaw.dans.easy.bagstore

import java.util.UUID

import scala.util.{Failure, Success, Try}

object Command extends App with BagStoreApp {
  import scala.language.reflectiveCalls
  val opts = CommandLineOptions(args, properties)
  opts.verify()
  override implicit val baseDir = opts.bagStoreBaseDir().toPath

  val result = opts.subcommand match {
    case Some(cmd@opts.add) =>
      val bagUuid = cmd.uuid.toOption.map(UUID.fromString)
      add(cmd.bag().toPath, bagUuid).map {
        bagId => s"Added Bag with bag-id: $bagId"
      }
    case Some(cmd@opts.get) =>
      for {
        itemId <- ItemId.fromString(cmd.itemId())
        _ <- Try { get(itemId, cmd.outputDir().toPath)}
      } yield s"Retrieved item with item-id: $itemId to ${cmd.outputDir().toPath}"
    case Some(cmd@opts.enum) => Try {
      cmd.bagId.toOption
        .map {
          s =>
            for {
              itemId <- ItemId.fromString(s)
              bagId <- ItemId.toBagId(itemId)
            } yield enumFiles(bagId)
              .iterator.foreach(println(_))
        } getOrElse {
        enumBags.iterator.foreach(println(_))
      }
      "Finished enumerating"
    }
    case Some(cmd@opts.hide) =>
      for {
        itemId <- ItemId.fromString(cmd.bagId())
        bagId <- ItemId.toBagId(itemId)
        _ <- hide(bagId)
      } yield s"Marked ${cmd.bagId()} as hidden"
//    case Some(cmd@opts.prune) =>

    case _ => throw new IllegalArgumentException(s"Unkown command: ${opts.subcommand}")
      Try { "Unknown "}
  }

  result match {
    case Success(msg) => println(s"OK: $msg")
    case Failure(e) => println(s"FAILED: ${e.getMessage}")
  }
}