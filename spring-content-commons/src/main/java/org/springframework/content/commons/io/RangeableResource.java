package org.springframework.content.commons.io;

public interface RangeableResource {

    /**
     * An optional aspect for Resources that wish to be given Range specifications so that they
     * can prepare InputStream appropriately
     *
     * @param range the range
     */
    void setRange(String range);
}
