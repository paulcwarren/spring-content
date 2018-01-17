package internal.org.springframework.content.jpa.io;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.content.jpa.io.BlobResource;
import org.springframework.jdbc.core.JdbcTemplate;

public class BlobResourceFactory {

    private static Log logger = LogFactory.getLog(BlobResourceFactory.class);

    private JdbcTemplate template;
    private JdbcTemplateServices templateServices;

    public BlobResourceFactory(JdbcTemplate template, JdbcTemplateServices templateServices) {
        this.template = template;
        this.templateServices = templateServices;
    }

    public BlobResource newBlobResource(String id) {
        return new MySQLBlobResource(id, template, templateServices);
    }
}
