package ch.hsr.geohash.util;

import ch.hsr.geohash.GeoHash;
import ch.hsr.geohash.WGS84Point;

import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: cne1x
 * Date: 2/2/12
 * Time: 1:34 PM
 *
 * A track is a collection of points (akin to a path).  Given a buffer-radius, this iterator presents
 * all of the unique GeoHashes that are within the radial distance of any segment on the track.
 * Optionally, a filtering rectangle can be supplied, so that only the GeoHashes that fall within
 * this rectangle are part of the iteration.
 *
 * The general idea is that this iterator should work much the same way you squeeze a tube of
 * toothpaste:  As the unique GeoHashes at the bottom are used up, the current latitude of
 * iteration should increase to the next level; repeat from the bottom of the bounded area to
 * the top.  (Note:  If you happen to be one of those people who squeezes the toothpaste tube
 * in the middle, then please accept both my sympathies as well as a substitute metaphor of
 * your own choosing.)  This works only because the LineSegmentBufferGeoHashIterators all
 * work from the bottom (lower latitude) up to the top (higher latitude).
 *
 * The intent is this:  Even if a single GeoHash is implicated by more than one line segment,
 * it should be reported exactly once.  If the code works as intended, the GeoHashes will be
 * returned from bottom-to-top, left-to-right.
 */
public class TrackGeoHashIterator extends GeoHashIterator {
    /**
     * Utility class to represent a 2-point line segment.
     */
    class Segment {
        WGS84Point LL;
        WGS84Point UR;
        WGS84Point a;
        WGS84Point b;

        /**
         * Simple constructor.
         * @param a one segment end-point
         * @param b another segment end-point
         */
        public Segment(WGS84Point a, WGS84Point b) {
            super();

            this.a = a;
            this.b = b;
            LL = new WGS84Point(Math.min(a.getLatitude(), b.getLatitude()), Math.min(a.getLongitude(), b.getLongitude()));
            UR = new WGS84Point(Math.max(a.getLatitude(), b.getLatitude()), Math.max(a.getLongitude(), b.getLongitude()));
        }

        /**
         * Quick-and-dirty test for intersection.  This is not perfect, as it treats the segment as a
         * rectangle, but it allows us to filter out some segments quickly.
         *
         * @param filterRectangle the rectangle with which to test intersection
         * @return whether the given segment's bounding box overlaps the filter-rectangle
         */
        public boolean intersects(WGS84Point[] filterRectangle) {
            if (filterRectangle==null) return true;
            if (filterRectangle.length != 2) return true;

            if (UR.getLatitude() < Math.min(filterRectangle[0].getLatitude(), filterRectangle[1].getLatitude())) return false;
            if (UR.getLongitude() < Math.min(filterRectangle[0].getLongitude(), filterRectangle[1].getLongitude())) return false;
            if (LL.getLatitude() > Math.max(filterRectangle[0].getLatitude(), filterRectangle[1].getLatitude())) return false;
            if (LL.getLongitude() > Math.max(filterRectangle[0].getLongitude(), filterRectangle[1].getLongitude())) return false;

            return true;
        }

        @Override
        public String toString() {
            String s = "Segment(";

            s = s + LL.toString() + ", " + UR.toString();

            return s + ")";
        }
    }


    /**
     * Map of current-iteration latitude to per-segment GeoHash iterators.
     */

    final TreeMap<Double, ArrayList<LineSegmentBufferGeoHashIterator>> segmentIterators = new TreeMap<Double, ArrayList<LineSegmentBufferGeoHashIterator>>();

    /**
     * The current latitude over which we are iterating.
     */
    double currentLatitude;

    /**
     *
     */
    double currentDistance;

    /**
     * The iterator over the current list of GeoHashes for this latitude.
     */
    Iterator<GeoHash> itrCurrentLatitudeGeoHashes;

    /**
     * A mapping from the list of GeoHashes defined for this latitude to the minimum distance from any segment.
     */
    private final HashMap<GeoHash, Double> mapGeoHashToMinDistance = new HashMap<GeoHash, Double>();

    /**
     * Buffer size in meters.
     */
    double radiusInMeters;

    /**
     * A suitably small number of degrees that we can treat it as essentially zero.
     */
    public final static double NEARLY_ZERO_DEGREES = GeoHashIterator.convertRadiusInMetersToDegrees(0.1);

    /**
     * Simple constructor.
     */
    public TrackGeoHashIterator(Iterable<WGS84Point> pointsGiven, WGS84Point[] filterRectangle, double radiusInMeters, int precision) {
        super();

        currentLatitude = Double.MAX_VALUE;
        currentDistance= Double.MAX_VALUE;
        this.radiusInMeters = radiusInMeters;

        // copy the points
        ArrayList<WGS84Point> points = new ArrayList<WGS84Point>();
        for (WGS84Point point : pointsGiven) points.add(point);

        // ensure there are at least two points
        if (points.size() == 1) {
            points.add(new WGS84Point(
                    points.get(0).getLatitude() + NEARLY_ZERO_DEGREES,
                    points.get(0).getLongitude() + NEARLY_ZERO_DEGREES
            ));
        }

        // loop over all segments
        WGS84Point lastPoint = null;
        for (WGS84Point point : points) {
            // if you have a previous point, use it
            if (lastPoint!=null) {
                // construct the segment
                Segment segment = new Segment(lastPoint, point);

                // only process segments that pass the geographic filter
                if (filterRectangle==null || segment.intersects(filterRectangle)) {
                    // create this single-line-segment GeoHash iterator
                    LineSegmentBufferGeoHashIterator segIter = new LineSegmentBufferGeoHashIterator(
                            segment.a.getLatitude(), segment.a.getLongitude(),
                            segment.b.getLatitude(), segment.b.getLongitude(),
                            radiusInMeters,
                            precision
                    );

                    // ensure that this iterator has at least one GeoHash in it
                    if (segIter.hasNext()) {
                        // fetch the latitude of the iterator's current GeoHash
                        double latitude = segIter.getCurrentGeoHash().getPoint().getLatitude();

                        // update the initial latitude
                        currentLatitude = Math.min(currentLatitude, latitude);

                        // store this iterator with any others that are associated with this (current) latitude
                        ArrayList<LineSegmentBufferGeoHashIterator> list;
                        if (!segmentIterators.containsKey(latitude)) {
                            list = new ArrayList<LineSegmentBufferGeoHashIterator>();
                        } else {
                            list = segmentIterators.get(latitude);
                        }
                        list.add(segIter);
                        segmentIterators.put(latitude, list);
                    }
                }
            }

            // update the last point
            lastPoint = point;
        }

        // if no segments passed through the filter, you have nothing to do
        if (segmentIterators.size() < 1) {
            doesHaveNext = false;
        } else {
            // initialize
            GeoHash[] ghBounds = getBoundingGeoHashes(points, precision, radiusInMeters);
            initialize(ghBounds, precision, false);
            refreshCurrentLatitudeIterator();
            doesHaveNext = advance();
        }
    }

    /**
     *
     */

    protected boolean refreshCurrentLatitudeIterator() {
        // clear out the state pertaining to the current latitude
        mapGeoHashToMinDistance.clear();

        if (segmentIterators.containsKey(currentLatitude)) {
            // list of segment-iterators whose map positions will need to be updated
            List<LineSegmentBufferGeoHashIterator> toUpdate = new ArrayList<LineSegmentBufferGeoHashIterator>();

            // iterate over all per-segment iterators whose minimum GeoHash exists at this current latitude
            for (LineSegmentBufferGeoHashIterator segItr : segmentIterators.get(currentLatitude)) {
                // add all of the GeoHashes at this latitude to the map of uniques, remembering the minimum GeoHash-to-segment distance for each
                while (segItr.hasNext() && segItr.getCurrentGeoHash().getPoint().getLatitude()==currentLatitude) {
                    // accumulate this GeoHash, and move the iterator forward one step
                    GeoHash gh = segItr.next();
                    double distance = segItr.getDistanceFromPointInMeters(gh.getPoint());
                    if (mapGeoHashToMinDistance.containsKey(gh)) {
                        mapGeoHashToMinDistance.put(gh, Math.min(mapGeoHashToMinDistance.get(gh), distance));
                    } else {
                        mapGeoHashToMinDistance.put(gh, distance);
                    }
                }

                // remember that we will need to update this segment-iterators position in the global map
                toUpdate.add(segItr);
            }

            // set the iterator that ranges over the GeoHashes we encountered for this latitude
            itrCurrentLatitudeGeoHashes = mapGeoHashToMinDistance.keySet().iterator();

            // update the segment-iterators that we exhausted...

            // remove these iterators from their old list (current latitude)
            ArrayList<LineSegmentBufferGeoHashIterator> oldList = segmentIterators.get(currentLatitude);
            oldList.removeAll(toUpdate);
            if (oldList.size() > 0) {
                segmentIterators.put(currentLatitude, oldList);
            } else {
                segmentIterators.remove(currentLatitude);
            }

            // for each of the iterators to update, put it in the next list
            for (LineSegmentBufferGeoHashIterator segItr : toUpdate) {
                // some iterators may have been entirely exhausted; only update those that have at least one more GeoHash
                if (segItr.hasNext()) {
                    ArrayList<LineSegmentBufferGeoHashIterator> list;
                    double latitude = segItr.getCurrentGeoHash().getPoint().getLatitude();

                    if (segmentIterators.containsKey(latitude)) {
                        list = segmentIterators.get(latitude);
                    } else {
                        list = new ArrayList<LineSegmentBufferGeoHashIterator>();
                    }
                    list.add(segItr);
                    segmentIterators.put(latitude, list);
                }
            }

            return true;
        }

        return false;
    }

    /**
     * Utility function to ensure that the next latitude we pick aligns with a GeoHash.
     *
     * @return
     */
    public double getNextLatitude() {
        double latitude = currentLatitude + incLatitudeDegrees;

        return GeoHash.withBitPrecision(latitude, ghBounds[0].getPoint().getLongitude(), precision).getPoint().getLatitude();
    }

    /**
     * Pre-fetch the next iteration result.
     *
     * @return whether a next iteration result could be found
     */
    public boolean advance() {
        if (itrCurrentLatitudeGeoHashes==null) return false;

        boolean isDone = false;

        // update the "current" distance
        if (getCurrentGeoHash()!=null) {
            if (mapGeoHashToMinDistance==null) System.err.println("Invalid mapGeoHashToMinDistance in advance!");
            if (!mapGeoHashToMinDistance.containsKey(getCurrentGeoHash())) System.err.println("Current GeoHash not in distance map:  " + getCurrentGeoHash());
            else currentDistance = mapGeoHashToMinDistance.get(getCurrentGeoHash());
        }

        while (!isDone) {
            // ensure that you have a valid list of GeoHashes to iterate over
            if (!itrCurrentLatitudeGeoHashes.hasNext()) {
                // reload the list of GeoHashes from the segments
                if (!refreshCurrentLatitudeIterator()) {
                    // you have exhausted the current latitude's iterator, so move it on up
                    currentLatitude = getNextLatitude();

                    // you have moved past the end of the available GeoHashes
                    if (currentLatitude > ghBounds[1].getPoint().getLatitude()) {
                        setCurrentGeoHash(null);
                        return false;
                    }
                }
            } else {
                // you have a valid GeoHash, so queue it up
                setCurrentGeoHash(itrCurrentLatitudeGeoHashes.next());
                isDone = true;
            }
        }

        return true;
    }

    /**
     * We have a current GeoHash, but wish to know the minimum distance between its center
     * and any of the segments belonging to this track.  Fortunately, we track this with each of
     * the unique GeoHashes we store for the current latitude.
     *
     * @return the minimum distance between the current GeoHash's center-point and any of
     * the segments belonging to this track
     */
    public double getMinDistanceFromCurrentPointToTrackInMeters() {
        return currentDistance;
    }
}
