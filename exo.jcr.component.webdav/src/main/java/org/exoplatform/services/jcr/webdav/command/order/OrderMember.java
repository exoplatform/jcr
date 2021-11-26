/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */

package org.exoplatform.services.jcr.webdav.command.order;

import org.exoplatform.common.util.HierarchicalProperty;

import javax.xml.namespace.QName;

/**
 * Created by The eXo Platform SAS.
 * @author Vitaly Guly - gavrikvetal@gmail.com.
 * 
 * @version $Id: $
 */

public class OrderMember
{

   /**
    * Represents order mamber.
    */
   private HierarchicalProperty member;

   /**
    * order segment.
    */
   private String segment;

   /**
    * status.
    */
   private int status;

   /**
    * Constructor.
    * 
    * @param member order member
    */
   public OrderMember(HierarchicalProperty member)
   {
      this.member = member;
      HierarchicalProperty segmentProperty = member.getChild(new QName("DAV:", "segment"));
      segment = segmentProperty.getValue();
   }

   /**
    * Segment getter.
    * 
    * @return segment
    */
   public String getSegment()
   {
      return segment;
   }

   /**
    * Position getter.
    * 
    * @return position property value.
    */
   public QName getPosition()
   {
      return member.getChild(new QName("DAV:", "position")).getChild(0).getName();
   }

   /**
    * Position segment getter.
    * 
    * @return position segment.
    */
   public String getPositionSegment()
   {
      HierarchicalProperty position = member.getChild(new QName("DAV:", "position"));
      return position.getChild(0).getChild(new QName("DAV:", "segment")).getValue();
   }

   /**
    * Status getter.
    * 
    * @return status
    */
   public int getStatus()
   {
      return status;
   }

   /**
    * Status setter.
    * 
    * @param status status
    */
   public void setStatus(int status)
   {
      this.status = status;
   }

}
