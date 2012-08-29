package ch.hsr.geohash.util;

import ch.hsr.geohash.BoundingBox;
import ch.hsr.geohash.GeoHash;
import ch.hsr.geohash.WGS84Point;
import org.junit.Assert;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;

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
    public void testCorners() {
        int bitsPrecision = 35;

        GeoHash ghBig = GeoHash.fromGeohashString("9q8ys0");
        BoundingBox bbox = ghBig.getBoundingBox();
        double d = 0.0;
        GeoHash ghLL = GeoHash.withBitPrecision(bbox.getMinLat()+d, bbox.getMinLon()+d, bitsPrecision);
        GeoHash ghUR = GeoHash.withBitPrecision(bbox.getMaxLat()-d, bbox.getMaxLon()-d, bitsPrecision);

        List<WGS84Point> list = new LinkedList<WGS84Point>();
        list.add(ghLL.getPoint());
        list.add(ghUR.getPoint());

        GeoHash[] ghBounds = GeoHashIterator.getBoundingGeoHashes(list, bitsPrecision, 0.0);
        //Assert.assertEquals("[RGHI.testCorners] LL boundary of the RGHI does not match", ghLL.toBinaryString(), ghBounds[0].toBinaryString());
        //Assert.assertEquals("[RGHI.testCorners] UR boundary of the RGHI does not match", ghUR.toBinaryString(), ghBounds[1].toBinaryString());
        //Assert.assertEquals("[RGHI.testCorners] LL does not start with right prefix", ghBig.toBase32(), ghBounds[0].toBase32().substring(0, 6));
        //Assert.assertEquals("[RGHI.testCorners] UR does not start with right prefix", ghBig.toBase32(), ghBounds[1].toBase32().substring(0, 6));
        //Assert.fail("LL:  " + ghBounds[0].toString());
        //Assert.fail("UR:  " + ghBounds[1].toString());

        // create an iterator
        RectangleGeoHashIterator rghi = new RectangleGeoHashIterator(
                bbox.getMinLat()+d, bbox.getMinLon()+d,
                bbox.getMaxLat()-d, bbox.getMaxLon()-d,
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

            WGS84Point pt = geoHash.getBoundingBoxCenterPoint();
            String s = geoHash.toString() + " == (" + pt.getLatitude() + "," + pt.getLongitude() + ") <-> (" + bbox.getMinLat() + "," + bbox.getMinLon() + "::" + bbox.getMaxLat() + "," + bbox.getMaxLon() + ")";
            Assert.assertEquals("Latitude too low "+s, false, pt.getLatitude() < bbox.getMinLat());
            Assert.assertEquals("Latitude too high "+s, false, pt.getLatitude() > bbox.getMaxLat());
            Assert.assertEquals("Longitude too low "+s, false, pt.getLongitude() < bbox.getMinLon());
            Assert.assertEquals("Longitude too high "+s, false, pt.getLongitude() > bbox.getMaxLon());
        }

        // there should have been 32 qualifying GeoHashes meeting these criteria
        Assert.assertEquals("Wrong number of RGHI iterations found", 32, count);
    }


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

        // ensure that the corners are where you would expect
        GeoHash ghLL = GeoHash.withBitPrecision(ptLL.getLatitude(), ptLL.getLongitude(), bitsPrecision);
        GeoHash ghUR = GeoHash.withBitPrecision(ptUR.getLatitude(), ptUR.getLongitude(), bitsPrecision);
        List<WGS84Point> list = new LinkedList<WGS84Point>();
        list.add(ptLL);
        list.add(ptUR);
        GeoHash[] ghBounds = rghi.getBoundingGeoHashes(list, bitsPrecision, 0.0);
        Assert.assertEquals("LL boundary of the RGHI does not match", ghLL.toBinaryString(), ghBounds[0].toBinaryString());
        Assert.assertEquals("UR boundary of the RGHI does not match", ghUR.toBinaryString(), ghBounds[1].toBinaryString());

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
        Assert.assertEquals("Wrong number of RGHI iterations found", 72, count);
    }
}
