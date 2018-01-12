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

public class LegacyUghImplementation implements FactoryInterface {

    @Override
    public ContentFileInterface createContentFile() {
        return new ContentFile();
    }

    @Override
    public DigitalDocumentInterface createDigitalDocument() {
        return new DigitalDocument();
    }

    @Override
    public MetadataInterface createMetadata(MetadataTypeInterface metadataType)
            throws MetadataTypeNotAllowedException {
        return new Metadata((MetadataType) metadataType);
    }

    @Override
    public MetadataGroupInterface createMetadataGroup(MetadataGroupTypeInterface metadataGroupType)
            throws MetadataTypeNotAllowedException {
        return new MetadataGroup((MetadataGroupType) metadataGroupType);
    }

    @Override
    public MetadataGroupTypeInterface createMetadataGroupType() {
        return new MetadataGroupType();
    }

    @Override
    public MetadataTypeInterface createMetadataType() {
        return new MetadataType();
    }

    @Override
    public MetsModsInterface createMetsMods(PrefsInterface prefs) throws PreferencesException {
        return new MetsMods((Prefs) prefs);
    }

    @Override
    public MetsModsImportExportInterface createMetsModsImportExport(PrefsInterface prefs) throws PreferencesException {
        return new MetsModsImportExport((Prefs) prefs);
    }
    
    @Override
    public PersonInterface createPerson(MetadataTypeInterface metadataType)
            throws MetadataTypeNotAllowedException {
        return new Person((MetadataType) metadataType);
    }

    @Override
    public PrefsInterface createPrefs() {
        return new Prefs();
    }

    @Override
    public FileformatInterface createRDFFile(PrefsInterface prefs) throws PreferencesException {
        return new RDFFile((Prefs) prefs);
    }

    @Override
    public RomanNumeralInterface createRomanNumeral() {
        return new RomanNumeral();
    }

    @Override
    public VirtualFileGroupInterface createVirtualFileGroup() {
        return new VirtualFileGroup();
    }

    @Override
    public FileformatInterface createXStream(PrefsInterface prefs) throws PreferencesException {
        return new XStream((Prefs) prefs);
    }

    @Override
    public PicaPlusInterface createPicaPlus(PrefsInterface prefs) {
        return new PicaPlus((Prefs) prefs);
    }

}
