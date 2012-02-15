package ch.hsr.geohash.util;

import ch.hsr.geohash.GeoHash;
import ch.hsr.geohash.WGS84Point;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by IntelliJ IDEA.
 * User: cne1x
 * Date: 1/20/12
 * Time: 11:09 AM
 *
 * Test driver for the RectangleGeoHashIterator class.
 */
public class RectangleGeoHashIteratorTest {
    @Test
    public void testSuccessfulIteration() {
        // test parameters
        double longitude = 60.0;
        double latitude = 35.0;
        double radiusMeters = 500.0;
        int bitsPrecision = 35;
        WGS84Point ptCenter = new WGS84Point(latitude, longitude);
        WGS84Point ptLL = VincentyGeodesy.moveInDirection(ptCenter, 225.0, radiusMeters*707./500.0);
        WGS84Point ptUR = VincentyGeodesy.moveInDirection(ptCenter, 45.0, radiusMeters*707.0/500.0);

        System.out.println("[RectangleGeoHashIterator]  On circle test-bed from " + ptLL + " to " + ptUR + "...");

        // create an iterator
        RectangleGeoHashIterator rghi = new RectangleGeoHashIterator(
                ptLL.getLatitude(), ptLL.getLongitude(),
                ptUR.getLatitude(), ptUR.getLongitude(),
                bitsPrecision
        );
        Assert.assertNotNull("Could not instantiate RectangleGeoHashIterator.", rghi);

        // iterate over all qualifying GeoHashes
        GeoHash geoHash;
        int count=0;
        while (rghi.hasNext()) {
            // grab the next GeoHash
            geoHash = rghi.next();
            Assert.assertNotNull("Fetched a NULL GeoHash.", geoHash);

            // dump it
            count++;
            System.out.println("RectangleGHI " + count + ".  " + geoHash);
        }

        // there should have been 72 qualifying GeoHashes meeting these criteria
        Assert.assertEquals("Expected 32 GeoHashes in the result set; found " + Integer.toString(count) + ".", count, 72);
    }
}
