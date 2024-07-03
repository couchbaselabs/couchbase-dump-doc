Simple application that gets all data for a particular document, including all xattrs.

The only dependency is Java 8+ JDK, which can be installed with https://sdkman.io.

Run the application with:

```
git clone https://github.com/couchbaselabs/couchbase-dump-doc
cd couchbase-dump-doc
./gradlew run --args="--host couchbase://localhost --username YourUsername --password YourPassword --bucket default --scope _default --collection _default --docId test"
```

Or compile it to an uberjar and run that:

```
git clone https://github.com/couchbaselabs/couchbase-dump-doc
cd couchbase-dump-doc
./gradlew shadowJar
java -jar ./build/libs/couchbase-dump-doc-0.1.0.jar --host couchbase://localhost --username YourUsername --password YourPassword --bucket default --scope _default --collection _default --docId test
```

If successful the output will include:

```
11:20:47 INFO  [com.couchbase.test.Main:65] Reading doc test from default._default._default
11:20:47 INFO  [com.couchbase.test.Main:76] $document contents: {"exptime":0,"deleted":false,"CAS":"0x17deac8bccdb0000","seqno":"0x0000000000041a7b","datatype":["json","xattr"],"vbucket_uuid":"0x0000cf31e91faf16","flags":33554432,"revid":"268098","value_crc32c":"0x5451e331","value_bytes":13,"last_modified":"1720001824"}
11:20:47 INFO  [com.couchbase.test.Main:77] $XTOC contents: ["txn"]
11:20:47 INFO  [com.couchbase.test.Main:78] Body length: 13 bytes
11:20:47 INFO  [com.couchbase.test.Main:79] Body written to test.content.bin
11:20:47 INFO  [com.couchbase.test.Main:85] Fetching xattr: txn
11:20:47 INFO  [com.couchbase.test.Main:90] xattr txn length: 14 bytes
11:20:47 INFO  [com.couchbase.test.Main:91] xattr txn content written to test.txn.content.bin
```