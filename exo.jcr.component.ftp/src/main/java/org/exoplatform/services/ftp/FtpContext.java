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
package org.exoplatform.services.ftp;

import org.apache.commons.chain.impl.ContextBase;
import org.exoplatform.services.ftp.client.FtpClientSession;

/**
 * Created by The eXo Platform SAS Author : Vitaly Guly gavrik-vetal@ukr.net/mail.ru
 * 
 * @version $Id: $
 */

public class FtpContext extends ContextBase
{

   protected FtpClientSession clientSession;

   protected String[] params;

   public FtpContext(FtpClientSession clientSession, String[] params)
   {
      super();
      this.clientSession = clientSession;
      this.params = params;
   }

   public String[] getParams()
   {
      return params;
   }

   public FtpClientSession getFtpClientSession()
   {
      return clientSession;
   }

}
