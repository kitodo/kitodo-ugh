package ugh.dl;

/***************************************************************
 * Copyright notice
 *
 * ugh.dl / MetadataGroupType.java
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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.kitodo.api.ugh.MetadataGroupTypeInterface;
import org.kitodo.api.ugh.MetadataTypeInterface;

/**
 * @author Robert Sehr
 */
public class MetadataGroupType implements MetadataGroupTypeInterface, Serializable {

    private static final long serialVersionUID = -2935555025180170310L;

    private List<MetadataTypeInterface> metadataTypeList = new ArrayList<MetadataTypeInterface>();

    // Unique name of MetadataType.
    private String name;

    // Maximum number of occurrences of this MetadataType for one DocStrct (can
    // be 1 (1), one or more (+) or as many as you want (*).
    private String max_number;

    // Hash containing all languages.
    private Map<String, String> allLanguages;

    @Override
    public List<MetadataTypeInterface> getMetadataTypeList() {
        return metadataTypeList;
    }

    public void setMetadataTypeList(List<MetadataTypeInterface> metadataTypeList) {
        this.metadataTypeList = metadataTypeList;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public void addMetadataType(MetadataTypeInterface metadataToAdd) {
        if (!metadataTypeList.contains(metadataToAdd)) {
            metadataTypeList.add(metadataToAdd);
        }
    }

    public void removeMetadataType(MetadataType metadataToRemove) {
        if (metadataTypeList.contains(metadataToRemove)) {
            metadataTypeList.remove(metadataToRemove);
        }
    }

    @Override
    public boolean equals(Object obj) {
        return this.name.equals(((MetadataGroupType) obj).getName());
    }

    @Override
    public Map<String, String> getAllLanguages() {
        return allLanguages;
    }

    @Override
    public void setAllLanguages(Map<String, String> allLanguages) {
        this.allLanguages = allLanguages;
    }

    /***************************************************************************
     * @param in
     * @return
     **************************************************************************/
    @Override
    public boolean setNum(String in) {

        if (!in.equals("1m") && !in.equals("1o") && !in.equals("+") && !in.equals("*")) {
            // Unknown syntax.
            return false;
        }
        this.max_number = in;

        return true;
    }

    @Override
    public String getLanguage(String theLanguage) {
        for (Map.Entry<String, String> lang : getAllLanguages().entrySet()) {
            if (lang.getKey().equals(theLanguage)) {
                return lang.getValue();
            }
        }

        return null;
    }

    /***************************************************************************
     * <p>
     * Retrieves the number of possible Metadata objects for a DocStruct. This is now based on the type of DocStruct and is therefor stored in the
     * DocStructType.
     * </p>
     *
     * TODO Was set to deprecated, who knows why?
     *
     * @return number of MetadataType
     **************************************************************************/
    public String getNum() {
        return this.max_number;
    }

    public MetadataGroupType copy() {

        MetadataGroupType newMDType = new MetadataGroupType();

        newMDType.setAllLanguages(this.allLanguages);
        newMDType.setName(this.name);
        if (this.max_number != null) {
            newMDType.setNum(this.max_number);
        }
        List<MetadataTypeInterface> newList = new LinkedList<MetadataTypeInterface>();
        for (MetadataTypeInterface mdt : metadataTypeList) {
            newList.add(((MetadataType) mdt).copy());
        }
        newMDType.setMetadataTypeList(newList);

        return newMDType;
    }
}
