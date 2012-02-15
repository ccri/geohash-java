package ch.hsr.geohash.util;

import ch.hsr.geohash.WGS84Point;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: cne1x
 * Date: 2/9/12
 * Time: 2:05 PM
 *
 * Drives the iteration over GeoHashes within a track.
 */
public class TrackGeoHashIteratorTest {
    @Test
    public void testInstantiation() {
        ArrayList<WGS84Point> points = new ArrayList<WGS84Point>();
        points.add(new WGS84Point(35.00, 60.00));
        TrackGeoHashIterator tghi = new TrackGeoHashIterator(points, null, 500.0, 35);

        Assert.assertNotNull("Could not instantiate a valid TrackGeoHashIterator.", tghi);
    }

    @Test
    public void testSinglePoint() {
        ArrayList<WGS84Point> points = new ArrayList<WGS84Point>();
        points.add(new WGS84Point(35.00, 60.00));
        
        int count = getIteratedGeoHashCount(
            points,
            "single-point"
        );

        Assert.assertEquals("Single-point GeoHash response count did not match expectation.", 48, count);
    }

    @Test
    public void testHorizontal() {
        int count = getIteratedGeoHashCount(
            Arrays.asList(
                new WGS84Point(35.00, 60.00),
                new WGS84Point(35.00, 60.01)
            ),
            "horizontal"
        );

        Assert.assertEquals("Horizontal GeoHash response count did not match expectation.", 98, count);
    }

    @Test
    public void testHorizontalSymmetry() {
        int countLR = getIteratedGeoHashCount(
                Arrays.asList(
                        new WGS84Point(35.00, 60.00),
                        new WGS84Point(35.00, 60.01)
                ),
                "horizontal L->R"
        );
        int countRL = getIteratedGeoHashCount(
                Arrays.asList(
                        new WGS84Point(35.00, 60.01),
                        new WGS84Point(35.00, 60.00)
                ),
                "horizontal R->L"
        );

        Assert.assertEquals("Horizontal L->R GeoHash response count did not match horizontal R->L count.", countLR, countRL);
    }

    @Test
    public void testVertical() {
        int count = getIteratedGeoHashCount(
                Arrays.asList(
                        new WGS84Point(35.01, 60.00),
                        new WGS84Point(35.00, 60.00)
                ),
                "vertical"
        );

        Assert.assertEquals("Vertical GeoHash response count did not match expectation.", 105, count);
    }

    @Test
    public void testVerticalSymmetry() {
        int countTB = getIteratedGeoHashCount(
                Arrays.asList(
                        new WGS84Point(35.01, 60.00),
                        new WGS84Point(35.00, 60.00)
                ),
                "vertical T->B"
        );
        int countBT = getIteratedGeoHashCount(
                Arrays.asList(
                        new WGS84Point(35.00, 60.00),
                        new WGS84Point(35.01, 60.00)
                ),
                "vertical B->T"
        );

        Assert.assertEquals("Vertical T->B GeoHash response count did not match vertical B->T count.", countTB, countBT);
    }

    /**
     * Utility method that is shared by some of the shape-specific tests above.
     *
     * @param points the points that define the track to test
     * @param label the label to be output so that the track-test can be identified
     * @return the number of GeoHashes that the iterator on this track returned
     */
    public int getIteratedGeoHashCount(List<WGS84Point> points, String label) {
        double radiusInMeters = 500.0;
        int geoHashPrecisionInBits = 35;
        WGS84Point[] filterRectangle = null;

        TrackGeoHashIterator tghi = new TrackGeoHashIterator(points, filterRectangle, radiusInMeters, geoHashPrecisionInBits);

        int count=0;
        while (tghi.hasNext()) {
            System.out.println("[TrackGHI " + label + "] " + (++count) + ".  " + tghi.next());
        }
        
        return count;
    }
}
