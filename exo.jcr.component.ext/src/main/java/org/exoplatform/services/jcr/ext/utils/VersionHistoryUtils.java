/*
 * Copyright (C) 2009 eXo Platform SEA.
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
package org.exoplatform.services.jcr.ext.utils;

import java.util.*;

import javax.jcr.Node;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;

import org.apache.commons.lang3.StringUtils;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * Created by The eXo Platform SEA
 * Author : eXoPlatform
 * toannh@exoplatform.com
 * On 8/26/15
 * Implement controlling versions history
 */
public class VersionHistoryUtils {

  protected static final Log log = ExoLogger.getLogger(VersionHistoryUtils.class);

  private static final int DOCUMENT_AUTO_DEFAULT_VERSION_MAX = 0;
  private static final int DOCUMENT_AUTO_DEFAULT_VERSION_EXPIRED = 0;

  public static final String NT_FILE          = "nt:file";
  public static final String MIX_VERSIONABLE  = "mix:versionable";
  private static String maxAllowVersionProp   = "jcr.documents.versions.max";
  private static String expirationTimeProp    = "jcr.documents.versions.expiration";

  private static int maxAllowVersion;
  private static long maxLiveTime;

  static {
    try {
      maxAllowVersion = Integer.parseInt(System.getProperty(maxAllowVersionProp));
      maxLiveTime = Integer.parseInt(System.getProperty(expirationTimeProp));
      //ignore invalid input config
      if(maxAllowVersion < 0) maxAllowVersion = DOCUMENT_AUTO_DEFAULT_VERSION_MAX;
      if(maxLiveTime < 0) maxLiveTime = DOCUMENT_AUTO_DEFAULT_VERSION_EXPIRED;
    }catch(NumberFormatException nex){
      maxAllowVersion = DOCUMENT_AUTO_DEFAULT_VERSION_MAX;
      maxLiveTime = DOCUMENT_AUTO_DEFAULT_VERSION_EXPIRED;
    }
    maxLiveTime = maxLiveTime * 24 * 60 * 60 * 1000;
  }

  /**
   * Create new version and clear redundant versions
   *
   * @param nodeVersioning
   */
  public static void createVersion(Node nodeVersioning) throws Exception {

    if(!nodeVersioning.isNodeType(NT_FILE)) {
      if(log.isDebugEnabled()){
        log.debug("Version history is not impact with non-nt:file documents, there'is not any version created.");
      }
      return;
    }

    if(!nodeVersioning.isNodeType(MIX_VERSIONABLE)){
      if(nodeVersioning.canAddMixin(MIX_VERSIONABLE)) {
        nodeVersioning.addMixin(MIX_VERSIONABLE);
        nodeVersioning.save();
      }
      return;
    }
     if (!nodeVersioning.isCheckedOut())
     {
        nodeVersioning.checkout();
     }
     else
     {
        nodeVersioning.checkin();
        nodeVersioning.checkout();
     }

    if(maxAllowVersion!= DOCUMENT_AUTO_DEFAULT_VERSION_MAX || maxLiveTime != DOCUMENT_AUTO_DEFAULT_VERSION_EXPIRED) {
      removeRedundant(nodeVersioning);
    }
    nodeVersioning.save();
  }

  /**
   * Remove redundant version
   * - Remove versions has been expired
   * - Remove versions over max allow
   * @param nodeVersioning
   * @throws Exception
   */
  private static void removeRedundant(Node nodeVersioning) throws Exception {
    VersionHistory versionHistory = nodeVersioning.getVersionHistory();
    String baseVersion = nodeVersioning.getBaseVersion().getName();
    String rootVersion = versionHistory.getRootVersion().getName();
    Date now = new Date();
    Map<Long, String> versionTimestampToName = new HashMap<>();
    List<Long> timestamps = new ArrayList<>();
    VersionIterator versions = versionHistory.getAllVersions();
    while (versions.hasNext()) {
      Version version = versions.nextVersion();
      String versionName = version.getName();
      if (rootVersion.equals(versionName) || baseVersion.equals(versionName)) {
        continue;
      }
      long created = version.getCreated().getTimeInMillis();
      if (maxLiveTime != DOCUMENT_AUTO_DEFAULT_VERSION_EXPIRED &&
              now.getTime() - created > maxLiveTime) {

        versionHistory.removeVersion(versionName);
        continue;
      }
      versionTimestampToName.put(created, versionName);
      timestamps.add(created);
    }
    if (maxAllowVersion != DOCUMENT_AUTO_DEFAULT_VERSION_MAX &&
            timestamps.size() >= maxAllowVersion) {
      Collections.sort(timestamps);
      removeExpiredVersions(versionHistory, timestamps, versionTimestampToName, rootVersion, baseVersion, true);
      if (timestamps.size() >= maxAllowVersion) {
        removeExpiredVersions(versionHistory, timestamps, versionTimestampToName, rootVersion, baseVersion, false);
      }
    }
  }

  /**
   * Remove expired version
   * Remove exceeded versions and skip base and root version depending on labeled or not
   *
   * @param versionHistory
   * @param timestamps
   * @param map
   * @param rootVersion
   * @param baseVersion
   * @param skipLabeled
   */
  private static void removeExpiredVersions(
          VersionHistory versionHistory,
          List<Long> timestamps,
          Map<Long, String> map,
          String rootVersion,
          String baseVersion,
          boolean skipLabeled) throws Exception {

    int i = 1;
    while (timestamps.size() >= maxAllowVersion && i < timestamps.size()) {
      long ts = timestamps.get(i);
      String name = map.get(ts);
      Version version = versionHistory.getVersion(name);
      if (rootVersion.equals(name) || baseVersion.equals(name)) {
        i++;
        continue;
      }
      if (skipLabeled) {
        String[] labels = versionHistory.getVersionLabels(version);
        boolean hasLabel = Arrays.stream(labels).anyMatch(StringUtils::isNotEmpty);
        if (hasLabel) {
          i++;
          continue;
        }
      }
      versionHistory.removeVersion(name);
      timestamps.remove(i);
    }
  }
}
