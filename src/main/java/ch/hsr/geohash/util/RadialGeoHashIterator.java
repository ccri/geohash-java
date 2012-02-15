package ch.hsr.geohash.util;

import ch.hsr.geohash.WGS84Point;

/**
 * Created by IntelliJ IDEA.
 * User: cne1x
 * Date: 12/15/11
 * Time: 3:23 PM
 *
 * Built upon a rectangular iterator, this class makes some estimates about in-circle
 * membership.
 */
public class RadialGeoHashIterator extends RectangleGeoHashIterator {
    private double radiusMeters;

    private double distanceInMeters = Double.NaN;

    private WGS84Point centerPoint = null;

    public RadialGeoHashIterator(double latitude, double longitude, double radiusMeters, int precision) {
        super();

        this.radiusMeters = radiusMeters;

        this.centerPoint = new WGS84Point(latitude, longitude);
        
        double effectiveRadiusMeters = Math.hypot(
                radiusMeters+0.6*getPrecisionInMetersLatitude(precision),
                radiusMeters+0.6*getPrecisionInMetersLongitude(precision)
        );
        
        System.out.println("Radius (m):  " + radiusMeters);
        System.out.println("Effective radius (m):  " + effectiveRadiusMeters);

        WGS84Point ptLL = VincentyGeodesy.moveInDirection(centerPoint, 225.0, effectiveRadiusMeters);
        WGS84Point ptUR = VincentyGeodesy.moveInDirection(centerPoint, 45.0, effectiveRadiusMeters);

        // invoke your ancestor's initializer that creates a GeoHash iterator over the bounding rectangle
        super.initialize(
                ptLL.getLatitude(), ptLL.getLongitude(),
                ptUR.getLatitude(), ptUR.getLongitude(),
                precision
        );
    }

    public boolean advance() {
        boolean isDone = false;

        // identify the next pair of coordinates that is likely to be inside the circle of interest
        while (!isDone) {
            // if you've run out of GeoHashes within the bounding rectangle, you're done
            if (!super.advance()) {
                distanceInMeters = Double.NaN;
                return false;
            }

            // this GeoHash is inside the bounding rectangle, but may not be within our circle...

            // compute the distance between the center point and the middle of the current GeoHash
            distanceInMeters = VincentyGeodesy.distanceInMeters(centerPoint, new WGS84Point(getCurrentGeoHash().getPoint()));
            isDone = (distanceInMeters <= radiusMeters);
        }

        // if you get this far, you have a valid next GeoHash cell
        return true;
    }

    /**
     * Returns the estimated distance of the current GeoHash from the center point.
     *
     * @return the estimated distance of the current GeoHash from the center point
     */
    public double getDistanceInMeters() {
        return distanceInMeters;
    }

}

