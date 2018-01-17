package internal.org.springframework.content.jpa.io;

import org.springframework.jdbc.core.RowCountCallbackHandler;

public class JdbcTemplateServicesImpl implements JdbcTemplateServices {

    @Override
    public RowCountCallbackHandler newRowCountCallbackHandler() {
        return new RowCountCallbackHandler();
    }

    @Override
    public InputStreamCallbackHandler newInputStreamCallbackHandler(String columnName) {
        return new InputStreamCallbackHandler(columnName);
    }

}
