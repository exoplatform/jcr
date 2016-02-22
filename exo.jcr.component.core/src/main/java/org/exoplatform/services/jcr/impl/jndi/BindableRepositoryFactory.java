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
package org.exoplatform.services.jcr.impl.jndi;

import org.apache.commons.collections.map.ReferenceMap;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.StandaloneContainer;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;

import java.util.Hashtable;
import java.util.Map;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;

/**
 * Created by The eXo Platform SAS.<br>
 * 
 * ObjectFactory to produce BindableRepositoryImpl objects
 * 
 * @author <a href="mailto:lautarul@gmail.com">Roman Pedchenko</a>
 * @version $Id: BindableRepositoryFactory.java 11907 2008-03-13 15:36:21Z ksm $
 */

public class BindableRepositoryFactory implements ObjectFactory
{

   static final String REPOSITORYNAME_ADDRTYPE = "repositoryName";

   static final String CONTAINERCONFIG_ADDRTYPE = "containerConfig";

   private static Map cache = new ReferenceMap(ReferenceMap.HARD, ReferenceMap.SOFT);

   public BindableRepositoryFactory()
   {
   }

   public Object getObjectInstance(Object obj, Name name, Context nameCtx, Hashtable environment) throws Exception
   {
      if (obj instanceof Reference)
      {
         Reference ref = (Reference)obj;
         synchronized (cache)
         {
            if (cache.containsKey(ref))
            {
               return cache.get(ref);
            }
            RefAddr containerConfig = ref.get(CONTAINERCONFIG_ADDRTYPE);
            String repositoryName = (String)ref.get(REPOSITORYNAME_ADDRTYPE).getContent();
            ExoContainer container = ExoContainerContext.getCurrentContainerIfPresent();
            if (containerConfig != null)
            {
               // here the code will work properly only when no StandaloneContainer instance created yet
               if (container == null)
               {
                  StandaloneContainer.setConfigurationURL((String)containerConfig.getContent());
                  container = StandaloneContainer.getInstance();
               }
            }
            ManageableRepository rep =
               ((RepositoryService)container.getComponentInstanceOfType(RepositoryService.class))
                  .getRepository(repositoryName);
            // BindableRepositoryImpl brep = new BindableRepositoryImpl(rep);
            cache.put(ref, rep);
            return rep;
         }
      }
      return null;
   }
}
