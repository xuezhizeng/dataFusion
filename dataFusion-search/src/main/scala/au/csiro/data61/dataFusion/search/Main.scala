package au.csiro.data61.dataFusion.search

import scala.reflect.runtime.universe.typeOf
import scala.util.control.NonFatal

import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.Logger

object Main {
  private val log = Logger(getClass)
  
  case class CliOption(indexer: Boolean, docFreq: Boolean, export: Boolean, filterQuery: Boolean, posQuery: String, slop: Int, numWorkers: Int)
  
  val defaultCliOption = CliOption(false, false, false, false, "unord", 0, Runtime.getRuntime.availableProcessors)
  
  val parser = new scopt.OptionParser[CliOption]("search") {
    head("search", "0.x")
    note("Run Lucene search web service unless one of --indexer, --docFreq or --filterQuery is specified.")
    opt[Unit]('i', "indexer") action { (_, c) =>
      c.copy(indexer = true)
    } text (s"create Lucene indices (default ${defaultCliOption.indexer})")
    opt[Unit]('d', "docFreq") action { (_, c) =>
      c.copy(docFreq = true)
    } text (s"output term document frequencies from index as CSV (default ${defaultCliOption.docFreq})")
    opt[Unit]('e', "export") action { (_, c) =>
      c.copy(export = true)
    } text (s"output the stored JSON for each doc (default ${defaultCliOption.export})")
    opt[Unit]('f', "filterQuery") action { (_, c) =>
      c.copy(filterQuery = true)
    } text (s"filter Query JSON from stdin to stdout, outputing only lines with all specified query terms in the index (default ${defaultCliOption.filterQuery})")
    opt[String]('p', "posQuery") action { (v, c) =>
      c.copy(posQuery = v)
    } text (s"position query uses ord | unord query, (default ${defaultCliOption.posQuery})")
    opt[Int]('s', "slop") action { (v, c) =>
      c.copy(slop = v)
    } text (s"slop for posQuery, (default ${defaultCliOption.slop})")
    opt[Int]('n', "numWorkers") action { (v, c) =>
      c.copy(numWorkers = v)
    } text (s"numWorkers for CLI queries, (default ${defaultCliOption.numWorkers} the number of CPUs)")
    help("help") text ("prints this usage text")
  }
    
  // TODO: perhaps use [Guava's BloomFilter](https://github.com/google/guava/blob/master/guava/src/com/google/common/hash/BloomFilter.java)
  // to internally filter out queries containing terms not in the index, rather than doing this filtering in pre-processing scripts.
  def main(args: Array[String]): Unit = {
    try {
      parser.parse(args, defaultCliOption).foreach { c => 
        if (c.indexer) Indexer.run(c)
        else if (c.docFreq) DocFreq.writeDocFreqs(c)
        else if (c.filterQuery) DocFreq.filterQuery(c)
        else if (c.export) Search.cliExportDocIds(c)
        else Search.cliPosDocSearch(c)
      }
    } catch {
      case NonFatal(e) => log.error("Main.main:", e)
    }
  }
  
}