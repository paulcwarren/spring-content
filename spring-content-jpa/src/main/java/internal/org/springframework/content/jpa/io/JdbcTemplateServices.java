package internal.org.springframework.content.jpa.io;

import org.springframework.jdbc.core.RowCountCallbackHandler;

public interface JdbcTemplateServices {

    RowCountCallbackHandler newRowCountCallbackHandler();
    InputStreamCallbackHandler newInputStreamCallbackHandler(String columnName);

}
