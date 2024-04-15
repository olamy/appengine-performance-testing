package org.example;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jetty.util.IO;

public class AppEngineServerBuilder
{
    public static String getRuntimeDeployment()
    {
        return System.getProperty("user.dir") + "/src/main/runtime-deployment";
    }

    public static String getAppLocation()
    {
        return System.getProperty("user.dir") + "/target/appengine-staging";
    }

    public static Process run(boolean httpConnector, int port, String runtimeDeployment, String appLocation) throws Exception
    {
        String javaHome = System.getProperty("java.home");
        List<String> command = new ArrayList<>();
        command.add(javaHome + "/bin/java");
        command.add("--add-opens");
        command.add("java.base/java.lang=ALL-UNNAMED");
        command.add("--add-opens");
        command.add("java.base/java.nio.charset=ALL-UNNAMED");
        command.add("--add-opens");
        command.add("java.logging/java.util.logging=ALL-UNNAMED");
        command.add("--add-opens");
        command.add("java.base/java.util.concurrent=ALL-UNNAMED");
        command.add("-showversion");
        command.add("-XX:+PrintCommandLineFlags");
        command.add("-XX:NativeMemoryTracking=summary");
        command.add("-Djava.class.path=" + runtimeDeployment + "/runtime-main.jar");
        command.add("-Dclasspath.runtimebase=" + runtimeDeployment + ":");
        command.add("-Dappengine.use.HttpConnector=" + httpConnector);
        command.add("com/google/apphosting/runtime/JavaRuntimeMainWithDefaults");
        command.add("--fixed_application_path=" + appLocation);
        command.add("--jetty_http_port=" + port);
        command.add(runtimeDeployment);

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.inheritIO();
        return processBuilder.start();
    }
}
