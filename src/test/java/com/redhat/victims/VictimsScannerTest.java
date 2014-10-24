package com.redhat.victims;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.junit.Test;

import com.redhat.victims.fingerprint.Algorithms;

public class VictimsScannerTest {

    public void jsonTestStream(String inFile, String jsonFile)
            throws IOException {
        OutputStream os = new ByteArrayOutputStream();
        File test = new File(inFile);
        try {
            VictimsScanner.scan(test.getAbsolutePath(), os);
            VictimsRecord result = VictimsRecord.fromJSON(os.toString().trim());
            String jstr = FileUtils.readFileToString(new File(jsonFile)).trim();
            VictimsRecord expected = VictimsRecord.fromJSON(jstr);
            assertTrue("Scanned record not equal to expected",
                    expected.equals(result));
        } catch (IOException e) {
            fail("Could not scan file: " + inFile);
        }
        os.flush();
        os.close();
    }

    public void jsonTestArray(String inFile, String jsonFile)
            throws IOException {
        ArrayList<VictimsRecord> records = new ArrayList<VictimsRecord>();
        File test = new File(inFile);
        int expected_count = 1;
        try {
            VictimsScanner.scan(test.getAbsolutePath(), records);
            int result_count = records.size();
            assertTrue(String.format("Expected %d records, found %d.",
                    expected_count, result_count), result_count == 1);
            // the output stream write adds a line to split records
            VictimsRecord result = records.get(0);
            String jstr = FileUtils.readFileToString(new File(jsonFile)).trim();
            VictimsRecord expected = VictimsRecord.fromJSON(jstr);
            assertTrue("Scanned record not equal to expected",
                    expected.equals(result));
        } catch (IOException e) {
            fail("Could not scan file: " + inFile);
        }
    }

    public void valueTest(String inFile, String sha1File, Boolean useStream)
            throws IOException {
        File test = new File(inFile);
        InputStream in = null;
        if (useStream) {
            in = new FileInputStream(test);
        }
        String expected = FileUtils.readFileToString(new File(sha1File)).trim();
        int expected_count = 1;

        System.setProperty(VictimsConfig.Key.ALGORITHMS, "SHA1");
        try {
            ArrayList<VictimsRecord> records = null;
            if (useStream) {
                records = VictimsScanner.getRecords(in,
                        FilenameUtils.getBaseName(inFile));
            } else {
                records = VictimsScanner.getRecords(test.getAbsolutePath());

            }
            int result_count = records.size();
            assertTrue(String.format("Expected %d records, found %d.",
                    expected_count, result_count), result_count == 1);
            Algorithms alg = Algorithms.SHA1;
            String result = records.get(0).getHash(alg).trim();
            assertEquals(alg.toString() + " mismatch", expected, result);
        } catch (IOException e) {
            fail("Could not scan file: " + inFile);
        } finally {
            System.setProperty(VictimsConfig.Key.ALGORITHMS, "");
        }
    }

    @Test
    public void testScanStringOutputStreamJar() throws IOException {
        jsonTestStream(Resources.JAR_FILE, Resources.JAR_JSON);
        jsonTestStream(Resources.POM_FILE, Resources.POM_JSON);
    }

    @Test
    public void testScanStringArrayListOfVictimsRecord() throws IOException {
        jsonTestArray(Resources.JAR_FILE, Resources.JAR_JSON);
        jsonTestArray(Resources.POM_FILE, Resources.POM_JSON);
    }

    @Test
    public void testGetRecordsString() throws IOException {
        valueTest(Resources.JAR_FILE, Resources.JAR_SHA1, false);
        valueTest(Resources.POM_FILE, Resources.POM_SHA1, false);
    }

    @Test
    public void testGetRecordsInputStreamString() throws IOException {
        valueTest(Resources.JAR_FILE, Resources.JAR_SHA1, true);
        valueTest(Resources.POM_FILE, Resources.POM_SHA1, true);
    }

}
