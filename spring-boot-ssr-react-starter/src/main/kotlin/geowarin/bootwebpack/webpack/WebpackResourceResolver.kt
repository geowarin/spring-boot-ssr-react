package geowarin.bootwebpack.webpack;

import org.springframework.core.io.Resource
import org.springframework.web.servlet.resource.ResourceResolver
import org.springframework.web.servlet.resource.ResourceResolverChain
import javax.servlet.http.HttpServletRequest

// TODO: test
open class WebpackResourceResolver(val assetStore: AssetStore) : ResourceResolver {

    override fun resolveResource(request: HttpServletRequest, requestPath: String, locations: List<Resource>, chain: ResourceResolverChain): Resource? {
        val modulePath = request.getParameter("modulePath")
        return resolve(requestPath, modulePath) ?:
                chain.resolveResource(request, requestPath, locations)
    }

    override fun resolveUrlPath(resourcePath: String, locations: List<Resource>, chain: ResourceResolverChain): String? {
        val resolvedResource = resolve(resourcePath)
        if (resolvedResource != null) {
            return resolvedResource.url.toString()
        }
        return chain.resolveUrlPath(resourcePath, locations)
    }

    private fun resolve(requestPath: String, modulePath: String? = null): Resource? {
        return assetStore.getAssetAsResource(requestPath, modulePath)
    }
}
