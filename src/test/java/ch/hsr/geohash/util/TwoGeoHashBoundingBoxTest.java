package ch.hsr.geohash.util;

import ch.hsr.geohash.GeoHash;
import junit.framework.Assert;
import org.junit.Test;

/**
 * Created with IntelliJ IDEA.
 * User: cne1x
 * Date: 8/21/12
 * Time: 8:38 AM
 *
 * Test class for the TwoGeoHashBoundingBox.
 */

public class TwoGeoHashBoundingBoxTest {
    /**
     * This test is designed to ensure that initializing
     * from odd-length GeoHash strings fails.
     */
    @Test
    public void failOddLengthString() {
        TwoGeoHashBoundingBox bbox = null;
        boolean failed = false;
        try {
            bbox = TwoGeoHashBoundingBox.fromBase32("tm5r15vtm5r15");
        } catch (Exception ex) {
            failed = true;
        }

        Assert.assertNull("TwoGeoHashBoundingBox should have been NULL", bbox);
        Assert.assertTrue("The TwoGeoHashBoundingBox should have failed to initialize, because the input string has an odd length", failed);
    }

    /**
     * This test is designed to ensure that initializing
     * from even-length GeoHash strings succeeds.
     */
    @Test
    public void succeedEvenLengthString() {
        TwoGeoHashBoundingBox bbox = null;
        boolean failed = false;
        try {
            bbox = TwoGeoHashBoundingBox.fromBase32("tq4xjq0tq4xjq5");
        } catch (Exception ex) {
            failed = true;
        }

        Assert.assertNotNull("TwoGeoHashBoundingBox should not have been NULL", bbox);
        Assert.assertFalse("The TwoGeoHashBoundingBox should have initialized successfully, because the input string has an even length", failed);
        Assert.assertEquals("Incorrect number of bits precision in resulting TwoGeoHashBoundingBox", 35, bbox.getBottomLeft().significantBits());
    }

    /**
     * Test defining corners of different base32-string-lengths to
     * ensure that the resulting TwoGeoHashBoundingBox extracts them
     * properly.
     */
    @Test
    public void succeedExtractGeoHashesOfDifferingLengths() {
        String longestBase32LL = "tw1hmg3m5";
        String longestBase32UR = "tw1pnetcu";

        for (int i=4; i<=9; i++) {
            String sLL = longestBase32LL.substring(0, i);
            GeoHash ghLL = GeoHash.fromGeohashString(sLL);

            String sUR = longestBase32UR.substring(0, i);
            GeoHash ghUR = GeoHash.fromGeohashString(sUR);

            TwoGeoHashBoundingBox bboxS = TwoGeoHashBoundingBox.fromBase32(sLL + sUR);
            TwoGeoHashBoundingBox bboxGH = new TwoGeoHashBoundingBox(ghLL, ghUR);

            System.out.println("[TwoGeoHashBoundingBoxTest] strings=(" + sLL + ", " + sUR + "); ghs=(" + ghLL.toBinaryString() + ", " + ghUR.toBinaryString());

            Assert.assertTrue("Bounding boxes were not equal between the string-initialized and geohash-initialized TwoGeoHashBoundingBox's", bboxS.getBoundingBox().equals(bboxGH.getBoundingBox()));
            Assert.assertEquals("Incorrect number of bits precision in resulting string-initialized TwoGeoHashBoundingBox", i*5, bboxS.getBottomLeft().significantBits());
        }
    }
}
