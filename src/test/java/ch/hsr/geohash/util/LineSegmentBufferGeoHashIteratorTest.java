package ch.hsr.geohash.util;

import ch.hsr.geohash.GeoHash;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by IntelliJ IDEA.
 * User: cne1x
 * Date: 1/23/12
 * Time: 11:53 AM
 *
 * Tests the LineSegmentBufferGeoHashIterator.
 */
public class LineSegmentBufferGeoHashIteratorTest {
    @Test
    public void testHorizontalLeftToRightCount() {
        Assert.assertEquals("Bad GeoHash count, horizontal L->R.", 291, getSuccessfulIterationCount(35.0, 60.0, 35.0, 60.01, true));
    }

    @Test
    public void testHorizontalSymmetryCount() {
        Assert.assertEquals("Bad GeoHash count, horizontal L->R vs R->L.", getSuccessfulIterationCount(35.0, 60.0, 35.0, 60.01, false), getSuccessfulIterationCount(35.0, 60.01, 35.0, 60.0, false));
    }

    @Test
    public void testVerticalTopToBottomCount() {
        Assert.assertEquals("Bad GeoHash count, vertical T->B.", 290, getSuccessfulIterationCount(35.01, 60.0, 35.0, 60.0, true));
    }

    @Test
    public void testVerticalSymmetryCount() {
        Assert.assertEquals("Bad GeoHash count, vertical T->B vs B->T.", getSuccessfulIterationCount(35.01, 60.0, 35.0, 60.0, false), getSuccessfulIterationCount(35.0, 60.0, 35.01, 60.0, false));
    }

    @Test
    public void testDiagonalLLToURCount() {
        Assert.assertEquals("Bad GeoHash count, diagonal LL->UR.", 346, getSuccessfulIterationCount(35.0, 60.0, 35.01, 60.01, true));
    }

    @Test
    public void testDiagonalLLRRSymmetryCount() {
        Assert.assertEquals("Bad GeoHash count, diagonal LL->UR vs UR->LL.", getSuccessfulIterationCount(35.0, 60.0, 35.01, 60.01, false), getSuccessfulIterationCount(35.01, 60.01, 35.0, 60.0, false));
    }

    @Test
    public void testDiagonalLRToULCount() {
        Assert.assertEquals("Bad GeoHash count, diagonal LR->UL.", 336, getSuccessfulIterationCount(35.0, 60.01, 35.01, 60.0, true));
    }

    @Test
    public void testDiagonalLRULSymmetryCount() {
        Assert.assertEquals("Bad GeoHash count, diagonal LR->UL vs UL->LR.", getSuccessfulIterationCount(35.0, 60.01, 35.01, 60.0, false), getSuccessfulIterationCount(35.01, 60.0, 35.0, 60.01, false));
    }

    public int getSuccessfulIterationCount(double lat0, double lon0, double lat1, double lon1, boolean isVerbose) {
        // test parameters
        double radiusMeters = 1000.0;
        int bitsPrecision = 35;

        if (isVerbose) System.out.println("\nTesting LineSegmentBufferGeoHashIterator over (" + lat0 +"," + lon0 + ") to (" + lat1 + "," + lon1 + ") @ " + bitsPrecision + " bits; radius of " + radiusMeters + " meters...");
        
        // create an iterator
        LineSegmentBufferGeoHashIterator lsbghi = new LineSegmentBufferGeoHashIterator(
                lat0, lon0,
                lat1, lon1,
                radiusMeters,
                bitsPrecision
        );
        if (lsbghi==null) return 0;

        // iterate over all qualifying GeoHashes
        GeoHash geoHash;
        int count=0;
        while (lsbghi.hasNext()) {
            // grab the next GeoHash
            geoHash = lsbghi.next();
            if (geoHash==null) return 0;

            // dump it, if applicable
            count++;
            if (isVerbose) System.out.println("  LineSegmentBufferGHI " + count + ".  \t\"" + geoHash.toBase32() + "\"," + "  " + geoHash);
        }
        
        return count;
    }
    
    @Test
    public void testRadiusInDegreesToMeters() {
        Assert.assertEquals("Could not convert radius from degrees to meters.", 4983.73, LineSegmentBufferGeoHashIterator.convertRadiusInDegreesToMeters(0.05), 0.01);
    }

    @Test
    public void testRadiusConversionEquality() {
        double degrees = 0.05;

        double f0_meters = LineSegmentBufferGeoHashIterator.convertRadiusInDegreesToMeters(degrees);
        double f1_degrees = LineSegmentBufferGeoHashIterator.convertRadiusInMetersToDegrees(f0_meters);

        Assert.assertTrue(
                "Round-trip radius conversions from degrees (" + degrees + ") -> meters (" + f0_meters + ") -> degrees (" + f1_degrees + ") do not agree sufficiently.",
                Math.abs(degrees - f1_degrees) <= 0.001
        );
    }
}
