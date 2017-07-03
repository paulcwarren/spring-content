package internal.org.springframework.content.elasticsearch;

public class CreateIndex extends io.searchbox.indices.CreateIndex {

    protected CreateIndex(Builder builder) {
        super(builder);
    }

    public String getIndexName() {
        return this.indexName;
    }
}
