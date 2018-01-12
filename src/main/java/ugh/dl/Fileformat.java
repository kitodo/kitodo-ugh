package ugh.dl;

import org.kitodo.api.ugh.DigitalDocumentInterface;
import org.kitodo.api.ugh.FileformatInterface;
import org.kitodo.api.ugh.exceptions.PreferencesException;
import org.kitodo.api.ugh.exceptions.ReadException;
import org.kitodo.api.ugh.exceptions.WriteException;

/*******************************************************************************
 * <p>
 * A Fileformat is an abstract description of a serialization of a complete
 * <code>DigitalDocument</code>. A Fileformat may store or read a
 * <code>DigitalDocument</code> object to/from a file.
 * </p>
 *
 * <p>
 * Depending on the implementation a Fileformat may store all or only part of
 * the information.
 * </p>
 *
 * <p>
 * Every fileformat may have methods to load, save or update a file. In an
 * implementation not all methods need to be available. Certain Fileformats are
 * just readable; other may not be updateable.
 * </p>
 *
 * <p>
 * <b>Differences between readable, updateable, writeable:</b><br>
 * <ul>
 * <li>readable: the fileformat can be read from a file
 * <li>updateable: after reading a fileformat, some information can be updated.
 * The result can be written back (to the same file).
 * <li>writeable: a <code>DigitalDocument</code> can be written to a completly
 * new file.
 * </ul>
 * </p>
 *
 * <p>
 * Internally every fileformat has a DigitalDocument instance, which will be
 * created while reading a file successfully. This instance can be obtained by
 * calling the GetDigitalDocument instance. Before writing a file, a
 * DigitalDocument instance must be available.
 * </p>
 *
 * @author Markus Enders
 * @version 2009-10-06
 * @see DigitalDocument
 ******************************************************************************/

public interface Fileformat extends FileformatInterface {

    /***************************************************************************
     * <p>
     * Returns the new DigitalDocument instance, which was created while reading
     * the file. If a file was unreadable, null is returned.
     * </p>
     *
     * @return DigitalDocument the DigitalDocument instance
     * @throws PreferencesException
     **************************************************************************/
    @Override
    public DigitalDocument getDigitalDocument() throws PreferencesException;

    /***************************************************************************
     * <p>
     * Reads a file and creates a DigitalDocument instance.
     * </p>
     *
     * @param filename
     *            full path to file, which should be read
     * @return a boolean value, true if everything was okay; false, if there
     *         there was an error (IO Error etc...).
     *
     * @throws ReadException
     **************************************************************************/
    @Override
    public boolean read(String filename) throws ReadException;

    /***************************************************************************
     * <p>
     * Writes the content of the DigitalDocument instance to a file. The file
     * format must already have a DigitalDocument instance.
     * </p>
     *
     * @param filename
     *            full path to the file
     * @return true, if everything is okay. Otherwise false, if an error occurred
     *         (IO-Error etc...)
     * @throws WriteException
     * @throws PreferencesException
     **************************************************************************/
    @Override
    public boolean write(String filename) throws WriteException,
            PreferencesException;

    /***************************************************************************
     * <p>
     * Updates a file, which had to be read before. Updating means, that the
     * same file, which was read will be written again. Changes made in the
     * Metadata-instances can be written back to the file. To support file
     * updates, the fileformat implementation must support the storage of native
     * objects (e.g. dom.elements-objects) in the metadata.
     * </p>
     *
     * @param filename
     *            full path of output file
     * @return true if updating was successful; otherwise false.
     **************************************************************************/
    public boolean update(String filename);

    /***************************************************************************
     * <p>
     * Sets a DigitalDocument instance. This instance must be available before a
     * file can be written or updated.
     * </p>
     *
     * @param inDoc
     * @return true; only if a problem occurred, false is returned.
     **************************************************************************/
    @Override
    public boolean setDigitalDocument(DigitalDocumentInterface inDoc);

}
