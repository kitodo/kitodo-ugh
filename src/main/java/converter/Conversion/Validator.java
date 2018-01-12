package converter.Conversion;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.kitodo.api.ugh.DocStructInterface;
import org.kitodo.api.ugh.MetadataInterface;
import org.kitodo.api.ugh.MetadataTypeInterface;
import org.kitodo.api.ugh.ReferenceInterface;
import org.kitodo.api.ugh.exceptions.PreferencesException;
import org.apache.logging.log4j.LogManager;

import ugh.dl.DigitalDocument;
import ugh.dl.DocStruct;
import ugh.dl.DocStructType;
import ugh.dl.Fileformat;
import ugh.dl.Metadata;
import ugh.dl.MetadataType;
import ugh.dl.Prefs;
import ugh.dl.Reference;

public class Validator {

    protected final Logger logger = LogManager.getLogger(Validator.class);

    List<DocStruct> docStructsOhneSeiten;

    boolean autoSave = false;

    public boolean validate(Prefs myPrefs, Fileformat gdzfile, String id) {

        /*
         * -------------------------------- Fileformat einlesen --------------------------------
         */

        return validate(gdzfile, myPrefs, id);
    }

    public boolean validate(Fileformat gdzfile, Prefs inPrefs, String id) {
        boolean ergebnis = true;

        DigitalDocument dd = null;
        try {
            dd = gdzfile.getDigitalDocument();
        } catch (Exception e) {
            logger.error("Can not get DigitalDocument[" + id + "]", e);
            ergebnis = false;
        }


        /*
         * -------------------------------- auf Docstructs ohne Seiten prüfen
         * --------------------------------
         */
        DocStruct logicalTop = dd.getLogicalDocStruct();
        if (logicalTop == null) {
            logger.info("[" + id + "] " + "Verifizierung nicht erfolgreich, keine Seiten zugewiesen");
            ergebnis = false;
        }

        docStructsOhneSeiten = new ArrayList<DocStruct>();
        this.checkDocStructsOhneSeiten(logicalTop);
        if (docStructsOhneSeiten.size() != 0) {
            for (Iterator<DocStruct> iter = docStructsOhneSeiten.iterator(); iter.hasNext();) {
                DocStruct ds = iter.next();
                logger.info("[" +id + "] Strukturelement ohne Seiten: " + ds.getType().getName());
            }
            ergebnis = false;
        }

        /*
         * -------------------------------- auf Seiten ohne Docstructs prüfen
         * --------------------------------
         */
        List<String> seitenOhneDocstructs = null;
        try {
            seitenOhneDocstructs = checkSeitenOhneDocstructs(gdzfile);
        } catch (PreferencesException e1) {
            logger.info("[" +id + "] Can not check pages without docstructs: ");
            ergebnis = false;
        }
        if (seitenOhneDocstructs != null && seitenOhneDocstructs.size() != 0) {
            for (Iterator<String> iter = seitenOhneDocstructs.iterator(); iter.hasNext();) {
                String seite = iter.next();
                logger.info("[" + id + "] " + "Seiten ohne Strukturelement: " + seite);
            }
            ergebnis = false;
        }

        /*
         * -------------------------------- auf mandatory Values der Metadaten
         * prüfen --------------------------------
         */
        List<String> mandatoryList = checkMandatoryValues(dd.getLogicalDocStruct(), new ArrayList<String>());
        if (mandatoryList.size() != 0) {
            for (Iterator<String> iter = mandatoryList.iterator(); iter.hasNext();) {
                String temp = iter.next();
                logger.info("[" + id + "] " + "Pflichtelement: " + temp);
            }
            ergebnis = false;
        }
        return ergebnis;
    }

    private void checkDocStructsOhneSeiten(DocStruct inStruct) {
        if (inStruct.getAllToReferences().size() == 0 && inStruct.getType().getAnchorClass() == null)
            docStructsOhneSeiten.add(inStruct);
        /* alle Kinder des aktuellen DocStructs durchlaufen */
        if (inStruct.getAllChildren() != null) {
            for (Iterator<DocStructInterface> iter = inStruct.getAllChildren().iterator(); iter.hasNext();) {
                DocStruct child = (DocStruct) iter.next();
                checkDocStructsOhneSeiten(child);
            }
        }
    }

    private List<String> checkSeitenOhneDocstructs(Fileformat inRdf) throws PreferencesException {
        List<String> rueckgabe = new ArrayList<String>();
        DocStruct boundbook = inRdf.getDigitalDocument().getPhysicalDocStruct();
        /* wenn boundbook null ist */
        if (boundbook == null || boundbook.getAllChildren() == null)
            return rueckgabe;

        /* alle Seiten durchlaufen und pruefen ob References existieren */
        for (Iterator<DocStructInterface> iter = boundbook.getAllChildren().iterator(); iter.hasNext();) {
            DocStruct ds = (DocStruct) iter.next();
            List<ReferenceInterface> refs = ds.getAllFromReferences();
            String physical = "";
            String logical = "";
            if (refs.size() == 0) {
                // System.out.println("   >>> Keine Seiten: "
                // + ((Metadata) ds.getAllMetadata().getFirst()).getValue());
                for (Iterator<MetadataInterface> iter2 = ds.getAllMetadata().iterator(); iter2.hasNext();) {
                    Metadata md = (Metadata) iter2.next();
                    if (md.getType().getName().equals("logicalPageNumber"))
                        logical = " (" + md.getValue() + ")";
                    if (md.getType().getName().equals("physPageNumber"))
                        physical = md.getValue();
                }
                rueckgabe.add(physical + logical);
            }
        }
        return rueckgabe;
    }

    private List<String> checkMandatoryValues(DocStruct inStruct, ArrayList<String> inList) {
        DocStructType dst = inStruct.getType();
        // System.out.println("----------------------- " + dst.getName());
        List<MetadataTypeInterface> allMDTypes = dst.getAllMetadataTypes();
        for (MetadataTypeInterface mdt : allMDTypes) {
            String number = dst.getNumberOfMetadataType(mdt);
            // System.out.println(mdt.getName());
            List<? extends ugh.dl.Metadata> ll = inStruct.getAllMetadataByType(mdt);
            int real = 0;
            if (ll != null && ll.size() > 0) {
                real = ll.size();

                if (number.equals("1m") && real == 1 && (ll.get(0).getValue() == null || ll.get(0).getValue().length() == 0)) {
                    inList.add(mdt.getName() + " in " + dst.getName() + " is empty.");
                }
                /* jetzt die Typen pruefen */
                if (number.equals("1m") && real != 1) {
                    inList.add(mdt.getName() + " in " + dst.getName() + " must exist 1 time but exists " + real + " times");
                }
                if ((number.equals("+") || number.equals("1o")) && real > 1) {
                    inList.add(mdt.getName() + " in " + dst.getName() + " must not exist more than 1 time but exists " + real + " times");
                }
            }
        }

        /* alle Kinder des aktuellen DocStructs durchlaufen */
        if (inStruct.getAllChildren() != null) {
            for (DocStructInterface child : inStruct.getAllChildren())
                checkMandatoryValues((DocStruct) child, inList);
        }
        return inList;
    }
}
