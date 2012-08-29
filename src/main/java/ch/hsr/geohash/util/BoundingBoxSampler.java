package ch.hsr.geohash.util;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import ch.hsr.geohash.GeoHash;

/**
 * Select random samples of geohashes within a bounding box, without replacement
 */
public class BoundingBoxSampler {
	private TwoGeoHashBoundingBox boundingBox;
	private Set<Integer> alreadyUsed = new HashSet<Integer>();
	private int maxSamples;
    private boolean withReplacement = false;
	private Random rand = new Random();

	/**
	 * @param bbox
	 * @throws IllegalArgumentException
	 *             if the number of geohashes contained in the bounding box
	 *             exceeds Integer.MAX_VALUE
	 */
	public BoundingBoxSampler(TwoGeoHashBoundingBox bbox) {
        this(bbox, false);
    }


    public BoundingBoxSampler(TwoGeoHashBoundingBox bbox, boolean withReplacement) {
        this.boundingBox = bbox;
        this.withReplacement = withReplacement;
        long maxSamplesLong = GeoHash.stepsBetween(bbox.getBottomLeft(), bbox.getTopRight());
        if (maxSamplesLong > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("This bounding box is too big too sample using this algorithm");
        }
        maxSamples = (int) maxSamplesLong;
    }

    public BoundingBoxSampler(TwoGeoHashBoundingBox bbox, long seed) {
   		this(bbox);
   		this.rand = new Random(seed);
   	}

    public BoundingBoxSampler(TwoGeoHashBoundingBox bbox, long seed, boolean withReplacement) {
   		this(bbox, withReplacement);
   		this.rand = new Random(seed);
   	}


    public TwoGeoHashBoundingBox getBoundingBox() {
        return boundingBox;
    }

    /**
	 * @return next sample, or NULL if all samples have been returned
	 */
	public GeoHash next() {
        if(!withReplacement) {
            if (alreadyUsed.size() == maxSamples) {
                return null;
            }
        }
        GeoHash gh = getNextGeoHash();
		while (!boundingBox.getBoundingBox().contains(gh.getPoint())) {
			gh = getNextGeoHash();
		}
		return gh;
	}

    private GeoHash getNextGeoHash() {
        int idx = rand.nextInt(maxSamples + 1);
        if(!withReplacement) {
            while (alreadyUsed.contains(idx)) {
                idx = rand.nextInt(maxSamples + 1);
            }
            alreadyUsed.add(idx);
        }
        return boundingBox.getBottomLeft().next(idx);
    }
}
