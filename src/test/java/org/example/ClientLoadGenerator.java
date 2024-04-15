package org.example;

import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.HdrHistogram.Histogram;
import org.mortbay.jetty.load.generator.LoadGenerator;
import org.mortbay.jetty.load.generator.Resource;
import org.mortbay.jetty.load.generator.listeners.ReportListener;

public class ClientLoadGenerator
{
    private static final String OUTPUT_FILE = "client-output.txt";

    public static void generateLoad(Duration duration, int resourceRate, String host, int port, int numWrites, int bufferSize, String logPrefix) throws Exception
    {
        Path output = Paths.get(logPrefix + OUTPUT_FILE);
        if (Files.exists(output))
            Files.delete(output);

        Resource resource = new Resource(String.format("/?bufferSize=%s&numWrites=%s", bufferSize, numWrites))
            .requestHeader("x-appengine-timeout-ms", "60000");
        ReportListener reportListener = new ReportListener(1, TimeUnit.MINUTES.toNanos(10), 3);
        LoadGenerator generator = LoadGenerator.builder()
            .scheme("http")
            .host(host)
            .port(port)
            .resource(resource)
            .resourceRate(resourceRate)
            .warmupIterationsPerThread(10)
            .runFor(duration.toMillis(), TimeUnit.MILLISECONDS)
            .listener(reportListener)
            .resourceListener(new Resource.NodeListener()
            {
                final AtomicBoolean failed = new AtomicBoolean(false);

                @Override
                public void onResourceNode(Resource.Info info)
                {
                    if (info.getFailure() != null && failed.compareAndSet(false, true))
                        logFailure(info.getFailure(), output);
                }
            })
            .resourceListener(reportListener)
            .build();
        generator.addBean(reportListener);

        generator.begin().get();
        ReportListener.Report report = reportListener.whenComplete().join();
        generateClientReport(report, output);
    }

    public static void generateClientReport(ReportListener.Report report, Path outputFile)
    {
        StringBuilder out = new StringBuilder();
        out.append("beginInstant: ").append(report.getBeginInstant()).append("\n");
        out.append("completeInstant: ").append(report.getCompleteInstant().toString()).append("\n");
        out.append("recordingDuration: ").append(report.getRecordingDuration().toMillis()).append("\n");
        out.append("availableProcessors: ").append(Runtime.getRuntime().availableProcessors()).append("\n");
        out.append("averageCPUPercent: ").append(report.getAverageCPUPercent()).append("\n");
        out.append("requestRate: ").append(report.getRequestRate()).append("\n");
        out.append("responseRate: ").append(report.getResponseRate()).append("\n");
        out.append("sentBytesRate: ").append(report.getSentBytesRate()).append("\n");
        out.append("receivedBytesRate: ").append(report.getReceivedBytesRate()).append("\n");
        out.append("failures: ").append(report.getFailures()).append("\n");
        out.append("1xx: ").append(report.getResponses1xx()).append("\n");
        out.append("2xx: ").append(report.getResponses2xx()).append("\n");
        out.append("3xx: ").append(report.getResponses3xx()).append("\n");
        out.append("4xx: ").append(report.getResponses4xx()).append("\n");
        out.append("5xx: ").append(report.getResponses5xx()).append("\n");

        Histogram histogram = report.getResponseTimeHistogram();
        out.append("histogram min: ").append(toMicros(histogram.getMinValue())).append("\n");
        out.append("histogram mean: ").append(toMicros(histogram.getMean())).append("\n");
        out.append("histogram max: ").append(toMicros(histogram.getMaxValue())).append("\n");
        out.append("histogram 50: ").append(toMicros(histogram.getValueAtPercentile(50))).append("\n");

        for (int i = 95; i < 100; i++)
        {
            out.append("histogram ").append(i).append(": ").append(toMicros(histogram.getValueAtPercentile(i))).append("\n");
        }

        try (PrintStream printStream = new PrintStream(new FileOutputStream(outputFile.toFile(), true)))
        {
            printStream.println(Instant.now());
            printStream.println(out);
        }
        catch (Throwable t)
        {
            t.printStackTrace(System.err);
        }
    }

    public static void logFailure(Throwable t, Path outputFile)
    {
        try (PrintStream printStream = new PrintStream(new FileOutputStream(outputFile.toFile(), true)))
        {
            printStream.println(Instant.now());
            t.printStackTrace(printStream);
        }
        catch (Throwable x)
        {
            x.printStackTrace(System.err);
        }
    }

    private static long toMicros(long nanos)
    {
        return nanos / 1000;
    }

    private static double toMicros(double nanos)
    {
        return nanos / 1000;
    }
}
