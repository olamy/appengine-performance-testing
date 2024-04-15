package org.example;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jetty.util.IO;

public class ServerMetrics
{
    private static final String OUTPUT_FILE = "server-output.txt";

    public static void recordMetrics(AtomicBoolean complete, String logPrefix) throws Exception
    {
        Path output = Paths.get(logPrefix + OUTPUT_FILE);
        if (Files.exists(output))
            Files.delete(output);

        long pid = getJavaProcess();
        new Thread(() ->
        {
            try
            {
                while (!complete.get())
                {

                    try (PrintStream printStream = new PrintStream(new FileOutputStream(output.toFile(), true)))
                    {
                        printStream.println("========== " + Instant.now() + "   Output: ");
                        printStream.println(getUsageStats(pid));
                    }
                    Thread.sleep(5000);
                }
            }
            catch (Throwable t)
            {
                t.printStackTrace(System.err);
            }
        }).start();
    }

    private static long getJavaProcess()
    {
        String javaHome = System.getProperty("java.home");
        String output = execute(javaHome + "/bin/jps", "-l");
        String regexPattern = "(\\b\\d+)\\s+" + "com/google/apphosting/runtime/JavaRuntimeMainWithDefaults";
        Pattern pattern = Pattern.compile(regexPattern);
        Matcher matcher = pattern.matcher(output);
        if (!matcher.find())
            throw new IllegalStateException("did not match pid");
        return Long.parseLong(matcher.group(1));
    }

    private static String getUsageStats(long pid) throws Exception
    {
        String javaHome = System.getProperty("java.home");
        return execute("ps", "-p", String.valueOf(pid), "-o", "%cpu,size,rss") + execute(javaHome + "/bin/jcmd", Long.toString(pid), "VM.native_memory", "summary");
    }

    private static String execute(String... command)
    {
        try
        {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream())))
            {
                return IO.toString(reader);
            }
        }
        catch (Throwable t)
        {
            return t.getMessage();
        }
    }
}
