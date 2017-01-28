package geowarin.bootwebpack.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

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

    /**
     * Bundles all your production dependencies (listed in your package.json)
     * in a file that will be cached and use in development for faster build
     * and reload times
     */
    private boolean enableDll = false;

    /**
     * By default, all your production dependencies (listed in your package.json)
     * are included in the DLL file, but you can add additional modules
     * here
     */
    private List<String> additionalDllLibs = new ArrayList<>();

    /**
     * Build options
     */
    private final Build build = new Build();

    public static class Build {
        /**
         * Should we generate a minified version of your assets?
         * Disable it only for debugging purposes
         */
        private boolean minify = true;

        /**
         * When you build, you can generate a json file containing
         * all your assets (.react-ssr/webpack-stats.json).
         *
         * This file can be analyzed to optimize your build size.
         * See: http://survivejs.com/webpack/optimizing-build/analyzing-build-statistics/
         */
        private boolean generateStats = false;

        public boolean isMinify() {
            return minify;
        }

        public void setMinify(boolean minify) {
            this.minify = minify;
        }

        public boolean isGenerateStats() {
            return generateStats;
        }

        public void setGenerateStats(boolean generateStats) {
            this.generateStats = generateStats;
        }
    }

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

    public boolean isEnableDll() {
        return enableDll;
    }

    public void setEnableDll(boolean enableDll) {
        this.enableDll = enableDll;
    }

    public List<String> getAdditionalDllLibs() {
        return additionalDllLibs;
    }

    public void setAdditionalDllLibs(List<String> additionalDllLibs) {
        this.additionalDllLibs = additionalDllLibs;
    }

    public Build getBuild() {
        return build;
    }
}
