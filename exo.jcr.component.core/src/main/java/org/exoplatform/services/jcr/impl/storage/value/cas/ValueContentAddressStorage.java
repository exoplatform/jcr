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
package org.exoplatform.services.jcr.impl.storage.value.cas;

import org.exoplatform.services.jcr.config.RepositoryConfigurationException;

import java.util.List;
import java.util.Properties;

/**
 * Created by The eXo Platform SAS .
 * 
 * Some concept description from Wikipedia: CAS system records a content address, which is an
 * identifier uniquely and permanently linked to the information content itself. A request to
 * retrieve information from a CAS system must provide the content identifier, from which the system
 * can determine the physical location of the data and retrieve it. Because the identifiers are
 * based on content, any change to a data element will necessarily change its content address. In
 * nearly all cases, a CAS device will not permit editing information once it has been stored.
 * Whether it can be deleted is often controlled by a policy.
 * 
 * Storage for content addresses. Physically content address is presented as ValueData content
 * checksum, assuming that here are no collisions in checksum calculation (i.e. an address is unique
 * for given content)
 * 
 * @author Gennady Azarenkov
 * @version $Id: ValueContentAddressStorage.java 34801 2009-07-31 15:44:50Z dkatayev $
 */

public interface ValueContentAddressStorage
{

   static final String DIGEST_ALGO_PARAM = "digest-algo";

   static final String VCAS_TYPE_PARAM = "vcas-type";

   /**
    * Deletes the address for given Property.
    * 
    * @param propertyId String Property ID
    * @throws RecordNotFoundException if Record not found
    */
   void deleteProperty(String propertyId) throws RecordNotFoundException, VCASException;

   /**
    * Deletes the address for given Property Value.
    * 
    * @param propertyId String Property ID
    * @param orderNumb int
    * @throws RecordNotFoundException if Record not found
    */
   void deleteValue(String propertyId, int orderNumb) throws RecordNotFoundException, VCASException;

   /**
    * Add Property Value address record to the storage.
    * 
    * @param propertyId String Property ID
    * @param orderNum int
    * @param identifier CAS ID (hash)
    * @throws RecordAlreadyExistsException
    *           if such propertyId/orderNumber already exists in storage
    */
   void addValue(String propertyId, int orderNum, String identifier) throws RecordAlreadyExistsException, VCASException;

   /**
    * Return CAS Identifier (address) by given property id and value order number.
    * 
    * @param propertyId
    * @param orderNum
    * @return identifier - content address
    * @throws RecordNotFoundException
    *           if no item found
    */
   String getIdentifier(String propertyId, int orderNum) throws RecordNotFoundException, VCASException;

   /**
    * Return identifiers (addresses) list for the given property.<br>
    * 
    * @param propertyId
    *          - property id
    * @param ownOnly
    *          - boolean, if true the list will contains only owned values, false - all, including
    *          shared
    * @return identifiers - content address list
    */
   List<String> getIdentifiers(String propertyId, boolean ownOnly) throws RecordNotFoundException, VCASException;

   /**
    * Tell if given property shares content with other properties.<br>
    * 
    * NOTE: not used now.
    * 
    * @param propertyId
    * @return boolean flag, true if given property shares content with other properties
    */
   boolean hasSharedContent(String propertyId) throws RecordNotFoundException, VCASException;

   /**
    * Initializes values CAS.
    * 
    * @param props
    * @throws VCASException
    */
   void init(Properties props) throws RepositoryConfigurationException, VCASException;
}
