package com.io7m.osgilog2.main;

import org.apache.felix.framework.util.FelixConstants;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Policy;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.TimeUnit;

public final class Main
{
  private static final Logger LOG;

  static {
    Policy.setPolicy(new PermissivePolicy());

    LOG = LoggerFactory.getLogger(Main.class);
  }

  private Main()
  {

  }

  private static final class FelixLogger extends org.apache.felix.framework.Logger
  {
    private static final Logger LOG;

    static {
      LOG = LoggerFactory.getLogger(FelixLogger.class);
    }

    FelixLogger()
    {

    }

    @Override
    protected void doLog(
      final Bundle bundle,
      final ServiceReference sr,
      final int level,
      final String msg,
      final Throwable throwable)
    {
      switch (level) {
        case LOG_DEBUG: {
          LOG.debug(
            "[{}]: {}: ",
            bundle.getSymbolicName(),
            msg,
            throwable);
          break;
        }
        case LOG_ERROR: {
          LOG.error(
            "[{}]: {}: ",
            bundle.getSymbolicName(),
            msg,
            throwable);
          break;
        }
        case LOG_INFO: {
          LOG.info("[{}]: {}: ", bundle.getSymbolicName(), msg, throwable);
          break;
        }
        case LOG_WARNING: {
          LOG.warn("[{}]: {}: ", bundle.getSymbolicName(), msg, throwable);
          break;
        }
      }
    }

    @Override
    protected void doLog(
      final int level,
      final String msg,
      final Throwable throwable)
    {
      switch (level) {
        case LOG_DEBUG: {
          LOG.debug("{}: ", msg, throwable);
          break;
        }
        case LOG_ERROR: {
          LOG.error("{}: ", msg, throwable);
          break;
        }
        case LOG_INFO: {
          LOG.info("{}: ", msg, throwable);
          break;
        }
        case LOG_WARNING: {
          LOG.warn("{}: ", msg, throwable);
          break;
        }
      }
    }
  }

  public static void main(
    final String[] args)
    throws Exception
  {
    final FrameworkFactory frameworkFactory =
      ServiceLoader.load(FrameworkFactory.class).iterator().next();

    final Map<String, Object> config = new HashMap<>();
    config.put(Constants.FRAMEWORK_STORAGE, "/tmp/felix");
    config.put(Constants.FRAMEWORK_STORAGE_CLEAN, "onFirstInit");
    config.put(Constants.FRAMEWORK_SECURITY, "osgi");
    config.put(FelixConstants.LOG_LEVEL_PROP, "999");
    config.put(FelixConstants.LOG_LOGGER_PROP, new FelixLogger());

    final StringBuilder sb = new StringBuilder(128);
    sb.append("org.slf4j; version=1.7.21");
    sb.append(",");
    sb.append("org.slf4j.*; version=1.7.21");
    config.put(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, sb.toString());

    final Object cast = config;
    @SuppressWarnings("unchecked")
    final Map<String, String> config_strings = (Map<String, String>) cast;

    Main.LOG.debug("starting framework");

    final Framework framework = frameworkFactory.newFramework(config_strings);
    framework.start();

    Main.LOG.debug("security manager: {}", System.getSecurityManager());

    try {
      final BundleContext c = framework.getBundleContext();

      Main.LOG.debug("installing bundles");
      final List<Bundle> bundles = new LinkedList<>();
      bundles.add(Main.install(c, "org.apache.felix.framework.security"));
      bundles.add(Main.install(c, "org.apache.felix.log"));
      bundles.add(Main.install(c, "logservice"));
      bundles.add(Main.install(c, "test-osgi-logging"));
      bundles.add(Main.install(c, "test-logback"));

      for (final Bundle bundle : bundles) {
        Main.LOG.debug("starting: {}", bundle);
        bundle.start();
      }

      for (final Bundle bundle : bundles) {
        final int state = bundle.getState();
        switch (state) {
          case Bundle.UNINSTALLED: {
            Main.LOG.debug("bundle {} is UNINSTALLED", bundle);
            break;
          }
          case Bundle.INSTALLED: {
            Main.LOG.debug("bundle {} is INSTALLED", bundle);
            break;
          }
          case Bundle.RESOLVED: {
            Main.LOG.debug("bundle {} is RESOLVED", bundle);
            break;
          }
          case Bundle.STARTING: {
            Main.LOG.debug("bundle {} is STARTING", bundle);
            break;
          }
          case Bundle.STOPPING: {
            Main.LOG.debug("bundle {} is STOPPING", bundle);
            break;
          }
          case Bundle.ACTIVE: {
            Main.LOG.debug("bundle {} is ACTIVE", bundle);
            break;
          }
        }
      }

    } finally {
      Main.LOG.debug("shutting down");
      framework.stop();
      framework.waitForStop(
        TimeUnit.MILLISECONDS.convert(2L, TimeUnit.SECONDS));
      Main.LOG.debug("exiting");
    }
  }

  private static Bundle install(
    final BundleContext c,
    final String pack)
    throws BundleException
  {
    Main.LOG.debug("installing {}", pack);
    return c.installBundle("file:/tmp/osgilog2/" + pack + ".jar");
  }
}
