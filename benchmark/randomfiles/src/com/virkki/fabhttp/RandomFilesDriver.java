
package com.virkki.fabhttp;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import javax.xml.xpath.XPathExpressionException;

import com.sun.faban.driver.BenchmarkDefinition;
import com.sun.faban.driver.BenchmarkDriver;
import com.sun.faban.driver.BenchmarkOperation;
import com.sun.faban.driver.ConfigurationException;
import com.sun.faban.driver.CycleType;
import com.sun.faban.driver.DriverContext;
import com.sun.faban.driver.FixedTime;
import com.sun.faban.driver.FlatMix;
import com.sun.faban.driver.HttpTransport;
import com.sun.faban.driver.Timing;
import com.sun.faban.driver.util.ContentSizeStats;
import com.sun.faban.driver.util.Random;


@BenchmarkDefinition (
    name    = "RandomFile",
    version = "0.1",
    metric  = "req/s"
)

@BenchmarkDriver (
    name             = "RandomFile",
    threadPerScale   = 1,
    opsUnit          = "requests",
    metric           = "req/s",
    responseTimeUnit = TimeUnit.MILLISECONDS
)

@FlatMix (
    operations = { "GetRandomFile" },
    mix = { 100.0 },
    deviation = 1
)

@FixedTime (
    cycleType = CycleType.THINKTIME,
    cycleTime = 0,
    cycleDeviation = 5
)


/** ***********************************************************************************************
 * Benchmark driver to retrieve one file randomly selected from 1..fileCount over and over.
 *
 */
public class RandomFilesDriver
{
    private ContentSizeStats contentStats = null;
    private DriverContext ctx;
    private Random rng;
    private String reqPrefix;
    private final HttpTransport http;
    private int fileCount;

    public RandomFilesDriver()
        throws XPathExpressionException, ConfigurationException, IOException
    {
        ctx = DriverContext.getContext();
        rng = ctx.getRandom();
        http = HttpTransport.newInstance();

        contentStats = new ContentSizeStats(ctx.getOperationCount());
        ctx.attachMetrics(contentStats);

        // Load user-configurable run parameters from the UI form
        String serverHost = ctx.getXPathValue("/randomFile/webServer/fa:hostConfig/fa:serverHost");

        String port = ctx.getXPathValue("/randomFile/webServer/fa:hostConfig/fa:serverPort");
        int serverPort = Integer.parseInt(port);

        String directory = ctx.getXPathValue("/randomFile/webServer/fa:hostConfig/fa:directory");

        String count = ctx.getXPathValue("/randomFile/webServer/fa:hostConfig/fa:fileCount");
        fileCount = Integer.parseInt(count);

        reqPrefix = "http://" + serverHost + ":" + serverPort + directory + "/";
    }

    @BenchmarkOperation (
        name    = "GetRandomFile",
        max90th = 10, //  in responseTimeUnits
        timing  = Timing.AUTO
    )
    public void getRandomFile() throws IOException
    {
        String fileNum = Long.toString(rng.lrandom(1, fileCount));
        String req = reqPrefix + fileNum;

        http.readURL(req);

        if (http.getResponseCode() != 200) {
          throw new IOException("GET " + req + " returned code: " + http.getResponseCode());
        }

        if (ctx.isTxSteadyState()) {
            contentStats.sumContentSize[ctx.getOperationId()] += http.getContentSize();
        }
    }

}

