package ca.uwaterloo.cs.bigdata2017w.assignment5

import org.apache.log4j._
import org.apache.spark.SparkContext
import org.apache.spark.SparkConf
import org.rogach.scallop._

object Q5 {
	val log = Logger.getLogger(getClass().getName())

	def main(argv: Array[String]) {
		val args = new Conf(argv)

		log.info("Input: " + args.input())

		val conf = new SparkConf().setAppName("Q5")
		val sc = new SparkContext(conf)

    val customer = sc.textFile(args.input() + "/customer.tbl")
      .map(line => (line.split("\\|")(0), line.split("\\|")(3).toInt))
      .filter(p => (p._2 == 3 || p._2 == 24))
    val custBroadcast = sc.broadcast(customer.collectAsMap())

    val nation = sc.textFile(args.input() + "/nation.tbl")
      .map(line => (line.split("\\|")(0).toInt, line.split("\\|")(1)))
    val nationBroadcast = sc.broadcast(nation.collectAsMap())

		val orders = sc.textFile(args.input() + "/orders.tbl")
			.map(line => (line.split("\\|")(0), line.split("\\|")(1)))
		
		val lineitem = sc.textFile(args.input() + "/lineitem.tbl")
			.map(line => {
        val orderKey = line.split("\\|")(0)
        val shipdate = line.split("\\|")(10)
        (orderKey, shipdate.substring(0, shipdate.lastIndexOf('-')))
      })
      .cogroup(orders)
      .filter(_._2._1.size != 0)
      .flatMap(p => {
        var list = scala.collection.mutable.ListBuffer[((String, String), Int)]()
        if (custBroadcast.value.contains(p._2._2.head)) {
          val nationKey = custBroadcast.value(p._2._2.head)
          val nationName = nationBroadcast.value(nationKey)
          val dates = p._2._1.iterator
          while (dates.hasNext) {
            list += (((dates.next(), nationName), 1))
          }
        }
        list
      })
      .reduceByKey(_ + _)
      .sortBy(_._1)
      .collect()
      .foreach(p => println(p._1._1, p._1._2, p._2))
	}
}
