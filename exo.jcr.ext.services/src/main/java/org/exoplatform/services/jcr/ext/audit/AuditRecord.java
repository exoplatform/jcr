/*
 * Copyright (C) 2003-2007 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.jcr.ext.audit;

import java.util.Calendar;

import javax.jcr.Value;

import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.observation.ExtendedEventType;

/**
 * Created by The eXo Platform SAS .
 * 
 * @author Gennady Azarenkov
 * @version $Id: $
 */

public class AuditRecord implements Comparable<AuditRecord> {

  private final String        userId;

  private final int           eventType;

  private final Calendar      date;

  private final InternalQName propertyName;

  private final Value[]       oldValue;

  private final Value[]       newValue;

  private final String        version;

  private final String        versionName;

  public AuditRecord(String userId,
                     int eventType,
                     Calendar date,
                     InternalQName propertyName,
                     Value[] oldValue,
                     Value[] newValue,
                     String version,
                     String versionName) {
    this.userId = userId;
    this.eventType = eventType;
    this.date = date;
    this.propertyName = propertyName;
    this.oldValue = oldValue;
    this.newValue = newValue;
    this.version = version;
    this.versionName = versionName;
  }

  public Calendar getDate() {
    return date;
  }

  public int getEventType() {
    return eventType;
  }

  public String getUserId() {
    return userId;
  }

  public String getEventTypeName() {
    return ExtendedEventType.nameFromValue(eventType);
  }

  public InternalQName getPropertyName() {
    return propertyName;
  }

  public Value[] getNewValues() {
    return newValue;
  }

  public Value[] getOldValues() {
    return oldValue;
  }

  public int compareTo(AuditRecord otherRecord) {
    return date.compareTo(otherRecord.getDate());
  }

  /**
   * Returns version UUID related to this audit record. <br>
   * Use Session.getNodeByUUID(String) to obtain the version Node instance. <br>
   * NOTE: Version UUID will has no sense if version will be removed.
   * 
   * @return String with version UUID or null if auditable node was not
   *         mix:versionable at the audit record time
   */
  public String getVersion() {
    return version;
  }

  /**
   * Returns version name related to this audit record. Version name for
   * information purpose only. <br>
   * NOTE: Version name can be helpful after the version will be removed.
   * 
   * @return String in format VERSION_NAME 'VERSION_LABEL_1' 'VERSION_LABEL_2'
   *         ... 'VERSION_LABEL_N' or null if auditable node was not
   *         mix:versionable at the audit record time
   */
  public String getVersionName() {
    return versionName;
  }

}
