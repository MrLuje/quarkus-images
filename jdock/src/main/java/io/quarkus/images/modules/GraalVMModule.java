package io.quarkus.images.modules;

import io.quarkus.images.BuildContext;
import io.quarkus.images.artifacts.Artifact;
import io.quarkus.images.commands.*;

import java.util.List;

public class GraalVMModule {
    public static final String GRAALVM_HOME = "/opt/graalvm";

    public static class GetImage extends AbstractModule {
        private final String url;
        private final String sha;
        private final String filename;

        private static final String TEMPLATE = """
                tar xzf %s -C /opt \\
                && mv /opt/graalvm-ce-*-%s* /opt/graalvm
                """;
        private final String graalvmVersion;

        public GetImage(String version, String arch, String javaVersion, String sha) {
            super("graalvm",
                    arch != null ? version + "-java" + javaVersion + "-" + arch : version + "-java" + javaVersion + "-amd64");

            if (arch == null) {
                arch = "amd64";
            } else if (arch.equalsIgnoreCase("arm64")) {
                arch = "aarch64";
            }
            this.filename = "graalvm-java%s-linux-%s-%s.tar.gz"
                    .formatted(javaVersion, arch, version);
            this.url = "https://github.com/graalvm/graalvm-ce-builds/releases/download/vm-%s/graalvm-ce-java%s-linux-%s-%s.tar.gz"
                    .formatted(version, javaVersion, arch, version);
            this.sha = sha;
            this.graalvmVersion = version;
        }

        @Override
        public List<Command> commands(BuildContext bc) {
            Artifact artifact = bc.addArtifact(new Artifact(filename, url, sha));
            String script = TEMPLATE.formatted(
                    "/tmp/" + artifact.name, // tar
                    graalvmVersion);

            return List.of(
                    new CopyCommand(artifact, "/tmp/" + artifact.name),
                    new RunCommand(script));
        }
    }

    public static class UseImage extends AbstractModule {
        private final String filename;
        private final String getImageStage;

        private static final String TEMPLATE = """
                %s/bin/gu --auto-yes install native-image \\
                && rm -Rf %s""";

        public UseImage(String version, String arch, String javaVersion, String sha, String getImageStage) {
            super("graalvm",
                    arch != null ? version + "-java" + javaVersion + "-" + arch : version + "-java" + javaVersion + "-amd64");

            if (arch == null) {
                arch = "amd64";
            } else if (arch.equalsIgnoreCase("arm64")) {
                arch = "aarch64";
            }
            this.filename = "graalvm-java%s-linux-%s-%s.tar.gz"
                    .formatted(javaVersion, arch, version);
            this.getImageStage = getImageStage;
        }

        @Override
        public List<Command> commands(BuildContext bc) {
            String script = TEMPLATE.formatted(
                    GRAALVM_HOME, // gu
                    "/tmp/" + this.filename); // rm

            return List.of(
                    new CopyCommand(this.getImageStage, "/opt", "/opt"),
                    new EnvCommand("JAVA_HOME", GRAALVM_HOME, "GRAALVM_HOME", GRAALVM_HOME),
                    new MicrodnfCommand("fontconfig", "freetype-devel"),
                    new RunCommand(script));
        }
    }
}
