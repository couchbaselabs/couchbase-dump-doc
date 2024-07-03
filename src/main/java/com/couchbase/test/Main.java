package com.couchbase.test;

import com.couchbase.client.java.*;
import com.couchbase.client.java.json.JsonArray;
import com.couchbase.client.java.json.JsonObject;
import com.couchbase.client.java.kv.*;
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
        Options options = new Options();

        options.addRequiredOption("h", "host", true, "Couchbase cluster hostname");
        options.addRequiredOption("u", "username", true, "Couchbase cluster username");
        options.addRequiredOption("p", "password", true, "Couchbase cluster password");
        options.addRequiredOption("b", "bucket", true, "Couchbase bucket name");
        options.addRequiredOption("s", "scope", true, "Couchbase scope name");
        options.addRequiredOption("c", "collection", true, "Couchbase collection name");
        options.addRequiredOption("d", "docId", true, "Couchbase document ID");

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            LOGGER.error("Error: " + e.getMessage());
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("java -jar <jar-file-name>", options);
            return;
        }

        String hostname = cmd.getOptionValue("host");
        String username = cmd.getOptionValue("username");
        String password = cmd.getOptionValue("password");
        String bucketName = cmd.getOptionValue("bucket");
        String scopeName = cmd.getOptionValue("scope");
        String collectionName = cmd.getOptionValue("collection");
        String docId = cmd.getOptionValue("docId");

        LOGGER.info("Connecting to cluster {} with user {}", hostname, username);

        try (Cluster cluster = Cluster.connect(hostname, ClusterOptions.clusterOptions(username, password)
                .environment(env -> env.securityConfig(sec ->
                        sec.enableCertificateVerification(false))))) {
            cluster.waitUntilReady(Duration.ofSeconds(30));
            Bucket bucket = cluster.bucket(bucketName);
            Scope scope = bucket.scope(scopeName);
            Collection collection = scope.collection(collectionName);
            String bodyFilename = docId + ".content.bin";

            LOGGER.info("Reading doc {} from {}.{}.{}", docId, bucketName, scopeName, collectionName);

            LookupInResult result = collection.lookupIn(docId, List.of(
                    LookupInSpec.get("$document").xattr(),
                    LookupInSpec.get("$XTOC").xattr(),
                    LookupInSpec.get("")));

            JsonObject documentVattr = result.contentAsObject(0);
            JsonArray xtocVattr = result.contentAsArray(1);
            byte[] bodyContent = result.contentAsBytes(2);

            LOGGER.info("$document contents: {}", documentVattr);
            LOGGER.info("$XTOC contents: {}", xtocVattr);
            LOGGER.info("Body length: {} bytes", bodyContent.length);
            LOGGER.info("Body written to {}", bodyFilename);

            Files.write(Paths.get(bodyFilename), bodyContent);

            for (Object v : xtocVattr.toList()) {
                String xattr = (String) v;
                LOGGER.info("Fetching xattr: {}", xattr);
                LookupInResult xattrResult = collection.lookupIn(docId, List.of(
                        LookupInSpec.get(xattr).xattr()));
                byte[] xattrContent = xattrResult.contentAsBytes(0);
                String xattrFilename = docId + "." + xattr + ".content.bin";
                LOGGER.info("xattr {} length: {} bytes", xattr, xattrContent.length);
                LOGGER.info("xattr {} content written to {}", xattr, xattrFilename);

                Files.write(Paths.get(xattrFilename), xattrContent);
            }
        } catch (IOException e) {
            LOGGER.error("Error writing to file: " + e.getMessage());
        }
    }
}