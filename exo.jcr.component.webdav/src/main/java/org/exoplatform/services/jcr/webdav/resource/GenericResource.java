/*
 * Copyright (C) 2009 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.services.jcr.webdav.resource;

import org.exoplatform.common.util.HierarchicalProperty;
import org.exoplatform.services.jcr.webdav.xml.WebDavNamespaceContext;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.net.URI;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.xml.namespace.QName;

/**
 * Created by The eXo Platform SARL .<br/>
 * Abstract WebDav Resource implementation It is recommended to extend this
 * class instead of implement Resource itself
 * 
 * @author Gennady Azarenkov
 * @version $Id: $
 */

public abstract class GenericResource implements Resource
{

   private static final Log LOG = ExoLogger.getLogger("org.exoplatform.services.jcr.webdav.resource.GenericResource");

   /**
    * Resource identifier.
    */
   protected final URI identifier;

   /**
    * Resource type.
    */
   protected final int type;

   /**
    * Namespace context.
    */
   protected final WebDavNamespaceContext namespaceContext;

   /**
    * Properties which are set by JCR.
    */
   protected static final Set<QName> PRESET_PROP = new HashSet<QName>();
   static
   {
      PRESET_PROP.add(DISPLAYNAME);
      PRESET_PROP.add(RESOURCETYPE);
      PRESET_PROP.add(CREATIONDATE);
   }

   /**
    * @param type resource type
    * @param identifier resource identifier
    * @param namespaceContext namespace context
    */
   public GenericResource(final int type, final URI identifier, final WebDavNamespaceContext namespaceContext)
   {
      this.type = type;
      this.identifier = identifier;
      this.namespaceContext = namespaceContext;
   }

   /**
    * {@inheritDoc}
    */
   public final URI getIdentifier()
   {
      return identifier;
   }

   /**
    * {@inheritDoc}
    */
   public final int getType()
   {
      return type;
   }

   /**
    * {@inheritDoc}
    */
   public Set<HierarchicalProperty> getProperties(boolean namesOnly) throws RepositoryException
   {
      Set<HierarchicalProperty> props = new HashSet<HierarchicalProperty>();

      Iterator<QName> propIter = PRESET_PROP.iterator();
      while (propIter.hasNext())
      {
         QName propertyName = propIter.next();

         try
         {
            props.add(namesOnly ? new HierarchicalProperty(propertyName) : getProperty(propertyName));
         }
         catch (Exception exc)
         {
            if (LOG.isTraceEnabled())
            {
               LOG.trace("An exception occurred: " + exc.getMessage());
            }
         }
      }

      return props;
   }

   /**
    * {@inheritDoc}
    */
   public final WebDavNamespaceContext getNamespaceContext()
   {
      return namespaceContext;
   }

   /**
    * Returns the information about lock.
    * 
    * @param token lock token
    * @param lockOwner lockowner
    * @param timeOut lock timeout
    * @return lock information
    */
   public static HierarchicalProperty lockDiscovery(String token, String lockOwner, String timeOut)
   {
      HierarchicalProperty lockDiscovery = new HierarchicalProperty(new QName("DAV:", "lockdiscovery"));

      HierarchicalProperty activeLock =
         lockDiscovery.addChild(new HierarchicalProperty(new QName("DAV:", "activelock")));

      HierarchicalProperty lockType = activeLock.addChild(new HierarchicalProperty(new QName("DAV:", "locktype")));
      lockType.addChild(new HierarchicalProperty(new QName("DAV:", "write")));

      HierarchicalProperty lockScope = activeLock.addChild(new HierarchicalProperty(new QName("DAV:", "lockscope")));
      lockScope.addChild(new HierarchicalProperty(new QName("DAV:", "exclusive")));

      HierarchicalProperty depth = activeLock.addChild(new HierarchicalProperty(new QName("DAV:", "depth")));
      depth.setValue("Infinity");

      if (lockOwner != null)
      {
         HierarchicalProperty owner = activeLock.addChild(new HierarchicalProperty(new QName("DAV:", "owner")));
         owner.setValue(lockOwner);
      }

      HierarchicalProperty timeout = activeLock.addChild(new HierarchicalProperty(new QName("DAV:", "timeout")));
      timeout.setValue("Second-" + timeOut);

      if (token != null)
      {
         HierarchicalProperty lockToken = activeLock.addChild(new HierarchicalProperty(new QName("DAV:", "locktoken")));
         HierarchicalProperty lockHref = lockToken.addChild(new HierarchicalProperty(new QName("DAV:", "href")));
         lockHref.setValue(token);
      }

      return lockDiscovery;
   }

   /**
    * The information about supported locks.
    * 
    * @return information about supported locks
    */
   protected HierarchicalProperty supportedLock()
   {
      HierarchicalProperty supportedLock = new HierarchicalProperty(new QName("DAV:", "supportedlock"));

      HierarchicalProperty lockEntry = new HierarchicalProperty(new QName("DAV:", "lockentry"));
      supportedLock.addChild(lockEntry);

      HierarchicalProperty lockScope = new HierarchicalProperty(new QName("DAV:", "lockscope"));
      lockScope.addChild(new HierarchicalProperty(new QName("DAV:", "exclusive")));
      lockEntry.addChild(lockScope);

      HierarchicalProperty lockType = new HierarchicalProperty(new QName("DAV:", "locktype"));
      lockType.addChild(new HierarchicalProperty(new QName("DAV:", "write")));
      lockEntry.addChild(lockType);

      return supportedLock;
   }

   /**
    * The information about supported methods.
    * 
    * @return information about supported methods
    */
   protected HierarchicalProperty supportedMethodSet()
   {
      HierarchicalProperty supportedMethodProp = new HierarchicalProperty(SUPPORTEDMETHODSET);

      supportedMethodProp.addChild(new HierarchicalProperty(new QName("DAV:", "supported-method"))).setAttribute(
         "name", "PROPFIND");
      supportedMethodProp.addChild(new HierarchicalProperty(new QName("DAV:", "supported-method"))).setAttribute(
         "name", "OPTIONS");
      supportedMethodProp.addChild(new HierarchicalProperty(new QName("DAV:", "supported-method"))).setAttribute(
         "name", "DELETE");
      supportedMethodProp.addChild(new HierarchicalProperty(new QName("DAV:", "supported-method"))).setAttribute(
         "name", "PROPPATCH");
      supportedMethodProp.addChild(new HierarchicalProperty(new QName("DAV:", "supported-method"))).setAttribute(
         "name", "CHECKIN");
      supportedMethodProp.addChild(new HierarchicalProperty(new QName("DAV:", "supported-method"))).setAttribute(
         "name", "CHECKOUT");
      supportedMethodProp.addChild(new HierarchicalProperty(new QName("DAV:", "supported-method"))).setAttribute(
         "name", "REPORT");
      supportedMethodProp.addChild(new HierarchicalProperty(new QName("DAV:", "supported-method"))).setAttribute(
         "name", "UNCHECKOUT");
      supportedMethodProp.addChild(new HierarchicalProperty(new QName("DAV:", "supported-method"))).setAttribute(
         "name", "PUT");
      supportedMethodProp.addChild(new HierarchicalProperty(new QName("DAV:", "supported-method"))).setAttribute(
         "name", "GET");
      supportedMethodProp.addChild(new HierarchicalProperty(new QName("DAV:", "supported-method"))).setAttribute(
         "name", "HEAD");
      supportedMethodProp.addChild(new HierarchicalProperty(new QName("DAV:", "supported-method"))).setAttribute(
         "name", "COPY");
      supportedMethodProp.addChild(new HierarchicalProperty(new QName("DAV:", "supported-method"))).setAttribute(
         "name", "MOVE");
      supportedMethodProp.addChild(new HierarchicalProperty(new QName("DAV:", "supported-method"))).setAttribute(
         "name", "VERSION-CONTROL");
      supportedMethodProp.addChild(new HierarchicalProperty(new QName("DAV:", "supported-method"))).setAttribute(
         "name", "LABEL");
      supportedMethodProp.addChild(new HierarchicalProperty(new QName("DAV:", "supported-method"))).setAttribute(
         "name", "LOCK");
      supportedMethodProp.addChild(new HierarchicalProperty(new QName("DAV:", "supported-method"))).setAttribute(
         "name", "UNLOCK");

      return supportedMethodProp;

   }
}
