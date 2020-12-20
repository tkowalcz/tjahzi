package pl.tkowalcz.tjahzi.github;

import org.agrona.collections.ArrayUtil;

public class ExceptionWithGitHubReference extends RuntimeException {

    public ExceptionWithGitHubReference(
            String message,
            GitHubDocs gitHubDocs,
            Object... formatArgs
    ) {
        super(
                String.format(
                        message + ". Check out documentation at %s",
                        ArrayUtil.add(
                                formatArgs,
                                gitHubDocs.getGitHubReference()
                        )
                )
        );
    }
}
