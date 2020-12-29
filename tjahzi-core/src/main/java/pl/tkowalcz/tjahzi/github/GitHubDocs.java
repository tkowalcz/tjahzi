package pl.tkowalcz.tjahzi.github;

public enum GitHubDocs {

    LOG_BUFFER_SIZING("https://github.com/tkowalcz/tjahzi/wiki/Log-buffer-sizing"),
    LABEL_NAMING("https://github.com/tkowalcz/tjahzi/wiki/Label-naming");

    private final String gitHubReference;

    GitHubDocs(String gitHubReference) {
        this.gitHubReference = gitHubReference;
    }

    public String getGitHubReference() {
        return gitHubReference;
    }

    public String getLogMessage() {
        return String.format("Check out documentation at %s.", getGitHubReference());
    }
}
