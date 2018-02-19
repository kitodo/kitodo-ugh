/*******************************************************************************
 * ugh.dl / DigitalDocument.java
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

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import com.thoughtworks.xstream.mapper.MapperWrapper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kitodo.api.ugh.ContentFileInterface;
import org.kitodo.api.ugh.DigitalDocumentInterface;
import org.kitodo.api.ugh.DocStructInterface;
import org.kitodo.api.ugh.DocStructTypeInterface;
import org.kitodo.api.ugh.MetadataInterface;
import org.kitodo.api.ugh.PersonInterface;
import org.kitodo.api.ugh.exceptions.ContentFileNotLinkedException;
import org.kitodo.api.ugh.exceptions.PreferencesException;
import org.kitodo.api.ugh.exceptions.WriteException;
import org.w3c.dom.Node;

/*******************************************************************************
 * <p>
 * A DigitalDocument represents a digital version of a work. This representation contains the following information:
 * </p>
 *
 * <ul>
 * <li>metadata</li>
 * <li>structure of a work</li>
 * <li>content</li>
 * </ul>
 *
 * <p>
 * Those three different objects can be linked to each other in ways forming a very complex object. The underlying document model tries to reduce the
 * complexity by defining some rules:
 * </p>
 *
 * <ul>
 * <li>every <code>DigitalDocument</code> has two kind of structures:
 *
 * <ul>
 * <li>logical structure: this structure represents the logical view. The logical view is normally represented by chapters, paragraphs etc.</li>
 *
 * <li>physical structure: The physical structure represents the physical representation of a work. For a book the physical binding and the pages can
 * be regarded a part of the physical structure.</li>
 *
 * Each structure has a single top structure element. These structure elements are represented by <code>DocStruct</code> objects and may have
 * children.
 *
 * </ul>
 * <li>metadata to this digital document is stored in structure entities</li>
 * <li>the content is represented by content files</li>
 * <li>ContentFiles can be linked to structure entities</li>
 * </ul>
 *
 * @author Markus Enders
 * @author Stefan E. Funk
 * @author Robert Sehr
 * @author Matthias Ronge &lt;matthias.ronge@zeutschel.de&gt;
 * @version 2014-06-18
 * @see DocStruct, Metadata, Prefs
 *
 *      TODOLOG
 *
 *      TODO Refactor all methods that do return always TRUE!!
 *
 *      TODO Refactor ALL these silly content file things!!
 *
 *      TODO Use private finals here for Metadata and DocStruct names!!
 *
 *      TODO Remove all XStream things from here and put it into the XStream class!!
 *
 *      TODO Maybe provide a possibility to change content file paths in addContentFileFromPhysicalPage()!!
 *
 ******************************************************************************/

public class DigitalDocument implements DigitalDocumentInterface, Serializable {

    private static final long serialVersionUID = 3806816628185949759L;

    private static final String VERSION = "2.0-20100223";

    private static final Logger logger = LogManager.getLogger(DigitalDocument.class);
    private static final String LINE = "--------------------" + "--------------------" + "--------------------" + "--------------------";

    private DocStruct topPhysicalStruct;
    private DocStruct topLogicalStruct;
    // Contains all files, which are referenced from this digital document (e.g.
    // imagefiles, textfiles etc...).
    private FileSet allImages;
    // This is the unique identifier for the whole document; usually Metadata
    // object from the logical DocStruct.
    private Metadata uniqueIdentifer;

    // This contains the list of techMds. Currently only one amdSec is allowed, to comply with DFG-Viewer
    private AmdSec amdSec;

    // private List<Node> techMd = new ArrayList<Node>();

    /***************************************************************************
     * <p>
     * Constructor.
     * </p>
     **************************************************************************/
    public DigitalDocument() {
        super();
    }

    //
    // Factory classes.
    //

    /***************************************************************************
     * <p>
     * Create a DocStruct instance for the Digital Document.
     * </p>
     *
     * @param dsType Is a DocStructType object.
     **************************************************************************/
    @Override
    public DocStruct createDocStruct(DocStructTypeInterface dsType) {

        DocStruct ds = new DocStruct((DocStructType) dsType);
        ds.setDigitalDocument(this);

        return ds;
    }

    //
    // Setter and Getter.
    //

    /***************************************************************************
     * @param inStruct
     **************************************************************************/
    @Override
    public void setLogicalDocStruct(DocStructInterface inStruct) {

        if (this.topLogicalStruct != null) {
            this.topLogicalStruct.setLogical(false);
        }

        this.topLogicalStruct = (DocStruct) inStruct;
        // Set DocStruct and all children to logical.
        ((DocStruct) inStruct).setLogical(true);
    }

    /***************************************************************************
     * @return
     **************************************************************************/
    @Override
    public DocStruct getLogicalDocStruct() {
        return this.topLogicalStruct;
    }

    /***************************************************************************
     * @param inStruct
     **************************************************************************/
    @Override
    public void setPhysicalDocStruct(DocStructInterface inStruct) {

        if (this.topPhysicalStruct != null) {
            this.topPhysicalStruct.setPhysical(false);
        }

        this.topPhysicalStruct = (DocStruct) inStruct;
        // Set DocStruct and all children to physical.
        ((DocStruct) inStruct).setPhysical(true);
    }

    /***************************************************************************
     * TODO Why is this method is returning always TRUE???
     *
     * @return
     **************************************************************************/
    @Override
    public DocStruct getPhysicalDocStruct() {
        return this.topPhysicalStruct;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {

        String result = "";

        // First get the fileset's information.
        if (this.getFileSet() != null) {
            result = this.getFileSet().toString();
        } else {
            result += LINE + "\n" + "FileSet" + "\n";
            result += LINE + "\n" + "NONE" + "\n";
        }

        // Then assemble the logical DocStruct.
        result += printCompleteDocStruct(this.topLogicalStruct);

        // Finally assemble the physical DocStruct.
        result += printCompleteDocStruct(this.topPhysicalStruct);

        return result.trim();
    }

    /***************************************************************************
     * <p>
     * Prints all the given DocStruct's data.
     * </p>
     *
     * @return
     **************************************************************************/
    private String printCompleteDocStruct(DocStruct theDocStruct) {

        String result = "";

        if (theDocStruct != null) {
            result += printChildDocStruct(theDocStruct, 0);
        }

        return result;
    }

    /***************************************************************************
     * <p>
     * Prints a DocStruct including persons and metadata.
     * </p>
     *
     * @param inDocStruct
     * @param hierarchy
     * @return
     **************************************************************************/
    private String printChildDocStruct(DocStruct inDocStruct, int hierarchy) {

        String result = "";

        StringBuffer hierarchyBuffer = new StringBuffer();
        for (int i = 0; i < hierarchy; i++) {
            hierarchyBuffer.append("\t");
        }

        // Type of this document structure (inDocStruct).
        DocStructType myType;

        if (inDocStruct == null) {
            return "";
        }

        // Get and print DocStruct type.
        myType = inDocStruct.getType();
        if (myType != null) {
            result += hierarchyBuffer + LINE + "\n";
            result += hierarchyBuffer + "DocStruct '" + myType.getName() + "'" + "\n";
            result += hierarchyBuffer + LINE + "\n";
        }

        // Get and print metadata.
        List<MetadataInterface> allMD = inDocStruct.getAllMetadata();
        if (allMD != null) {
            for (MetadataInterface currentMD : allMD) {
                result += hierarchyBuffer + currentMD.toString();
            }
        }

        // Get and print persons.
        List<PersonInterface> allPS = inDocStruct.getAllPersons();
        if (allPS != null) {
            for (PersonInterface currentPS : allPS) {
                result += hierarchyBuffer + currentPS.toString();
            }
        }

        // Get and print contentFiles.
        List<ContentFileInterface> allCF = inDocStruct.getAllContentFiles();
        if (allCF != null) {
            for (ContentFileInterface currentCF : allCF) {
                result += hierarchyBuffer + currentCF.toString();
            }
        }

        // Get children.
        List<DocStructInterface> allChildren = inDocStruct.getAllChildren();
        if (allChildren != null) {
            for (DocStructInterface testChild : allChildren) {
                result += printChildDocStruct((DocStruct) testChild, hierarchy + 1);
            }
        }

        return result;
    }

    /***************************************************************************
     * <p>
     * Gets all document structures of a certain type, independent of their location in the structure tree and indepedent, if they belong to the
     * logical or physical tree.
     * </p>
     *
     * @param inTypeName
     * @return List Containing DocStruct objects or null, if none are available.
     **************************************************************************/
    public List<DocStruct> getAllDocStructsByType(String inTypeName) {

        List<DocStruct> physicallist = null;
        List<DocStruct> logicallist = null;
        List<DocStruct> commonlist = new LinkedList<DocStruct>();

        if (this.topPhysicalStruct != null) {
            physicallist = getAllDocStructsByTypePrivate(this.topPhysicalStruct, inTypeName);
            if (physicallist != null && !physicallist.isEmpty()) {
                commonlist.addAll(physicallist);
            }
        }

        if (this.topLogicalStruct != null) {
            logicallist = getAllDocStructsByTypePrivate(this.topLogicalStruct, inTypeName);
            if (logicallist != null && !logicallist.isEmpty()) {
                commonlist.addAll(logicallist);
            }
        }

        if (commonlist == null || commonlist.isEmpty()) {
            return null;
        }

        return commonlist;
    }

    /***************************************************************************
     * @param inStruct
     * @param inTypeName
     * @return
     **************************************************************************/
    private List<DocStruct> getAllDocStructsByTypePrivate(DocStruct inStruct, String inTypeName) {

        List<DocStruct> selectedChildren = new LinkedList<DocStruct>();
        List<DocStructInterface> children = inStruct.getAllChildren();

        if (children == null) {
            return null;
        }

        Iterator<DocStructInterface> it = children.iterator();

        while (it.hasNext()) {
            DocStruct child = (DocStruct) it.next();

            if (child.getType().getName().equals(inTypeName)) {
                selectedChildren.add(child);
            }

            List<DocStruct> anotherselectedlist = getAllDocStructsByTypePrivate(child, inTypeName);

            if (anotherselectedlist != null && !anotherselectedlist.isEmpty()) {
                selectedChildren.addAll(anotherselectedlist);
            }
        }

        return selectedChildren;
    }

    /***************************************************************************
     * @param inSet
     * @return
     **************************************************************************/
    public boolean setFileSet(FileSet inSet) {
        this.allImages = inSet;
        return true;
    }

    /***************************************************************************
     * @return
     **************************************************************************/
    @Override
    public FileSet getFileSet() {
        return this.allImages;
    }

    /***************************************************************************
     * <p>
     * Reads an XStream XML DigitalDocument from disk.
     * </p>
     *
     * <p>
     * Reads all given DocStructTypes and MetadataTypes from the given Preferences and gives all needed information to the DigitalDocument we just
     * read. Checks inconsistencies and updates the DigitalDocument.
     * </p>
     *
     * @param filename
     * @return
     * @throws FileNotFoundException
     * @throws UnsupportedEncodingException
     * @throws PreferencesException
     **************************************************************************/
    public DigitalDocument readXStreamXml(String theFilename, Prefs thePrefs) throws FileNotFoundException, UnsupportedEncodingException {

        BufferedReader infile = new BufferedReader(new InputStreamReader(new FileInputStream(theFilename), "UTF8"));

        // Read the DigitalDocument from an XStream file.
        // XStream xStream = new XStream(new DomDriver());
        XStream xStream = new XStream() {
            @Override
            protected MapperWrapper wrapMapper(MapperWrapper next) {
                return new MapperWrapper(next) {
                    @Override
                    public boolean shouldSerializeMember(Class definedIn, String fieldName) {
                        if (definedIn == Object.class) {
                            return false;
                        }
                        return super.shouldSerializeMember(definedIn, fieldName);
                    }
                };
            }
        };

        DigitalDocument digDoc = (DigitalDocument) xStream.fromXML(infile);

        // Set the loaded DigitalDocument to this.
        this.setLogicalDocStruct(digDoc.getLogicalDocStruct());
        this.setPhysicalDocStruct(digDoc.getPhysicalDocStruct());
        this.setFileSet(digDoc.getFileSet());

        // Update the logical and physical DocStruct recursively, if digdoc is
        // not null.
        logger.info("Updating DigitalDocument with data from Preferences");

        try {
            if (this.getLogicalDocStruct() != null) {
                updateLogicalDocStruct(thePrefs);
            }
            if (this.getPhysicalDocStruct() != null) {
                updatePhysicalDocStruct(thePrefs);
            }
        } catch (PreferencesException e) {
            logger.warn("Updating DocStruct failed due to a PreferencesException!", e);
        }

        // Process files from the physical metadata, if no fileset is existing
        // yet.
        logger.info("Updating FileSet from physical metadata");

        restoreFileSetFromPhysicalMetadata();

        return this;
    }

    /***************************************************************************
     * <p>
     * Sorts all metadata and persons alphabetically (and recursively).
     * </p>
     ***************************************************************************/
    public synchronized void sortMetadataRecursivelyAbcdefg() {

        // Sort metadata of top logical struct.
        sortMetadataRecursivelyAbcdefg(this.topLogicalStruct);

        // Sort metadata of top physical struct.
        sortMetadataRecursivelyAbcdefg(this.topPhysicalStruct);
    }

    /***************************************************************************
     * <p>
     * Sorts all metadata and persons recursively in this DocStruct according to their occurrence in the preferences file.
     * </p>
     **************************************************************************/
    public synchronized void sortMetadataRecursively(Prefs thePrefs) {

        // Sort metadata of top logical struct.
        sortMetadataRecursively(this.topLogicalStruct, thePrefs);

        // Sort metadata of top physical struct.
        sortMetadataRecursively(this.topPhysicalStruct, thePrefs);
    }

    /***************************************************************************
     * <p>
     * Updates the top logical DocStruct.
     * </p>
     *
     * @param thePrefs
     * @throws PreferencesException
     **************************************************************************/
    private void updateLogicalDocStruct(Prefs thePrefs) throws PreferencesException {

        this.setLogicalDocStruct(updateDocStruct(this.getLogicalDocStruct(), thePrefs));
    }

    /***************************************************************************
     * <p>
     * Updates the top physical DocStruct.
     * </p>
     *
     * @param thePrefs
     * @throws PreferencesException
     **************************************************************************/
    private void updatePhysicalDocStruct(Prefs thePrefs) throws PreferencesException {

        this.setPhysicalDocStruct(updateDocStruct(this.getPhysicalDocStruct(), thePrefs));
    }

    /***************************************************************************
     * <p>
     * Updates a DocStruct tree.
     * </p>
     *
     * <p>
     * NOTE This method only is needed for XStream de-serialisation!
     * </p>
     *
     * @param theStruct
     * @param thePrefs
     * @throws PreferencesException
     **************************************************************************/
    private DocStruct updateDocStruct(DocStruct theStruct, Prefs thePrefs) throws PreferencesException {

        // If prefs are empty, throw exception!
        if (thePrefs == null) {
            throw new PreferencesException("No preferences loaded!");
        }

        // If struct is empty, just return.
        if (theStruct == null) {
            logger.warn("DocStruct is empty! Update of DocStruct from Prefs failed!");
            return null;
        }

        DocStructType structTypeFromDigdoc = theStruct.getType();
        DocStructType structTypeFromPrefs = thePrefs.getDocStrctTypeByName(structTypeFromDigdoc.getName());

        // Check, if the current DocStruct name (from DigDoc) is contained in
        // the Prefs.
        if (structTypeFromPrefs != null) {
            logger.debug("DocStruct '" + structTypeFromDigdoc.getName() + "' from DigitalDocument contained in prefs");

            // Update DocStructType from the prefs.
            theStruct.setType(structTypeFromPrefs);
            logger.trace("Updated DocStructType '" + structTypeFromDigdoc.getName() + "' from prefs");

            // Update MetadataTypes from Prefs.
            structTypeFromPrefs.getAllMetadataTypes();
            List<MetadataInterface> mList = theStruct.getAllMetadata();
            if (mList != null) {
                for (MetadataInterface m : mList) {
                    // Get MetadataType from prefs.
                    MetadataType mtypeFromPrefs = thePrefs.getMetadataTypeByName(((DocStruct) m).getType().getName());
                    if (mtypeFromPrefs != null) {
                        m.setType(mtypeFromPrefs);
                        logger.trace("Updated MetadataType '" + ((DocStruct) m).getType().getName() + "' from prefs");
                    }
                }
            }
        } else {
            PreferencesException pe =
                    new PreferencesException("DocStruct '" + structTypeFromDigdoc.getName() + "' from DigitalDocument NOT contained in prefs!");
            logger.error(pe.getMessage());
            throw new PreferencesException();
        }

        logger.debug("DocStructType '" + structTypeFromDigdoc.getName() + "' and all MetadataTypes updated from prefs");

        // Recursively call all DocStructs.
        if (theStruct.getAllChildren() != null) {
            for (DocStructInterface ds : theStruct.getAllChildren()) {
                updateDocStruct((DocStruct) ds, thePrefs);
            }
        }

        return theStruct;
    }

    /***************************************************************************
     * <p>
     * Sorts all metadata and persons recursively for the given DocStruct alphabetically (and recursively).
     * </p>
     **************************************************************************/
    private synchronized void sortMetadataRecursivelyAbcdefg(DocStruct theStruct) {

        if (theStruct == null) {
            return;
        }

        if (theStruct.getAllChildren() != null) {
            for (DocStructInterface d : theStruct.getAllChildren()) {
                sortMetadataRecursivelyAbcdefg((DocStruct) d);
            }
        }

        theStruct.sortMetadataAbcdefg();
    }

    /***************************************************************************
     * <p>
     * Sorts all metadata and persons recursively for the given DocStruct according to their occurrence in the preferences file.
     * </p>
     **************************************************************************/
    private synchronized void sortMetadataRecursively(DocStruct theSruct, Prefs thePrefs) {

        if (thePrefs == null) {
            logger.warn("Cannot sort metadata according to prefs! No prefs available!");
            return;
        }

        if (theSruct == null) {
            return;
        }

        if (theSruct.getAllChildren() != null) {
            for (DocStructInterface d : theSruct.getAllChildren()) {
                sortMetadataRecursively((DocStruct) d, thePrefs);
            }
        }

        theSruct.sortMetadata(thePrefs);
    }

    /***************************************************************************
     * <p>
     * Writes a DigitalDocument to disk as an XStream XML file.
     * </p>
     *
     * @param filename
     * @deprecated
     * @throws FileNotFoundException
     * @throws UnsupportedEncodingException
     **************************************************************************/
    @Deprecated
    public void writeXStreamXml(String filename) throws FileNotFoundException, UnsupportedEncodingException {

        // Write the DigitalDocument as an XStream file.
        XStream xStream = new XStream(new DomDriver());

        BufferedWriter outfile = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename), "UTF8"));
        xStream.toXML(this, outfile);
    }

    /***************************************************************************
     * <p>
     * Restores all content files to the digital document according to the pathimagefiles metadata. All FileSet data (ContentFiles, VrtualFileGroups,
     * etc.) will be lost!
     * </p>
     **************************************************************************/
    private void restoreFileSetFromPhysicalMetadata() {

        FileSet newFileSet = new FileSet();

        // Get the physical DocStruct.
        DocStruct physicalDocStruct = this.getPhysicalDocStruct();

        // Iterate through all the physical docstruct's metadata.
        if (physicalDocStruct != null && physicalDocStruct.getAllChildren() != null) {

            // Iterate over all DocStructs "page" with metadata "physPageNumber"
            // and add a content file each, if none is existing.
            for (DocStructInterface ds : physicalDocStruct.getAllChildren()) {
                for (MetadataInterface m : ds.getAllMetadata()) {
                    if (((DocStruct) m).getType().getName().equals("physPageNumber")) {
                        createContentFile((DocStruct) ds, m.getValue());
                        newFileSet.addFile(ds.getAllContentFiles().get(0));
                    }
                }
            }
        }

        // If files were found in the pages, set the new FileSet and override
        // the old one (fixes DPD-406).
        if (!newFileSet.getAllFiles().isEmpty()) {
            this.setFileSet(newFileSet);
        }
    }

    /**************************************************************************
     * <p>
     * Adds a content file to a DocStruct "page"! All FileSet data (ContentFiles, VrtualFileGroups, etc.) will be lost!
     * </p>
     *
     * @param theStruct
     **************************************************************************/
    public void addContentFileFromPhysicalPage(DocStruct theStruct) {

        // Return, if called with a DocStruct other than "page" or a content
        // file is already existing.
        if (!theStruct.getType().getName().equals("page") || theStruct.getAllContentFiles() != null) {
            return;
        }

        // Iterate over all metadata with type name "physpagenumber".
        List<MetadataInterface> metadataList = theStruct.getAllMetadata();
        if (metadataList != null) {
            for (MetadataInterface md : metadataList) {
                if (((DocStruct) md).getType().getName().equals("physPageNumber")) {
                    // Create new content file.
                    createContentFile(theStruct, md.getValue());
                }
            }
        }
    }

    /***************************************************************************
     * <p>
     * Add all content files to the digital document according to the pathimagefiles metadata. The pages in the physical DocStruct must already exist!
     * </p>
     *
     *
     **************************************************************************/

    @Override
    public void addAllContentFiles() {

        // Get the physical DocStruct.
        DocStruct tp = this.getPhysicalDocStruct();

        // Delete the existing fileset before adding the files, and save the
        // virtualFileGroups!
        if (this.getFileSet() != null && this.getFileSet().getVirtualFileGroups() != null) {
            List<VirtualFileGroup> vfgList = this.getFileSet().getVirtualFileGroups();
            this.setFileSet(new FileSet());
            this.getFileSet().setVirtualFileGroups(vfgList);
        }

        String representative = "";

        // Iterate through all the physical docstruct's metadata.
        if (tp != null && tp.getAllMetadata() != null) {

            // Set the path to the images.
            String pif = "";
            for (MetadataInterface md : tp.getAllMetadata()) {
                if (((DocStruct) md).getType().getName().equals("pathimagefiles")) {
                    pif = md.getValue();
                } else if (((DocStruct) md).getType().getName().equals("_representative")) {
                    representative = md.getValue();
                }
            }

            // Iterate over all pages and add all the content files.
            if (tp.getAllChildren() != null) {
                for (DocStructInterface ds : tp.getAllChildren()) {
                    ContentFile cf = new ContentFile();

                    if (((DocStruct) ds).getType().getName().equals("page")) {
                        // Iterate over all metadata.
                        for (MetadataInterface md : ds.getAllMetadata()) {
                            if (((DocStruct) md).getType().getName().equals("physPageNumber")) {
                                cf.setLocation(pif + "/" + new DecimalFormat("00000000").format(Integer.parseInt(md.getValue())) + ".tif");
                                cf.setMimetype("image/tiff");
                                if (!representative.isEmpty() && representative.equals(md.getValue())) {
                                    cf.setRepresentative(true);
                                }
                                // Remove all content files from the page, if
                                // existing.
                                if (ds.getAllContentFiles() != null) {
                                    for (ContentFileInterface oldCf : ds.getAllContentFiles()) {
                                        try {
                                            ds.removeContentFile(oldCf);
                                            cf.setLocation(oldCf.getLocation());
                                        } catch (ContentFileNotLinkedException e) {
                                            // Do nothing, because we want to
                                            // remove them anyway. If they do
                                            // not exist, we have no problem!
                                        }
                                    }
                                }
                                // Add the current content file to page.
                                ds.addContentFile(cf);

                                logger.trace("Added file '" + cf.getLocation() + "' to DocStruct '" + ((DocStruct) ds).getType().getName() + "'");
                            }
                        }
                    }
                }
            }
        }
    }

    /***************************************************************************
     * Retrieves the name of the anchor structure, if any, or null otherwise.
     * Anchors are a special type of document structure, which group other
     * structure entities together, but have no own content. Imagine a
     * periodical as such an anchor. The periodical itself is a virtual
     * structure entity without any own content, but groups all years of
     * appearance together. Years may be anchors again for volumes, etc.
     *
     * @return String, which is null, if it cannot be used as an anchor
     **************************************************************************/
    public String getAnchorClass() {
        return topLogicalStruct.getAnchorClass();
    }

    /***************************************************************************
     * <p>
     * Overrides ContentFiles of DigitalDocument with new names for images. Code mostly taken from old addAllContentFiles method.
     * </p>
     *
     * @param a List of sorted image names
     *
     * @author Robert Sehr
     *
     **************************************************************************/

    @Override
    public void overrideContentFiles(List<String> images) {

        // Get the physical DocStruct.
        DocStruct tp = this.getPhysicalDocStruct();

        // Delete the existing fileset before adding the files, and save the
        // virtualFileGroups!
        List<VirtualFileGroup> vfgList = this.getFileSet().getVirtualFileGroups();
        this.setFileSet(new FileSet());
        this.getFileSet().setVirtualFileGroups(vfgList);
        String representative = "";
        // Iterate through all the physical docstruct's metadata.
        if (tp != null && tp.getAllMetadata() != null) {

            // Set the path to the images.
            String pif = "";
            for (MetadataInterface md : tp.getAllMetadata()) {
                if (((DocStruct) md).getType().getName().equals("pathimagefiles")) {
                    pif = md.getValue();
                } else if (((DocStruct) md).getType().getName().equals("_representative")) {
                    representative = md.getValue();
                }
            }

            // Iterate over all pages and add all the content files.
            if (tp.getAllChildren() != null) {
                for (DocStructInterface ds : tp.getAllChildren()) {
                    ContentFile cf = new ContentFile();

                    if (((DocStruct) ds).getType().getName().equals("page")) {
                        // Iterate over all metadata.
                        for (MetadataInterface md : ds.getAllMetadata()) {
                            if (((DocStruct) md).getType().getName().equals("physPageNumber")) {

                                if (!representative.isEmpty() && representative.equals(md.getValue())) {
                                    cf.setRepresentative(true);
                                }
                                // Using parseInt instead of new Integer() now;
                                int value = Integer.parseInt(md.getValue());
                                cf.setLocation(pif + File.separator + images.get(value - 1));
                                // Remove all content files from the page, if
                                // existing.
                                if (ds.getAllContentFiles() != null) {
                                    for (ContentFileInterface oldCf : ds.getAllContentFiles()) {
                                        cf.setMimetype(((ContentFile) oldCf).getMimetype());
                                        cf.setLocation(oldCf.getLocation());
                                        try {
                                            ds.removeContentFile(oldCf);
                                        } catch (ContentFileNotLinkedException e) {
                                            // Do nothing, because we want to
                                            // remove them anyway. If they do
                                            // not exist, we have no problem!
                                        }
                                    }
                                } else {
                                    cf.setLocation(pif + File.separator + images.get(value - 1));
                                    cf.setMimetype("image/tiff");
                                }
                                ds.addContentFile(cf);
                            }
                        }
                    }
                }
            }
        }
    }

    /**************************************************************************
     * <p>
     * Just returns the path to the image files.
     * </p>
     *
     * @return
     **************************************************************************/
    private String getPathToImages() {

        String pathToImageFiles = "";
        if (this.getPhysicalDocStruct() != null && this.getPhysicalDocStruct().getAllMetadata() != null) {
            for (MetadataInterface md : this.getPhysicalDocStruct().getAllMetadata()) {
                if (((DocStruct) md).getType().getName().equals("pathimagefiles")) {
                    pathToImageFiles = md.getValue();
                    break;
                }
            }
        }
        return pathToImageFiles;
    }

    /**************************************************************************
     * <p>
     * Adds a single content file to a DocStruct.
     * </p>
     *
     * TODO Get the mimetype from anywhere, and not assume it was tiff!
     *
     * @param theStruct
     * @param theName
     **************************************************************************/
    private void createContentFile(DocStruct theStruct, String theName) {

        // Create new content file, set location and mimetype.
        ContentFile newCf = new ContentFile();
        newCf.setLocation(getPathToImages() + "/" + new DecimalFormat("00000000").format(Integer.parseInt(theName)) + ".tif");
        newCf.setMimetype("image/tiff");

        // Remove all content files from the page, if existing.
        if (theStruct.getAllContentFiles() != null) {
            for (ContentFileInterface oldCf : theStruct.getAllContentFiles()) {
                try {
                    theStruct.removeContentFile(oldCf);
                } catch (ContentFileNotLinkedException e) {
                    // Do nothing, because we want to remove them anyway. If
                    // they do not exist, we have no problem!
                }
            }
        }

        // Set the fileset of the current DigitalDocument, set DigitalDocument
        // first.
        theStruct.setDigitalDocument(this);
        theStruct.addContentFile(newCf);

        logger.trace("Added file '" + newCf.getLocation() + "' to DocStruct '" + theStruct.getType().getName() + "'");
    }

    /***************************************************************************
     * @return
     **************************************************************************/
    public static String getVersion() {
        return VERSION;
    }

    /***************************************************************************
     * <p>
     * Overloaded equals method, compares this DigitalDocument with the DigitalDocument in parameter digitalDocument.
     * </p>
     *
     * <p>
     * This method is not yet working within normal parameters! Please use with care (or do not use it at all!)
     * </p>
     *
     * TODO Make this method work properly!!
     *
     * @author Wulf Riebensahm
     * @param digitalDocument
     * @return TRUE if documents can be considered equal, false if they are different.
     **************************************************************************/
    public boolean equals(DigitalDocument digitalDocument) {

        logger.debug("test phys pair");
        if (DigitalDocument.quickPairCheck(this.getPhysicalDocStruct(), digitalDocument.getPhysicalDocStruct()) == ListPairCheck.isNotEqual) {
            logger.debug("phys pair false returned");
            return false;
        }

        logger.debug("test log pair");
        if (DigitalDocument.quickPairCheck(this.getLogicalDocStruct(), digitalDocument.getLogicalDocStruct()) == ListPairCheck.isNotEqual) {
            logger.debug("log pair false returned");
            return false;
        }

        logger.debug("in depth test phys pair");
        if (!(DigitalDocument.quickPairCheck(this.getPhysicalDocStruct(), digitalDocument.getPhysicalDocStruct()) == ListPairCheck.isEqual)
                && !this.getPhysicalDocStruct().equals(digitalDocument.getPhysicalDocStruct())) {
            logger.debug("ind. phys pair false returned");
            return false;
        }

        logger.debug("in depth test log pair");
        if (!(DigitalDocument.quickPairCheck(this.getLogicalDocStruct(), digitalDocument.getLogicalDocStruct()) == ListPairCheck.isEqual)
                && !this.getLogicalDocStruct().equals(digitalDocument.getLogicalDocStruct())) {
            logger.debug("ind. log pair false returned");
            return false;
        }

        return true;
    }

    /***************************************************************************
     * <p>
     * Helps simplifying code in equals method, reused in equals methods subsequent to this one (other objects of digdoc), hence protected, not
     * private.
     * </p>
     *
     * @author Wulf Riebensahm
     * @param o1
     * @param o2
     * @return
     **************************************************************************/
    protected static ListPairCheck quickPairCheck(Object o1, Object o2) {

        if (o1 == null && o2 == null) {
            return ListPairCheck.isEqual;
        }
        if (o1 == null || o2 == null) {
            return ListPairCheck.isNotEqual;
        }

        return ListPairCheck.needsFurtherChecking;
    }

    /**
     * @param techMd the techMd to set
     */
    public void addTechMd(Node techMdNode) {
        if (this.amdSec == null) {
            amdSec = new AmdSec(new ArrayList<Md>());
        }
        Md techMd = new Md(techMdNode);
        this.amdSec.addTechMd(techMd);
    }

    /**
     * @param techMd the techMd to set
     */
    public void addTechMd(Md techMd) {
        if (this.amdSec == null) {
            amdSec = new AmdSec(new ArrayList<Md>());
        }
        this.amdSec.addTechMd(techMd);
    }

    /**
     * @return the techMd
     */
    public List<Node> getTechMdsAsNodes() {
        return amdSec.getTechMdsAsNodes();
    }

    /**
     * @return the techMd
     */
    public List<Md> getTechMds() {
        if (amdSec == null) {
            return new ArrayList<Md>();
        }
        return amdSec.getTechMdList();
    }

    public Md getTechMd(String id) {
        if (amdSec == null || amdSec.getTechMdList() == null) {
            return null;
        }

        for (Md techMd : this.amdSec.getTechMdList()) {
            if (techMd.getId() != null && techMd.getId().trim().contentEquals(id.trim())) {
                return techMd;
            }
        }
        return null;
    }

    public void setAmdSec(String id) {
        this.amdSec = new AmdSec(new ArrayList<Md>());
        this.amdSec.setId(id);
    }

    public AmdSec getAmdSec(String id) {
        if (amdSec == null) {
            return null;
        } else if (amdSec.getId().trim().contentEquals(id.trim())) {
            return amdSec;
        }
        return null;
    }

    public AmdSec getAmdSec() {
        return amdSec;
    }

    /***************************************************************************
     * <p>
     * Return values for method ListPairCheck.
     * </p>
     *
     * @author Wulf Riebensahm
     * @see {@link quickPairCheck}
     **************************************************************************/
    protected static enum ListPairCheck {
        isEqual, isNotEqual, needsFurtherChecking
    }


    /***************************************************************************
     * <p>
     * Creates a deep copy of the DigitalDocument.
     * </p>
     *
     * @return the new DigitalDocument instance
     **************************************************************************/

    public DigitalDocument copyDigitalDocument() throws WriteException {

        DigitalDocument newDigDoc = null;

        try {

            // remove techMd list for serialization
            ArrayList<Md> tempList = new ArrayList<Md>(getTechMds());
            getTechMds().clear();

            // Write the object out to a byte array.
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(bos);
            out.writeObject(this);
            out.flush();
            out.close();

            // Make an input stream from the byte array and read
            // a copy of the object back in.
            ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()));
            newDigDoc = (DigitalDocument) in.readObject();

            // reattach techMd list
            for (Md md : tempList) {
                newDigDoc.addTechMd(md);
            }

        } catch (IOException e) {
            String message = "Couldn't obtain OutputStream!";
            logger.error(message, e);
            throw new WriteException(message, e);
        } catch (ClassNotFoundException e) {
            String message = "Could not find some class!";
            logger.error(message, e);
            throw new WriteException(message, e);
        }

        return newDigDoc;
    }
}
