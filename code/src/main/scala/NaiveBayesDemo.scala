/* NaiveBayesDemo.scala */
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.SparkConf
import org.apache.spark.rdd.RDD
import org.apache.spark.mllib.feature.HashingTF
import org.apache.spark.mllib.linalg.Vector
import org.apache.spark.mllib.classification.{NaiveBayes, NaiveBayesModel}
import org.apache.spark.mllib.regression.LabeledPoint


object NaiveBayesDemo {
  def main(args: Array[String]) {
    // SparkContext set up
    val conf = new SparkConf().setAppName("Naive Bayes").set("spark.executor.memory", "12g")
    val sc = new SparkContext(conf)

    // Load all files from the positive and negative directories
    val posData = "hdfs://ip-172-31-25-189.us-west-2.compute.internal:9000/txt_sentoken/pos/"
    val posDocuments: RDD[Seq[String]] = sc.wholeTextFiles(posData).map{case (a, b) => b.split(" ").toSeq}
    val negData = "hdfs://ip-172-31-25-189.us-west-2.compute.internal:9000/txt_sentoken/neg/"
    val negDocuments: RDD[Seq[String]] = sc.wholeTextFiles(negData).map{case (a, b) => b.split(" ").toSeq}

    // Feature Extraction using Term frequency-inverse document frequency
    val hashingTF = new HashingTF()
    // Need to merge the positive set and negative set before building the vectors
    val totalDocuments = posDocuments ++ negDocuments
    val totalTf: RDD[Vector] = hashingTF.transform(totalDocuments)

    // This is needed because we cannot pass "key-ed" data to hashingTF.transform()
    val totalIndexedTf = totalTf.zipWithIndex()
    val totalLabelData = totalIndexedTf.map { case(a, b) =>
      LabeledPoint(if (b < 1000) 1.0 else 0.0, a)
    }

    // Split the data into 80% training set and 20% testing set
    val splits = totalLabelData.randomSplit(Array(0.8, 0.2), seed = 11L)
    val training = splits(0)
    val test = splits(1)

    // Apply Naive Bayes training
    val model = NaiveBayes.train(training, lambda = 1.0, modelType = "multinomial")

    // Apply prediction
    val predictionAndLabel = test.map(p => (model.predict(p.features), p.label))

    // Compare the predicted results and label to compute accuracy
    val accuracy = 1.0 * predictionAndLabel.filter(x => x._1 == x._2).count() / test.count()
    println("Accuracy: %s".format(accuracy))

    // References:
    // https://spark.apache.org/docs/1.6.0/mllib-feature-extraction.html#tf-idf
    // https://spark.apache.org/docs/1.6.0/mllib-naive-bayes.html
  }
}
