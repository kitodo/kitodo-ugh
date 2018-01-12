/*******************************************************************************
 * ugh.dl / DocStruct.java
 *
 * Copyright 2010 Center for Retrospective Digitization, Göttingen (GDZ)
 *
 * http://gdz.sub.uni-goettingen.de
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
 ******************************************************************************/
package ugh.dl;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeSet;

import org.apache.logging.log4j.Logger;
import org.kitodo.api.ugh.ContentFileInterface;
import org.kitodo.api.ugh.DigitalDocumentInterface;
import org.kitodo.api.ugh.DocStructInterface;
import org.kitodo.api.ugh.DocStructTypeInterface;
import org.kitodo.api.ugh.MetadataGroupInterface;
import org.kitodo.api.ugh.MetadataGroupTypeInterface;
import org.kitodo.api.ugh.MetadataInterface;
import org.kitodo.api.ugh.MetadataTypeInterface;
import org.kitodo.api.ugh.PersonInterface;
import org.kitodo.api.ugh.PrefsInterface;
import org.kitodo.api.ugh.ReferenceInterface;
import org.kitodo.api.ugh.exceptions.ContentFileNotLinkedException;
import org.kitodo.api.ugh.exceptions.DocStructHasNoTypeException;
import org.kitodo.api.ugh.exceptions.IncompletePersonObjectException;
import org.kitodo.api.ugh.exceptions.MetadataTypeNotAllowedException;
import org.kitodo.api.ugh.exceptions.PreferencesException;
import org.kitodo.api.ugh.exceptions.TypeNotAllowedAsChildException;
import org.kitodo.api.ugh.exceptions.TypeNotAllowedForParentException;
import org.kitodo.api.ugh.exceptions.UGHException;
import org.apache.logging.log4j.LogManager;

import ugh.dl.DigitalDocument.ListPairCheck;
import ugh.fileformats.mets.MetsModsImportExport;

/**
 * One node of a tree depicting the structure of the document.
 * <p>
 * A DocStruct object represents a structure entity in work. Every document
 * consists of a structure, which can be separated into several structure
 * entities, which build hierarchical structure. Usually a
 * {@link DigitalDocument} contains two structures; a logical and a physical
 * one. Each structure consists of a top DocStruct element that is embedded in
 * some kind of structure. This structure is represented by parent and children
 * of {@code DocStruct} objects.
 * <p>
 * This class contains methods to:
 * <ul>
 * <li>Retrieve information about the structure (add, move and remove children),
 * <li>set the parent (the top element has no parent),
 * <li>set and retrieve metadata, which describe a structure entity,
 * <li>handle content files, which are linked to a structure entity.
 * </ul>
 *
 * Every structure entity is of a special kind. The kind of entity is stored in
 * a {@link DocStructType} element. Depending on the type of structure entities
 * certain metadata and children a permitted or forbidden.
 *
 * @author Markus Enders
 * @author Stefan E. Funk
 * @author Robert Sehr
 * @author Wulf Riebensahm
 * @author Matthias Ronge
 * @see DigitalDocument
 */

public class DocStruct implements DocStructInterface, Serializable {

    private static final long serialVersionUID = -4531356062293054921L;

    private static final Logger logger = LogManager.getLogger(DocStruct.class);
    private static final String HIDDEN_METADATA_CHAR = "_";

    private static final List<String> IDENTIFIER_METADATA_FIELDS_FOR_TOSTRING = Arrays.asList(
        new String[] { "TitleDocMain", "CatalogIDDigital", "TitleDocMainShort", "MetsPointerURL" }
    );

    private static final Set<String> FOREIGN_CHILD_METADATA_TYPES_TO_COPY = new HashSet<String>(
            Arrays.asList(new String[] { MetsModsImportExport.CREATE_MPTR_ELEMENT_TYPE,
                    MetsModsImportExport.CREATE_LABEL_ATTRIBUTE_TYPE,
                    MetsModsImportExport.CREATE_ORDERLABEL_ATTRIBUTE_TYPE }));

    /**
     *  List containing all Metadata instances.
     */
    private List<MetadataInterface> allMetadata;

    /**
     * List containing meta-data instances which have been removed. These
     * instances must be deleted from database etc.
     *
     * @deprecated This field is decommissioned. However, it must remain in
     *             place to allow deserialization XStream files created in days
     *             of yore.
     */
    @SuppressWarnings("unused")
    private List<Metadata> removedMetadata;

    private List<MetadataGroupInterface> allMetadataGroups;

    /**
     * @deprecated This field is decommissioned. However, it must remain in
     *             place to allow deserialization XStream files created in days
     *             of yore.
     */
    @SuppressWarnings("unused")
    private List<MetadataGroup> removedMetadataGroups;

    /**
     * List containing all DocStrct-instances being children of this instance.
     */
    private List<DocStructInterface> children;

    /**
     * List containing all references to Contentfile objects.
     */
    private List<ContentFileReference> contentFileReferences = new LinkedList<ContentFileReference>();

    /**
     * List of all persons; list containing all Person objects.
     */
    private List<PersonInterface> persons;

    private DocStruct parent;

    /**
     * All references to other DocStrct instances (containing References
     * objects).
     */
    private final List<ReferenceInterface> docStructRefsTo = new LinkedList<ReferenceInterface>();

    /**
     * All references from another DocStruct to this one.
     */
    private final List<ReferenceInterface> docStructRefsFrom = new LinkedList<ReferenceInterface>();

    /**
     * Type of this instance.
     */
    private DocStructType type;

    /**
     * Local identifier of this docstruct.
     */
    private String identifier = null;

    /**
     * Digital document, to which this DocStruct belongs.
     */
    private DigitalDocument digdoc;
    private Object origObject = null;

    /**
     * ID in database table, 4 bytes long.
     *
     * @deprecated This field is decommissioned. However, it must remain in
     *             place to allow deserialization XStream files created in days
     *             of yore.
     */
    @SuppressWarnings("unused")
    private final long databaseid = 0;

    private boolean logical = false;
    private boolean physical = false;

    /**
     * String containing an identifier or a URL to the anchor.
     */
    private String referenceToAnchor;

    /**
     * the amdSec referenced by this docStruct, if any
     */
    private AmdSec amdSec;
    /**
     * the list of techMd sections referenced by this docStruct, if any
     */
    private List<Md> techMdList;

    /**
     * This is needed so we can exclude the possibility to run eternal loops with non hierarchial references, will be filled with {@code super.toString()}
     * signature of the compared DocStruct.
     */
    private HashMap<String, Object> signaturesForEqualsMethodRefsFrom;
    private HashMap<String, Object> signaturesForEqualsMethodRefsTo;

    /**
     * Constructor just used to be compatible with JavaBeans.
     *
     * @deprecated
     */
    @Deprecated
    public DocStruct() {
        super();
    }

    /**
     * Creates a new DocStruct with a given type. The type can be changed later using the method {@link #setType(DocStructType)}.
     *
     * @param inType type of this DocStruct
     * @throws TypeNotAllowedForParentException is never thrown
     */
    protected DocStruct(DocStructType inType) throws TypeNotAllowedForParentException {

        // We have to check, if this type is allowed here, this depends on the
        // parent DocStruct.
       setType(inType);
    }

    protected void setDigitalDocument(DigitalDocument dd) {
        this.digdoc = dd;
    }

    /**
     * Sets the type of this DocStruct. When changing the type, the allowed metadata elements and children are <i>not</i> checked. Therefore it is
     * possible to create documents that are not valid against the current preferences file.
     *
     * @param inType type to set
     * @return always true
     */
    @Override
    public boolean setType(DocStructTypeInterface inType) {

        // Usually we had to check, if the new type is allowed. Search for
        // parent and see if the parent allows this type.
        this.type = (DocStructType) inType;

        return true;
    }

    /**
     * Get the type of this DocStruct.
     *
     * @return the type of this DocStruct
     */
    @Override
    public DocStructType getType() {
        return this.type;
    }

    /**
     * Returns a list containing all children of this DocStruct. If this instance has no children, {@code null} is returned.
     *
     * @return all children of this DocStruct
     */
    @Override
    public List<DocStructInterface> getAllChildren() {

        if (this.children == null || this.children.isEmpty()) {
            return null;
        }

        return this.children;
    }

    /**
     * Returns all real successors, i.e. all child nodes that are of a different
     * or no anchor class at all, of an instance as a flat list. Doesn’t return
     * unreal successors; that are those who are nothing but METS pointers.
     * <p>
     * If this instance has no children, an empty list is returned.
     *
     * @return all child nodes that are of a different or no anchor class at all
     */
    public List<DocStruct> getAllRealSuccessors() {
        LinkedList<DocStruct> result = new LinkedList<DocStruct>();
        if (children != null) {
            for (DocStructInterface child : children) {
                if (type.getAnchorClass().equals(child.getType().getAnchorClass())) {
                    result.addAll(((DocStruct) child).getAllRealSuccessors());
                } else if (!((DocStruct) child).hasMetadata(MetsModsImportExport.CREATE_MPTR_ELEMENT_TYPE)) {
                    result.add(((DocStruct) child));
                }
            }
        }
        return result;
    }

    @Deprecated
    public String getreferenceToAnchor() {
        return getReferenceToAnchor();
    }

    /**
     * Returns the identifier of the {@link DigitalDocument} this instance is
     * anchored on.
     * <p>
     * A {@code DocStruct} can be anchored on another {@code DocStruct} which is
     * located in a different {@code DigitalDocument} (different METS file). For
     * example, a periodical volume can be anchored on a journal. Both
     * {@code DocStruct}s are stored in different {@code DigitalDocument}s and
     * are linked by the identifier of the {@code DigitalDocument} that an
     * instance is anchored on (in the example, the identifier of the journal).
     * The identifier of that other {@code DocStruct} should be stored here, if
     * this instance is anchored on it. In the {@code DigitalDocument} that this
     * instance is anchored on, the identifier is stored as a {@link Metadata}
     * field.
     *
     * @return the identifier of the {@code DigitalDocument} this instance is
     *         anchored on
     */
    public String getReferenceToAnchor() {
        return this.referenceToAnchor;
    }

    @Deprecated
    public void setreferenceToAnchor(String in) {
        setReferenceToAnchor(in);
    }

    /**
     * Sets the identifier of the {@link DigitalDocument} this instance is
     * anchored on.
     *
     * @param in
     *            the identifier of the {@code DigitalDocument} this instance is
     *            anchored on
     * @see #getReferenceToAnchor()
     */
    public void setReferenceToAnchor(String in) {
        this.referenceToAnchor = in;
    }

    /**
     * Returns all children of this instance which are of a given type and have
     * a given type of meta-data attached. For example, you can get all articles
     * which have an author. It is possible to use "{@code *}" as wildcard
     * character value for {@code theDocTypeName} and {@code theMDTypeName}.
     * <p>
     * If this instance has no children, null is returned.
     *
     * @param theDocTypeName
     *            name of the structural type
     * @param theMDTypeName
     *            name of the meta-data type
     * @return all children of the given type and with the given meta-data
     */
    @Override
    public List<DocStructInterface> getAllChildrenByTypeAndMetadataType(String theDocTypeName, String theMDTypeName) {

        List<DocStructInterface> resultList = new LinkedList<DocStructInterface>();
        boolean docTypeTestPassed = false;
        boolean mdTypeTestPassed = false;
        List<MetadataInterface> allMD;

        if (this.children == null || this.children.isEmpty()) {
            return null;
        }

        for (DocStructInterface child : this.children) {
            docTypeTestPassed = false;

            // Check doctype.
            if (theDocTypeName.equals("*")) {
                // Wildcard; we do not have to check the doctype.
                docTypeTestPassed = true;
            } else {
                DocStructType singleType = (DocStructType) child.getType();
                String singlename = singleType.getName();
                if (singlename != null && singlename.equals(theDocTypeName)) {
                    docTypeTestPassed = true;
                } else {
                    // Wrong type.
                    continue;
                }
            }

            // Get all Metadatatypes.
            allMD = child.getAllMetadata();
            // Child has no metadata.
            if (allMD == null) {
                // MetadataType doesn't matter anyhow, so we can add this one,
                // too.
                if (theMDTypeName.equals("*")) {
                    mdTypeTestPassed = true;
                } else {
                    mdTypeTestPassed = false;
                }
            } else {
                for (MetadataInterface md : allMD) {
                    mdTypeTestPassed = false;
                    if (theMDTypeName.equals("*")) {
                        mdTypeTestPassed = true;
                        break;
                    } else {
                        MetadataType mdtype = (MetadataType) md.getType();
                        String mdtypename = mdtype.getName();

                        if (mdtypename != null && mdtypename.equals(theMDTypeName)) {
                            mdTypeTestPassed = true;
                            break;
                        }
                    }
                }
            }
            if (mdTypeTestPassed && docTypeTestPassed) {
                // Doctype and metadatatype test passed, add it.
                resultList.add(child);
            }
        }

        if (resultList.isEmpty()) {
            return null;
        }

        return resultList;
    }

    /**
     * Sets the local identifier.
     * <p>
     * Currently there is no check, if the identifier is used for another
     * {@code DocStruct} or {@link Metadata} element.
     *
     * @return always true
     */
    public boolean setIdentifier(String in) {
        this.identifier = in;

        return true;
    }

    public String getIdentifier() {
        return this.identifier;
    }

    /**
     * Returns all {@code Metadata} objects which are identifiers. Identifiers
     * are all {@link Metadata} objects whose {@link MetadataType#isIdentifier}
     * flag is set to {@code true}.
     * <p>
     * If none were found, {@code null} is returned.
     *
     * @return all {@code Metadata} objects which are identifiers
     */
    @Override
    public List<MetadataInterface> getAllIdentifierMetadata() {

        List<MetadataInterface> result = new LinkedList<MetadataInterface>();

        if (this.allMetadata == null) {
            return null;
        }

        for (MetadataInterface md : this.allMetadata) {
            if (((MetadataType) md.getType()).isIdentifier) {
                result.add(md);
            }
        }

        if (result.isEmpty()) {
            return null;
        }

        return result;
    }

    /**
     * Creates a copy of this instance, with some or all {@code Metadata} and
     * {@code Person} objects attached.
     *
     * @param cpmetadata
     *            if true, copies {@link Metadata} objects
     * @param recursive
     *            if true, copies all children as well; if null, copies all
     *            children which are of the same anchor class; if false, doesn’t
     *            copy any children
     * @return a new DocStruct instance
     */
    @Override
    public DocStructInterface copy(boolean cpmetadata, Boolean recursive) {

        DocStruct newStruct = null;
        try {
            newStruct = new DocStruct(this.getType());
        } catch (TypeNotAllowedForParentException e) {
            // This should never happen as we are creating the same
            // DocStructType.
            String message = "This " + e.getClass().getName() + " should not have been occurred!";
            logger.error(message, e);
        }

        // Copy the link to the parent.
        newStruct.setParent(this.getParent());
        newStruct.origObject = this.origObject;
        if (this.logical) {
            newStruct.logical = this.logical;
        }

        // Copy metadata and persons.
        if (cpmetadata) {
            if (this.getAllMetadata() != null) {
                for (MetadataInterface md : this.getAllMetadata()) {
                    try {
                        Metadata mdnew = new Metadata((MetadataType) md.getType());
                        mdnew.setValue(md.getValue());
                        if (((Metadata) md).getValueQualifier() != null && ((Metadata) md).getValueQualifierType() != null) {
                            mdnew.setValueQualifier(((Metadata) md).getValueQualifier(), ((Metadata) md).getValueQualifierType());
                        }
                        if (((Metadata) md).getAuthorityID() != null && ((Metadata) md).getAuthorityValue() != null && ((Metadata) md).getAuthorityURI() != null) {
                            mdnew.setAutorityFile(((Metadata) md).getAuthorityID(), ((Metadata) md).getAuthorityURI(), ((Metadata) md).getAuthorityValue());
                        }
                        newStruct.addMetadata(mdnew);
                    } catch (DocStructHasNoTypeException e) {
                        // This should never happen, as we are adding the same
                        // MetadataType.
                        String message = "This " + e.getClass().getName() + " should not have been occurred!";
                        logger.error(message, e);
                    } catch (MetadataTypeNotAllowedException e) {
                        // This should never happen, as we are adding the same
                        // MetadataType.
                        String message = "This " + e.getClass().getName() + " should not have been occurred!";
                        logger.error(message, e);
                    }
                }
            }

            if (this.getAllMetadataGroups() != null) {
                for (MetadataGroupInterface md : this.getAllMetadataGroups()) {
                    try {
                        MetadataGroup mdnew = new MetadataGroup((MetadataGroupType) md.getType());
                        mdnew.setDocStruct(newStruct);
                        List<MetadataInterface> newmdlist = new LinkedList<MetadataInterface>();
                        Collection<PersonInterface> newPersonList = new LinkedList<PersonInterface>();
                        for (MetadataInterface meta : md.getMetadataList()) {
                            Metadata newMeta = new Metadata((MetadataType) meta.getType());
                            newMeta.setValue(meta.getValue());
                            if (((Metadata) meta).getValueQualifier() != null && ((Metadata) meta).getValueQualifierType() != null) {
                                newMeta.setValueQualifier(((Metadata) meta).getValueQualifier(), ((Metadata) meta).getValueQualifierType());
                            }
                            if (((Metadata) meta).getAuthorityID() != null && ((Metadata) meta).getAuthorityValue() != null && ((Metadata) meta).getAuthorityURI() != null) {
                                newMeta.setAutorityFile(((Metadata) meta).getAuthorityID(), ((Metadata) meta).getAuthorityURI(), ((Metadata) meta).getAuthorityValue());
                            }
                            newmdlist.add(newMeta);
                        }

                        for (PersonInterface ps : md.getPersonList()) {
                            Person newps = new Person((MetadataType) ps.getType());
                            if (ps.getLastname() != null) {
                                newps.setLastname(ps.getLastname());
                            }
                            if (ps.getFirstname() != null) {
                                newps.setFirstname(ps.getFirstname());
                            }
                            if (((Metadata) ps).getAuthorityID() != null && ps.getAuthorityURI() != null && ps.getAuthorityValue() != null) {
                                newps.setAutorityFile(((Metadata) ps).getAuthorityID(), ps.getAuthorityURI(), ps.getAuthorityValue());
                            }
                            if (((Person) ps).getInstitution() != null) {
                                newps.setInstitution(((Person) ps).getInstitution());
                            }
                            if (((Person) ps).getAffiliation() != null) {
                                newps.setAffiliation(((Person) ps).getAffiliation());
                            }
                            if (ps.getRole() != null) {
                                newps.setRole(ps.getRole());
                            }
                            newPersonList.add(newps);
                        }
                        mdnew.setMetadataList(newmdlist);
                        mdnew.setPersonList(newPersonList);
                        newStruct.addMetadataGroup(mdnew);

                        mdnew.setMetadataList(newmdlist);
                        newStruct.addMetadataGroup(mdnew);
                    } catch (DocStructHasNoTypeException e) {
                        // This should never happen, as we are adding the same
                        // MetadataType.
                        String message = "This " + e.getClass().getName() + " should not have been occurred!";
                        logger.error(message, e);
                    } catch (MetadataTypeNotAllowedException e) {
                        // This should never happen, as we are adding the same
                        // MetadataType.
                        String message = "This " + e.getClass().getName() + " should not have been occurred!";
                        logger.error(message, e);
                    }

                }
            }

            // Copy the persons.
            if (this.getAllPersons() != null) {
                for (PersonInterface ps : this.getAllPersons()) {
                    try {
                        Person newps = new Person((MetadataType) ps.getType());
                        if (ps.getLastname() != null) {
                            newps.setLastname(ps.getLastname());
                        }
                        if (ps.getFirstname() != null) {
                            newps.setFirstname(ps.getFirstname());
                        }

                        if (((Metadata) ps).getAuthorityID() != null && ps.getAuthorityURI() != null && ps.getAuthorityValue() != null) {
                            newps.setAutorityFile(((Metadata) ps).getAuthorityID(), ps.getAuthorityURI(), ps.getAuthorityValue());
                        }

                        if (((Person) ps).getInstitution() != null) {
                            newps.setInstitution(((Person) ps).getInstitution());
                        }
                        if (((Person) ps).getAffiliation() != null) {
                            newps.setAffiliation(((Person) ps).getAffiliation());
                        }
                        if (ps.getRole() != null) {
                            newps.setRole(ps.getRole());
                        }
                        newStruct.addPerson(newps);
                    } catch (IncompletePersonObjectException e) {
                        // This should never happen as we are adding the same
                        // person type.
                        String message = "This " + e.getClass().getName() + " should not have been occurred!";
                        logger.error(message, e);
                    } catch (MetadataTypeNotAllowedException e) {
                        // This should never happen as we are adding the same
                        // person type.
                        String message = "This " + e.getClass().getName() + " should not have been occurred!";
                        logger.error(message, e);
                    }
                }
            }
            }

        // Iterate over all children, if recursive set to true.
        if ((recursive == null || recursive == true) && this.getAllChildren() != null) {
            for (DocStructInterface child : this.getAllChildren()) {
                if (recursive == null
                        && (type == null || type.getAnchorClass() == null || child.getType() == null || !type
                                .getAnchorClass().equals(child.getType().getAnchorClass()))) {
                    continue;
                }
                DocStruct copiedChild = (DocStruct) child.copy(cpmetadata, recursive);
                try {
                    newStruct.addChild(copiedChild);
                } catch (TypeNotAllowedAsChildException e) {
                    String message = "This " + e.getClass().getName() + " should not have been occurred!";
                    logger.error(message, e);
                }
            }
        }

        return newStruct;
    }

    /**
     * Returns a partial copy the structural tree with all structural elements
     * down to one level below the given anchor class, and meta-data attached
     * only to elements of the given anchor class.
     *
     * @param anchorClass
     *            anchor class below which the copy shall be truncated
     * @return a partial copy of the structure tree
     */
    public DocStruct copyTruncated(String anchorClass) {
        return copyTruncated(anchorClass, parent);
    }

    /**
     * Returns a partial copy the structural tree with all structural elements
     * down to one level below the given anchor class, and meta-data attached
     * only to elements of the given anchor class.
     *
     * @param anchorClass
     *            anchor class below which the copy shall be truncated
     * @param parent
     *            parent class of the copy to create
     * @return a partial copy of the structure tree
     */
    private DocStruct copyTruncated(String anchorClass, DocStruct parent) {

        try {
            DocStruct newStruct = new DocStruct(type);
            newStruct.parent = parent;
            newStruct.logical = this.logical;

            if (anchorClass == null ? type.getAnchorClass() == null : anchorClass.equals(type.getAnchorClass())) {
                if (allMetadata != null) {
                    for (MetadataInterface md : allMetadata) {
                        if (MetsModsImportExport.CREATE_MPTR_ELEMENT_TYPE.equals(md.getType().getName())) {
                            continue;
                        }
                        Metadata mdnew = new Metadata((MetadataType) md.getType());
                        mdnew.setValue(md.getValue());
                        if (((Metadata) md).getValueQualifier() != null && ((Metadata) md).getValueQualifierType() != null) {
                            mdnew.setValueQualifier(((Metadata) md).getValueQualifier(), ((Metadata) md).getValueQualifierType());
                        }
                        if (((Metadata) md).getAuthorityID() != null && ((Metadata) md).getAuthorityValue() != null
                                && ((Metadata) md).getAuthorityURI() != null) {
                            mdnew.setAutorityFile(((Metadata) md).getAuthorityID(), ((Metadata) md).getAuthorityURI(), ((Metadata) md).getAuthorityValue());
                        }
                        newStruct.addMetadata(mdnew);
                    }
                }

                if (allMetadataGroups != null) {
                    for (MetadataGroupInterface md : this.getAllMetadataGroups()) {
                        MetadataGroup mdnew = new MetadataGroup((MetadataGroupType) md.getType());
                        mdnew.setDocStruct(newStruct);
                        List<MetadataInterface> newmdlist = new LinkedList<MetadataInterface>();
                        Collection<PersonInterface> newPersonList = new LinkedList<PersonInterface>();
                        for (MetadataInterface meta : md.getMetadataList()) {
                            Metadata newMeta = new Metadata((MetadataType) meta.getType());
                            newMeta.setValue(meta.getValue());
                            if (((Metadata) meta).getValueQualifier() != null && ((Metadata) meta).getValueQualifierType() != null) {
                                newMeta.setValueQualifier(((Metadata) meta).getValueQualifier(), ((Metadata) meta).getValueQualifierType());
                            }
                            if (((Metadata) meta).getAuthorityID() != null && ((Metadata) meta).getAuthorityValue() != null
                                    && ((Metadata) meta).getAuthorityURI() != null) {
                                newMeta.setAutorityFile(((Metadata) meta).getAuthorityID(), ((Metadata) meta).getAuthorityURI(),
                                        ((Metadata) meta).getAuthorityValue());
                            }
                            newmdlist.add(newMeta);
                        }

                        for (PersonInterface ps : md.getPersonList()) {
                            Person newps = new Person((MetadataType) ps.getType());
                            if (ps.getLastname() != null) {
                                newps.setLastname(ps.getLastname());
                            }
                            if (ps.getFirstname() != null) {
                                newps.setFirstname(ps.getFirstname());
                            }
                            if (((Metadata) ps).getAuthorityID() != null && ps.getAuthorityURI() != null
                                    && ps.getAuthorityValue() != null) {
                                newps.setAutorityFile(((Metadata) ps).getAuthorityID(), ps.getAuthorityURI(), ps.getAuthorityValue());
                            }
                            if (((Person) ps).getInstitution() != null) {
                                newps.setInstitution(((Person) ps).getInstitution());
                            }
                            if (((Person) ps).getAffiliation() != null) {
                                newps.setAffiliation(((Person) ps).getAffiliation());
                            }
                            if (ps.getRole() != null) {
                                newps.setRole(ps.getRole());
                            }
                            newPersonList.add(newps);
                        }
                        mdnew.setMetadataList(newmdlist);
                        mdnew.setPersonList(newPersonList);
                        newStruct.addMetadataGroup(mdnew);

                        mdnew.setMetadataList(newmdlist);
                        newStruct.addMetadataGroup(mdnew);

                    }
                }

                // Copy the persons.
                if (this.getAllPersons() != null) {
                    for (PersonInterface ps : this.getAllPersons()) {

                        Person newps = new Person((MetadataType) ps.getType());
                        if (ps.getLastname() != null) {
                            newps.setLastname(ps.getLastname());
                        }
                        if (ps.getFirstname() != null) {
                            newps.setFirstname(ps.getFirstname());
                        }

                        if (((Person) ps).getAuthorityID() != null && ps.getAuthorityURI() != null
                                && ps.getAuthorityValue() != null) {
                            newps.setAutorityFile(((Person) ps).getAuthorityID(), ps.getAuthorityURI(), ps.getAuthorityValue());
                        }

                        if (((Person) ps).getInstitution() != null) {
                            newps.setInstitution(((Person) ps).getInstitution());
                        }
                        if (((Person) ps).getAffiliation() != null) {
                            newps.setAffiliation(((Person) ps).getAffiliation());
                        }
                        if (ps.getRole() != null) {
                            newps.setRole(ps.getRole());
                        }
                        newStruct.addPerson(newps);

                    }
                }
            } else if (allMetadata != null
                    && parent != null
                    && parent.getType().getAnchorClass() != null
                    && parent.getType().getAnchorClass().equals(anchorClass)
                    && (anchorClass == null ? type.getAnchorClass() != null : !anchorClass
                            .equals(type.getAnchorClass()))) {
                for (MetadataInterface md : allMetadata) {
                    if (!FOREIGN_CHILD_METADATA_TYPES_TO_COPY.contains(md.getType().getName())) {
                        continue;
                    }
                    Metadata mdnew = new Metadata((MetadataType) md.getType());
                    mdnew.setValue(md.getValue());
                    newStruct.addMetadata(mdnew);
                }
            } else if (allMetadata != null
                    && children != null
                    && (anchorClass == null ? type.getAnchorClass() != null : !anchorClass
                            .equals(type.getAnchorClass())))
                for (DocStructInterface child : children)
                    if (anchorClass == null ? child.getAnchorClass() == null : anchorClass.equals(child
                            .getAnchorClass())) {
                        for (MetadataInterface md : allMetadata) {
                            if (!FOREIGN_CHILD_METADATA_TYPES_TO_COPY.contains(md.getType().getName())) {
                                continue;
                            }
                            Metadata mdnew = new Metadata((MetadataType) md.getType());
                            mdnew.setValue(md.getValue());
                            newStruct.addMetadata(mdnew);
                        }
                        break;
                    }

            if (children != null
                    && (anchorClass.equals(type.getAnchorClass()) || parent == null || !parent.getType()
                            .getAnchorClass().equals(anchorClass))) {
                for (DocStructInterface child : this.getAllChildren()) {
                    if ((anchorClass == null ? type.getAnchorClass() == null : anchorClass.equals(type.getAnchorClass()))
                            || !((DocStruct) child).isMetsPointerStruct() ) {
                        DocStruct copiedChild = ((DocStruct) child).copyTruncated(anchorClass, this);
                        newStruct.addChild(copiedChild);
                    }
                }
            }

            return newStruct;
        } catch (UGHException thisShouldNeverHappen) {
            throw new RuntimeException(thisShouldNeverHappen.getMessage(), thisShouldNeverHappen);
        }
    }

    /**
     * Returns whether this instance contains METS pointers by itself or whose
     * children are, without exception, METS pointers in the same sense.
     *
     * @return whether this contains only METS pointers
     */
    public boolean isMetsPointerStruct() {
        if (getMetadataByType(MetsModsImportExport.CREATE_MPTR_ELEMENT_TYPE).size() > 0) {
            return true;
        }
        if (children == null || children.isEmpty()) {
            return false;
        }
        for (DocStructInterface child : children) {
            if (!((DocStruct) child).isMetsPointerStruct()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns incoming or outgoing {@code Reference}s.
     *
     * @param in
     *            can be "{@code to}" or "{@code from}"
     * @return incoming or outgoing {@code Reference}s
     */
    @Override
    public List<ReferenceInterface> getAllReferences(String in) {

        if (in == null) {
            return null;
        }
        if (in.equals("to")) {
            return this.docStructRefsTo;
        }
        if (in.equals("from")) {
            return this.docStructRefsFrom;
        }

        return null;
    }

    /**
     * Returns all references that are directed from this instance to another.
     * This are all {@code Reference}s in which this instance is the source.
     *
     * @return all outgoing {@code Reference}s
     */
    @Override
    public Collection<ReferenceInterface> getAllToReferences() {
        return this.docStructRefsTo;
    }

    /**
     * Returns all references that are directed from this instance to another
     * and have a given type. For example, the type "{@code logical_physical}"
     * refers to references from logical structures to physical structures.
     *
     * @param theType
     *            type of the references to return
     * @return all outgoing {@code Reference}s of the given type
     */
    @Override
    public Collection<ReferenceInterface> getAllToReferences(String theType) {

        List<ReferenceInterface> refs = new LinkedList<ReferenceInterface>();

        if (this.docStructRefsTo != null) {
            for (ReferenceInterface ref : this.docStructRefsTo) {
                if (ref.getType().equals(theType)) {
                    refs.add(ref);
                }
            }
        }

        if (refs == null || refs.isEmpty()) {
            return null;
        }

        return refs;
    }

    /**
     * Returns all references that are directed from another instance to this
     * instance. This are all {@code Reference}s in which this instance is the
     * target.
     *
     * @return all incoming {@code Reference}s
     */
    @Override
    public List<ReferenceInterface> getAllFromReferences() {
        return this.docStructRefsFrom;
    }

    /**
     * Returns all references that are directed from another instance to this
     * instance and have a given type. For example, the type
     * "{@code logical_physical}" refers to references from logical structures
     * to physical structures.
     *
     * @return all incoming {@code Reference}s of the given type
     */
    public List<Reference> getAllFromReferences(String theType) {

        List<Reference> refs = new LinkedList<Reference>();

        if (this.docStructRefsFrom != null) {
            for (ReferenceInterface ref : this.docStructRefsFrom) {
                if (ref.getType().equals(theType)) {
                    refs.add((Reference) ref);
                }
            }
        }

        if (refs == null || refs.isEmpty()) {
            return null;
        }

        return refs;
    }

    /**
     * Sets the parent. Usually, setting the parent is not necessary as the
     * parent is set automatically when an instance is added as a child.
     *
     * @return true, if parent was set successfully
     */
    public boolean setParent(DocStruct inParent) {

        if (inParent != null) {
            // Remove this DocStruct instance fromt he child's list.
            inParent.removeChild(this);
        }

        // Usually we had to check if this parent allows this instance being a
        // child because of its DocStructType.

        // Add child to this parent.
        this.parent = inParent;

        return true;
    }

    /**
     * Returns the parent of this instance. Returns {@code null} if this
     * instance is the root of the tree.
     *
     * @return the parent, if any
     */
    @Override
    public DocStruct getParent() {
        return this.parent;
    }

    /**
     * Returns all meta-data groups from this instance. If no
     * {@link MetadataGroup} is available, null is returned.
     *
     * @return all meta-data groups from this instance
     */
    @Override
    public List<MetadataGroupInterface> getAllMetadataGroups() {

        if (this.allMetadataGroups == null || this.allMetadataGroups.isEmpty()) {
            return null;
        }

        return this.allMetadataGroups;
    }

    /**
     * Replaces all meta-data groups on this instance. {@link MetadataGroup}s
     * which are already members of this instance will be overwritten, they are
     * not added. The meta-data groups are contained in a List.
     *
     * @param inList
     *            list of meta-data groups to set
     * @return always true
     */
    public boolean setAllMetadataGroups(List<MetadataGroupInterface> inList) {
        this.allMetadataGroups = inList;

        return true;
    }

    /**
     * Returns all meta-data from this instance. If no {@link Metadata} is
     * available, {@code null} is returned.
     *
     * @return all meta-data from this instance
     */
    @Override
    public List<MetadataInterface> getAllMetadata() {

        if (this.allMetadata == null || this.allMetadata.isEmpty()) {
            return null;
        }

        return this.allMetadata;
    }

    /**
     * Replaces all meta-data on this instance. {@link Metadata} which is
     * already members of this instance will be overwritten, the elements passed
     * in are not added. The meta-data is contained in a List.
     *
     * @param inList
     *            list of meta-data to set
     * @return always true
     */
    public boolean setAllMetadata(List<MetadataInterface> inList) {
        this.allMetadata = inList;

        return true;
    }

    /**
     * Returns all content files from this instance. If no {@link ContentFile}
     * is available, {@code null} is returned.
     *
     * @return the content files from this instance
     */
    @Override
    public List<ContentFileInterface> getAllContentFiles() {

        List<ContentFileInterface> contentFiles = new LinkedList<ContentFileInterface>();

        if (this.contentFileReferences == null || this.contentFileReferences.isEmpty()) {
            return null;
        }

        for (ContentFileReference contentFileReference : this.contentFileReferences) {
            // Add it, if it is not null AND it doesn't already belong to the
            // list.
            if (contentFileReference != null && !contentFiles.contains(contentFileReference.getCf())) {
                contentFiles.add(contentFileReference.getCf());
            }
        }

        return contentFiles;
    }

    /**
     * Returns whether this instance has a meta-data group of the given type.
     *
     * @param inMDT
     *            type to look for
     * @return whether this has an object of that type
     */
    public boolean hasMetadataGroupType(MetadataGroupType inMDT) {

        // Check metadata.
        List<MetadataGroupInterface> allMDs = this.getAllMetadataGroups();
        if (allMDs != null) {
            for (MetadataGroupInterface md : allMDs) {
                MetadataGroupType mdt = (MetadataGroupType) md.getType();
                if (inMDT != null && inMDT.getName().equals(mdt.getName())) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Returns whether this instance has a meta-data or person object of the
     * given type.
     *
     * @param inMDT
     *            type to look for
     * @return whether this has an object of that type
     */
    public boolean hasMetadataType(MetadataType inMDT) {

        // Check metadata.
        List<MetadataInterface> allMDs = this.getAllMetadata();
        if (allMDs != null) {
            for (MetadataInterface md : allMDs) {
                MetadataType mdt = (MetadataType) md.getType();
                if (inMDT != null && inMDT.getName().equals(mdt.getName())) {
                    return true;
                }
            }
        }

        // Check persons.
        List<PersonInterface> allPersons = this.getAllPersons();
        if (allPersons != null) {
            for (PersonInterface per : allPersons) {
                MetadataType mdt = (MetadataType) per.getType();
                if (inMDT != null && inMDT.getName().equals(mdt.getName())) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Returns all references to content files.
     *
     * @return all references to content files
     * @see ContentFileReference
     */
    public List<ContentFileReference> getAllContentFileReferences() {
        return this.contentFileReferences;
    }

    /**
     * Adds a new reference to a content file, and adds the content file to the
     * file set.
     *
     * @param theFile
     *            content file to add
     * @return always true
     * @see ContentFile
     * @see ContentFileReference
     * @see FileSet
     */
    @Override
    public void addContentFile(ContentFileInterface theFile) {

        // Create a new FileSet if there is none available.
        FileSet fs;
        if (this.digdoc.getFileSet() == null) {
            fs = new FileSet();
            this.digdoc.setFileSet(fs);
        } else {
            fs = this.digdoc.getFileSet();
        }

        // Add the file, existence check is done in FileSet.addFile() now.
        fs.addFile(theFile);

        if (this.contentFileReferences == null) {
            this.contentFileReferences = new LinkedList<ContentFileReference>();
        }
        // Now we can add the reference to the ContentFile, if the reference is
        // not existing yet.
        ContentFileReference cfr = new ContentFileReference();
        cfr.setCf((ContentFile) theFile);
        if (!this.contentFileReferences.contains(cfr)) {
            this.contentFileReferences.add(cfr);
            ((ContentFile) theFile).addDocStructAsReference(this);
        }

    }

    /**
     * Adds an area reference to a content file to this instance. There is no
     * check, if a {@link ContentFile} is already linked to this instance.
     * Before adding the content file, make sure it has been added to the
     * {@link FileSet}.
     *
     * @param inCF
     *            file reference to add
     * @param inArea
     *            selected area in the file
     * @return always true
     * @see ContentFileArea
     */
    public void addContentFile(ContentFile inCF, ContentFileArea inArea) {

        if (this.contentFileReferences == null) {
            // Re-added this line, maybe was it's deletion an error?
            this.contentFileReferences = new LinkedList<ContentFileReference>();
        }

        // Check if ContentFile belongs already to the FileSet.
        FileSet fs = this.digdoc.getFileSet();
        // Get all content files of this digital document.
        Collection<ContentFileInterface> allCFs = fs.getAllFiles();
        if (!allCFs.contains(inCF)) {
            // Doesn't contain this content file.
            fs.addFile(inCF);
        }

        // Now add reference to ContentFile.
        ContentFileReference cfr = new ContentFileReference();
        cfr.setCfa(inArea);
        cfr.setCf(inCF);
        this.contentFileReferences.add(cfr);
        inCF.addDocStructAsReference(this);

    }

    /**
     * Removes all links from this instance to a given content file. If the
     * given {@link ContentFile} is referenced more than once from this
     * instance, all links are removed. For that reason, all attached
     * {@link ContentFileReference} objects are searched.
     *
     * @param theContentFile
     *            the content file to be removed
     * @return true, if successful
     * @throws ContentFileNotLinkedException
     *             if the {@code ContentFile} is not linked to this instance
     */
    @Override
    public boolean removeContentFile(ContentFileInterface theContentFile) throws ContentFileNotLinkedException {

        boolean removed = false;

        if (this.contentFileReferences == null) {
            return false;
        }

        List<ContentFileReference> copiedContentFileReferences = new LinkedList<ContentFileReference>(this.contentFileReferences);

        for (ContentFileReference cfr : copiedContentFileReferences) {
            if (cfr.getCf() != null && cfr.getCf().equals(theContentFile)) {
                // The ContentFile is in the Reference; so remove file and
                // reference.
                this.contentFileReferences.remove(cfr);
                ContentFile cf = cfr.getCf();
                cf.removeDocStructAsReference(this);
                removed = true;
            }
        }

        // Given ContentFile is NOT member.
        if (!removed) {
            String message = "Content file '" + theContentFile.getLocation() + "' is not a member of DocStruct '" + this.getType().getName() + "'";
            throw new ContentFileNotLinkedException(message);
        }

        return true;
    }

    /**
     * Adds an outgoing reference to another {@code DocStruct} instance.
     * {@link Reference}s are always linked both ways. Both {@code DocStruct}
     * instances are storing a reference to the other {@code DocStruct}
     * instance. This methods stores the outgoing reference. The
     * {@code DocStruct} instance given as a parameter is the target of the
     * Reference (to which is linked). The corresponding back-reference (from
     * the target to the source—this instance) is set automatically. Each
     * reference can contain a type.
     *
     * @param inDocStruct
     *            target to link to
     * @param theType
     *            the type of reference
     * @return a newly created References object containing information about
     *         linking both DocStructs
     */
    @Override
    public ReferenceInterface addReferenceTo(DocStructInterface inDocStruct, String theType) {

        Reference ref = new Reference();
        ref.setSource(this);
        ref.setTarget((DocStruct) inDocStruct);
        ref.setType(theType);
        this.docStructRefsTo.add(ref);
        ((DocStruct) inDocStruct).docStructRefsFrom.add(ref);
        return ref;
    }

    /**
     * Adds an incoming reference from another {@code DocStruct} instance. The
     * current instance is the target of the {@link Reference}. The
     * corresponding forward reference is added automatically to the source
     * {@code DocStruct}. For more details, see
     * {@link #addReferenceTo(DocStruct, String)} method.
     *
     * @param inDocStruct
     *            DocStruct object which is the source of the reference
     * @param theType
     *            linking information
     * @return a newly created References object containing information about
     *         linking both DocStructs
     */
    public Reference addReferenceFrom(DocStruct inDocStruct, String theType) {

        Reference ref = new Reference();
        ref.setTarget(this);
        ref.setSource(inDocStruct);
        ref.setType(theType);
        this.docStructRefsFrom.add(ref);
        inDocStruct.docStructRefsTo.add(ref);
        return ref;
    }

    /**
     * Removes an outgoing reference. An outgoing reference is a reference to
     * another {@code DocStruct} instance. The corresponding incoming
     * {@code Reference} in the target {@code DocStruct} is also deleted.
     *
     * @param target
     *            {@code DocStruct}
     * @return true, if successful
     */
    @Override
    public boolean removeReferenceTo(DocStructInterface inStruct) {

        @SuppressWarnings("unchecked")
        List<Reference> ll = new LinkedList<Reference>((Collection<? extends Reference>) this.docStructRefsTo);

        for (Reference ref : ll) {
            if (ref.getTarget().equals(inStruct)) {
                // Remove reference from this instance.
                this.docStructRefsTo.remove(ref);
                DocStruct targetStruct = ref.getTarget();
                List<ReferenceInterface> ll2 = targetStruct.docStructRefsFrom;
                // Remove the reference from target.
                if (ll2 != null) {
                    ll2.remove(ref);
                }
            }
        }

        return true;
    }

    /**
     * Removes an incoming reference. An incoming {@link Reference} is a
     * reference from another {@code DocStruct} to this instance. The
     * corresponding outgoing reference in the source {@code DocStruct} object
     * is also deleted.
     *
     * @param inStruct
     *            source {@code DocStruct}
     * @return true, if successful
     */
    public boolean removeReferenceFrom(DocStruct inStruct) {

        @SuppressWarnings("unchecked")
        List<Reference> ll = new LinkedList<Reference>((Collection<? extends Reference>) this.docStructRefsFrom);

        for (Reference ref : ll) {
            if (ref.getTarget().equals(inStruct)) {
                // Remove reference from this instance.
                this.docStructRefsFrom.remove(ref);
                DocStruct targetStruct = ref.getTarget();
                List<ReferenceInterface> ll2 = targetStruct.docStructRefsTo;
                // Remove the reference from source.
                if (ll2 != null) {
                    ll2.remove(ref);
                }
            }
        }

        return true;
    }

    /**
     * Adds a meta-data group to this instance. The method checks, if it is
     * allowed to add it, based on the configuration. If so, the object is added
     * and the method returns {@code true}, otherwise it returns {@code false}.
     * The {@link MetadataGroup} object must already include all necessary
     * information, such as {@link MetadataGroupType} and value.
     * <p>
     * For internal reasons, this method replaces the {@code MetadataGroupType}
     * object by a local copy, which is retrieved from the {@link DocStructType}
     * of this instance. The internal name of both {@code MetadataGroupType}
     * objects will still be identical afterwards. If a local copy cannot be
     * found, which means that the meta-data type is invalid on this instance,
     * false is returned.
     *
     * @param theMetadataGroup
     *            meta-data group to be added
     * @return true, if the meta-data group was successfully added, false
     *         otherwise
     * @throws MetadataTypeNotAllowedException
     *             if the {@code DocStructType} of this instance does not allow
     *             the {@code MetadataGroupType}, or if the maximum number of
     *             meta-data groups of this type has already been added
     * @throws DocStructHasNoTypeException
     *             if no {@code DocStructType} is set on this instance. In this
     *             case, the meta-data group cannot be added because we cannot
     *             check whether the the meta-data group type is allowed or not.
     * @see MetadataGroup
     */
    @Override
    public boolean addMetadataGroup(MetadataGroupInterface theMetadataGroup) throws MetadataTypeNotAllowedException, DocStructHasNoTypeException {

        MetadataGroupType inMdType = ((MetadataGroup) theMetadataGroup).getType();
        String inMdName = inMdType.getName();
        // Integer, number of metadata allowed for this metadatatype.
        String maxnumberallowed;
        // Integer, number of metadata already available.
        int number;
        // Metadata can only be inserted if set to true.
        boolean insert = false;
        // Prefs MetadataType.
        MetadataGroupType prefsMdType;

        // First get MetadataType object for the DocStructType to which this
        // document structure belongs to get global MDType.
        if (this.type == null) {
            String message = "Error occurred while adding metadata group of type '" + inMdName + "' to " + identify(this) + " DocStruct: DocStruct has no type.";
            logger.error(message);
            throw new DocStructHasNoTypeException(message);
        }

        prefsMdType = this.type.getMetadataGroupByGroup(inMdType);

        // Ask DocStructType instance to get MetadataType by Type. At this point
        // we are creating a local copy of the MetadataType object.
        if (prefsMdType == null && !(inMdName.startsWith(HIDDEN_METADATA_CHAR))) {
            MetadataTypeNotAllowedException e = new MetadataTypeNotAllowedException("Metadata not allowed for DocStruct '" + this.getType().getName() + "'");
            logger.error(e.getMessage());
            throw e;
        }

        // Check, if it's an internal MetadataType - all internal types begin
        // with the HIDDEN_METADATA_CHAR, we can have as many as we want.
        if (inMdName.startsWith(HIDDEN_METADATA_CHAR)) {
            maxnumberallowed = "*";
            prefsMdType = inMdType;
        } else {
            maxnumberallowed = this.type.getNumberOfMetadataGroups(prefsMdType);
        }

        // Check, if another Metadata instance is allowed.
        //
        // How many metadata are already available.
        number = countMDofthisType(inMdName);

        // As many as we want (zero or more).
        if (maxnumberallowed.equals("*")) {
            insert = true;
        }

        // Once or more.
        if (maxnumberallowed.equals("+") || maxnumberallowed.equals("+")) {
            insert = true;
        }

        // Only one, if we have already one, we cannot add it.
        if (maxnumberallowed.equalsIgnoreCase("1m") || maxnumberallowed.equalsIgnoreCase("1o")) {
            if (number < 1) {
                insert = true;
            } else {
                insert = false;
            }
        }

        // Add metadata.
        if (insert) {
            // Set type to MetadataType of the DocStructType.
            ((MetadataGroup) theMetadataGroup).setType(prefsMdType);
            // Set this document structure as myDocStruct.
            ((MetadataGroup) theMetadataGroup).setDocStruct(this);
            if (this.allMetadataGroups == null) {
                // Create list, if not already available.
                this.allMetadataGroups = new LinkedList<MetadataGroupInterface>();
            }
            this.allMetadataGroups.add(theMetadataGroup);
        } else {
            logger.debug("Not allowed to add metadata '" + inMdName + "'");
            MetadataTypeNotAllowedException mtnae = new MetadataTypeNotAllowedException("Metadata not allowed for DocStruct '" + this.getType().getName() + "'");
            logger.error(mtnae.getMessage());
            throw mtnae;
        }

        return true;
    }

    /**
     * Removes a meta-data group from this instance. If (according to
     * configuration) at least one meta-data group of this type is required on
     * this instance, the meta-data group will <i>not be removed</i>.
     * <p>
     * If you want to remove a meta-data group just to replace it, use the
     * method {{@link #changeMetadataGroup(MetadataGroup, MetadataGroup)}
     * instead.
     *
     * @param theMd
     *            meta-data group which should be removed
     * @return true, if the meta-data group was removed, false otherwise
     * @see #canMetadataGroupBeRemoved(MetadataGroupType)
     */
    @Override
    public boolean removeMetadataGroup(MetadataGroupInterface inMD) {
        ((MetadataGroup) inMD).myDocStruct = null;
        this.allMetadataGroups.remove(inMD);
        return true;
    }

    /**
     * Replaces a meta-data group. Only {@link MetadataGroup}s of the same
     * {@link MetadataGroupType} can be exchanged. This method can be used
     * instead of doing a remove and a later add. This method must be used, if a
     * meta-data group cannot be removed because the {@link Preferences} state
     * that there must always be at least one.
     * <p>
     * The meta-data group type of the new meta-data group is copied locally, as
     * it is done in {@link #addMetadataGroup(MetadataGroup)}.
     *
     * @param theOldMd
     *            meta-data group which shall be replaced
     * @param theNewMd
     *            new meta-data group
     * @return true, if the meta-data group could be replaced
     */
    public boolean changeMetadataGroup(MetadataGroup theOldMd, MetadataGroup theNewMd) {

        MetadataGroupType oldMdt;
        MetadataGroupType newMdt;
        String oldName;
        String newName;
        int counter = 0;

        // Get MetadataTypes.
        oldMdt = theOldMd.getType();
        newMdt = theNewMd.getType();

        // Get names.
        oldName = oldMdt.getName();
        newName = newMdt.getName();

        if (oldName.equals(newName)) {
            // Different metadata types.
            return false;
        }

        // Remove old object; get place of old object in list.
        for (MetadataGroupInterface m : this.allMetadataGroups) {
            // Found old metadata object.
            if (m.equals(theOldMd)) {
                // Get out of loop.
                break;
            }
            counter++;
        }

        // Ask DocStructType instance to get a new MetadataType object of the
        // same kind.
        MetadataGroupType mdType = this.type.getMetadataGroupByGroup(theOldMd.getType());
        theNewMd.setType(mdType);

        this.allMetadataGroups.remove(theOldMd);
        this.allMetadataGroups.add(counter, theNewMd);

        return true;
    }

    /**
     * Returns all meta-data groups of a given type.
     * <p>
     * If no {@link MetadataGroup}s are available, an empty list is returned.
     *
     * @param inType
     *            MetadataType we are looking for.
     * @return all meta-data groups of that type
     */
    public List<MetadataGroup> getAllMetadataGroupsByType(MetadataGroupType inType) {

        List<MetadataGroup> resultList = new LinkedList<MetadataGroup>();

        // Check all metadata.
        if (inType != null && this.allMetadataGroups != null) {
            for (MetadataGroupInterface md : this.allMetadataGroups) {
                if (md.getType() != null && md.getType().getName().equals(inType.getName())) {
                    resultList.add((MetadataGroup) md);
                }
            }
        }

        return resultList;
    }

    /**
     * Adds a meta-data object to this instance. The method checks, if it is
     * allowed to add it, based on the configuration. If so, the object is added
     * and the method returns {@code true}, otherwise it returns {@code false}.
     * <p>
     * The {@link Metadata} object must already include all necessary
     * information, such as {@link MetadataType} and value.
     * <p>
     * For internal reasons, this method replaces the {@code MetadataType}
     * object by a local copy, which is retrieved from the {@link DocStructType}
     * of this instance. The internal name of both {@code MetadataType} objects
     * will still be identical afterwards. If a local copy cannot be found,
     * which means that the meta-data type is invalid on this instance, false is
     * returned.
     *
     * @param theMetadata
     *            meta-data object to add
     * @return true, if the meta-data object could be added
     * @throws MetadataTypeNotAllowedException
     *             if this instance does not allow the meta-data type to be
     *             added, or if the maximum allowed number of meta-data of this
     *             type has already been added
     * @throws DocStructHasNoTypeException
     *             if no {@code DocStructType} is set on this instance. In this
     *             case, the meta-data element cannot be added because we cannot
     *             check whether the the meta-data type is allowed or not.
     * @see Metadata
     */
    @Override
    public boolean addMetadata(MetadataInterface theMetadata) throws MetadataTypeNotAllowedException, DocStructHasNoTypeException {

        MetadataType inMdType = ((Metadata) theMetadata).getType();
        String inMdName = inMdType.getName();
        // Integer, number of metadata allowed for this metadatatype.
        String maxnumberallowed;
        // Integer, number of metadata already available.
        int number;
        // Metadata can only be inserted if set to true.
        boolean insert = false;
        // Prefs MetadataType.
        MetadataType prefsMdType;

        // First get MetadataType object for the DocStructType to which this
        // document structure belongs to get global MDType.
        if (this.type == null) {
            String message = "Error occurred while adding metadata of type '" + inMdName + "' to " + identify(this) + " DocStruct: DocStruct has no type.";
            logger.error(message);
            throw new DocStructHasNoTypeException(message);
        }

        prefsMdType = this.type.getMetadataTypeByType(inMdType);

        // Ask DocStructType instance to get MetadataType by Type. At this point
        // we are creating a local copy of the MetadataType object.
        if (prefsMdType == null && !(inMdName.startsWith(HIDDEN_METADATA_CHAR))) {
            MetadataTypeNotAllowedException e = new MetadataTypeNotAllowedException("Metadata of " + (inMdType == null ? "unknown type" : "type '" + inMdType.getName() + "'") + " not allowed for DocStruct '" + this.getType().getName() + "'");
            logger.error(e.getMessage());
            throw e;
        }

        // Check, if it's an internal MetadataType - all internal types begin
        // with the HIDDEN_METADATA_CHAR, we can have as many as we want.
        if (inMdName.startsWith(HIDDEN_METADATA_CHAR)) {
            maxnumberallowed = "*";
            prefsMdType = inMdType;
        } else {
            maxnumberallowed = this.type.getNumberOfMetadataType(prefsMdType);
        }

        // Check, if another Metadata instance is allowed.
        //
        // How many metadata are already available.
        number = countMDofthisType(inMdName);

        // As many as we want (zero or more).
        if (maxnumberallowed.equals("*")) {
            insert = true;
        }

        // Once or more.
        if (maxnumberallowed.equals("+") || maxnumberallowed.equals("+")) {
            insert = true;
        }

        // Only one, if we have already one, we cannot add it.
        if (maxnumberallowed.equalsIgnoreCase("1m") || maxnumberallowed.equalsIgnoreCase("1o")) {
            if (number < 1) {
                insert = true;
            } else {
                insert = false;
            }
        }

        // Add metadata.
        if (insert) {
            // Set type to MetadataType of the DocStructType.
            theMetadata.setType(prefsMdType);
            // Set this document structure as myDocStruct.
            theMetadata.setDocStruct(this);
            if (this.allMetadata == null) {
                // Create list, if not already available.
                this.allMetadata = new LinkedList<MetadataInterface>();
            }
            this.allMetadata.add((theMetadata));
        } else {
            logger.debug("Not allowed to add metadata '" + inMdName + "'");
            MetadataTypeNotAllowedException mtnae = new MetadataTypeNotAllowedException("Metadata of " + (inMdType == null ? "unknown type" : "type '" + inMdType.getName() + "'") + " not allowed for DocStruct '" + this.getType().getName() + "'");
            logger.error(mtnae.getMessage());
            throw mtnae;
        }

        return true;
    }

    /**
     * Removes a meta-datum from this instance. If (according to configuration)
     * at least one {@link Metadata} of this type is required on this instance,
     * the meta-datum will <i>not be removed</i>.
     * <p>
     * If you want to remove a meta-data group just to replace it, use the
     * method {@link #changeMetadata(Metadata, Metadata)} instead.
     *
     * @param theMd
     *            meta-datum which should be removed
     * @return true, if the meta-datum removed, false otherwise
     * @see #canMetadataBeRemoved(MetadataType)
     */
    @Override
    public boolean removeMetadata(MetadataInterface inMD) {
        ((Metadata) inMD).myDocStruct = null;
        this.allMetadata.remove(inMD);
        return true;
    }

    /**
     * Replaces a meta-datum by another. Only {@link Metadata} of the same
     * {@link MetadataType} can be exchanged. This method can be used instead of
     * doing a remove and a later add. This method must be used, if a meta-datum
     * cannot be removed because the {@link Preferences} state that there must
     * always be at least one.
     * <p>
     * The meta-data type of the new meta-datum is copied locally, as it is done
     * in {@link #addMetadata(Metadata)}.
     *
     * @param theOldMd
     *            meta-data group which shall be replaced
     * @param theNewMd
     *            new meta-data group
     * @return true, if the meta-data group could be replaced
     */
    public boolean changeMetadata(Metadata theOldMd, Metadata theNewMd) {

        MetadataType oldMdt;
        MetadataType newMdt;
        String oldName;
        String newName;
        int counter = 0;

        // Get MetadataTypes.
        oldMdt = theOldMd.getType();
        newMdt = theNewMd.getType();

        // Get names.
        oldName = oldMdt.getName();
        newName = newMdt.getName();

        if (oldName.equals(newName)) {
            // Different metadata types.
            return false;
        }

        // Remove old object; get place of old object in list.
        for (MetadataInterface m : this.allMetadata) {
            // Found old metadata object.
            if (m.equals(theOldMd)) {
                // Get out of loop.
                break;
            }
            counter++;
        }

        // Ask DocStructType instance to get a new MetadataType object of the
        // same kind.
        MetadataType mdType = this.type.getMetadataTypeByType(theOldMd.getType());
        theNewMd.setType(mdType);

        this.allMetadata.remove(theOldMd);
        this.allMetadata.add(counter, theNewMd);

        return true;
    }

    /**
     * Returns all meta-data of a given type, including persons. Can be used to
     * get all titles, authors, etc.
     * <p>
     * If no {@link MetadataGroup}s are available, an empty list is returned.
     *
     * @param inType
     *            meta-data type to look for
     * @return all meta-data of the given type
     */
    @Override
    public List<? extends Metadata> getAllMetadataByType(MetadataTypeInterface inType) {

        List<Metadata> resultList = new LinkedList<Metadata>();

        // Check all metadata.
        if (inType != null && this.allMetadata != null) {
            for (MetadataInterface md : this.allMetadata) {
                if (md.getType() != null && md.getType().getName().equals(inType.getName())) {
                    resultList.add((Metadata) md);
                }
            }
        }

        // Check all persons.
        if (inType != null && this.persons != null) {
            for (PersonInterface md : this.persons) {
                if (md.getType() != null && md.getType().getName().equals(inType.getName())) {
                    resultList.add((Person) md);
                }
            }
        }

        return resultList;
    }

    /**
     * Returns all persons of a given type. {@link Person}s only.
     * <p>
     * If no {@code Person} objects are available, null is returned.
     *
     * @param inType
     *            meta-data type to look for
     * @return all meta-data of the given type
     */
    @Override
    public List<PersonInterface> getAllPersonsByType(MetadataTypeInterface inType) {

        List<PersonInterface> resultList = new LinkedList<PersonInterface>();

        if (inType == null) {
            return null;
        }

        // Check all persons.
        if (this.persons != null) {
            for (PersonInterface per : this.persons) {
                if (per.getType() != null && per.getType().getName().equals(inType.getName())) {
                    resultList.add(per);
                }
            }
        }

        // List is empty.
        if (resultList.size() == 0) {
            return null;
        }

        return resultList;
    }

    /**
     * Returns all meta-data for this instance that shall be displayed. This
     * excludes all {@link Metadata} whose {@link MetadataType} starts with the
     * {@link #HIDDEN_METADATA_CHAR}.
     *
     * @return all meta-data that shall be displayed
     */
    @Override
    public List<Metadata> getAllVisibleMetadata() {

        // Start with the list of all metadata.
        List<Metadata> result = new LinkedList<Metadata>();

        // Iterate over all metadata.
        if (getAllMetadata() != null) {
            for (MetadataInterface md : getAllMetadata()) {
                // If the metadata does not start with the HIDDEN_METADATA_CHAR,
                // add it to the result list.
                if (md.getType().getName() != null && !md.getType().getName().startsWith(HIDDEN_METADATA_CHAR)) {
                    result.add((Metadata) md);
                }
            }
        }

        if (result.isEmpty()) {
            result = null;
        }

        return result;
    }

    /**
     * Returns all meta-data group types that shall be displayed even if they
     * have no value.
     * <p>
     * Comprises all meta-data group types whose attribute
     * {@code defaultDisplay="true"} is set in the {@link Preferences}. Hidden
     * meta-data groups, whose {@link MetadataGroupType} starts with the
     * {@link #HIDDEN_METADATA_CHAR}, will not be included.
     *
     * @return all meta-data group types that shall always be displayed
     */
    public List<MetadataGroupType> getDefaultDisplayMetadataGroupTypes() {

        List<MetadataGroupType> result = new LinkedList<MetadataGroupType>();

        if (this.type == null) {
            return null;
        }

        // Start with the list of MetadataTypes, which are having the
        // "defaultDisplay" attribute set.
        List<MetadataGroupType> allDefaultMdTypes = this.type.getAllDefaultDisplayMetadataGroups();

        if (allDefaultMdTypes != null) {
            // Iterate over all defaultDisplay metadata types and check, if
            // metadata of this type is already available.
            for (MetadataGroupType mdt : allDefaultMdTypes) {
                if (!hasMetadataGroup(mdt.getName()) && !mdt.getName().startsWith(HIDDEN_METADATA_CHAR)) {
                    // If none of these metadata is available, AND it is not a
                    // hidden metadata type, add it to the result list.
                    result.add(mdt);
                }
            }
        }

        if (result.isEmpty()) {
            result = null;
        }

        return result;
    }

    /**
     * Returns all meta-data group types that shall be displayed even if they
     * have no value.
     * <p>
     * Includes all meta-data group with attribute {@code defaultDisplay="true"}
     * in the {@link Preferences}. Hidden meta-data, whose
     * {@link MetadataGroupType} starts with the {@link #HIDDEN_METADATA_CHAR},
     * will not be included.
     *
     * @return all meta-data group types that shall always be displayed
     * @deprecated This is a misnomer and will be deleted. Use
     *             {@link #getDefaultDisplayMetadataGroupTypes()}.
     */
    @Deprecated
    public List<MetadataGroupType> getDisplayMetadataGroupTypes() {
        return getDefaultDisplayMetadataGroupTypes();
    }

    private boolean hasMetadataGroup(String metadataGroupTypeName) {

        if (this.allMetadataGroups != null) {
            for (MetadataGroupInterface md : this.allMetadataGroups) {
                MetadataGroupType mdt = (MetadataGroupType) md.getType();
                if (mdt == null) {
                    continue;
                }
                String name = mdt.getName();
                if (name.equals(metadataGroupTypeName)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Returns all meta-data types that shall be displayed even if they have no
     * value.
     * <p>
     * Comprises all meta-data types whose attribute
     * {@code defaultDisplay="true"} is set in the {@link Preferences}. Hidden
     * meta-data, whose {@link MetadataType} starts with the
     * {@link #HIDDEN_METADATA_CHAR}, will not be included.
     *
     * @return all meta-data group types that shall always be displayed
     */
    public List<MetadataTypeInterface> getDefaultDisplayMetadataTypes() {

        List<MetadataTypeInterface> result = new LinkedList<MetadataTypeInterface>();

        if (this.type == null) {
            return null;
        }

        // Start with the list of MetadataTypes, which are having the
        // "defaultDisplay" attribute set.
        List<MetadataType> allDefaultMdTypes = this.type.getAllDefaultDisplayMetadataTypes();

        if (allDefaultMdTypes != null) {
            // Iterate over all defaultDisplay metadata types and check, if
            // metadata of this type is already available.
            for (MetadataType mdt : allDefaultMdTypes) {
                if (!hasMetadata(mdt.getName()) && !mdt.getName().startsWith(HIDDEN_METADATA_CHAR)) {
                    // If none of these metadata is available, AND it is not a
                    // hidden metadata type, add it to the result list.
                    result.add(mdt);
                }
            }
        }

        if (result.isEmpty()) {
            result = null;
        }

        return result;
    }

    /**
     * Returns all meta-data types that shall be displayed even if they have no
     * value.
     * <p>
     * Comprises all meta-data types whose attribute
     * {@code defaultDisplay="true"} is set in the {@link Preferences}. Hidden
     * meta-data, whose {@link MetadataType} starts with the
     * {@link #HIDDEN_METADATA_CHAR}, will not be included.
     *
     * @return all meta-data group types that shall always be displayed
     * @deprecated This is a misnomer and will be deleted. Use
     *             {@link #getDefaultDisplayMetadataTypes()}.
     */
    @Override
    @Deprecated
    public List<MetadataTypeInterface> getDisplayMetadataTypes() {
        return getDefaultDisplayMetadataTypes();
    }

    private boolean hasMetadata(String metadataTypeName) {

        if (this.allMetadata != null) {
            for (MetadataInterface md : this.allMetadata) {
                MetadataType mdt = (MetadataType) md.getType();
                if (mdt == null) {
                    continue;
                }
                String name = mdt.getName();
                if (name.equals(metadataTypeName)) {
                    return true;
                }
            }
        }

        if (this.persons != null) {
            for (PersonInterface per : this.persons) {
                MetadataType mdt = (MetadataType) per.getType();
                if (mdt == null) {
                    continue;
                }
                String name = mdt.getName();
                if (name.equals(metadataTypeName)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Returns number of meta-data elements of the given type associated with
     * this instance. The type must be given as its internal name, as it is
     * returned from {@link MetadataType#getName()}.
     * <p>
     * This method does not only get the number of {@link Metadata} elements,
     * but also the number of {@link Person} objects belonging to this instance.
     *
     * @param inTypeName
     *            meta-data type name
     * @return number of meta-data elements
     */
    public int countMDofthisType(String inTypeName) {

        MetadataType testtype;
        int counter = 0;

        if (this.allMetadata != null) {
            for (MetadataInterface md : this.allMetadata) {
                testtype = (MetadataType) md.getType();
                if (testtype != null && testtype.getName().equals(inTypeName)) {
                    // Another one is available.
                    counter++;
                }
            }
        }

        if (allMetadataGroups != null) {
            for (MetadataGroupInterface mdg : allMetadataGroups) {
                MetadataGroupType mgt = (MetadataGroupType) mdg.getType();
                if (mgt != null && mgt.getName().equals(inTypeName)) {
                    // Another one is available.
                    counter++;
                }
            }

        }

        if (this.persons != null) {
            for (PersonInterface per : this.persons) {
                testtype = (MetadataType) per.getType();
                if (testtype != null && testtype.equals(inTypeName)) {
                    // Another one is available.
                    counter++;
                }
            }
        }

        return counter;
    }

    /**
     * Returns all meta-data group types that can be added to this instance and
     * shall be visible to the user. This method considers already added
     * {@link MetadataGroup}s, so meta-data group types which can only be
     * available once cannot be added a second time. Therefore these
     * {@link MetadataGroupType}s will not be included in this list.
     * <p>
     * Internal meta-data groups, whose {@code MetadataGroupType} starts with
     * the {@link #HIDDEN_METADATA_CHAR}, will also not be included.
     *
     * @return all meta-data group types that users can add to this instance
     */
    @Override
    public List<MetadataGroupTypeInterface> getAddableMetadataGroupTypes() {

        // If e.g. the topstruct has no Metadata, or something...
        if (this.type == null) {
            return null;
        }

        // Get all Metadatatypes for my DocStructType.
        List<MetadataGroupTypeInterface> addableMetadata = new LinkedList<MetadataGroupTypeInterface>();
        List<MetadataGroupType> allTypes = this.type.getAllMetadataGroupTypes();

        // Get all metadata types which are known, iterate over them and check,
        // if they are still addable.
        for (MetadataGroupType mdt : allTypes) {

            // Metadata beginning with the HIDDEN_METADATA_CHAR are internal
            // metadata are not user addable.
            if (!mdt.getName().startsWith(HIDDEN_METADATA_CHAR)) {
                String maxnumber = this.type.getNumberOfMetadataGroups(mdt);

                // Metadata can only be available once; so we have to check if
                // it is already available.
                if (maxnumber.equals("1m") || maxnumber.equals("1o")) {
                    // Check metadata here only.
                    List<? extends MetadataGroup> availableMD = this.getAllMetadataGroupsByType(mdt);

                    if (availableMD.size() < 1) {
                        // Metadata is NOT available; we are allowed to add it.
                        addableMetadata.add(mdt);
                    }
                } else {
                    // We can add as many metadata as we want (+ or *).
                    addableMetadata.add(mdt);
                }
            }
        }

        if (addableMetadata == null || addableMetadata.isEmpty()) {
            return null;
        }

        return addableMetadata;
    }

    /**
     * Returns all meta-data group types that can be added to this instance.
     * Includes meta-data groups, whose {@code MetadataGroupType} starts with
     * the {@link #HIDDEN_METADATA_CHAR}.
     * <p>
     * This method considers already added {@link MetadataGroup}s, so meta-data
     * group types which can only be available once cannot be added a second
     * time. Therefore these {@link MetadataGroupType}s will not be included in
     * this list.
     *
     * @return all meta-data group types that can be added to this instance
     */
    public List<MetadataGroupType> getPossibleMetadataGroupTypes() {
        // If e.g. the topstruct has no Metadata, or something...
        if (this.type == null) {
            return null;
        }

        // Get all Metadatatypes for my DocStructType.
        List<MetadataGroupType> addableMetadata = new LinkedList<MetadataGroupType>();
        List<MetadataGroupType> allTypes = this.type.getAllMetadataGroupTypes();

        // Get all metadata types which are known, iterate over them and check,
        // if they are still addable.
        for (MetadataGroupType mdt : allTypes) {

            String maxnumber = this.type.getNumberOfMetadataGroups(mdt);

            // Metadata can only be available once; so we have to check if
            // it is already available.
            if (maxnumber.equals("1m") || maxnumber.equals("1o")) {
                // Check metadata here only.
                List<? extends MetadataGroup> availableMD = this.getAllMetadataGroupsByType(mdt);

                if (availableMD.size() < 1) {
                    // Metadata is NOT available; we are allowed to add it.
                    addableMetadata.add(mdt);
                }
            } else {
                // We can add as many metadata as we want (+ or *).
                addableMetadata.add(mdt);
            }

        }

        if (addableMetadata == null || addableMetadata.isEmpty()) {
            return null;
        }

        return addableMetadata;
    }

    /**
     * Returns all meta-data types that can be added to this instance and shall
     * be visible to the user. This method considers already added
     * {@link Metadata}, so meta-data types which can only be available once
     * cannot be added a second time. Therefore these {@link MetadataType}s will
     * not be included in this list.
     * <p>
     * Internal meta-data groups, whose {@code MetadataGroupType} starts with
     * the {@link #HIDDEN_METADATA_CHAR}, will also not be included.
     *
     * @return all meta-data types that users can add to this instance
     */
    @Override
    public List<MetadataTypeInterface> getAddableMetadataTypes() {

        // If e.g. the topstruct has no Metadata, or something...
        if (this.type == null) {
            return null;
        }

        // Get all Metadatatypes for my DocStructType.
        List<MetadataTypeInterface> addableMetadata = new LinkedList<MetadataTypeInterface>();
        List<MetadataTypeInterface> allTypes = this.type.getAllMetadataTypes();

        // Get all metadata types which are known, iterate over them and check,
        // if they are still addable.
        for (MetadataTypeInterface mdt : allTypes) {

            // Metadata beginning with the HIDDEN_METADATA_CHAR are internal
            // metadata are not user addable.
            if (!mdt.getName().startsWith(HIDDEN_METADATA_CHAR)) {
                String maxnumber = this.type.getNumberOfMetadataType(mdt);

                // Metadata can only be available once; so we have to check if
                // it is already available.
                if (maxnumber.equals("1m") || maxnumber.equals("1o")) {
                    // Check metadata here only.
                    List<? extends Metadata> availableMD = this.getAllMetadataByType(mdt);

                    if (!((MetadataType) mdt).isPerson && (availableMD.size() < 1)) {
                        // Metadata is NOT available; we are allowed to add it.
                        addableMetadata.add(mdt);
                    }

                    // Then check persons here.
                    boolean used = false;
                    if (mdt.getIsPerson() && this.getAllPersons() != null) {
                        for (PersonInterface per : this.getAllPersons()) {
                            // If the person of the current metadata type is
                            // already used, set the flag.
                            if (per.getRole().equals(mdt.getName())) {
                                used = true;
                            }
                        }

                        // Only add the metadata type, if the person was not
                        // already used.
                        if (!used) {
                            addableMetadata.add(mdt);
                        }
                    }
                } else {
                    // We can add as many metadata as we want (+ or *).
                    addableMetadata.add(mdt);
                }
            }
        }

        if (addableMetadata == null || addableMetadata.isEmpty()) {
            return null;
        }

        return addableMetadata;
    }

    /**
     * Returns all meta-data types that can be added to this instance. Includes
     * meta-data groups, whose {@code MetadataGroupType} starts with the
     * {@link #HIDDEN_METADATA_CHAR}.
     * <p>
     * This method considers already added {@link Metadata}, so meta-data types
     * which can only be available once cannot be added a second time. Therefore
     * these {@link MetadataType}s will not be included in this list.
     *
     * @return all meta-data types that can be added to this instance
     */
    @Override
    public List<MetadataTypeInterface> getPossibleMetadataTypes() {
        // If e.g. the topstruct has no Metadata, or something...
        if (this.type == null) {
            return null;
        }

        // Get all Metadatatypes for my DocStructType.
        List<MetadataTypeInterface> addableMetadata = new LinkedList<MetadataTypeInterface>();
        List<MetadataTypeInterface> allTypes = this.type.getAllMetadataTypes();

        // Get all metadata types which are known, iterate over them and check,
        // if they are still addable.
        for (MetadataTypeInterface mdt : allTypes) {

            String maxnumber = this.type.getNumberOfMetadataType(mdt);

            // Metadata can only be available once; so we have to check if
            // it is already available.
            if (maxnumber.equals("1m") || maxnumber.equals("1o")) {
                // Check metadata here only.
                List<? extends Metadata> availableMD = this.getAllMetadataByType(mdt);

                if (!((MetadataType) mdt).isPerson && (availableMD.size() < 1)) {
                    // Metadata is NOT available; we are allowed to add it.
                    addableMetadata.add(mdt);
                }

                // Then check persons here.
                boolean used = false;
                if (mdt.getIsPerson() && this.getAllPersons() != null) {
                    for (PersonInterface per : this.getAllPersons()) {
                        // If the person of the current metadata type is
                        // already used, set the flag.
                        if (per.getRole().equals(mdt.getName())) {
                            used = true;
                        }
                    }

                    // Only add the metadata type, if the person was not
                    // already used.
                    if (!used) {
                        addableMetadata.add(mdt);
                    }
                }
            } else {
                // We can add as many metadata as we want (+ or *).
                addableMetadata.add(mdt);
            }

        }

        if (addableMetadata == null || addableMetadata.isEmpty()) {
            return null;
        }

        return addableMetadata;
    }

    /**
     * Adds another {@code DocStruct} as a child to this instance. The new child
     * will automatically become the last child in the list. When adding a
     * {@code DocStruct}, configuration is checked, whether a {@code DocStruct}
     * of this type can be added. If not, a
     * {@link TypeNotAllowedAsChildException} is thrown. The parent of this
     * child (this instance) is set automatically.
     *
     * @param inchild
     *            DocStruct to be added
     * @return whether inchild isn’t null and its type isn’t null
     * @throws TypeNotAllowedAsChildException
     *             if a child should be added, but it's DocStruct type isn't
     *             member of this instance's DocStruct type
     */
    @Override
    public boolean addChild(DocStructInterface inchild) throws TypeNotAllowedAsChildException {
        return addChild((Integer) null, inchild);
    }

    /**
     * Adds a DocStruct object as a child to this instance. The new child will
     * become the element at the specified position in the child list while the
     * element currently at that position (if any) and any subsequent elements
     * are shifted to the right (so that one gets added to their indices), or
     * the last child in the list if index is null. When adding a DocStruct,
     * configuration is checked, wether a DocStruct of this type can be added.
     * If not, a TypeNotAllowedAsChildException is thrown. The parent of this
     * child (this instance) is set automatically.
     *
     * @param index
     *            index at which the child is to be inserted
     * @param inchild
     *            DocStruct to be added
     * @return wether inchild isn’t null and its type isn’t null
     * @throws TypeNotAllowedAsChildException
     *             if a child should be added, but it's DocStruct type isn't
     *             member of this instance's DocStruct type
     */
    @Override
    public boolean addChild(Integer index, DocStructInterface inchild) throws TypeNotAllowedAsChildException {

        if (inchild == null || inchild.getType() == null) {
            logger.warn("DocStruct or DocStructType is null");
            return false;
        }

        DocStructType childtype;
        boolean allowed = false;

        // Check, if type of child is allowed.
        childtype = ((DocStruct) inchild).getType();
        // Get all allowed types.
        for (String tempType : this.type.getAllAllowedDocStructTypes()) {
            if ((childtype.getName()).equals(tempType)) {
                allowed = true;
            }
        }

        if (!allowed) {
            TypeNotAllowedAsChildException tnaace = new TypeNotAllowedAsChildException("Child of type '" + childtype.getName() + "' is not allowed for parent; unfortunately we don't have any information about the parent");
            logger.error("DocStruct type '" + childtype + "' not allowed as child of type '" + this.getType().getName() + "'");
            throw tnaace;
        }

        // Create List for children, if not already available.
        if (this.children == null) {
            this.children = new LinkedList<DocStructInterface>();
        }

        // Set status to logical or physical.
        if (this.isLogical()) {
            ((DocStruct) inchild).setLogical(true);
        }
        if (this.isPhysical()) {
            ((DocStruct) inchild).setPhysical(true);
        }

        ((DocStruct) inchild).setParent(this);

        if (index == null) {
            // Add child to end of List.
            children.add(inchild);
        } else {
            children.add(index.intValue(), inchild);
        }

        // Child was added.
        return true;
    }

    /**
     * Adds a DocStruct object to a child of this instance, where is the
     * position to add it. The new child will automatically become the last
     * child in the list. When adding a DocStruct, configuration is checked,
     * wether a DocStruct of this type can be added. If not, it is not added and
     * false is returned. The parent of this child is set automatically.
     *
     * @param where
     *            where to add the DocStruct
     * @param inchild
     *            DocStruct to be added
     * @return true, if child was added; otherwise false
     * @throws TypeNotAllowedAsChildException
     *             if a child should be added, but it's DocStruct type isn't
     *             member of this instance's DocStruct type
     */
    public boolean addChild(String where, DocStruct inchild) throws TypeNotAllowedAsChildException {
        if (where == null || inchild == null || inchild.getType() == null) {
            logger.warn("DocStruct or DocStructType is null");
            return false;
        }

        // get next position of index
        int next = where.indexOf(44) + 1;

        return next != 0 ? ((DocStruct) children.get(Integer.parseInt(where.substring(0, next - 1)))).addChild(where.substring(next),
                inchild) : addChild(Integer.valueOf(where), inchild);
    }

    /**
     * Removes a child from this instance.
     *
     * @return true, if child was removed, otherwise false
     */
    @Override
    public boolean removeChild(DocStructInterface inchild) {

        if (this.children.remove(inchild)) {
            // Delete reference to parent.
            ((DocStruct) inchild).setParent(null);
            // It's not in the logical tree anymore.
            if (this.isLogical()) {
                ((DocStruct) inchild).setLogical(false);
            }
            // It's not in the physical tree anymore.
            if (this.isPhysical()) {
                ((DocStruct) inchild).setPhysical(false);
            }

            // Delete the parent reference.
            ((DocStruct) inchild).setParent(null);
            return true;
        }

        return false;
    }

    /**
     * Moves a child to another position in the list of children. The DocStruct
     * to be moved must already be child of this DocStruct.
     *
     * @param inchild
     *            DocStruct to be moved
     * @param position
     *            first child has position 1
     * @return true, if successful; otherwise false
     */
    public boolean moveChild(DocStruct inchild, int position) {

        if (position < 0) {
            return false;
        }
        if (position > this.children.size()) {
            position = this.children.size();
        }
        // Remove child first.
        if (!this.children.remove(inchild)) {
            return false;
        }

        // Add to the new position.
        try {
            this.children.add(position, inchild);
        } catch (UnsupportedOperationException uoe) {
            return false;
        } catch (ClassCastException cce) {
            return false;
        } catch (IllegalArgumentException iae) {
            return false;
        }

        return true;
    }

    /**
     * Moves a child below another child in the list of all children. Both
     * {@code DocStruct} objects must already be children of this instance.
     *
     * @param inchild
     *            DocStruct to be moved
     * @param below
     *            child below which the DocStruct in question should be moved
     * @return true, if it worked, otherwise false
     */
    public boolean moveChildafter(DocStruct inchild, DocStruct below) {

        DocStruct test;

        for (int i = 0; i < this.children.size(); ++i) {
            test = (DocStruct) this.children.get(i);
            // Child found.
            if (test.equals(below)) {
                if (moveChild(inchild, i + 1)) {
                    return true;
                }
                return false;
            }
        }

        return false;
    }

    /**
     * Moves a child right above another child in the list of all children. Both
     * {@code DocStruct} objects must already be children of this object.
     *
     * @param inchild
     *            DocStruct to be moved
     * @param above
     *            child above which the DocStruct should be moved
     * @return true, if it worked; otherwise false
     */
    public boolean moveChildbefore(DocStruct inchild, DocStruct above) {

        DocStruct test;

        for (int i = 0; i < this.children.size(); ++i) {
            test = (DocStruct) this.children.get(i);
            // Child found.
            if (test.equals(above)) {
                if (moveChild(inchild, i)) {
                    return true;
                }
                return false;
            }
        }

        return false;
    }

    /**
     * Returns the position of a child in the list of all children.
     *
     * @param inchild
     *            {@code DocStruct} whose position should be returned
     * @return position, or {@code -1} if child is not in the list
     */
    public int getPositionofChild(DocStruct inchild) {

        DocStruct test;

        for (int i = 0; i < this.children.size(); ++i) {
            test = (DocStruct) this.children.get(i);
            // Child found.
            if (test.equals(inchild)) {
                return i;
            }
        }

        return -1;
    }

    /**
     * Returns the next child in the list of all children. If the given
     * {@code DocStruct} isn’t a child of the current instance, {@code null} is
     * returned.
     *
     * @param inChild
     *            {@code DocStruct} whose successor shall be returned
     * @return the next {@code DocStruct} after {@code inChild}, {@code null}
     *         otherwise
     */
    @Override
    public DocStructInterface getNextChild(DocStructInterface inChild) {

        DocStruct nextchild;
        DocStruct test;

        for (int i = 0; i < this.children.size(); ++i) {
            test = (DocStruct) this.children.get(i);
            // Child found.
            if (test.equals(inChild)) {
                if (i != this.children.size()) {
                    nextchild = (DocStruct) this.children.get(i + 1);
                    return nextchild;
                }

                // This is already the last child.
                return null;
            }
        }

        // inChild is not member of children.
        return null;
    }

    /**
     * Returns the previous child in the list of all children. If the given
     * {@code DocStruct} isn’t a child of the current instance, {@code null} is
     * returned.
     *
     * @param inChild
     *            {@code DocStruct} whose predecessor shall be returned
     * @return the next {@code DocStruct} before {@code inChild}, {@code null}
     *         otherwise
     */
    public DocStruct getPreviousChild(DocStruct inChild) {

        DocStruct prevchild;
        DocStruct test;

        for (int i = 0; i < this.children.size(); ++i) {
            test = (DocStruct) this.children.get(i);
            // CHild found.
            if (test.equals(inChild)) {
                if (i != 0) {
                    prevchild = (DocStruct) this.children.get(i - 1);
                    return prevchild;
                }

                // This is already the last child.
                return null;
            }
        }

        // inChild is not member of children.
        return null;
    }

    /**
     * Returns whether a {@code DocStruct} of the given {@code DocStructType} is
     * allowed to be added to this instance.
     *
     * @param inType
     *            the {@code DocStructType} in question
     * @return true, if {@code DocStruct} of this type can be added; otherwise
     *         false
     */
    @Override
    public boolean isDocStructTypeAllowedAsChild(DocStructTypeInterface inType) {

        List<String> allTypes = this.type.getAllAllowedDocStructTypes();
        String typename = inType.getName();
        String testname;

        for (int i = 0; i < allTypes.size(); ++i) {
            testname = allTypes.get(i);
            // Jep, it's in here.
            if (testname.equals(typename)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns whether a meta-data group of the given type can be removed from
     * this instance.
     *
     * @see #removeMetadataGroup(MetadataGroup, boolean)
     * @param inMDType
     *            meta-data group type in question
     * @return true, if it can be removed, otherwise false
     */
    public boolean canMetadataGroupBeRemoved(MetadataGroupType inMDType) {

        // How many metadata of this type do we have already.
        int typesavailable = countMDofthisType(inMDType.getName());
        // How many types must be at least available.
        String maxnumbersallowed = this.type.getNumberOfMetadataGroups(inMDType);

        if (typesavailable == 1 && maxnumbersallowed.equals("+")) {
            // There must be at least one.
            return false;
        }

        if (typesavailable == 1 && maxnumbersallowed.equals("1m")) {
            // There must be at least one.
            return false;
        }

        return true;
    }

    /**
     * Returns whether a meta-datum of the given type can be removed from this
     * instance. There is no separate function to check whether persons can be
     * removed. As the {@link Person} inherits from the {@link Metadata} it has
     * a {@link MetadataType}. Therefore, this method can be used to check if a
     * person is removable or not as well.
     *
     * @see #removeMetadata(Metadata)
     * @see #removePerson(Person)
     * @param inMDType
     *            meta-data type in question
     * @return true, if it can be removed, otherwise false
     */
    public boolean canMetadataBeRemoved(MetadataType inMDType) {

        // How many metadata of this type do we have already.
        int typesavailable = countMDofthisType(inMDType.getName());
        // How many types must be at least available.
        String maxnumbersallowed = this.type.getNumberOfMetadataType(inMDType);

        if (typesavailable == 1 && maxnumbersallowed.equals("+")) {
            // There must be at least one.
            return false;
        }

        if (typesavailable == 1 && maxnumbersallowed.equals("1m")) {
            // There must be at least one.
            return false;
        }

        return true;
    }

    @Override
    public boolean addPerson(PersonInterface in) throws MetadataTypeNotAllowedException, IncompletePersonObjectException {

        // Max number of persons (from configuration).
        String maxnumberallowed = null;
        // Number of persons currently available.
        int number = 0;
        // Store, wether we can or cannot add information.
        boolean insert = false;

        // Check, if person is complete.
        if (in.getType() == null) {
            IncompletePersonObjectException ipoe = new IncompletePersonObjectException();
            logger.error("Incomplete data for person metadata");
            throw ipoe;
        }

        // Get MetadataType of this person get MetadataType from docstructType
        // object with the same name.
        MetadataType mdtype = this.type.getMetadataTypeByType((MetadataType) in.getType());
        if (mdtype == null) {
            MetadataTypeNotAllowedException mtnae = new MetadataTypeNotAllowedException();
            logger.error("MetadataType " + in.getType().getName() + " is not available for DocStruct '" + this.getType().getName() + "'");
            throw mtnae;
        }

        // Check, if docstruct may have this person ??? depends on the role
        // value of person.
        maxnumberallowed = this.type.getNumberOfMetadataType(mdtype);

        // Check, if another Person of this type is allowed. How many persons
        // are already available.
        number = countMDofthisType(mdtype.getName());

        // As many as we want (zero or more).
        if (maxnumberallowed.equals("*")) {
            insert = true;
        }
        // One or more.
        if (maxnumberallowed.equals("+") || maxnumberallowed.equals("+")) {
            insert = true;
        }
        // Only one, if we have already one, we cannot add it.
        if (maxnumberallowed.equals("1m") || maxnumberallowed.equals("1o")) {
            if (number < 1) {
                insert = true;
            } else {
                insert = false;
            }
        }

        // We can add this person.
        if (insert) {
            if (this.persons == null) {
                this.persons = new LinkedList<PersonInterface>();
            }
            this.persons.add(in);

            return true;
        }

        MetadataTypeNotAllowedException mtnae = new MetadataTypeNotAllowedException();
        logger.error("Person MetadataType '" + in.getType().getName() + "' not allowed for DocStruct '" + this.getType().getName() + "'");
        throw mtnae;
    }

    /**
     * Removes a person from this instance.
     *
     * @param in
     *            person which should be removed
     * @return true, if {@code in} is not {@code null}
     * @throws IncompletePersonObjectException
     *            if {@code in} does not have a {@link MetadataType}
     */
    @Override
    public boolean removePerson(PersonInterface in) throws IncompletePersonObjectException {
        if (this.persons == null) {
            return false;
        }

        if (in.getType() == null) {
            throw new IncompletePersonObjectException("Incomplete person: MetadataType is null");
        }

        this.persons.remove(in);

        return true;
    }

    /**
     * Returns a list of all persons. If no {@link Person} objects are
     * available, {@code null} is returned.
     *
     * @return all persons
     */
    @Override
    public List<PersonInterface> getAllPersons() {

        if (this.persons == null || this.persons.isEmpty()) {
            return null;
        }

        return this.persons;
    }

    public boolean isLogical() {
        return this.logical;
    }

    public void setLogical(boolean logical) {

        this.logical = logical;

        List<DocStructInterface> childList = this.getAllChildren();
        if (childList != null) {
            for (DocStructInterface ds : childList) {
                ((DocStruct) ds).setLogical(logical);
            }
        }
    }

    /**
     * @deprecated This is a misnomer and will be removed. Use
     *             {@link #getOrigObject()}.
     */
    @Deprecated
    public Object getOrig_object() {
        return this.origObject;
    }

    /**
     * @deprecated This is a misnomer and will be removed. Use
     *             {@link #setOrigObject(Object)}.
     */
    @Deprecated
    public void setOrig_object(Object theOrigObject) {
        this.origObject = theOrigObject;
    }

    public Object getOrigObject() {
        return this.origObject;
    }

    public void setOrigObject(Object theOrigObject) {
        this.origObject = theOrigObject;
    }

    public boolean isPhysical() {
        return this.physical;
    }

    public void setPhysical(boolean physical) {

        this.physical = physical;

        List<DocStructInterface> childList = this.getAllChildren();
        if (childList != null) {
            for (DocStructInterface ds : childList) {
                ((DocStruct) ds).setPhysical(physical);
            }
        }
    }

    /**
     * Creates a list of meta-data and persons to be displayed in a meta-data
     * form. The list is based on the {@code defaultDisplay} attribute in the
     * {@link Preferences} file. This list includes {@link Metadata} and
     * {@link Person} objects which already exist (and have content) and empty
     * objects (objects without any content), which are created by this method.
     * These empty objects are not only added to the list, but also to the
     * internal meta-data and person lists of the this instance.
     * <p>
     * After the form has been displayed an processed, you may want to call the
     * method {@link #deleteUnusedPersonsAndMetadata()} to delete unused objects
     * created by this method.
     *
     * @param lang
     *            language whose rules are to use for sorting the list
     * @param personsTop
     *            if true, persons will appear are at the beginning of the list,
     *            otherwise at the end
     * @return meta-data and persons
     */
    @SuppressWarnings("unchecked")
    public List<Metadata> showMetadataForm(String lang, boolean personsTop) throws MetadataTypeNotAllowedException {

        // Get all MetadataType elements which have the DefaultDisplay attribute
        // set.
        List<MetadataTypeInterface> dmt = this.getDefaultDisplayMetadataTypes();

        List<MetadataInterface> allMDs = this.getAllMetadata();
        // No default metadata.
        if (dmt == null) {
            return null;
        }

        // Iterator over DMT.
        Iterator<MetadataTypeInterface> mdtIterator = dmt.iterator();
        while (mdtIterator.hasNext()) {
            MetadataType mdt = (MetadataType) mdtIterator.next();

            // Check, if mdt is already in the allMDs Metadata list.
            boolean notIncluded = true;
            for (int i = 0; i < allMDs.size(); i++) {
                Metadata md = (Metadata) allMDs.get(i);
                MetadataType mdt2 = md.getType();

                // Compare the display MetadataType and the type of current
                // Metadata.
                if (mdt.getName().equals(mdt2.getName())) {
                    // Is included; need not to be displayed seperatly.
                    notIncluded = false;
                    break;
                }
            }

            // Create new Metadata or Person element.
            if (notIncluded) {
                if (mdt.isPerson) {
                    // It's a person, create person element.
                    Person psFoo = new Person(mdt);
                    // The role is the name of the metadata type.
                    psFoo.setRole(mdt.getName());
                    try {
                        // Add this new metadata element.
                        this.addPerson(psFoo);
                    } catch (DocStructHasNoTypeException e) {
                        continue;
                    } catch (MetadataTypeNotAllowedException e) {
                        continue;
                    }
                } else {
                    // It's metadata, so create a new Metadata element.
                    Metadata metaFoo = new Metadata(mdt);
                    try {
                        // Add this new metadata element.
                        this.addMetadata(metaFoo);
                    } catch (DocStructHasNoTypeException e) {
                        continue;
                    } catch (MetadataTypeNotAllowedException e) {
                        continue;
                    }
                }
            }
        }

        // Sort all Metadata by typename.
        LinkedList<Metadata> resultList = new LinkedList<Metadata>();

        for (MetadataInterface md : this.getAllMetadata()) {
            // If nothing is in the result list, just add it.
            if (resultList.size() == 0) {
                resultList.add((Metadata) md);
                // Continue with next iteration.
                continue;
            }

            String compare = md.getType().getNameByLanguage(lang);

            // Iterate over result list and find position for the metadata.
            boolean elementinserted = false;
            for (int i = 0; i < resultList.size(); i++) {
                Metadata mdcomp = resultList.get(i);
                String mdcompLang = mdcomp.getType().getNameByLanguage(lang);

                // Compare both strings.
                if (compare.compareTo(mdcompLang) < 0 || compare.compareTo(mdcompLang) == 0) {
                    // Add md before mdcomp.
                    resultList.add(i, (Metadata) md);
                    elementinserted = true;
                    // Get out of loop.
                    break;
                }
            }

            // If metadata element has not been inserted, we insert it to the
            // end.
            if (!elementinserted) {
                resultList.addLast((Metadata) md);
            }

        }

        // Currently we don't sort Persons; we simple add Persons on the top or
        // the end of the resultList.
        if (this.getAllPersons() != null && !this.getAllPersons().isEmpty()) {
            // Just add persons, if any person is available.
            if (personsTop) {
                // On top of list.
                resultList.addAll(0, (Collection<? extends Metadata>) this.getAllPersons());
            } else {
                // At end of list..
                resultList.addAll((Collection<? extends Metadata>) this.getAllPersons());
            }
        }

        // The Result list contains Persons and Metadata in one list.
        return resultList;
    }

    /**
     * This method cleans the meta-data and person list of instances which do
     * not have a value. This method is usually used in conjunction with the
     * method {@link #showMetadataForm(String, boolean)}. After
     * {@code showMetadataForm()} has been called and the form has been
     * displayed, this method should be called to delete the created empty
     * meta-data instances.
     * <p>
     * An empty metadata instance is:
     * <ul>
     * <li>A meta-data object with a value of null.</li>
     * <li>A person object with neither a lastname, nor a firstname, an
     * identifier, nor an institution.</li>
     * </ul>
     */
    @Override
    public void deleteUnusedPersonsAndMetadata() {

        // Handle Persons first: Person objects are available.
        if (this.getAllPersons() != null) {
            List<PersonInterface> personlist = this.getAllPersons();
            // Copy person list, so we can iterate over this list and delete
            // from the persons list.
            @SuppressWarnings("unchecked")
            List<Person> iteratorList = new LinkedList<Person>((Collection<? extends Person>) personlist);
            for (Person per : iteratorList) {
                if (per.getLastname() == null && per.getFirstname() == null && per.getInstitution() == null) {
                    // Delete this person from list of all Persons.
                    if (this.getAllPersons() != null) {
                        this.getAllPersons().remove(per);
                    }
                }
            }
        }

        // Handle Metadata: Metadata objects are available.
        if (this.getAllMetadata() != null) {
            List<MetadataInterface> metadatalist = this.getAllMetadata();
            // Copy Metadata list, so we can iterate over this list and delete
            // from the metadata list.
            @SuppressWarnings("unchecked")
            List<Metadata> iteratorList = new LinkedList<Metadata>((Collection<? extends Metadata>) metadatalist);
            for (Metadata md : iteratorList) {
                if (md.getValue() == null) {
                    if (this.getAllMetadata() != null) {
                        // Delete the metadata element.
                        this.getAllMetadata().remove(md);
                    }
                }
            }
        }

        if (this.getAllMetadataGroups() != null) {
            List<MetadataGroupInterface> metadatalist = this.getAllMetadataGroups();

            @SuppressWarnings("unchecked")
            List<MetadataGroup> iteratorList = new LinkedList<MetadataGroup>((Collection<? extends MetadataGroup>) metadatalist);
            for (MetadataGroup md : iteratorList) {
                boolean isEmpty = true;
                for (MetadataInterface meta : md.getMetadataList()) {
                    if (meta.getValue() != null) {
                        isEmpty = false;
                        break;
                    }
                }
                if (isEmpty) {
                    this.getAllMetadataGroups().remove(md);
                }
            }
        }
    }

    /**
     * Sorts the meta-data and persons in this instance according to their
     * occurrence in the {@code Preferences} file.
     *
     * @param thePrefs
     *            preferences file to use for sorting
     */
    @SuppressWarnings("unchecked")
    public synchronized void sortMetadata(Prefs thePrefs) {

        List<MetadataInterface> newMetadata = new LinkedList<MetadataInterface>();
        List<PersonInterface> newPersons = new LinkedList<PersonInterface>();
        List<Metadata> oldMetadata = new LinkedList<Metadata>();
        List<Person> oldPersons = new LinkedList<Person>();

        if (this.allMetadata != null) {
            oldMetadata = new LinkedList<Metadata>((Collection<? extends Metadata>) this.allMetadata);
        }
        if (this.persons != null) {
            oldPersons = new LinkedList<Person>((Collection<? extends Person>) this.persons);
        }

        // Get all MetadataTypes defined in the prefs for this DocStruct.
        DocStructType docStructType = thePrefs.getDocStrctTypeByName(this.getType().getName());

        // If the DocStructType is NOT existing, we have no metadata to sort,
        // just do return.
        if (docStructType == null) {
            return;
        }

        List<MetadataTypeInterface> prefsMetadataTypeList = docStructType.getAllMetadataTypes();

        // Iterate over all that metadata types.
        for (MetadataTypeInterface mType : prefsMetadataTypeList) {

            // Go through all persons of the current DocStruct.
            List<PersonInterface> op = this.getAllPersons();
            if (op != null && !op.isEmpty()) {
                for (PersonInterface p : op) {
                    if (p.getType() != null && mType.getName().equals(p.getType().getName())) {
                        // Add to the new list and remove from the old, if names
                        // do match.
                        newPersons.add(p);
                        oldPersons.remove(p);
                    }
                }
            }

            // Go through all metadata of the curretn DocStruct.
            List<MetadataInterface> om = this.getAllMetadata();
            if (om != null && !om.isEmpty()) {
                for (MetadataInterface m : om) {
                    if (mType.getName().equals(m.getType().getName())) {
                        // Add to the new list and remove from the old, if names
                        // do match.
                        newMetadata.add(m);
                        oldMetadata.remove(m);
                    }
                }
            }
        }

        // Add left-over types.
        if (oldPersons != null && oldPersons.size() > 0) {
            newPersons.addAll(oldPersons);
        }
        if (oldMetadata != null && oldMetadata.size() > 0) {
            newMetadata.addAll(oldMetadata);
        }

        // Re-set the lists.
        this.allMetadata = newMetadata;
        this.persons = newPersons;

        // TODO groups
    }

    /**
     * Sorts the meta-data and persons in this instance alphabetically.
     */
    @SuppressWarnings("unchecked")
    public synchronized void sortMetadataAbcdefg() {

        // Create empty (sorted) TreeSets and lists.
        TreeSet<Metadata> newMetadata = new TreeSet<Metadata>(new MetadataComparator());
        TreeSet<Person> newPersons = new TreeSet<Person>(new MetadataComparator());
        List<MetadataInterface> metadataList = new LinkedList<MetadataInterface>();
        List<PersonInterface> personList = new LinkedList<PersonInterface>();

        // Add all metadata to the new TreeSets (sorted).
        if (this.allMetadata != null) {
            newMetadata.addAll((Collection<? extends Metadata>) this.allMetadata);
        }
        if (this.persons != null) {
            newPersons.addAll((Collection<? extends Person>) this.persons);
        }

        // Re-transfer the sorted sets to the linked lists.
        metadataList.addAll(newMetadata);
        personList.addAll(newPersons);

        // Re-set the lists.
        this.allMetadata = metadataList;
        this.persons = personList;
        // TODO groups
    }

    /**
     * Used to register a signature the first time a DocStruct Object runs into
     * the non hierarchial branch of referenced DocStruct Objects by way of the
     * equals method.
     */
    private boolean registerToRef(DocStruct docStruct) {

        if (this.signaturesForEqualsMethodRefsTo == null) {
            this.signaturesForEqualsMethodRefsTo = new HashMap<String, Object>();
        }

        // If not null then we have the case of looping, then we must return
        // false here.
        if (this.signaturesForEqualsMethodRefsTo.get(docStruct.toString()) != null) {
            return false;
        }

        this.signaturesForEqualsMethodRefsTo.put(docStruct.toString(), docStruct);
        return true;
    }

    /**
     * Used to register a signature the first time a DocStruct Object runs into
     * the non hierarchial branch of DocStruct Objects referencing this
     * DocStruct by way of the equals method.
     */
    private boolean registerFromRef(DocStruct docStruct) {

        if (this.signaturesForEqualsMethodRefsFrom == null) {
            this.signaturesForEqualsMethodRefsFrom = new HashMap<String, Object>();
        }

        // If not null then we have the case of looping, then we must return
        // false here.
        if (this.signaturesForEqualsMethodRefsFrom.get(docStruct.toString()) != null) {
            return false;
        }

        this.signaturesForEqualsMethodRefsFrom.put(docStruct.toString(), docStruct);
        return true;
    }

    private void unregisterToRefs(DocStruct docStruct) {

        this.signaturesForEqualsMethodRefsTo.remove(docStruct.toString());
        // Set to null if no element is left.
        if (this.signaturesForEqualsMethodRefsTo.size() == 0) {
            this.signaturesForEqualsMethodRefsTo = null;
        }
    }

    private void unregisterFromRefs(DocStruct docStruct) {

        this.signaturesForEqualsMethodRefsFrom.remove(docStruct.toString());
        // Sset to null if no element is left.
        if (this.signaturesForEqualsMethodRefsFrom.size() == 0) {
            this.signaturesForEqualsMethodRefsFrom = null;
        }
    }

    public boolean equals(DocStruct docStruct) {

        logger.debug("\r\n" + this.getClass() + " ->id:" + this.getType().getName() + " other:" + docStruct.getType().getName() + "\r\n");

        // Simple attributes.
        if (this.isLogical() != docStruct.isLogical()) {
            logger.debug("isLogical=false");
            return false;
        }

        if (this.isPhysical() != docStruct.isPhysical()) {
            logger.debug("isPhysical=false");
            return false;
        }

        if (!((this.getReferenceToAnchor() == null && docStruct.getReferenceToAnchor() == null) || this.getReferenceToAnchor().equals(
                docStruct.getReferenceToAnchor()))) {
            logger.debug("getreferenceAnchor=false");
            return false;
        }

        // Compare types.
        if (!this.getType().equals(docStruct.getType())) {
            logger.debug("getType=false");
            return false;
        }

        // ListPairCheck.isNotEqual is returned, if one List Object is null
        // while the other List Object refers to an instance. In this case
        // equals can already return false.
        // If needsFurtherChecking is returned we need to compare the instances,
        // or rather the instances of the listed Objects.
        // For a quick test in this case we first compare the number referenced
        // objects contained in the lists: If the number of referenced objects
        // already differs, equals again can return false already.
        // Only if also the number of Objects in the lists is the same we need
        // an exhausting in depth comparism of the Objects contained.
        // Simply using the List.equals method doesn't help us, because the
        // lists may only have two separate instances of equal objects but
        // never the same instances.
        ListPairCheck lpcResult = null;

        // Metadata.
        lpcResult = DigitalDocument.quickPairCheck(this.getAllMetadata(), docStruct.getAllMetadata());
        if (lpcResult == ListPairCheck.isNotEqual) {
            logger.debug("1 false returned");
            return false;
        }
        if (lpcResult == ListPairCheck.needsFurtherChecking && this.getAllMetadata().size() != docStruct.getAllMetadata().size()) {
            logger.debug("2 false returned");
            return false;
        }

        // DocStructs (children).
        lpcResult = DigitalDocument.quickPairCheck(this.getAllChildren(), docStruct.getAllChildren());
        if (lpcResult == ListPairCheck.isNotEqual) {
            logger.debug("3 false returned");
            return false;
        }
        if (lpcResult == ListPairCheck.needsFurtherChecking && this.getAllChildren().size() != docStruct.getAllChildren().size()) {
            logger.debug("4 false returned");
            return false;
        }

        // FileReferences.
        lpcResult = DigitalDocument.quickPairCheck(this.getAllContentFileReferences(), docStruct.getAllContentFileReferences());
        if (lpcResult == ListPairCheck.isNotEqual) {
            logger.debug("5 false returned");
            return false;
        }

        if (lpcResult == ListPairCheck.needsFurtherChecking
                && this.getAllContentFileReferences().size() != docStruct.getAllContentFileReferences().size()) {
            logger.debug("6 false returned");
            return false;
        }

        // Persons.
        lpcResult = DigitalDocument.quickPairCheck(this.getAllPersons(), docStruct.getAllPersons());
        if (lpcResult == ListPairCheck.isNotEqual) {
            logger.debug("7 false returned");
            return false;
        }
        if (lpcResult == ListPairCheck.needsFurtherChecking && this.getAllPersons().size() != docStruct.getAllPersons().size()) {
            logger.debug("8 false returned");
            return false;
        }

        // To references.
        lpcResult = DigitalDocument.quickPairCheck(this.getAllToReferences(), docStruct.getAllToReferences());
        if (lpcResult == ListPairCheck.isNotEqual) {
            logger.debug("9 false returned");
            return false;
        }
        if (lpcResult == ListPairCheck.needsFurtherChecking && this.getAllToReferences().size() != docStruct.getAllToReferences().size()) {
            logger.debug("10 false returned");
            return false;
        }

        // From references.
        lpcResult = DigitalDocument.quickPairCheck(this.getAllFromReferences(), docStruct.getAllFromReferences());
        if (lpcResult == ListPairCheck.isNotEqual) {
            logger.debug("11 false returned");
            return false;
        }
        if (lpcResult == ListPairCheck.needsFurtherChecking && this.getAllFromReferences().size() != docStruct.getAllFromReferences().size()) {
            logger.debug("12 false returned");
            return false;
        }

        // If we got this far we need to take a deeper look into the referenced
        // Objects trying to find a match, only if no match is found we can
        // exclude that the compared Objects are equal.
        boolean flagFound = false;

        // If both lists are null, isEqual is returned, no in depth check
        // needed.
        if (DigitalDocument.quickPairCheck(this.getAllChildren(), docStruct.getAllChildren()) != DigitalDocument.ListPairCheck.isEqual) {

            for (DocStructInterface ds1 : this.getAllChildren()) {
                int i = this.getAllChildren().indexOf(ds1);
                if (!ds1.equals(docStruct.getAllChildren().get(i))) {
                    return false;
                }
            }
        }

        // If both lists are null, isEqual is returned, no in depth check
        // needed.
        if (DigitalDocument.quickPairCheck(this.getAllMetadata(), docStruct.getAllMetadata()) != DigitalDocument.ListPairCheck.isEqual) {
            for (MetadataInterface md1 : this.getAllMetadata()) {
                flagFound = false;

                for (MetadataInterface md2 : docStruct.getAllMetadata()) {
                    if (md1.equals(md2)) {
                        logger.debug("equals=true: MD1=" + md1.getType().getName() + ";MD2=" + md2.getType().getName());
                        flagFound = true;
                        break;
                    }

                    logger.debug("equals=false: MD1=" + md1.getType().getName() + ", MD2=" + md2.getType().getName());
                }

                // If equal Metadata couldn't be found this DocStruct cannot be
                // equal either.
                if (!flagFound) {
                    return false;
                }
            }
        }

        // If both lists are null, isEqual is returned, no in depth check
        // needed.
        if (DigitalDocument.quickPairCheck(this.getAllMetadataGroups(), docStruct.getAllMetadataGroups()) != DigitalDocument.ListPairCheck.isEqual) {

            for (MetadataGroupInterface md1 : this.getAllMetadataGroups()) {
                flagFound = false;

                for (MetadataGroupInterface md2 : docStruct.getAllMetadataGroups()) {
                    if (md1.equals(md2)) {
                        logger.debug("equals=true: MD1=" + md1.getType().getName() + ";MD2=" + md2.getType().getName());
                        flagFound = true;
                        break;
                    }

                    logger.debug("equals=false: MD1=" + md1.getType().getName() + ", MD2=" + md2.getType().getName());
                }

                // If equal Metadata couldn't be found this DocStruct cannot be
                // equal either.
                if (!flagFound) {
                    return false;
                }
            }
        }

        // If both lists are null, isEqual is returned, no in depth check
        // needed.
        if (DigitalDocument.quickPairCheck(this.getAllPersons(), docStruct.getAllPersons()) != DigitalDocument.ListPairCheck.isEqual) {

            for (PersonInterface p1 : this.getAllPersons()) {
                flagFound = false;
                for (PersonInterface p2 : docStruct.getAllPersons()) {
                    if (p1.equals(p2)) {
                        flagFound = true;
                        break;
                    }
                }
                // If equal Person couldn't be found this DocStruct cannot be
                // equal either.
                if (!flagFound) {
                    logger.debug("15 false returned");
                    return false;
                }
            }
        }

        // If both lists are null, isEqual is returned, no in depth check
        // needed.
        if (DigitalDocument.quickPairCheck(this.getAllContentFileReferences(), docStruct.getAllContentFileReferences()) != DigitalDocument.ListPairCheck.isEqual) {

            // flagFound = true;
            for (ContentFileReference cfr1 : this.getAllContentFileReferences()) {
                int i = this.getAllContentFileReferences().indexOf(cfr1);
                if (!cfr1.equals(docStruct.getAllContentFileReferences().get(i))) {
                    return false;
                }

                /*
                 * for (ContentFileReference cfr2 : docStruct .getAllContentFileReferences()) { if (cfr1.equals(cfr2)) { flagFound = true; break; } }
                 * if (!flagFound) { logger.debug("16 false returned"); return false; }
                 */
            }
        }

        // If both lists are null, isEqual is returned, no in depth check
        // needed.
        if (DigitalDocument.quickPairCheck(this.getAllFromReferences(), docStruct.getAllFromReferences()) != DigitalDocument.ListPairCheck.isEqual) {// now
            // The tricky part: before we go in to the equal method of the next
            // DocStruct we have to register the signature and respectively
            // check if signature is already listed if signature is already
            // listed then we entered a loop and equals has to return true.

            // This interrupts the loop acknowledging that docStruct had been
            // here before - unregister is done one loop down the stack.
            if (!registerFromRef(docStruct)) {
                return true;
            }

            for (ReferenceInterface rf1 : this.getAllFromReferences()) {
                flagFound = false;

                for (ReferenceInterface rf2 : docStruct.getAllFromReferences()) {
                    if (rf1.getTarget().equals(rf2.getTarget())) {
                        flagFound = true;
                        break;
                    }
                }

                if (!flagFound) {
                    unregisterFromRefs(docStruct);
                    logger.debug("17 false returned");
                    return false;
                }
            }

            unregisterFromRefs(docStruct);
        }

        // If both lists are null, isEqual is returned, no in depth check
        // needed.
        if (DigitalDocument.quickPairCheck(this.getAllToReferences(), docStruct.getAllToReferences()) != DigitalDocument.ListPairCheck.isEqual) {

            // Interrupt the loop.
            if (!registerToRef(docStruct)) {
                return true;
            }

            for (ReferenceInterface rt1 : this.getAllToReferences()) {
                flagFound = false;

                for (ReferenceInterface rt2 : docStruct.getAllToReferences()) {
                    if (rt1.getTarget().equals(rt2.getTarget())) {
                        flagFound = true;
                        break;
                    }
                }

                if (!flagFound) {
                    unregisterToRefs(docStruct);
                    logger.debug("18 false returned");
                    return false;
                }
            }

            unregisterToRefs(docStruct);
        }

        // Finally we are through and can assume that this DocStruct is the same
        // as parameter docStruct and we return true.
        return true;
    }

    /**
     * Compares meta-data (and persons) according to their type names
     * alphabetically.
     */
    class MetadataComparator implements Comparator<Object> {

        @Override
        public int compare(Object o1, Object o2) {

            Metadata m1 = (Metadata) o1;
            Metadata m2 = (Metadata) o2;

            if (m1.getType().getName().equals(m2.getType().getName())) {
                return 0;
            }

            return m1.getType().getName().compareTo(m2.getType().getName());
        }

    }

    /**
     * Compares meta-data groups according to their type names alphabetically.
     */
    class MetadataGroupComparator implements Comparator<Object> {

        @Override
        public int compare(Object o1, Object o2) {

            MetadataGroup m1 = (MetadataGroup) o1;
            MetadataGroup m2 = (MetadataGroup) o2;

            return m1.getType().getName().compareTo(m2.getType().getName());
        }

    }

    public AmdSec getAmdSec() {
        return amdSec;
    }

    public void setAmdSec(AmdSec amdSec) {
        this.amdSec = amdSec;
    }

    public List<Md> getTechMds() {
        return techMdList;
    }

    public void addTechMd(Md techMd) {
        if (techMdList == null) {
            techMdList = new ArrayList<Md>();
        }
        if (techMd != null) {
            techMdList.add(techMd);
        }
    }

    public void setTechMds(List<Md> mds) {
        if (mds != null) {
            this.techMdList = mds;
        }
    }

    @Override
    public String getImageName() {
        if (contentFileReferences != null && !contentFileReferences.isEmpty()) {
            for (ContentFileReference cfr : contentFileReferences) {
                if (cfr.getCf() != null) {
                    String location = cfr.getCf().getLocation();
                    if (location != null && location.length() > 0) {
                        File imagefile = new File(location);
                        return imagefile.getName();
                    }
                }
            }
        }
        return null;
    }

    @Override
    public void setImageName(String newfilename) {
        if (contentFileReferences != null && !contentFileReferences.isEmpty()) {
            for (ContentFileReference cfr : contentFileReferences) {
                if (cfr.getCf() != null) {
                    cfr.getCf().setLocation(newfilename);
                    return;
                } else {
                    ContentFile cf = new ContentFile();
                    cf.setLocation(newfilename);
                    cfr.setCf(cf);
                    return;
                }
            }
        } else {
            ContentFile cf = new ContentFile();
            cf.setLocation(newfilename);
            this.addContentFile(cf);
        }
    }

    /**
     * Returns the index of the first occurrence of the specified element in
     * this DocStruct, or throws an exception. More formally, returns the lowest
     * index of the DocStruct in this DocStruct. If there is no such index, a
     * NoSuchElementException will be thrown.
     *
     * @param d
     *            DocStruct to search for
     * @return the index of the first occurrence of the specified DocStruct in
     *         this DocStruct, separated by comma
     * @throws NoSuchElementException
     *             if this DocStruct does not contain the DocStruct
     */
    public String indexOf(DocStruct d) throws NoSuchElementException {
        return indexOf(d, null);
    }

    /**
     * Returns the index of the first occurrence of the specified element in
     * this DocStruct after the specified index or throws an exception. More
     * formally, returns the lowest index of the DocStruct in this DocStruct
     * after the index. If there is no such index, a NoSuchElementException will
     * be thrown. The search will be fast-forwarded to the specified element so
     * that the search will continue right after the specified element. If there
     * is no such index, a NoSuchElementException will be thrown.
     *
     * @param d
     *            DocStruct to search for
     * @param afterIndex
     *            index after which to start searching
     * @return the index of the first occurrence of the specified DocStruct in
     *         this DocStruct, separated by comma
     * @throws NoSuchElementException
     *             if this DocStruct does not contain the DocStruct
     */
    public String indexOf(DocStruct d, String afterIndex) throws NoSuchElementException {
        int from = 0;
        String subIndex = null;
        if (afterIndex != null) {
            int comma = afterIndex.indexOf(',');
            if (comma >= 0) {
                from = Integer.parseInt(afterIndex.substring(0, comma));
                subIndex = afterIndex.substring(comma + 1);
            } else {
                from = Integer.parseInt(afterIndex) + 1;
            }
        }

        if (children != null) {
            for (int i = from; i < children.size(); i++) {
                DocStruct child = (DocStruct) children.get(i);
                if (subIndex == null && child.equals(d)) {
                    return Integer.toString(i);
                }
                try {
                    return Integer.toString(i) + ',' + child.indexOf(d, subIndex);
                } catch (NoSuchElementException go_on) {
                }
                subIndex = null;
            }
        }
        throw new NoSuchElementException("No " + d + " in " + this);
    }

    /**
     * Retrieves the name of the anchor structure, if any, or null otherwise.
     * Anchors are a special type of document structure, which group other
     * structure entities together, but have no own content. Imagine a
     * periodical as such an anchor. The periodical itself is a virtual
     * structure entity without any own content, but groups all years of
     * appearance together. Years may be anchors again for volumes, etc.
     *
     * @return String, which is null, if it cannot be used as an anchor
     */
    @Override
    public String getAnchorClass() {
        if (type == null) {
            return null;
        }
        return type.getAnchorClass();
    }

    /**
     * The function getAllAnchorClasses() traverses the structure tree and
     * returns an ordered list of all anchor classes that are used by this
     * structure.
     *
     * @return an ordered collection of all used anchors
     * @throws PreferencesException
     *             if an anchor class name is encountered a second time after
     *             having been descending right into a hierarchy to be
     *             maintained in another anchor class already
     */
    public Collection<String> getAllAnchorClasses() throws PreferencesException {
        LinkedHashSet<String> result = new LinkedHashSet<String>();
        String anchorClass = getAnchorClass();
        if (anchorClass != null) {
            result.add(anchorClass);
            List<DocStruct> docStructs = getAllRealSuccessors();
            do {
                anchorClass = null;
                List<DocStruct> nextLevel = new LinkedList<DocStruct>();
                for (DocStruct docStruct : docStructs) {
                    String ancora = docStruct.getAnchorClass();
                    if (ancora != null) {
                        if (anchorClass == null) {
                            anchorClass = ancora;
                        } else if (!anchorClass.equals(ancora)) {
                            throw new PreferencesException(
                                    "All real successors of an anchor class that are of an anchor class themselves "
                                            + "must belong to the same anchor class. The given logical document "
                                            + "structure in combination with the anchor names configured would result "
                                            + "in the hierarchical level " + docStruct.getParent().getType().getName()
                                            + "\u200A\u2014\u200Abelonging to the anchor class "
                                            + docStruct.getParent().getType().getAnchorClass() + "\u200A\u2014\u200Ato"
                                            + " have children which belong to the different anchor classes "
                                            + anchorClass + " and " + ancora + ", which is not supported.");
                        }
                        nextLevel.addAll(docStruct.getAllRealSuccessors());
                    }
                }
                if(anchorClass != null && !result.add(anchorClass)) {
                    String last = "";
                    for (String entry : result) {
                        last = entry;
                    }
                    throw new PreferencesException(
                            "All levels of the logical document structure that belong to the same anchor file must "
                                    + "immediately  follow each other as children. The given logical document "
                                    + "structure in combination with the anchor names configured would result in an "
                                    + "interruption of the elements being stored in the " + anchorClass + " anchor by "
                                    + "elements to be stored in the " + last + " anchor,  which isn’t possible.");
                }
                docStructs = nextLevel;
            } while (docStructs.size() > 0);
        }
        return result;
    }

    /**
     * The function getChild() returns a child element from this structural
     * entity by numeric reference.
     *
     * @param reference
     *            reference to the child entity to get
     * @return child entity, if found
     * @throws IndexOutOfBoundsException
     *             if the child indicated cannot be reached
     */
    public DocStruct getChild(String reference) {
        if(children == null) {
            throw new IndexOutOfBoundsException(reference);
        }
        int fieldSeparator;
        if ((fieldSeparator = reference.indexOf(',')) > -1) {
            int index = Integer.parseInt(reference.substring(0, fieldSeparator));
            return ((DocStruct) children.get(index)).getChild(reference.substring(fieldSeparator + 1));
        } else {
            return (DocStruct) children.get(Integer.parseInt(reference));
        }
    }

    /**
     * The function addMetadata() adds a meta data field with the given name to
     * this DocStruct and sets it to the given value.
     *
     * @param fieldName
     *            name of the meta data field to add
     * @param value
     *            value to set the field to
     * @return the object, to be able to write several statements in-line
     * @throws MetadataTypeNotAllowedException
     *             if no corresponding MetadataType object is returned by
     *             getAddableMetadataTypes()
     */
    @Override
    public DocStruct addMetadata(String fieldName, String value) throws MetadataTypeNotAllowedException {
        boolean success = false;
        for (MetadataTypeInterface fieldType : type.getAllMetadataTypes()) {
            if (fieldType.getName().equals(fieldName)) {
                Metadata field = new Metadata((MetadataType) fieldType);
                field.setValue(value);
                addMetadata(field);
                success = true;
                break;
            }
        }
        if (!success) {
            throw new MetadataTypeNotAllowedException("Couldn’t add " + fieldName + " to " + type.getName()
                    + ": No corresponding MetadataType object in result of DocStruc.getAllMetadataTypes().");
        }
        return this;
    }

    /**
     * The function createChild() creates a child DocStruct below a DocStruct.
     * This is a convenience function to add a DocStruct by its type name
     * string.
     *
     * @param type
     *            structural type of the child to create
     * @param caudexDigitalis
     *            act to create the child in
     * @param ruleset
     *            rule set the act is based on
     * @return the child created
     * @throws TypeNotAllowedForParentException
     *             is thrown, if this DocStruct is not allowed for a parent
     * @throws TypeNotAllowedAsChildException
     *             if a child should be added, but it's DocStruct type isn't
     *             member of this instance's DocStruct type
     */
    @Override
    public DocStructInterface createChild(String type, DigitalDocumentInterface caudexDigitalis, PrefsInterface ruleset)
            throws TypeNotAllowedForParentException, TypeNotAllowedAsChildException {
        DocStructInterface result = caudexDigitalis.createDocStruct(ruleset.getDocStrctTypeByName(type));
        addChild(result);
        return result;
    }

    /**
     * The function getChild() returns a child of a DocStruct, identified by its
     * type and an identifier in a meta data field of choice. More formally,
     * returns the first child matching the given conditions and does not work
     * recursively. If no matching child is found, throws
     * NoSuchElementException.
     *
     * @param type
     *            structural type of the child to locate
     * @param identifierField
     *            meta data field that holds the identifer to locate the child
     * @param identifier
     *            identifier of the child to locate
     * @return the child, if found
     * @throws NoSuchElementException
     *             if no matching child is found
     */
    @Override
    public DocStruct getChild(String type, String identifierField, String identifier) throws NoSuchElementException {
        List<DocStructInterface> children = getAllChildrenByTypeAndMetadataType(type, identifierField);
        if (children == null) {
            children = Collections.emptyList();
        }
        for (DocStructInterface child : children) {
            for (MetadataInterface metadataElement : child.getAllMetadata()) {
                if (metadataElement.getType().getName().equals(identifierField)
                        && metadataElement.getValue().equals(identifier)) {
                    return (DocStruct) child;
                }
            }
        }
        throw new NoSuchElementException("No child " + type + " with " + identifierField + " = " + identifier + " in "
                + this + '.');
    }

    /**
     * The function getMetadataByType() returns a list of all meta data elements
     * that are associated with this element and of a given type.
     *
     * @param typeName
     *            name of the type of meta data to look for
     * @return a list of all meta data elements of that type
     */
    public List<Metadata> getMetadataByType(String typeName) {
        LinkedList<Metadata> result = new LinkedList<Metadata>();
        if (allMetadata != null) {
            for (MetadataInterface metadata : allMetadata) {
                if (metadata.getType().getName().equals(typeName)) {
                    result.add((Metadata) metadata);
                }
            }
        }
        return result;
    }

    /**
     * The function toString() returns a concise but informative representation
     * of this DocStruct that is easy for a person to read.
     *
     * The toString method for class DocStruct returns a string consisting of
     * the type name of which the DocStruct is an instance, an (optionally
     * truncated) identifier, if one is found, and the children of the
     * DocStruct, if any.
     *
     * @return a string representation of the DocStruct
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        final int EM_DASH = 0x2014;
        final int HORIZONTAL_ELLIPSIS = 0x2026;
        final short MAX_CHARS = 12;

        StringBuilder result = new StringBuilder();
        if (type == null || type.getName() == null) {
            result.append(identify(this));
        } else {
            result.append(type.getName());
        }
        if(type != null) {
            result.append(' ');
        }
        result.append('(');
        if (allMetadata == null) {
            result.appendCodePoint(EM_DASH);
        } else {
            String out = null;
            Iterator<String> iter = IDENTIFIER_METADATA_FIELDS_FOR_TOSTRING.iterator();
            while (out == null && iter.hasNext()) {
                Iterator<Metadata> mdIter = getMetadataByType(iter.next()).iterator();
                while (mdIter.hasNext() && out == null) {
                    out = mdIter.next().getValue();
                }
            }
            if (out != null && out.length() > MAX_CHARS) {
                result.append(out.substring(0, MAX_CHARS - 1));
                result.appendCodePoint(HORIZONTAL_ELLIPSIS);
            } else if (out != null) {
                result.append(out);
            } else {
                result.append("\u2026 ");
                result.append(allMetadata.size());
                result.append(" \u2026");
            }
        }
        result.append(')');
        if(children == null) {
            result.append("[\u2014]");
        } else {
            result.append(children.toString());
        }
        return result.toString();
    }

    /**
     * Returns whether a downwards METS pointer must be written. This is the
     * case if the parent docStruct is of the the anchor class of the file thas
     * is currently written, but this docStruct isn’t.
     *
     * @param fileClass
     *            anchor class of the file to write
     * @return whether a downwards METS pointer must be written
     */
    public boolean mustWriteDownwardsMptrIn(String fileClass) {
        if (fileClass == null || parent == null) {
            return false;
        }
        return fileClass.equals(parent.getType().getAnchorClass())
                && !fileClass.equals(type.getAnchorClass());
    }

    /**
     * Returns whether an upwards METS pointer must be written. This is the case
     * if the metadata of this docStruct is not kept in the file currently under
     * construction, and either this docStruct has no parent and the anchor
     * class of the file to create is different from the anchor class of this
     * docStruct, or if the parent of this docStruct belongs to a different
     * anchor class and the anchor class of the file to create appears after the
     * anchor class of the parent of this docStruct in the list of anchor
     * classes for the logical document structure.
     *
     * @param fileClass
     *            anchor class of the file to write
     * @return whether an upwards METS pointer must be written
     * @throws PreferencesException
     *             if an anchor class name is encountered a second time after
     *             having been descending right into a hierarchy to be
     *             maintained in another anchor class already
     */
    public boolean mustWriteUpwardsMptrIn(String fileClass) throws PreferencesException {
        String anchorClass = type.getAnchorClass();
        if (fileClass == null && anchorClass == null || fileClass != null && fileClass.equals(anchorClass)) {
            return false;
        }
        if (this.parent == null) {
            return anchorClass == null ? false : !anchorClass.equals(fileClass);
        }
        String parentClass = parent.getType().getAnchorClass();
        if (parentClass == null || parentClass.equals(anchorClass)) {
            return false;
        }
        Collection<String> anchorChain = getTopStruct().getAllAnchorClasses();
        anchorChain.add(null);
        Iterator<String> capstan = anchorChain.iterator();
        String link;
        do {
            link = capstan.next();
            if (link.equals(fileClass)) {
                return false;
            }
        } while (!link.equals(parentClass));
        return true;
    }

    /**
     * Returns the topmost DocStruct
     *
     * @return the topmost DocStruct
     */
    public DocStruct getTopStruct() {
        return parent == null ? this : parent.getTopStruct();
    }

    /**
     * Returns a readable name for a DocStruct.
     *
     * @param obj
     *            DocStruct whose name is to return
     * @return a readable name for the DocStruct
     */
    private static String identify(DocStruct obj) {
        DocStructType objectType = obj.getType();
        if (objectType != null && objectType.getName() != null) {
            return "'" + objectType.getName() + "'";
        }
        DocStruct parent = obj.getParent();
        if (parent == null) {
            return "top level";
        }
        List<DocStructInterface> parentsChildren = parent.getAllChildren();
        if (parentsChildren == null || parentsChildren.isEmpty()) {
            return "orphan";
        }
        int position = parentsChildren.indexOf(obj);
        if (position < 0) {
            return "orphan";
        }
        String childOfParent = " child of " + identify(parent);
        if (position == 0) {
            return "first" + childOfParent;
        }
        int childNo = position + 1;
        if (childNo == parentsChildren.size()) {
            return "last" + childOfParent;
        }
        String childIndex = Integer.toString(childNo);
        switch (Integer.valueOf(childIndex.substring(childIndex.length() - 1))) {
        case 1:
            return childIndex + "st" + childOfParent;
        case 2:
            return childIndex + "nd" + childOfParent;
        case 3:
            return childIndex + "rd" + childOfParent;
        default:
            return childIndex + "th" + childOfParent;
        }
    }
}
