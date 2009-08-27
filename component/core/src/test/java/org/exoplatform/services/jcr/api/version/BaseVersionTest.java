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
package org.exoplatform.services.jcr.api.version;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.InvalidItemStateException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;

import org.exoplatform.services.jcr.JcrAPIBaseTest;

/**
 * Created by The eXo Platform SAS Author : Peter Nedonosko
 * peter.nedonosko@exoplatform.com.ua 24.02.2006
 */
public class BaseVersionTest
   extends JcrAPIBaseTest
{

   protected Version version = null;

   protected Version version2 = null;

   protected Node versionableNode = null;

   protected Node nonVersionableNode = null;

   class BaseVersionFinder
   {

      private Version baseVersion = null;

      private boolean baseVersionFound = false;

      BaseVersionFinder(Version baseVersion)
      {
         this.baseVersion = baseVersion;
      }

      public Version getBaseVersion()
      {
         return baseVersion;
      }

      public boolean check(Version someVersion) throws RepositoryException
      {

         if (baseVersionFound)
            return false;

         baseVersionFound = baseVersion.isSame(someVersion);
         return baseVersionFound;
      }

      public boolean isBaseVersionFound()
      {
         return baseVersionFound;
      }
   }

   public void setUp() throws Exception
   {
      super.setUp();

      NodeTypeManager ntManager = session.getWorkspace().getNodeTypeManager();

      // build persistent versionable and non-versionable nodes
      try
      {
         versionableNode = prepareVersionableNode(root, "versionableFolder1", ntManager.getNodeType("nt:folder"));
      }
      catch (RepositoryException e)
      {
         fail("Failed to create versionable test node." + e.getMessage());
      }

      try
      {
         nonVersionableNode = root.addNode("nonVersionableFolder1", "nt:unstructured");
         root.save();
      }
      catch (RepositoryException e)
      {
         fail("Failed to create non-versionable test node." + e.getMessage());
      }
   }

   protected Node prepareVersionableNode(Node parent, String name, NodeType nodetype) throws RepositoryException
   {
      Node versionable = parent.addNode(name, nodetype.getName());
      if (!nodetype.isNodeType("mix:versionable"))
      {
         versionable.addMixin("mix:versionable");
      }
      parent.save();

      return versionable;
   }

   protected void checkNotExisted(String nodeRelPath) throws RepositoryException
   {
      try
      {
         Node doc = versionableNode.getNode(nodeRelPath);
         fail("A child node '" + nodeRelPath + "' must not be found");
      }
      catch (PathNotFoundException e)
      {
         // success
      }
   }

   protected Node checkExisted(String nodeRelPath) throws RepositoryException
   {
      return checkExisted(nodeRelPath, null);
   }

   protected Node checkExisted(String nodeRelPath, String[] properties) throws RepositoryException
   {
      try
      {
         Node doc = versionableNode.getNode(nodeRelPath);
         // PropertyIterator pi = doc1.getProperties();
         // NodeIterator ni = doc1.getNodes();
         if (log.isDebugEnabled())
            log.debug("Node '" + nodeRelPath + "' found in versionable node: " + versionableNode.getPath());
         if (properties != null)
         {
            for (String p : properties)
            {
               try
               {
                  String pValue = doc.getProperty(p).getString();
                  if (log.isDebugEnabled())
                     log.debug("Node '" + nodeRelPath + "' property " + p + ": " + pValue);
               }
               catch (PathNotFoundException e)
               {
                  fail("A child node's '" + nodeRelPath + "' property '" + p + "' must be found");
               }
            }
         }
         return doc;
      }
      catch (PathNotFoundException e)
      {
         fail("A child node '" + nodeRelPath + "' must be found");
      }
      return null;
   }

   protected void makeVersionable(Node anNode) throws RepositoryException
   {
      if (!anNode.isNodeType("mix:versionable"))
      {
         anNode.addMixin("mix:versionable");
      }
   }

   // [PN] 06.05.06
   protected void checkVersionHistory(Node versionable, int versionCount) throws RepositoryException
   {

      VersionHistory vHistory = versionable.getVersionHistory();
      Version rootVersion = vHistory.getRootVersion();
      if (log.isDebugEnabled())
         log.debug("rootVersion " + rootVersion.getPath());
      Version baseVersion = versionable.getBaseVersion();
      if (log.isDebugEnabled())
         log.debug("baseVersion " + baseVersion.getPath());

      BaseVersionFinder baseVersionFinder = new BaseVersionFinder(baseVersion);

      List<Value> refs = traverseVersionSubTree(rootVersion, baseVersionFinder, vHistory, "  --");
      if (refs.size() != versionCount)
      {
         fail("Version history contains not all versions for node " + versionable.getPath() + ", expected:"
                  + versionCount + " was:" + refs.size());
      }
      if (!baseVersionFinder.isBaseVersionFound())
      {
         fail("Base version not founded in version history tree for node " + versionable.getPath()
                  + ", but exists if call versionable.getBaseVersion() " + baseVersion.getPath());
      }
   }

   protected void showVersionable(Node versionable) throws RepositoryException
   {

      VersionHistory vHistory = versionable.getVersionHistory();
      Version baseVersion = versionable.getBaseVersion();

      BaseVersionFinder baseVersionFinder = new BaseVersionFinder(baseVersion);

      String vinfo = "";
      // show prececessors
      try
      {
         Property predecessors = versionable.getProperty("jcr:predecessors");
         String pstr = "";
         for (Value p : predecessors.getValues())
         {
            pstr += "\n    -- ";
            Version pv = (Version) session.getNodeByUUID(p.getString());
            if (baseVersionFinder.check(pv))
               pstr += pv.getPath() + " >>>Base version<<<  ";
            else
               pstr += pv.getPath();
            String[] pvls = vHistory.getVersionLabels(pv);
            for (String pvl : pvls)
               pstr += ", " + pvl;
         }
         vinfo += "\n  jcr:predecessors " + pstr;
      }
      catch (PathNotFoundException e)
      {
      }

      // show successors
      try
      {
         Property sucessors = versionable.getProperty("jcr:successors");
         String pstr = "";
         for (Value s : sucessors.getValues())
         {
            pstr += "\n    -- ";
            Version sv = (Version) session.getNodeByUUID(s.getString());
            if (baseVersionFinder.check(sv))
               pstr += sv.getPath() + " >>>Base version<<<  ";
            else
               pstr += sv.getPath();
            String[] svls = vHistory.getVersionLabels(sv);
            for (String svl : svls)
               pstr += ", " + svl;
         }
         vinfo += "\n  jcr:successors " + pstr;
      }
      catch (PathNotFoundException e)
      {
      }

      if (log.isDebugEnabled())
         log.debug("versionable " + versionable.getPath() + "\n  jcr:baseVersion " + baseVersion.getPath() + vinfo);
   }

   protected List<Value> traverseVersionSubTree(Version ver, BaseVersionFinder baseVersionFinder,
            VersionHistory vHistory, String outPrefix) throws RepositoryException
   {

      List<Value> successorsRefs = new ArrayList<Value>();
      String vlInfo = "";
      String[] versionLabels = vHistory.getVersionLabels(ver);
      for (String vl : versionLabels)
      {
         vlInfo += (vlInfo.length() > 0 ? ", " + vl : vl);
      }
      if (baseVersionFinder.check(ver))
      {
         // this is a base version
         vlInfo = (vlInfo.length() > 0 ? " [" + vlInfo + "]" : "") + " >>>Base version<<<  ";
      }
      else
      {
         vlInfo = (vlInfo.length() > 0 ? " [" + vlInfo + "]" : "");
      }
      if (log.isDebugEnabled())
         log.debug(outPrefix + " " + ver.getName() + vlInfo);
      Value[] versionSuccessors = getSucessors(ver);
      if (versionSuccessors != null)
      {
         for (Value sv : versionSuccessors)
         {
            Version successor = (Version) session.getNodeByUUID(sv.getString());
            if (successor != null)
            {
               successorsRefs.add(sv);
               List<Value> successorSuccessors =
                        traverseVersionSubTree(successor, baseVersionFinder, vHistory, "  " + outPrefix);
               if (successorSuccessors != null)
               {
                  successorsRefs.addAll(successorSuccessors);
               }
            }
            else
            {
               fail("No item for successor UUID " + sv.getString());
            }
         }
      }
      return successorsRefs;
   }

   protected Value[] showSucessors(Version ver, String outPrefix) throws RepositoryException
   {
      String vp = (outPrefix != null ? outPrefix : " ? ");
      try
      {
         Value[] refs = getSucessors(ver);
         for (Value ref : refs)
         {
            if (log.isDebugEnabled())
               log.debug(vp + " sucessor: " + ref.getString());
         }
         return refs;
      }
      catch (PathNotFoundException e)
      {
         // fail("Property jcr:successors must exists for " + vp);
         return null;
      }
   }

   protected Value[] getSucessors(Version ver) throws RepositoryException
   {
      try
      {
         Property successors = ver.getProperty("jcr:successors");
         return successors.getValues();
      }
      catch (PathNotFoundException e)
      {
         // fail("Property jcr:successors must exists for " + vp);
         return null;
      }
   }

   protected void tearDown() throws Exception
   {
      try
      {
         versionableNode.checkout();
         versionableNode.remove();
         nonVersionableNode.remove();
         root.save();
      }
      catch (InvalidItemStateException e)
      {
         log.error("Error of tear down: " + e);
      }

      super.tearDown();
   }
}
