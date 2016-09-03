package com.io7m.osgilog2.logservice;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogReaderService;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public final class Activator implements BundleActivator
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(Activator.class);
  }

  private final SLF4JLogReader logger;
  private final List<LogReaderService> readers;
  private final ServiceListener listener;

  public Activator()
  {
    this.logger = new SLF4JLogReader();
    this.readers = new LinkedList<>();
    this.listener = event -> {
      final ServiceReference<?> ref = event.getServiceReference();
      if (ref == null) {
        return;
      }

      final Bundle bundle = ref.getBundle();
      if (bundle == null) {
        return;
      }

      final BundleContext context = bundle.getBundleContext();
      if (context == null) {
        return;
      }

      final LogReaderService reader = (LogReaderService) context.getService(ref);
      if (reader != null) {
        if (event.getType() == ServiceEvent.REGISTERED) {
          this.readers.add(reader);
          reader.addLogListener(this.logger);
        } else if (event.getType() == ServiceEvent.UNREGISTERING) {
          reader.removeLogListener(Activator.this.logger);
          this.readers.remove(reader);
        }
      }
    };
  }

  @Override
  public void start(
    final BundleContext context)
    throws Exception
  {
    final ServiceTracker<LogReaderService, LogReaderService> tracker =
      new ServiceTracker<>(context, LogReaderService.class.getName(), null);
    tracker.open();

    final Object[] current_readers = tracker.getServices();
    if (current_readers != null) {
      for (int index = 0; index < current_readers.length; index++) {
        final LogReaderService reader =
          (LogReaderService) current_readers[index];

        this.readers.add(reader);
        reader.addLogListener(this.logger);
      }
    }

    tracker.close();

    final String filter =
      "(objectclass=" + LogReaderService.class.getName() + ")";

    try {
      context.addServiceListener(this.listener, filter);
    } catch (final InvalidSyntaxException e) {
      Activator.LOG.error("error adding service listener: ", e);
    }
  }

  @Override
  public void stop(
    final BundleContext context)
    throws Exception
  {
    final Iterator<LogReaderService> iter = this.readers.iterator();
    while (iter.hasNext()) {
      final LogReaderService reader = iter.next();
      reader.removeLogListener(this.logger);
      iter.remove();
    }
  }
}
