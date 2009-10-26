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
package org.exoplatform.services.jcr.ext.initializer.impl;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>
 * Date: 20.03.2009
 * 
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a>
 * @version $Id: InitializationErrorPacket.java 111 2008-11-11 11:11:11Z rainf0x $
 */
public class InitializationErrorPacket extends ErrorPacket
{

   /**
    * INITIALIZATION_ERROR_PACKET. the pocket type for errors
    */
   public static final int INITIALIZATION_ERROR_PACKET = 101;

   /**
    * InitializationErrorPacket constructor.
    * 
    * @param type
    *          int, the packet type
    * @param message
    *          String, the error message
    */
   public InitializationErrorPacket(int type, String message)
   {
      super(type, message, -1);
   }
}
