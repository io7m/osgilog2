/*
 * Copyright © 2016 <code@io7m.com> http://io7m.com
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 * ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 * OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Policy;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.TimeUnit;

/**
 * A command line program that starts up an OSGi container and configures
 * logging such that all log messages produced inside the container end up
 * going through the host program's logging configuration.
 */

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

  /**
   * An Apache Felix specific logger. This logger is used to capture messages
   * from the actual framework itself.
   */

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

  /**
   * The main program.
   *
   * @param args Command line arguments
   *
   * @throws Exception On errors
   */

  public static void main(
    final String[] args)
    throws Exception
  {
    /*
     * The path to the temporary directory. This directory is expected
     * to be populated with bundles that will be installed into the container.
     */

    final Path root = Files.createDirectories(Paths.get("/tmp/osgilog2"));
    final Path root_lib = Files.createDirectories(root.resolve("lib"));
    final Path root_cache = Files.createDirectories(root.resolve("cache"));

    LOG.debug("root:       {}", root);
    LOG.debug("root cache: {}", root_cache);
    LOG.debug("root lib:   {}", root_lib);

    /*
     * Get access to an OSGi framework factory and configure the required
     * properties to enable logging. Note the use of an Apache Felix specific
     * configuration value that passes in a Logger implementation.
     */

    final FrameworkFactory frameworkFactory =
      ServiceLoader.load(FrameworkFactory.class).iterator().next();

    final Map<String, Object> config = new HashMap<>();
    config.put(Constants.FRAMEWORK_STORAGE, root_cache.toString());
    config.put(Constants.FRAMEWORK_STORAGE_CLEAN, "onFirstInit");
    config.put(Constants.FRAMEWORK_SECURITY, "osgi");
    config.put(FelixConstants.LOG_LEVEL_PROP, "999");
    config.put(FelixConstants.LOG_LOGGER_PROP, new FelixLogger());

    /*
     * Expose the host's SLF4J API to the container. This ensures that
     * any time a package requires the SLF4J, the actual implementation will
     * be resolved to the one on the host.
     */

    final StringBuilder sb = new StringBuilder(128);
    sb.append("org.slf4j; version=1.7.21");
    sb.append(",");
    sb.append("org.slf4j.*; version=1.7.21");
    config.put(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, sb.toString());

    final Object cast = config;
    @SuppressWarnings("unchecked")
    final Map<String, String> config_strings = (Map<String, String>) cast;

    /*
     * Start the framework.
     */

    Main.LOG.debug("starting framework");

    final Framework framework = frameworkFactory.newFramework(config_strings);
    framework.start();

    Main.LOG.debug("security manager: {}", System.getSecurityManager());

    try {
      final BundleContext c = framework.getBundleContext();

      /*
       * Install all of the bundles.
       */

      Main.LOG.debug("installing bundles");
      final List<Bundle> bundles = new LinkedList<>();
      bundles.add(
        Main.install(c, root_lib, "org.apache.felix.framework.security"));
      bundles.add(
        Main.install(c, root_lib, "logservice"));
      bundles.add(
        Main.install(c, root_lib, "org.apache.felix.log"));
      bundles.add(
        Main.install(c, root_lib, "test-osgi-logging"));
      bundles.add(
        Main.install(c, root_lib, "test-logback"));

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
    final Path lib,
    final String pack)
    throws BundleException
  {
    final String file = "file:" + lib + "/" + pack + ".jar";
    Main.LOG.debug("installing {} ({})", pack, file);
    return c.installBundle(file);
  }
}
