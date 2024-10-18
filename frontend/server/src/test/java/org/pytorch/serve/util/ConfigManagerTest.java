package org.pytorch.serve.util;

import io.netty.handler.ssl.SslContext;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputFilter;
import java.lang.reflect.Field;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.mockito.Mockito;
import org.pytorch.serve.TestUtils;
import org.pytorch.serve.metrics.Dimension;
import org.pytorch.serve.metrics.Metric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import static org.mockito.Mockito.verify;
import static org.testng.Assert.assertEquals;

public class ConfigManagerTest {
    static {
        TestUtils.init();
    }

    private Metric createMetric(String metricName, String requestId) {
        List<Dimension> dimensions = new ArrayList<>();
        Metric metric = new Metric();
        metric.setMetricName(metricName);
        metric.setRequestId(requestId);
        metric.setUnit("Milliseconds");
        metric.setTimestamp("1542157988");
        Dimension dimension = new Dimension();
        dimension.setName("Level");
        dimension.setValue("Model");
        dimensions.add(dimension);
        metric.setDimensions(dimensions);
        return metric;
    }

    @Test
    public void test() throws IOException, GeneralSecurityException, ReflectiveOperationException {
        ConfigManager.Arguments args = new ConfigManager.Arguments();
        args.setModels(new String[] {"noop_v0.1"});
        ConfigManager.init(args);
        ConfigManager configManager = ConfigManager.getInstance();
        configManager.setProperty("keystore", "src/test/resources/keystore.p12");
        assertEquals("true", configManager.getEnableEnvVarsConfig());

        Dimension dimension;
        List<Metric> metrics = new ArrayList<>();
        // Create two metrics and add them to a list

        metrics.add(createMetric("TestMetric1", "12345"));
        metrics.add(createMetric("TestMetric2", "23478"));
        Logger logger = LoggerFactory.getLogger(ConfigManager.MODEL_SERVER_METRICS_LOGGER);
        logger.debug("{}", metrics);
        Assert.assertTrue(new File("build/logs/ts_metrics.log").exists());

        logger = LoggerFactory.getLogger(ConfigManager.MODEL_METRICS_LOGGER);
        logger.debug("{}", metrics);
        Assert.assertTrue(new File("build/logs/model_metrics.log").exists());

        Logger modelLogger = LoggerFactory.getLogger(ConfigManager.MODEL_LOGGER);
        modelLogger.debug("test model_log");
        Assert.assertTrue(new File("build/logs/model_log.log").exists());

        SslContext ctx = configManager.getSslContext();
        Assert.assertNotNull(ctx);
    }

    @Test
    public void testNoEnvVars() throws ReflectiveOperationException, IOException {
        System.setProperty("tsConfigFile", "src/test/resources/config_test_env.properties");
        ConfigManager.Arguments args = new ConfigManager.Arguments();
        args.setModels(new String[] {"noop_v0.1"});
        args.setSnapshotDisabled(true);
        ConfigManager.init(args);
        ConfigManager configManager = ConfigManager.getInstance();
        assertEquals("false", configManager.getEnableEnvVarsConfig());
        assertEquals(4, configManager.getJsonIntValue("noop", "1.0", "batchSize", 1));
        assertEquals(4, configManager.getJsonIntValue("vgg16", "1.0", "maxWorkers", 1));
    }

    @Test
    public void testWarnOnDefaultAllowedUrls() throws IOException {
        ConfigManager configManager = ConfigManager.getInstance();

        String allowedUrls = configManager.getProperty(ConfigManager.TS_ALLOWED_URLS, ConfigManager.getDefaultTsAllowedUrls());

        System.out.println("Retrieved allowed URLs: " + allowedUrls);  // Print retrieved value

        if (allowedUrls.equals(ConfigManager.getDefaultTsAllowedUrls())) {
            configManager.getLogger().warn(
                    "YOUR torchserve instance can access any URL to load models."
                            + " When deploying to production, make sure to limit the set of allowed URLs in the config.properties");
        } else {
            System.out.println("Using custom allowed URLs (not default)");  // Print if not default
        }
    }


    @Test
     void testGetAllowedUrls() throws IOException {
        ConfigManager configManager = Mockito.mock(ConfigManager.class); // Create a mock instance
        String customAllowedUrls = "https://example.com,https://my-custom-server.com";

        // Mock getProp() behavior
        Properties mockProperties = new Properties();
        mockProperties.setProperty(ConfigManager.TS_ALLOWED_URLS, customAllowedUrls);
        Mockito.when(configManager.getProp()).thenReturn(mockProperties);

        System.out.println("Mocked Properties object:");
        System.out.println(mockProperties);  // Print the mocked Properties object

        List<String> allowedUrls = configManager.getAllowedUrls();
        System.out.println("List returned by getAllowedUrls():");
        System.out.println(allowedUrls);  // Print the list returned by getAllowedUrls()

        List<String> expectedUrls = Collections.emptyList();
        assertEquals(expectedUrls, allowedUrls);
    }

}
