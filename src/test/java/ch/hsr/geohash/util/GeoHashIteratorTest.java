package ch.hsr.geohash.util;

import ch.hsr.geohash.GeoHash;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Created with IntelliJ IDEA.
 * User: cne1x
 * Date: 5/17/12
 * Time: 1:10 PM
 * <p/>
 * Test drives some of the GeoHashIterator methods for sanity's sake,
 * primarily some of the base geographic manipulation functions.
 */
public class GeoHashIteratorTest {
    // re-used GeoHashIterator implementation
    protected TestGHI ghi = new TestGHI();

    // number of GeoHash bits precision to use
    int precision = 20;

    /**
     * The class we are testing is abstract, so stub-in the abstract
     * method (that we don't care about for these tests anyway).
     */
    class TestGHI extends GeoHashIterator {
        protected boolean advance() {
            return false;
        }
    }

    /**
     * Initialize the GeoHashIterator that can be re-used among the tests.
     */
    @Before
    public void setup() {
        GeoHash ghLL = GeoHash.withBitPrecision(30.0, 60.0, precision);
        GeoHash ghUR = GeoHash.withBitPrecision(40.0, 70.0, precision);
        boolean shouldAdvance = false;

        ghi.initialize(new GeoHash[]{ghLL, ghUR}, precision, shouldAdvance);
    }

    /**
     * Test-drive the computation of how many GeoHash cells exist over the
     * rectangular region (LL, UR).
     */
    @Test
    public void testNumberOfResults() {
        Assert.assertEquals("Number of latitude-cells did not agree", 58, ghi.getSpanBitsLatitude());
        Assert.assertEquals("Number of longitude-cells did not agree", 30, ghi.getSpanBitsLongitude());
    }

    /**
     * Verify that the GeoHash precision translates to a precision-in-meters at a few points up
     * the latitude scale.  The measure should be constant for latitude, but should decrease for
     * longitude as the latitude varies from 0 to almost 90 degrees.
     */
    @Test
    public void testDimensionPrecision() {
        // near-latitude, latitude-precision, longitude-precision
        double[][] expectedResults = {
                {0.0, 19535.15625, 245898.10205},
                {30.0, 19535.15625, 212954.00311},
                {60.0, 19535.15625, 122949.05102},
                {89.0, 19535.15625, 4291.51362}
        };

        // loop over all test cases
        for (int i = 0; i < expectedResults.length; i++) {
            Assert.assertEquals("", expectedResults[i][1], GeoHashIterator.getDimensionPrecisionInMeters(expectedResults[i][0], true, precision >> 1), 1e-5);
            Assert.assertEquals("", expectedResults[i][2], GeoHashIterator.getDimensionPrecisionInMeters(expectedResults[i][0], false, (1 + precision) >> 1), 1e-5);
        }
    }

    /**
     * The TrackGeoHashIterator uses some of the meters-to-degrees functions,
     * so we might as well test-drive those, too.  These are innately silly
     * methods that should not be relied upon to give particularly meaningful
     * results.
     */
    @Test
    public void testRadiusDegreesAndMeters() {
        System.out.println("Testing radius degrees and meters...");

        for (double meters = 1.0e5; meters >= 10.0; meters /= 2.0) {
            double m2d = GeoHashIterator.convertRadiusInMetersToDegrees(meters);
            double d2m = GeoHashIterator.convertRadiusInDegreesToMeters(m2d);
            double error = Math.abs(d2m - meters);
            double errorPercent = error / meters;

            System.out.println("  " + meters + " meters -> " + m2d + " degrees -> " + d2m + " meters");

            Assert.assertTrue("Round trip conversion between degrees and meters failed to match; error " + Double.toString(d2m) + "/" + Double.toString(meters) + " (" + Double.toString(100.0 * errorPercent) + "%)", errorPercent <= 0.05);
        }
    }
}
