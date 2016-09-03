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

import java.security.AllPermission;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Permissions;
import java.security.Policy;
import java.security.ProtectionDomain;
import java.security.Provider;

/**
 * <p>A Policy implementation that simply allows everything by default. The
 * OSGi container is responsible for configuring more restrictive per-bundle
 * permissions.</p>
 *
 * <p>Note that the installation of this policy <i>must</i> be the very first
 * operation that occurs in the program. The reason for this is that many
 * parts of the JDK library may implicitly cause a default policy implementation
 * to be installed, and once that has happened, the default permissions do not
 * allow it to be changed.</p>
 */

public final class PermissivePolicy extends Policy
{
  private final Permissions permissions_default;

  /**
   * Construct a new policy.
   */

  public PermissivePolicy()
  {
    final Permissions q = new Permissions();
    q.add(new AllPermission());
    q.setReadOnly();
    this.permissions_default = q;
  }

  @Override
  public Provider getProvider()
  {
    return null;
  }

  @Override
  public void refresh()
  {
    // Nothing
  }

  @Override
  public PermissionCollection getPermissions(
    final CodeSource codesource)
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public PermissionCollection getPermissions(
    final ProtectionDomain domain)
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getType()
  {
    return null;
  }

  @Override
  public Parameters getParameters()
  {
    return null;
  }

  @Override
  public boolean implies(
    final ProtectionDomain domain,
    final Permission permission)
  {
    return this.permissions_default.implies(permission);
  }
}
