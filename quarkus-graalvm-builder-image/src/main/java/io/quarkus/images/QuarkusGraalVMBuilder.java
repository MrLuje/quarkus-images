package io.quarkus.images;

import io.quarkus.images.config.Config;
import io.quarkus.images.config.Variant;
import io.quarkus.images.modules.*;

import java.util.HashMap;
import java.util.Map;

public class QuarkusGraalVMBuilder {

    public static MultiStageDockerFile getGraalvmDockerFile(String base, String version, String javaVersion, String arch,
            String sha) {
        String getReleaseStageName = "get-github-release";

        MultiStageDockerFile df = Dockerfile.multistages()
                .stage(getReleaseStageName,
                        Dockerfile.from(base)
                                .user("root")
                                .install("tar", "gzip")
                                .module(new GraalVMModule.GetRelease(version, arch, javaVersion, sha)))
                .stage(Dockerfile.from(base)
                        .user("root")
                        .install("tar", "gzip", "gcc", "glibc-devel", "zlib-devel", "shadow-utils", "unzip", "gcc-c++")
                        .install("glibc-langpack-en")
                        .module(new UsLangModule())
                        .module(new QuarkusUserModule())
                        .module(new QuarkusDirectoryModule())
                        .module(new UpxModule(arch))
                        .module(new GraalVMModule.UseRelease(version, arch, javaVersion, sha, getReleaseStageName))
                        .env("PATH", "$PATH:$JAVA_HOME/bin")
                        .label("io.k8s.description",
                                "Quarkus.io executable image providing the `native-image` executable.",
                                "io.k8s.display-name", "Quarkus.io executable (GraalVM Native)",
                                "io.openshift.tags", "executable,java,quarkus,graalvm,native",
                                "maintainer", "Quarkus Team <quarkus-dev@googlegroups.com>")
                        .user("1001")
                        .workdir("/project")
                        .entrypoint("native-image"));

        return df;
    }

    public static MultiStageDockerFile getGraalvmDockerFile(Config.ImageConfig image, Variant variant, String base) {
        return getGraalvmDockerFile(base, image.graalvmVersion(), Integer.toString(image.javaVersion()), variant.arch(),
                variant.sha());
    }

    public static Map<String, Buildable> collect(Config.ImageConfig image, String base) {
        Map<String, Buildable> architectures = new HashMap<>();
        for (Variant variant : image.variants) {
            MultiStageDockerFile df = QuarkusGraalVMBuilder.getGraalvmDockerFile(image, variant, base);
            architectures.put(variant.arch(), df);
        }
        return architectures;
    }
}
