package com.io7m.osgilog2.test_logback;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Activator implements BundleActivator
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(Activator.class);
  }

  @Override
  public void start(final BundleContext context)
    throws Exception
  {
    Activator.LOG.debug("starting test-logback");
  }

  @Override
  public void stop(final BundleContext context)
    throws Exception
  {
    Activator.LOG.debug("stopping test-logback");
  }
}
