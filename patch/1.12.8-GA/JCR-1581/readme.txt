Summary

    * Status: NFS stale handle
    * CCP Issue: CCP-766, Product Jira Issue: JCR-1581.
    * Complexity: high

The Proposal
Problem description

What is the problem to fix?

    *  We are running in a two node cluster sharing a single NFS mount for the data/gatein directory with the following configuration.properties settings:

      Data
      gatein.data.dir=${jboss.server.data.dir}/gatein
      DB
      gatein.db.data.dir=${gatein.data.dir}/db

      # JCR
      gatein.jcr.config.type=cluster
      gatein.jcr.datasource.name=java:gatein-jcr
      gatein.jcr.datasource.dialect=auto

      gatein.jcr.data.dir=${gatein.data.dir}/jcr
      gatein.jcr.storage.data.dir=${gatein.jcr.data.dir}/values
      gatein.jcr.cache.config=classpath:/conf/jcr/jbosscache/${gatein.jcr.config.type}/config.xml
      gatein.jcr.lock.cache.config=classpath:/conf/jcr/jbosscache/${gatein.jcr.config.type}/lock-config.xml
      gatein.jcr.index.data.dir=${gatein.jcr.data.dir}/lucene
      gatein.jcr.index.changefilterclass=org.exoplatform.services.jcr.impl.core.query.jbosscache.JBossCacheIndexChangesFilter
      gatein.jcr.index.cache.config=classpath:/conf/jcr/jbosscache/cluster/indexer-config.xml
      gatein.jcr.jgroups.config=classpath:/conf/jcr/jbosscache/cluster/udp-mux.xml

      We are seeing the following exceptions in server.log file

      java.io.IOException: Stale NFS file handle
      at java.io.RandomAccessFile.readBytes(Native Method)
      at java.io.RandomAccessFile.read(RandomAccessFile.java:322)
      at org.apache.lucene.store.FSDirectory$FSIndexInput.readInternal(FSDirectory.java:596)
      at org.apache.lucene.store.BufferedIndexInput.readBytes(BufferedIndexInput.java:136)
      at org.apache.lucene.index.CompoundFileReader$CSIndexInput.readInternal(CompoundFileReader.java:247)
      at org.apache.lucene.store.BufferedIndexInput.refill(BufferedIndexInput.java:157)
      at org.apache.lucene.store.BufferedIndexInput.readByte(BufferedIndexInput.java:38)
      at org.apache.lucene.store.IndexInput.readVInt(IndexInput.java:78)

Fix description

How is the problem fixed?

    *  It is fixed by addition IndexInfos.write() call exactly after replaceIndex operations done by IndexMerger

Patch information:
Patch files: JCR-1581.patch

Tests to perform

Reproduction test

    * Content manipulation:

            Run 2 nodes, try to

   1.  Go to System Tab
   2.  Click on Import Node
   3.  Browse files to add 
   4.  Select Zip option and click on import 

    *  We also reproduce the problem when we start the fourth node without any content manipulation

Tests performed at DevLevel

    * Run 2 nodes with NFS3 shared folder for lucene indexes. Simultaneously add a huge number of pdf and txt files on both nodes via WebDav or FTP.

Tests performed at QA/Support Level
*


Documentation changes

Documentation changes:

    * none

Configuration changes

Configuration changes:

    * none

Will previous configuration continue to work?

    * yes

Risks and impacts

Can this bug fix have any side effects on current client projects?

    * no

Is there a performance risk/cost?

    * no

Validation (PM/Support/QA)

PM Comment
*Patch approved by PM

Support Comment
*Support review : patch validated

QA Feedbacks
*

