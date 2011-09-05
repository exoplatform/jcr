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
package org.exoplatform.services.jcr.impl.xml;

/**
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version $Id: XmlMapping.java 11907 2008-03-13 15:36:21Z ksm $
 */
public enum XmlMapping {
   /**
    * The document view is designed to be more human-readable than the system view, though it
    * achieves this at the expense of completeness. In level 1 the document view is used as the
    * format for the virtual XML stream against which an XPath query is run (see 6.6 Searching
    * Repository Content). As well, in level 1, export to document view format is supported (see 6.5
    * Exporting Repository Content). In level 2, document view also allows for the import of
    * arbitrary XML (see 7.3.2 Import from Document View). The document view mapping in fact consists
    * of a family of related mappings whose precise features vary according to the context in which
    * it is used (export, import or XPath query) and which optional features are supported by the
    * particular implementation in question.
    */
   DOCVIEW,
   /**
    * The system view mapping provides a complete serialization of workspace content to XML without
    * loss of information.
    */
   SYSVIEW,
   /**
    * Optimized for back up system view mapping.
    */
   BACKUP
}
