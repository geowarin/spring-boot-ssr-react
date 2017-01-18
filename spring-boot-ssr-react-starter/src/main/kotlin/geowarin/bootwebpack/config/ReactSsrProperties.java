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
    private String bootSsrNodeModulePath = "node_modules/spring-boot-ssr-react-node";

    /**
     * Directory where your React pages can be found. This must be relative to
     * the project react.jsSourceDirectory
     */
    private String pageDir = "pages";

    /**
     * Does this server runs in production or in development mode?
     * By default, we try to detect if the webpack compiled resources
     * are present on the classpath, in this case, we can assume a production
     * environment.
     * Otherwise, we are in development.
     */
    private RunMode mode = RunMode.auto;

    /**
     * When compiled statically, the webpack assets will end up in this
     * directory, in the production jar.
     */
    private String webpackAssetsLocation = "webpack_assets";

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

    public RunMode getMode() {
        return mode;
    }

    public void setMode(RunMode mode) {
        this.mode = mode;
    }

    public String getWebpackAssetsLocation() {
        return webpackAssetsLocation;
    }

    public void setWebpackAssetsLocation(String webpackAssetsLocation) {
        this.webpackAssetsLocation = webpackAssetsLocation;
    }
}
