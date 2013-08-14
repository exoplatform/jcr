/*
 * Copyright (C) 2013 eXo Platform SAS.
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
package org.exoplatform.applications.jcr.examples.scope;

import org.exoplatform.container.spi.DefinitionByType;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

/**
 * This class is the entry point to all the ids.
 * 
 * @author <a href="mailto:nfilotto@exoplatform.com">Nicolas Filotto</a>
 * @version $Id$
 *
 */
@Dependent
@DefinitionByType
public class RootIdProvider
{
   private final RequestIdProvider rip;

   private final SessionIdProvider seip;

   private final ApplicationIdProvider aip;

   private final DependentIdProvider dip;

   private final SingletonIdProvider siip;

   @Inject
   public RootIdProvider(RequestIdProvider rip, SessionIdProvider seip, ApplicationIdProvider aip, DependentIdProvider dip,
      SingletonIdProvider siip)
   {
      this.rip = rip;
      this.seip = seip;
      this.aip = aip;
      this.dip = dip;
      this.siip = siip;
   }

   public int getIdRequest()
   {
      return rip.getId();
   }

   public int getIdSession()
   {
      return seip.getId();
   }

   public int getIdApplication()
   {
      return aip.getId();
   }

   public int getIdDependent()
   {
      return dip.getId();
   }

   public int getIdSingleton()
   {
      return siip.getId();
   }
}
