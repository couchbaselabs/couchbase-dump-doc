package com.couchbase.test;

import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.ClusterOptions;
import com.couchbase.client.java.json.JsonObject;
import com.couchbase.client.java.kv.LookupInSpec;
import com.couchbase.client.java.kv.MutateInOptions;
import com.couchbase.client.java.kv.MutateInSpec;
import com.couchbase.client.java.kv.StoreSemantics;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;

public class Main {
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        var options = new Options();

        options.addRequiredOption("h", "host", true, "Couchbase cluster hostname");
        options.addRequiredOption("u", "username", true, "Couchbase cluster username");
        options.addRequiredOption("p", "password", true, "Couchbase cluster password");
        options.addRequiredOption("b", "bucket", true, "Couchbase bucket name");
        options.addRequiredOption("s", "scope", true, "Couchbase scope name");
        options.addRequiredOption("c", "collection", true, "Couchbase collection name");
        options.addRequiredOption("d", "docId", true, "Couchbase document ID");

        var parser = new DefaultParser();
        CommandLine cmd;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            LOGGER.error("Error: " + e.getMessage());
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("java -jar <jar-file-name>", options);
            return;
        }

        var hostname = cmd.getOptionValue("host");
        var username = cmd.getOptionValue("username");
        var password = cmd.getOptionValue("password");
        var bucketName = cmd.getOptionValue("bucket");
        var scopeName = cmd.getOptionValue("scope");
        var collectionName = cmd.getOptionValue("collection");
        var docId = cmd.getOptionValue("docId");

        LOGGER.info("Connecting to cluster {} with user {}", hostname, username);

        try (var cluster = Cluster.connect(hostname, ClusterOptions.clusterOptions(username, password)
                .environment(env -> env.securityConfig(sec ->
                        sec.enableCertificateVerification(false))))) {
            cluster.waitUntilReady(Duration.ofSeconds(30));
            var bucket = cluster.bucket(bucketName);
            var scope = bucket.scope(scopeName);
            var collection = scope.collection(collectionName);
            var bodyFilename = docId + ".content.bin";

            LOGGER.info("Reading doc {} from {}.{}.{}", docId, bucketName, scopeName, collectionName);

            var result = collection.lookupIn(docId, List.of(
                    LookupInSpec.get("$document").xattr(),
                    LookupInSpec.get("$XTOC").xattr(),
                    LookupInSpec.get("")));

            var documentVattr = result.contentAsObject(0);
            var xtocVattr = result.contentAsArray(1);
            var bodyContent = result.contentAsBytes(2);

            LOGGER.info("$document contents: {}", documentVattr);
            LOGGER.info("$XTOC contents: {}", xtocVattr);
            LOGGER.info("Body length: {} bytes", bodyContent.length);
            LOGGER.info("Body written to {}", bodyFilename);

            Files.write(Paths.get(bodyFilename), bodyContent);

            for (Object v : xtocVattr.toList()) {
                String xattr = (String) v;
                LOGGER.info("Fetching xattr: {}", xattr);
                var xattrResult = collection.lookupIn(docId, List.of(
                        LookupInSpec.get(xattr).xattr()));
                var xattrContent = xattrResult.contentAsBytes(0);
                var xattrFilename = docId + "." + xattr + ".content.bin";
                LOGGER.info("xattr {} length: {} bytes", xattr, xattrContent.length);
                LOGGER.info("xattr {} content written to {}", xattr, xattrFilename);

                Files.write(Paths.get(xattrFilename), xattrContent);
            }
        } catch (IOException e) {
            LOGGER.error("Error writing to file: " + e.getMessage());
        }
    }
}