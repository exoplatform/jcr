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
package org.exoplatform.services.jcr.ext.backup.impl;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by The eXo Platform SAS Author : Peter Nedonosko peter.nedonosko@exoplatform.com.ua
 * 15.01.2008
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: BackupMessagesLog.java 760 2008-02-07 15:08:07Z pnedonosko $
 */
public class BackupMessagesLog
{

   public static final int MESSAGES_MAXSIZE = 250;

   protected Log log = ExoLogger.getLogger("ext.BackupMessagesLog");

   private final List<BackupMessage> messages = new ArrayList<BackupMessage>();

   private final int messagesMaxSize;

   public BackupMessagesLog()
   {
      this.messagesMaxSize = MESSAGES_MAXSIZE;
   }

   public BackupMessagesLog(int messagesMaxSize)
   {
      this.messagesMaxSize = messagesMaxSize;
   }

   public void addError(String message, Throwable e)
   {
      synchronized (messages)
      {
         messages.add(new BackupError(message, e));
         removeEldest();
      }
   }

   public void addMessage(String message)
   {
      synchronized (messages)
      {
         messages.add(new BackupMessage(message));
         removeEldest();
      }
   }

   private void removeEldest()
   {
      if (messages.size() > messagesMaxSize)
      {
         // remove eldest
         int curentSize = messages.size();
         for (Iterator<BackupMessage> i = messages.iterator(); i.hasNext() && (curentSize > messagesMaxSize);)
         {
            i.next();
            i.remove();
            curentSize--;
         }
      }
   }

   public BackupMessage[] getMessages()
   {
      synchronized (messages)
      {
         BackupMessage[] copy = new BackupMessage[messages.size()];
         for (int i = 0; i < messages.size(); i++)
         {
            BackupMessage m = messages.get(i);
            copy[i] =
               m instanceof BackupError ? new BackupError(((BackupError)m).getMessage(), ((BackupError)m).stackTraces)
                  : new BackupMessage(m.getMessage());
         }
         return copy;
      }
   }

   public void clear()
   {
      messages.clear();
   }

}
