package ch.hsr.geohash.util;

import ch.hsr.geohash.GeoHash;
import ch.hsr.geohash.WGS84Point;

import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: cne1x
 * Date: 1/20/12
 * Time: 9:52 AM
 *
 * Iterates over a line that has a fixed-distance buffer around it.  That is, given two points,
 * a buffer distance, and a GeoHash resolution (in bits), iterate over all of the GeoHashes at
 * that resolution that are within the buffer distance of the line connecting the two points.
 */
public class LineSegmentBufferGeoHashIterator extends GeoHashIterator {
    private static final double NEARLY_ZERO = 1e-4;
    
    private WGS84Point pointLL;

    private WGS84Point pointUR;

    private double radiusMeters;

    /**
     * Unit vector from point LL to point UR.
     */
    private WGS84Point U;

    /**
     * Unit vector that is perpendicular to the line segment (LL, UR).
     */
    private WGS84Point V;

    /**
     * The current latitude of iteration.
     */
    protected double latitude;

    /**
     * The current longitude of iteration.
     */
    protected double longitude;

    /**
     * Maximum latitude that is valid for iteration.
     */
    protected double maxLatitude;

    /**
     * The minimum longitude that is valid for the current latitude.
     */
    protected double minLongitude;

    /**
     * The maximum longitude that is valid for the current latitude.
     */
    protected double maxLongitude;

    private double radiusInDegrees;

    /**
     * Constructor.  This routine is responsible for identifying the effective bounds of iteration
     * (including the buffer radius), and initializing the base iterator within the ancestor.
     */
    public LineSegmentBufferGeoHashIterator(double latitudeLL, double longitudeLL, double latitudeUR, double longitudeUR, double radiusMeters, int precision) {
        // invoke your ancestor's constructor
        super();

        this.pointLL = new WGS84Point(latitudeLL, longitudeLL);
        this.pointUR = new WGS84Point(latitudeUR, longitudeUR);
        this.radiusMeters = radiusMeters;

        initialize(precision);
    }

    /**
     * Takes care of class-specific initialization, notably computing unit vectors and establishing
     * the initial iteration positions.
     *
     * @param precision the number of bits on which this iterator should compute GeoHashes
     */
    protected void initialize(int precision) {
        // allow your ancestor to take care of the work-in-common
        ArrayList<WGS84Point> points = new ArrayList<WGS84Point>();
        points.add(pointLL);
        points.add(pointUR);
        super.initialize(getBoundingGeoHashes(points, precision, radiusMeters), precision, false);

        // now do class-specific initialization...

        // compute the (rough) equivalent radius in degrees
        radiusInDegrees = convertRadiusInMetersToDegrees(radiusMeters);

        // compute the unit vector from LL to UR
        U = getUnitVectorBetweenPoints(pointLL, pointUR);
        V = getPerpendicularVector(U);
        
        // compute the first (initial, minimum) latitude based on the GeoHash cell to which it belongs
        maxLatitude = composeGeoHashFromBits(latBitsUR, lonBitsUR, latPrecision, lonPrecision).getPoint().getLatitude();
        setLatitude(composeGeoHashFromBits(latBitsLL, lonBitsLL, latPrecision, lonPrecision).getPoint().getLatitude());
        
        // advance
        doesHaveNext = advance();
    }

    /**
     * Pre-fetch the next iteration result.
     *
     * For a given latitude, we know the range of longitudes that are within the given radius,
     * so we use those as the bounds.
     *
     * @return whether a next iteration result could be found
     */
    public boolean advance() {
        boolean isDone = false;

        double distanceToLineInMeters=0.0;

        // identify the next pair of coordinates that is likely to be inside the area of interest
        while (!isDone) {
            // increment the longitude
            longitude += incLongitudeDegrees;

            // have we exceeded the longitudinal bounds for this latitude?
            while (longitude > maxLongitude) {
                setLatitude(latitude + incLatitudeDegrees);
                longitude = minLongitude;

                // if you have exceeded the maximum allowable latitude, you have exhausted the iteration
                if (latitude > maxLatitude) {
                    setCurrentGeoHash(null);
                    return false;
                }
            }

            // update the geohash for the new coordinates
            setCurrentGeoHash(GeoHash.withBitPrecision(latitude, longitude, precision));

            // if you get this far, there exists another GeoHash
            isDone = true;
        }
        
        // if you get this far, you have a valid next GeoHash cell
        return true;
    }

    /**
     * Mutator for the current latitude of iteration.  Also responsible for asking for the horizontal
     * bounds that define the range of GeoHashes that might be within radius.
     *
     * @param newLatitude the new latitude value to assume
     */
    protected void setLatitude(double newLatitude) {
        latitude = GeoHash.withBitPrecision(newLatitude, pointLL.getLongitude(), precision).getPoint().getLatitude();

        // update the longitudinal extremes
        double[] extremes = getSegmentHorizontalExtremes(latitude, pointLL, pointUR);
        minLongitude = extremes[0];
        maxLongitude = extremes[1];
        
        // position the longitude (to something invalid)
        longitude = minLongitude - incLongitudeDegrees;
    }

    public static boolean isPointWithinSegment(WGS84Point q, WGS84Point a, WGS84Point b) {
        if (Math.signum(a.getLatitude()-b.getLatitude()) != Math.signum(a.getLatitude()-q.getLatitude()))
            return false;
        
        if (q.getLatitude() < Math.min(a.getLatitude(),b.getLatitude()))
            return false;
        if (q.getLatitude() > Math.max(a.getLatitude(),b.getLatitude()))
            return false;
        
        return true;
    }

    public static boolean isOnStraightAnswer(WGS84Point q, WGS84Point a, WGS84Point b, WGS84Point v, double radiusInDegrees, int dir) {
        if (isPointWithinSegment(
                q,
                new WGS84Point(
                        a.getLatitude()+dir*radiusInDegrees*v.getLatitude(),
                        a.getLongitude()+dir*radiusInDegrees*v.getLongitude()
                        ),
                new WGS84Point(
                        b.getLatitude()+dir*radiusInDegrees*v.getLatitude(),
                        b.getLongitude()+dir*radiusInDegrees*v.getLongitude()
                )
        )) return true;

        return false;
    }

    public static void considerMinMax(double x, double[] xs) {
        if (Double.isNaN(xs[0]) | (x < xs[0])) xs[0] = x;
        if (Double.isNaN(xs[1]) | (x > xs[1])) xs[1] = x;
    }

    public static void considerCandidate(WGS84Point a, WGS84Point b, WGS84Point v, double Y0, double x, WGS84Point pt, double[] xs, double radiusInDegrees, int dir) {
        WGS84Point q = null;
        
        if ((Y0 >= -90.0) && (Y0 <= 90.0) && (x >= -180.0) && (x <= 180.0))
            q = new WGS84Point(Y0, x);
        
        if (q!=null && isOnStraightAnswer(q, a, b, v, radiusInDegrees, dir)) {
            considerMinMax(x, xs);
        } else {
            double underRad = Math.pow(radiusInDegrees,2.0) - Math.pow(pt.getLatitude()-Y0,2.0);
            if (underRad >= 0) considerMinMax(pt.getLongitude() - Math.sqrt(underRad), xs);
            if (underRad >= 0) considerMinMax(pt.getLongitude() + Math.sqrt(underRad), xs);
        }
    }

    /**
     * Given a Y-value (latitude), a line segment AB, two unit vectors U and V, and a radius in
     * degrees, computes the horizontal bounds on the bounds that could possibly be within
     * radius.
     *
     * @param y0 the latitude band for which the horizontal bounds are sought
     * @param a one of the segment end-points
     * @param b one of the segment end-points
     * @return the minimum and maximum X-values that are in-radius to the line segment
     */
    public double[] getSegmentHorizontalExtremes(double y0, WGS84Point a, WGS84Point b) {
        // initialize candidates for this Y-value
        double[] xs = {Double.NaN, Double.NaN};

        double posLift = (Math.abs(U.getLatitude()) > NEARLY_ZERO) ? (U.getLongitude()*(y0 - a.getLatitude() + (radiusInDegrees*V.getLatitude()))/U.getLatitude()) : Double.NaN;
        double negLift = (Math.abs(U.getLatitude()) > NEARLY_ZERO) ? (U.getLongitude()*(y0 - a.getLatitude() - (radiusInDegrees*V.getLatitude()))/U.getLatitude()) : Double.NaN;

        // consider min/max candidates
        considerCandidate(a, b, V, y0, a.getLongitude() - (radiusInDegrees*V.getLongitude()) + posLift, a, xs, radiusInDegrees, -1);
        considerCandidate(a, b, V, y0, a.getLongitude() + (radiusInDegrees*V.getLongitude()) + negLift, a, xs, radiusInDegrees, +1);
        considerCandidate(a, b, V, y0, a.getLongitude() - (radiusInDegrees*V.getLongitude()) + posLift, b, xs, radiusInDegrees, -1);
        considerCandidate(a, b, V, y0, a.getLongitude() + (radiusInDegrees*V.getLongitude()) + negLift, b, xs, radiusInDegrees, +1);

        if (Double.isNaN(xs[0]) && Double.isNaN(xs[1])) return new double[]{1.0, 0.0};
        if (Double.isNaN(xs[0])) xs[0] = xs[1];
        if (Double.isNaN(xs[1])) xs[1] = xs[0];

        // standardize the coordinates to values that align nicely with GeoHashes
        xs[0] = ghBounds[0].getPoint().getLongitude() + Math.round((xs[0] - ghBounds[0].getPoint().getLongitude()) / incLongitudeDegrees) * incLongitudeDegrees;
        xs[1] = ghBounds[0].getPoint().getLongitude() + Math.round((xs[1] - ghBounds[0].getPoint().getLongitude()) / incLongitudeDegrees) * incLongitudeDegrees;

        return xs;
    }

    /**
     * Computes the unit vector from A to B.
     *
     * @param a the starting point of the line segment
     * @param b the ending point of the line segment
     * @return the unit vector from A to B
     */
    public static WGS84Point getUnitVectorBetweenPoints(WGS84Point a, WGS84Point b) {
        double len = java.lang.Math.hypot(a.getLongitude()-b.getLongitude(), a.getLatitude()-b.getLatitude());

        return new WGS84Point(
                (b.getLatitude() - a.getLatitude()) / len,
                (b.getLongitude() - a.getLongitude()) / len
        );
    }

    /**
     * Computes a perpendicular vector to the one given.
     *
     * @param u the input vector
     * @return the vector perpendicular to the input vector
     */
    public static WGS84Point getPerpendicularVector(WGS84Point u) {
        double dx = 0.0;
        double dy = 0.0;

        if (Math.abs(u.getLatitude()) < Math.abs(u.getLongitude())) {
            dx = u.getLatitude();
            dy = -1.0 * u.getLongitude();
        } else {
            dx = -1.0 * u.getLatitude();
            dy = u.getLongitude();
        }

        return new WGS84Point(dy, dx);
    }

    /**
     * @return the approximate distance between the given point and this line segment
     */
    public double getDistanceFromPointInMeters(WGS84Point point) {
        double distanceInMeters = 0.0;

        double distanceInDegrees = Math.abs(((U.getLatitude()*(point.getLongitude() - pointLL.getLongitude())) + (U.getLongitude() * (pointLL.getLatitude() - point.getLatitude()))) / ((V.getLatitude() * U.getLongitude()) - (V.getLongitude() * U.getLatitude())));
        double k = ((V.getLatitude() * (point.getLongitude() - pointLL.getLongitude())) + (V.getLongitude() * (pointLL.getLatitude() - point.getLatitude()))) / ((U.getLongitude() * V.getLatitude()) - (U.getLatitude() * V.getLongitude()));

        // if the intersection is outside of the line's bounds
        if (k < 0.0) {
            distanceInMeters = getSegmentLengthInMeters(point, pointLL);
        } else if (k > getSegmentLengthInDegrees(pointLL, pointUR)) {
            distanceInMeters = getSegmentLengthInMeters(point, pointUR);
        } else {
            // make an educated guess as to how this distance in degrees relates to the distance in meters
            distanceInMeters = convertRadiusInDegreesToMeters(distanceInDegrees);
        }

        return distanceInMeters;
    }
}
