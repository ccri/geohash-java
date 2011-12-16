package ch.hsr.geohash.util;

import ch.hsr.geohash.GeoHash;
import ch.hsr.geohash.WGS84Point;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by IntelliJ IDEA.
 * User: cne1x
 * Date: 12/15/11
 * Time: 3:38 PM
 * Simple test case.
 */
public class RadialGeoHashIteratorTest {
    @Test
    public void testIter() {
        // test parameters
        double longitude = 60.0;
        double latitude = 35.0;
        double radiusMeters = 500.0;
        int bitsPrecision = 35;
        WGS84Point ptCenter = new WGS84Point(latitude, longitude);

        // instantiate the iterator
        RadialGeoHashIterator rghi = new RadialGeoHashIterator(latitude, longitude, radiusMeters, bitsPrecision);
        Assert.assertNotNull("Could not instantiate RadialGeoHashIterator as expected.", rghi);

        // iterate over all qualifying GeoHashes
        GeoHash geoHash;
        int count=0;
        while (rghi.hasNext()) {
            // grab the next GeoHash
            geoHash = rghi.next();
            Assert.assertNotNull("Fetched a NULL GeoHash.", geoHash);

            // ensure that this GeoHash's center point is, in fact, within the specified radius
            double distanceMeters = VincentyGeodesy.distanceInMeters(ptCenter, geoHash.getBoundingBoxCenterPoint());
            Assert.assertTrue("Fetched point is " + Double.toString(distanceMeters) + " meters, outside of the specified radius of " + Double.toString(radiusMeters) + " meters.", distanceMeters <= radiusMeters);

            count++;

            System.out.println("RGHI " + count + ".  " + geoHash.toBase32() + " = " + distanceMeters + " meters");
        }

        // there should have been 21 qualifying GeoHashes meeting these criteria
        Assert.assertEquals("Expected 21 GeoHashes in the result set; found " + Integer.toString(count) + ".", count, 21);
    }
}
