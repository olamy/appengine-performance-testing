package org.example;

import java.io.Serializable;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import org.eclipse.jetty.util.IO;
import org.example.jdk.LocalJdk;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mortbay.jetty.orchestrator.Cluster;
import org.mortbay.jetty.orchestrator.NodeArray;
import org.mortbay.jetty.orchestrator.NodeArrayFuture;
import org.mortbay.jetty.orchestrator.configuration.ClusterConfiguration;
import org.mortbay.jetty.orchestrator.configuration.Jvm;
import org.mortbay.jetty.orchestrator.configuration.Node;
import org.mortbay.jetty.orchestrator.configuration.SimpleClusterConfiguration;
import org.mortbay.jetty.orchestrator.configuration.SimpleNodeArrayConfiguration;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

public class AppenginePerformanceTest implements Serializable
{
    private static final String OUTPUT_DIR = "target/reports";

    public static Stream<Arguments> arguments()
    {
        return Stream.of(
            Arguments.of(3000, 5, 1024, Duration.ofSeconds(11), true, "newMode-"),
            Arguments.of(3000, 5, 1024, Duration.ofSeconds(11), false, "oldMode-")
        );
    }

    @BeforeAll
    public static void beforeAll()
    {
        Path outputDir = Paths.get(OUTPUT_DIR);
        IO.delete(outputDir);
    }

    private String getEnvVar(String name, String defaultValue)
    {
        String value = System.getenv(name);
        return value == null ? defaultValue : value;
    }

    @ParameterizedTest
    @MethodSource("arguments")
    public void test(int resourceRate, int numWrites, int bufferSize, Duration duration, boolean useHttpConnectorMode, String logPrefix) throws Exception
    {
        int serverPort = 8080;
        String clientHost = getEnvVar("SERVER_NAME", "localhost");
        String serverHost = getEnvVar("CLIENT_NAME", "localhost");
        String jdkName = getEnvVar("JDK_TO_USE", "jdk21");

        ClusterConfiguration cfg = new SimpleClusterConfiguration()
            .jvm(new Jvm(new LocalJdk(jdkName)))
            .nodeArray(new SimpleNodeArrayConfiguration("client")
                .node(new Node(clientHost)))
            .nodeArray(new SimpleNodeArrayConfiguration("server")
                .node(new Node(serverHost)));

        try (Cluster cluster = new Cluster(cfg))
        {
            NodeArray server = cluster.nodeArray("server");
            Path serverRootDir = server.rootPathOf(serverHost);
            NodeArrayFuture serverFuture = server.executeOnAll(tools ->
            {
                Process process = null;
                try
                {
                    String appLocation = Path.of(Objects.requireNonNull(getClass().getResource("/appengine-staging")).toURI()).toString();
                    String runtimeDeployment = Path.of(Objects.requireNonNull(getClass().getResource("/runtime-deployment")).toURI()).toString();
                    process = AppEngineServerBuilder.run(useHttpConnectorMode, serverPort, runtimeDeployment, appLocation);

                    AtomicBoolean complete = new AtomicBoolean();
                    ServerMetrics.recordMetrics(complete, logPrefix);
                    tools.barrier("await-test-completion", 2).await();
                    complete.set(true);
                }
                finally
                {
                    if (process != null)
                    {
                        process.destroy();
                        int exitCode = process.waitFor();
                        System.out.println("Forked JVM exited with code: " + exitCode);
                    }
                }
            });

            NodeArray client = cluster.nodeArray("client");
            Path clientRootDir = client.rootPathOf(clientHost);
            NodeArrayFuture clientFuture = client.executeOnAll(tools ->
            {
                await().atMost(30, SECONDS).until(() -> isPortAvailable(serverHost, serverPort));
                ClientLoadGenerator.generateLoad(duration, resourceRate, serverHost, serverPort, numWrites, bufferSize, logPrefix);
                tools.barrier("await-test-completion", 2).await();
            });

            // Wait for client to complete running its test, then the server should exit.
            clientFuture.get(duration.plusMinutes(1).toMillis(), TimeUnit.MILLISECONDS);
            serverFuture.get(10, TimeUnit.SECONDS);

            // Download the results.
            Path outputDir = Paths.get(OUTPUT_DIR);
            Files.createDirectories(outputDir);
            IO.copyDir(serverRootDir, outputDir);
            IO.copyDir(clientRootDir, outputDir);
        }
    }

    public static boolean isPortAvailable(String host, int port) {
        try {
            Socket socket = new Socket(host, port);
            socket.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
