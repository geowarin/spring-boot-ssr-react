package geowarin.bootwebpack.webpack;

import org.springframework.core.io.Resource
import org.springframework.web.servlet.resource.ResourceResolver
import org.springframework.web.servlet.resource.ResourceResolverChain
import javax.servlet.http.HttpServletRequest

// TODO: test
open class WebpackResourceResolver(val assetStore: AssetStore) : ResourceResolver {
    private val ignoredPaths = listOf("api")

    override fun resolveResource(request: HttpServletRequest, requestPath: String, locations: List<Resource>, chain: ResourceResolverChain): Resource? {
        val modulePath = request.getParameter("modulePath")
        val resolvedResource = resolve(requestPath, modulePath)
        return resolvedResource
    }

    override fun resolveUrlPath(resourcePath: String, locations: List<Resource>, chain: ResourceResolverChain): String? {
        val resolvedResource = resolve(resourcePath)
        return resolvedResource?.url.toString()
    }

    private fun resolve(requestPath: String, modulePath: String? = null): Resource? {
        if (isIgnored(requestPath)) {
            return null
        }
        return assetStore.getAssetAsResource(requestPath, modulePath)
    }

    private fun isIgnored(path: String): Boolean {
        return ignoredPaths.contains(path)
    }
}
