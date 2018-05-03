package org.springframework.content.commons.renditions;

/**
 * Oggetto definisce le capacità dei rendition providers.
 */
public enum RenditionCapability {
	NOT_CAPABLE(0),
	LOW_CAPABILITY(32),
	PRETTY_CAPABLE(64),
	GOOD_CAPABILITY(96),
	BEST_FIT(128);

	protected int val ;
	
	RenditionCapability(int val) {
		this.val = val ;
	}
	
	public int getVal() {
		return val ;
	}

	public Boolean isBest() {
		return (val == 128) ? true : false ;
	}
	
	public Boolean isBetterThan(RenditionCapability r) {
		return (this.val > r.val) ? true : false ;
	}
}
