package io.quarkus.images;

import io.quarkus.images.config.Config;
import io.quarkus.images.config.Variant;
import io.quarkus.images.modules.*;

import java.util.HashMap;
import java.util.Map;

public class QuarkusGraalVMBuilder {

    public static MultiStageDockerFile getGraalvmDockerFile(String base, String version, String javaVersion, String arch,
            String sha) {
        MultiStageDockerFile df = Dockerfile.multistages()
                .stage("get-github-release",
                        Dockerfile.from(base)
                                .user("root")
                                .install("tar", "gzip")
                                .module(new GraalVMModule2(version, arch, javaVersion, sha)))
                .stage("final-image",
                        Dockerfile.from(base)
                                .user("root")
                                .install("tar", "gzip", "gcc", "glibc-devel", "zlib-devel", "shadow-utils", "unzip", "gcc-c++")
                                .install("glibc-langpack-en")
                                .module(new UsLangModule())
                                .module(new QuarkusUserModule())
                                .module(new QuarkusDirectoryModule())
                                .module(new UpxModule(arch))
                                .copyFromStage("get-github-release", "/opt", "/opt")
                                .module(new GraalVMModule(version, arch, javaVersion, sha))
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
