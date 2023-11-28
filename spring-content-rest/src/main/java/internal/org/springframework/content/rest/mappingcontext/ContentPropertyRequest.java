package internal.org.springframework.content.rest.mappingcontext;

import internal.org.springframework.content.rest.utils.StoreUtils;
import lombok.Data;
import org.springframework.util.Assert;

import java.net.URI;

import static org.apache.commons.lang.StringUtils.join;

@Data
public class ContentPropertyRequest {

    private final String store;
    private final String id;
    private final String contentPropertyPath;

    private ContentPropertyRequest(String store, String id, String contentPropertyPath) {
        Assert.hasLength(store, "invalid store");
        this.store = store;
        Assert.hasLength(store, "invalid id");
        this.id = id;
        Assert.notNull(store, "invalid content property path");
        this.contentPropertyPath = contentPropertyPath;
    }

    public String getRequestURI() {
        return String.format("/%s/%s/%s", store, id, contentPropertyPath);
    }

    /**
     * Creates a ContentPropertyRequest from a storeLookupPath
     *
     * @param storeLookupPath
     *          a storeLookupPath as created with {@link StoreUtils#storeLookupPath(String, URI) storeLookupPath}
     * @return a ContentPropertyRequest
     */
    public static ContentPropertyRequest from(String storeLookupPath) {
        Assert.notNull(storeLookupPath, "storeLookupPath must not be null");

        String[] path = storeLookupPath.split("/");
        if (path.length < 3) {
            throw new IllegalStateException("invalid content property request");
        }

        return new ContentPropertyRequest(path[1], path[2], join(path, "/", 3, path.length));
    }

    /**
     * Creates a ContentPropertyRequest from a storeLookupPath
     *
     * @param store the store
     * @param id the id
     * @param contentPropertyPath the path to the content property
     *
     * @return a ContentPropertyRequest
     */
    public static ContentPropertyRequest from(String store, String id, String contentPropertyPath) {
        return new ContentPropertyRequest(store, id, contentPropertyPath);
    }
}