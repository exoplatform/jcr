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
package org.exoplatform.services.ftp.listcode;

import org.exoplatform.services.ftp.FtpConst;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import javax.jcr.Node;
import javax.jcr.Property;

/**
 * Created by The eXo Platform SAS Author : Vitaly Guly gavrik-vetal@ukr.net/mail.ru
 * 
 * @version $Id: $
 */

public class FtpFileInfoImpl implements FtpFileInfo
{

   protected static final Log LOG = ExoLogger.getLogger(FtpConst.FTP_PREFIX + "FtpFileInfoImpl");

   protected String _name = "";

   protected boolean _isCollection = true;

   protected long _size = 0;

   protected String dateTime = "";

   protected String _month = "Jan";

   protected int _day = 1;

   protected String _time = "00:00";

   public static final String[] MONTHES =
      {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

   public static final String TIME_MASK = "HH:mm";

   public void initFromNode(Node node) throws Exception
   {
      _name = node.getName();

      if (!(node.isNodeType(FtpConst.NodeTypes.NT_FOLDER) || (node.isNodeType(FtpConst.NodeTypes.NT_FILE))))
      {
         return;
      }

      // JCR_CREATED
      Calendar calendar;

      if (node.isNodeType(FtpConst.NodeTypes.NT_FILE))
      {
         _isCollection = false;
         Node contentNode = node.getNode(FtpConst.NodeTypes.JCR_CONTENT);
         Property dataProp = contentNode.getProperty(FtpConst.NodeTypes.JCR_DATA);
         _size = dataProp.getLength();
         calendar = contentNode.getProperty(FtpConst.NodeTypes.JCR_LASTMODIFIED).getDate();
      }
      else
      {
         calendar = node.getProperty(FtpConst.NodeTypes.JCR_CREATED).getDate();
      }

      SimpleDateFormat dateFormat = new SimpleDateFormat(TIME_MASK, Locale.ENGLISH);

      _time = dateFormat.format(calendar.getTime());
      _month = MONTHES[calendar.getTime().getMonth()];
      _day = calendar.getTime().getDate();
   }

   public void setName(String name)
   {
      _name = name;
   }

   public String getName()
   {
      return _name;
   }

   public void setType(boolean collection)
   {
      _isCollection = collection;
   }

   public boolean isCollection()
   {
      return _isCollection;
   }

   public void setSize(long size)
   {
      _size = size;
   }

   public long getSize()
   {
      return _size;
   }

   public void setDateTime(String dateTime)
   {
      this.dateTime = dateTime;
   }

   public String getDateTime()
   {
      return dateTime;
   }

   public String getMonth()
   {
      return _month;
   }

   public int getDay()
   {
      return _day;
   }

   public String getTime()
   {
      return _time;
   }

}
