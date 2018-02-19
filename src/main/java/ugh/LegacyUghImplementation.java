/*******************************************************************************
 * ugh / LegacyUghImplementation.java
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

package ugh;

import org.kitodo.api.ugh.ContentFileInterface;
import org.kitodo.api.ugh.DigitalDocumentInterface;
import org.kitodo.api.ugh.FactoryInterface;
import org.kitodo.api.ugh.FileformatInterface;
import org.kitodo.api.ugh.MetadataGroupInterface;
import org.kitodo.api.ugh.MetadataGroupTypeInterface;
import org.kitodo.api.ugh.MetadataInterface;
import org.kitodo.api.ugh.MetadataTypeInterface;
import org.kitodo.api.ugh.MetsModsImportExportInterface;
import org.kitodo.api.ugh.MetsModsInterface;
import org.kitodo.api.ugh.PersonInterface;
import org.kitodo.api.ugh.PicaPlusInterface;
import org.kitodo.api.ugh.PrefsInterface;
import org.kitodo.api.ugh.RomanNumeralInterface;
import org.kitodo.api.ugh.VirtualFileGroupInterface;
import org.kitodo.api.ugh.exceptions.MetadataTypeNotAllowedException;
import org.kitodo.api.ugh.exceptions.PreferencesException;

import ugh.dl.ContentFile;
import ugh.dl.DigitalDocument;
import ugh.dl.Metadata;
import ugh.dl.MetadataGroup;
import ugh.dl.MetadataGroupType;
import ugh.dl.MetadataType;
import ugh.dl.Person;
import ugh.dl.Prefs;
import ugh.dl.RomanNumeral;
import ugh.dl.VirtualFileGroup;
import ugh.fileformats.excel.RDFFile;
import ugh.fileformats.mets.MetsMods;
import ugh.fileformats.mets.MetsModsImportExport;
import ugh.fileformats.mets.XStream;
import ugh.fileformats.opac.PicaPlus;

/**
 * Factory comprising the API constructors.
 *
 * @see "https://en.wikipedia.org/wiki/Abstract_factory_pattern"
 */
public class LegacyUghImplementation implements FactoryInterface {

    /**
     * Creates a new content file.
     *
     * @return the new content file
     */
    @Override
    public ContentFileInterface createContentFile() {
        return new ContentFile();
    }

    /**
     * Creates a new digital document.
     *
     * @return the new digital document
     */
    @Override
    public DigitalDocumentInterface createDigitalDocument() {
        return new DigitalDocument();
    }

    /**
     * Creates a new meta-data entry.
     *
     * @param metadataType
     *            the type of the entry
     * @return the new meta-data entry
     * @throws MetadataTypeNotAllowedException
     *             if the type is {@code null}
     */
    @Override
    public MetadataInterface createMetadata(MetadataTypeInterface metadataType) throws MetadataTypeNotAllowedException {
        return new Metadata((MetadataType) metadataType);
    }

    /**
     * Creates a new meta-data group.
     *
     * @param metadataGroupType
     *            the type of the meta-data group
     * @return the new meta-data group
     * @throws MetadataTypeNotAllowedException
     *             if the type is {@code null}
     */
    @Override
    public MetadataGroupInterface createMetadataGroup(MetadataGroupTypeInterface metadataGroupType)
            throws MetadataTypeNotAllowedException {
        return new MetadataGroup((MetadataGroupType) metadataGroupType);
    }

    /**
     * Creates a new, empty meta-data group type.
     *
     * @return the new meta-data group type
     */
    @Override
    public MetadataGroupTypeInterface createMetadataGroupType() {
        return new MetadataGroupType();
    }

    /**
     * Creates a new, empty meta-data type.
     *
     * @return the new meta-data type
     */
    @Override
    public MetadataTypeInterface createMetadataType() {
        return new MetadataType();
    }

    /**
     * Creates a new METS-intern read-writer.
     *
     * @param prefs
     *            rule set to base the read-writer on
     * @return the new METS read-writer
     * @throws PreferencesException
     *             if there is no {@code <METS>} section in the rule set
     */
    @Override
    public MetsModsInterface createMetsMods(PrefsInterface prefs) throws PreferencesException {
        return new MetsMods((Prefs) prefs);
    }

    /**
     * Creates a new METS/MODS export writer.
     *
     * @param prefs
     *            rule set to base the writer on
     * @return the new METS read-writer
     * @throws PreferencesException
     *             if there is no {@code <METS>} section in the rule set
     */
    @Override
    public MetsModsImportExportInterface createMetsModsImportExport(PrefsInterface prefs) throws PreferencesException {
        return new MetsModsImportExport((Prefs) prefs);
    }

    /**
     * Creates a new person-type meta-data entry.
     *
     * @param metadataType
     *            the type of the entry
     * @return the new person entry
     * @throws MetadataTypeNotAllowedException
     *             if the type is {@code null}
     */
    @Override
    public PersonInterface createPerson(MetadataTypeInterface metadataType) throws MetadataTypeNotAllowedException {
        return new Person((MetadataType) metadataType);
    }

    /**
     * Creates a new PICA plus import reader.
     *
     * @param prefs
     *            rule set to base the reader on
     * @return the new PICA plus reader
     */
    @Override
    public PrefsInterface createPrefs() {
        return new Prefs();
    }

    /**
     * Creates a new, empty rule set.
     *
     * @return the new rule set.
     */
    @Override
    public FileformatInterface createRDFFile(PrefsInterface prefs) throws PreferencesException {
        return new RDFFile((Prefs) prefs);
    }

    /**
     * Creates a new Agora-RDF read-writer.
     *
     * @param prefs
     *            rule set to base the read-writer on
     * @return the new RDF read-writer
     * @throws PreferencesException
     *             if there is no {@code <RDF>} section in the rule set
     */
    @Override
    public RomanNumeralInterface createRomanNumeral() {
        return new RomanNumeral();
    }

    /**
     * Creates a new roman numeral with a value of I.
     *
     * @return the new roman numeral
     */
    @Override
    public VirtualFileGroupInterface createVirtualFileGroup() {
        return new VirtualFileGroup();
    }

    /**
     * Creates a new virtual file group.
     *
     * @return the new virtual file group
     */
    @Override
    public FileformatInterface createXStream(PrefsInterface prefs) throws PreferencesException {
        return new XStream((Prefs) prefs);
    }

    /**
     * Creates a new XStream-intern read-writer.
     *
     * @param prefs
     *            rule set to base the read-writer on
     * @return the new XStream read-writer
     * @throws PreferencesException
     *             is never thrown
     */
    @Override
    public PicaPlusInterface createPicaPlus(PrefsInterface prefs) {
        return new PicaPlus((Prefs) prefs);
    }

}
