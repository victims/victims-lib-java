package com.redhat.victims;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import com.redhat.victims.fingerprint.Algorithms;

public class VictimsRecordTest {
    @Test
    public void testEquals() throws IOException {
        String jstr = FileUtils.readFileToString(new File(Resources.JAR_JSON))
                .trim();
        VictimsRecord vr = VictimsRecord.fromJSON(jstr);
        assertTrue("Equality check for Victims Record failed.", vr.equals(vr));

        String jstr2 = FileUtils.readFileToString(new File(Resources.POM_JSON))
                .trim();
        VictimsRecord vr2 = VictimsRecord.fromJSON(jstr2);
        assertFalse("Non Equality check for Victims Records failed.",
                vr.equals(vr2));

    }

    @Test
    public void testEqualsArrayList() throws IOException {
        String jstr1 = FileUtils.readFileToString(new File(Resources.JAR_JSON))
                .trim();
        String jstr2 = FileUtils.readFileToString(new File(Resources.POM_JSON))
                .trim();
        VictimsRecord vr1 = VictimsRecord.fromJSON(jstr1);
        VictimsRecord vr2 = VictimsRecord.fromJSON(jstr2);

        // test equals is correctly used in an ArrayList
        ArrayList<VictimsRecord> a = new ArrayList<VictimsRecord>();
        a.add(vr1);

        VictimsRecord vr3 = VictimsRecord.fromJSON(jstr1);
        ArrayList<VictimsRecord> b = new ArrayList<VictimsRecord>();
        b.add(vr2);
        b.add(vr3);

        assertTrue("Equality in ArrayList positive case", b.containsAll(a));
        assertFalse("Equality in ArrayList negative case", a.containsAll(b));
    }

    @Test
    public void testContainsAll() throws IOException {
        String jstr1 = FileUtils.readFileToString(new File(Resources.JAR_JSON))
                .trim();
        String jstr2 = FileUtils.readFileToString(
                new File(Resources.RECORD_CLIENT_JAR)).trim();

        VictimsRecord rec1 = VictimsRecord.fromJSON(jstr1);
        VictimsRecord rec1clone = VictimsRecord.fromJSON(jstr1);
        VictimsRecord rec2 = VictimsRecord.fromJSON(jstr2);

        assertFalse("ContainsAll negative case", rec1.containsAll(rec2));
        assertTrue("ContainsAll positive case", rec1.containsAll(rec1clone));

        // remove a hash from rec1clone
        for (String key : rec1.getHashes(Algorithms.SHA512).keySet()) {
            rec1clone.getHashes(Algorithms.SHA512).remove(key);
            break;
        }
        assertTrue("ContainsAll subset case", rec1.containsAll(rec1clone));

    }

}
