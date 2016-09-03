package com.io7m.osgilog2.test_osgi_log;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;

public final class Activator implements BundleActivator
{
  @Override
  public void start(final BundleContext context)
    throws Exception
  {
    final ServiceTracker<LogService, LogService> tracker =
      new ServiceTracker<>(context, LogService.class.getName(), null);

    tracker.open();
    try {
      final LogService service = tracker.getService();
      if (service != null) {
        System.out.println("test-osgi-logging: stdout: service is not null");
        service.log(LogService.LOG_ERROR, "LOG SERVICE HELLO!");
      } else {
        System.out.println("test-osgi-logging: stdout: service is null");
      }
    } finally {
      tracker.close();
    }
  }

  @Override
  public void stop(final BundleContext context)
    throws Exception
  {

  }
}
