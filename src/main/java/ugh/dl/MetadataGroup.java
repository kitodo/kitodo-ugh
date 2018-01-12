package ugh.dl;

/***************************************************************
 * Copyright notice
 *
 * ugh.dl / MetadataGroup.java
 *
 * (c) 2013 Robert Sehr <robert.sehr@intranda.com>
 *
 * All rights reserved
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or (at your
 * option) any later version.
 *
 * This Library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library. If not, see <http://www.gnu.org/licenses/>.
 ***************************************************************/

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kitodo.api.ugh.MetadataGroupInterface;
import org.kitodo.api.ugh.MetadataInterface;
import org.kitodo.api.ugh.MetadataTypeInterface;
import org.kitodo.api.ugh.PersonInterface;
import org.kitodo.api.ugh.exceptions.MetadataTypeNotAllowedException;

/*******************************************************************************
 * <p>
 * A MetadataGroup object represents a single MetadataGroup element. Each MetadataGroup element has at least a metadata element. The type of a
 * MetadataGroup element is stored as a {@link MetadataGroupType} object.
 * </p>
 *
 * <p>
 * MetadataGroups are a list of {@link Metadata}
 * </p>
 *
 * @author Robert Sehr
 * @version 2013-05-08
 * @see MetadataGroupType
 *
 *
 ******************************************************************************/

public class MetadataGroup implements MetadataGroupInterface, Serializable {

    private static final long serialVersionUID = -6283388063178498292L;

    private static final Logger logger = LogManager.getLogger(MetadataGroup.class);

    protected MetadataGroupType MDType;
    // Document structure to which this metadata type belongs to.
    protected DocStruct myDocStruct;

    private List<MetadataInterface> metadataList;
    private Collection<PersonInterface> personList;

    /***************************************************************************
     * <p>
     * Constructor.
     * </p>
     *
     * @throws MetadataTypeNotAllowedException
     **************************************************************************/
    public MetadataGroup(MetadataGroupType theType) throws MetadataTypeNotAllowedException {

        // Check for NULL MetadataTypes.
        if (theType == null) {
            String message = "MetadataType must not be null at Metadata creation!";
            throw new MetadataTypeNotAllowedException(message);
        }

        this.MDType = theType;

        metadataList = new LinkedList<MetadataInterface>();
        personList = new LinkedList<PersonInterface>();
        for (MetadataTypeInterface mdt : MDType.getMetadataTypeList()) {
            if (mdt.getIsPerson()) {
                Person p = new Person((MetadataType) mdt);
                p.setRole(mdt.getName());
                personList.add(p);
            } else {
                Metadata md = new Metadata((MetadataType) mdt);
                metadataList.add(md);
            }
        }

    }

    /***************************************************************************
     * <p>
     * Sets the Document structure entity to which this object belongs to.
     * </p>
     *
     * @param inDoc
     **************************************************************************/
    public void setDocStruct(DocStruct inDoc) {
        this.myDocStruct = inDoc;
    }

    /***************************************************************************
     * <p>
     * Returns the DocStruct instance, to which this metadataGroup object belongs. This is extremely helpful, if only the metadata instance is stored
     * in a list; the reference to the associated DocStrct instance is always kept.
     * </p>
     *
     * @return DocStruct instance.
     **************************************************************************/
    public DocStruct getDocStruct() {
        return this.myDocStruct;
    }

    /***************************************************************************
     * <p>
     * Returns the type of the metadataGroup instance; The MetadataGroupType object which is returned, may have the same name, but be a different
     * object than the MetadataGroupType object from another MetadataGroupType.
     * </p>
     *
     * @return MetadataGroupType instance
     **************************************************************************/
    @Override
    public MetadataGroupType getType() {
        return this.MDType;
    }

    /***************************************************************************
     * <p>
     * Sets the MetadataGroupType for this instance; only a MetadataGroupType instance is used as the only parameter. The method returns true if
     * MDType was set; false if not.
     * </p>
     *
     * @param inType
     * @return
     **************************************************************************/
    public boolean setType(MetadataGroupType inType) {
        this.MDType = inType;
        return true;
    }

    @Override
    public List<MetadataInterface> getMetadataList() {
        return metadataList;
    }

    public void setMetadataList(List<MetadataInterface> metadataList) {
        this.metadataList = metadataList;
    }

    @Override
    public void addMetadata(MetadataInterface metadata) {
        this.metadataList.add(metadata);
    }

    @Override
    public Collection<PersonInterface> getPersonList() {
        return personList;
    }

    public void setPersonList(Collection<PersonInterface> personList) {
        this.personList = personList;
    }

    @Override
    public void addPerson(PersonInterface person) {
        this.personList.add(person);
    }
    @Override
    public String toString() {
        return "MetadataGroup [MDType=" + MDType + ", myDocStruct=" + myDocStruct + ", metadataList=" + metadataList + ", personList=" + personList + "]";
    }

    @Override
    public List<MetadataInterface> getMetadataByType(String theType) {
        List<MetadataInterface> returnList = new ArrayList<MetadataInterface>();
        for (MetadataInterface md : metadataList) {
            if (md.getType().getName().equals(theType)) {
                returnList.add(md);
            }
        }
        return returnList;
    }

    @Override
    public List<PersonInterface> getPersonByType(String theType) {
        List<PersonInterface> returnList = new ArrayList<PersonInterface>();
        for (PersonInterface md : personList) {
            if (md.getType().getName().equals(theType)) {
                returnList.add(md);
            }
        }
        return returnList;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((MDType == null) ? 0 : MDType.hashCode());
        result = prime * result + ((metadataList == null) ? 0 : metadataList.hashCode());
        result = prime * result + ((personList == null) ? 0 : personList.hashCode());
        result = prime * result + ((myDocStruct == null) ? 0 : myDocStruct.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        MetadataGroup other = (MetadataGroup) obj;
        if (MDType == null) {
            if (other.MDType != null)
                return false;
        } else if (!MDType.equals(other.MDType))
            return false;
        if (metadataList == null) {
            if (other.metadataList != null)
                return false;
        } else if (!metadataList.equals(other.metadataList))
            return false;
        if (personList == null) {
            if (other.personList != null)
                return false;
        } else if (!personList.equals(other.personList))
            return false;
        if (myDocStruct == null) {
            if (other.myDocStruct != null)
                return false;
        } else if (!myDocStruct.equals(other.myDocStruct))
            return false;
        return true;
    }

}
