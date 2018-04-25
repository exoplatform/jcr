/*
 * Copyright (C) 2012 eXo Platform SAS.
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
package org.exoplatform.services.jcr.ext.organization;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * Contains commons instances of needed classes.
 * 
 * @author <a href="abazko@exoplatform.com">Anatoliy Bazko</a>
 * @version $Id: CommonHandler.java 34360 2009-07-22 23:58:59Z tolusha $
 */
public class JCROrgServiceHandler
{
   /**
    * Logger.
    */
   protected static final Log LOG = ExoLogger.getLogger("exo-jcr-services.JCROrgServiceHandler");

   protected final JCROrganizationServiceImpl service;

   protected final JCRCacheHandler cache;

   protected final UserHandlerImpl userHandler;

   protected final UserProfileHandlerImpl userProfileHandler;

   protected final MembershipHandlerImpl membershipHandler;

   protected final GroupHandlerImpl groupHandler;
   
   protected final MembershipTypeHandlerImpl typeHandler;

   protected final Utils utils;

   /**
    * CommonHandler constructor.
    */
   JCROrgServiceHandler(JCROrganizationServiceImpl service)
   {
      this.service = service;
      this.utils = new Utils(service);
      this.cache = service.getCacheHandler();
      this.userHandler = (UserHandlerImpl)service.getUserHandler();
      this.userProfileHandler = (UserProfileHandlerImpl)service.getUserProfileHandler();
      this.membershipHandler = (MembershipHandlerImpl)service.getMembershipHandler();
      this.groupHandler = (GroupHandlerImpl)service.getGroupHandler();
      this.typeHandler = (MembershipTypeHandlerImpl)service.getMembershipTypeHandler();
   }
}
