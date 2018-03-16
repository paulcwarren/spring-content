package internal.org.springframework.content.jpa.config;

import org.springframework.content.jpa.config.JpaStoreProperties;

public class JpaStorePropertiesImpl implements JpaStoreProperties {

    private int commitTimeout = 30;

    public org.springframework.content.jpa.config.JpaStoreProperties commitTimeout(int seconds) {
        commitTimeout = seconds;
        return this;
    }

    public int getCommitTimeout() {
        return commitTimeout;
    }

}
