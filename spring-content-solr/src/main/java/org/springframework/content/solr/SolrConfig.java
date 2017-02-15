package org.springframework.content.solr;


import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix="solr")
public class SolrConfig {

    private String url;
    private String username;
    private String password;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Bean
    public SolrProperties solrProperties() {

        SolrProperties props = new SolrProperties();
        if (url == null) {
            this.url = "http://localhost:8983/solr/solr";
        }
        props.setUrl(url);
        props.setPassword(password);
        props.setUser(username);
        return props;
    }
}
