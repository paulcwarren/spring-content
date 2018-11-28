package internal.org.springframework.versions.jpa;

public interface VersioningService {
    Object establishAncestralRoot(Object entity);

    Object establishAncestor(Object entity, Object successor);

    Object establishSuccessor(Object successorCandidate, String versionNo, String versionLabel, Object ancestralRoot, Object ancestor);
}
