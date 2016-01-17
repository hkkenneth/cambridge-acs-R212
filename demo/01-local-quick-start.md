# Spark demo - local mode

### Download dependencies

Java is the only dependency for local mode.

```bash
sudo apt-get update
sudo apt-get install default-jdk
```

### Download Spark

Obtain latest version of Spark from http://spark.apache.org/downloads.html

* Choose a Spark release: 1.6.0 (Jan 04 2016)
* Choose a package type:  Pre-built for Hadoop 2.6 and later

```bash
wget http://d3kbcqa49mib13.cloudfront.net/spark-1.6.0-bin-hadoop2.6.tgz
tar xzpf spark-1.6.0-bin-hadoop2.6.tgz
cd spark-1.6.0-bin-hadoop2.6/
```

### Run Spark interactive shell

Run Spark Scala shell

```bash
./bin/spark-shell
# To exit the shell, press ctrl+d
```

Check if you can access the monitoring interface at
http://your_public_ip:4040 . If your IP starts with 172., it is probably the internal IP.
Also, take a look at the configuration parameters at http://ip:4040/environment/ , e.g. spark.master (probably local[\*]), spark.scheduler.mode (probably FIFO) etc.
Exit the shell if it works well.

### Run a sample program

1. Generate a file consisting of 40,000,000 random 8-character strings. File size should be about 350MB.

  ```bash
  cat /dev/urandom | tr -dc 'a-zA-Z0-9' | fold -w 8 | head -n 40000000 > data/0.txt

  du -h data/0.txt
  # 344M    data/0.txt
  ```

2. Start the Spark Scala shell again

  ```bash
  # Create the directory for Spark history, this location is the default for "spark.eventLog.dir" configuration
  mkdir /tmp/spark-events

  # Start Spark Scala shell with history support
  ./bin/spark-shell --conf spark.eventLog.enabled=true
  ```

3. Run the sample program in shell

  ```scala
  val dataFile = "/home/ubuntu/spark-1.6.0-bin-hadoop2.6/data/0.txt"
  val data = sc.textFile(dataFile).cache()
  val numAs = data.filter(line => line.contains("a")).count()
  val numBs = data.filter(line => line.contains("b")).count()
  println("Lines with a: %s, Lines with b: %s".format(numAs, numBs))
  ```
You may see warning like this, because the default driver does not have a lot of memory allocated to it. Let's fix it by quitting the shell and start again with a higher memory capacity.
  ```
  16/01/10 20:04:31 WARN MemoryStore: Not enough space to cache rdd_1_0 in memory! (computed 104.4 MB so far)
  ```
4. Restart the shell and execute the above code snippet again.

  ```bash
  ./bin/spark-shell --driver-memory=12g --conf spark.eventLog.enabled=true
  ```
You may want to verify these:
  * The specified value of "spark.driver.memory" at http://ip:4040/environment/
  * Larger "Storage Memory" at http://ip:4040/executors/
In local mode, we change the driver's memory instead of the executor's memory because the driver and executor run in the same JVM.

5. Browse around the web UI to understand various terms and values. Pay attention to:
  * Stage duration for the first run of count() versus the subsequent one.
  * Size of input for the first run of count() versus the subsequent one.
  * Size of the cached RDDs.

6. Install scala build tool: sbt by following http://www.scala-sbt.org/0.13/tutorial/Installing-sbt-on-Linux.html

  ```bash
  echo "deb https://dl.bintray.com/sbt/debian /" | sudo tee -a /etc/apt/sources.list.d/sbt.list
  sudo apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 642AC823
  sudo apt-get update
  sudo apt-get install sbt
  ```

7. Follow self-contained example on Scala quick start guide http://spark.apache.org/docs/latest/quick-start.html#self-contained-applications
and make changes based on our above Scala code snippet.

  ```bash
  mkdir -p quick-start/src/main/scala/

  vi quick-start/simple.sbt
  vi quick-start/src/main/scala/SimpleApp.scala

  cd quick-start/
  sbt package
  # This will take a bit of time when dependencies are downloaded
  ```

8. Submit the packaged jar using spark-submit
  ```bash
  cd ..
  ./bin/spark-submit \
    --class "SimpleApp" \
    --master local[*] \
    --driver-memory=12g \
    --conf spark.eventLog.enabled=true \
    quick-start/target/scala-2.10/simple-project_2.10-1.0.jar  > stdout.log 2> stderr.log
  ```
In this case, we can only access the web UI (on port 4040) when the application is running. How do we access the logs/statuses in the future?

9. Start history server

  ```bash
  ./sbin/start-history-server.sh
  ```
Since we have enabled event log, we can access previous logs from http://ip:18080/

10. Set up HDFS according to https://hadoop.apache.org/docs/current/hadoop-project-dist/hadoop-common/SingleCluster.html
  ```bash
  # Download dependencies
  sudo apt-get install ssh
  sudo apt-get install rsync

  # Download Hadoop
  wget http://mirror.ox.ac.uk/sites/rsync.apache.org/hadoop/common/hadoop-2.6.3/hadoop-2.6.3.tar.gz
  tar xzpf hadoop-2.6.3.tar.gz

  # Find the location of java binary
  which java
  # /usr/bin/java

  # set to the root of your Java installation
  export JAVA_HOME=/usr

  cd hadoop-2.6.3/

  # set up passphraseless ssh
  # Hadoop requires master to be able to ssh to slaves
  ssh localhost
  # If you cannot ssh to localhost without a passphrase, execute the following commands:
  ssh-keygen -t dsa -P '' -f ~/.ssh/id_dsa
  cat ~/.ssh/id_dsa.pub >> ~/.ssh/authorized_keys
  export HADOOP_PREFIX=`pwd`

  # edit JAVA_HOME
  vi etc/hadoop/hadoop-env.sh

  # edit HDFS configuration
  # fs.defaultFS
  vi etc/hadoop/core-site.xml
  # dfs.replication
  vi etc/hadoop/hdfs-site.xml

  # Format HDFS
  bin/hdfs namenode -format

  # Start HDFS, you may be asked for confirmation of authenticity of host (for ssh)
  sbin/start-dfs.sh
  ```
The HDFS monitoring UI can be accessed at http://ip:50070/

11. Upload data file to HDFS

  ```bash
  bin/hdfs dfs -mkdir /spark
  bin/hdfs dfs -put ../spark-1.6.0-bin-hadoop2.6/data/0.txt /spark/0.txt
  ```

12. Repeat step 7 and 8 with the HDFS file location (i.e. hdfs://localhost:9000/spark/0.txt).
