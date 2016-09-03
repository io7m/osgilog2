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

package com.io7m.osgilog2.test_osgi_log;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;

/**
 * A simple bundle activator that writes a message to any available OSGi log
 * service. The purpose of this is to test that logged messages ultimately end
 * up going through the host's logging configuration, rather than one inside the
 * OSGi container.
 */

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
