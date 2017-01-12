package geowarin.bootwebpack.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "react")
public class ReactSsrProperties {
    /**
     * Location of your js source code. This must be relative to your
     * project root.
     */
    private String jsSourceDirectory = "src/main/js";

    /**
     * Location of the boot-ssr node module directory. Can be absolute or relative
     * to the project react.jsSourceDirectory
     */
    private String bootSsrNodeModulePath = "node_modules/boot-ssr";

    /**
     * Directory where your React pages can be found. This must be relative to
     * the project react.jsSourceDirectory
     */
    private String pageDir = "pages";

    public String getBootSsrNodeModulePath() {
        return bootSsrNodeModulePath;
    }

    public void setBootSsrNodeModulePath(String bootSsrNodeModulePath) {
        this.bootSsrNodeModulePath = bootSsrNodeModulePath;
    }

    public String getJsSourceDirectory() {
        return jsSourceDirectory;
    }

    public void setJsSourceDirectory(String jsSourceDirectory) {
        this.jsSourceDirectory = jsSourceDirectory;
    }

    public String getPageDir() {
        return pageDir;
    }

    public void setPageDir(String pageDir) {
        this.pageDir = pageDir;
    }
}
