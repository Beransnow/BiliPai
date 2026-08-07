package com.android.purebilibili.feature.cast

import com.android.purebilibili.core.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.w3c.dom.Element
import org.xml.sax.InputSource
import org.xml.sax.SAXException
import java.io.StringReader
import java.net.URI
import javax.xml.parsers.DocumentBuilderFactory

/**
 * SSDP/DLNA caster.
 * 通过 SOAP 调 AVTransport。
 */
object SsdpCastClient {
    private const val TAG = "SsdpCastClient"
    private val soapContentType = "text/xml; charset=utf-8".toMediaType()

    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        // Local device description endpoints are often slow or half-awake.
        .connectTimeout(4, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(6, java.util.concurrent.TimeUnit.SECONDS)
        .callTimeout(8, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    data class AvTransportEndpoint(
        val controlUrl: String,
        val serviceType: String
    )

    data class SsdpDeviceProfile(
        val friendlyName: String,
        val modelName: String?,
        val avTransportEndpoint: AvTransportEndpoint?
    )

    suspend fun cast(
        device: SsdpDiscovery.SsdpDevice,
        mediaUrl: String,
        title: String,
        creator: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val endpoint = fetchDeviceProfile(device.location)?.avTransportEndpoint
                ?: error("设备不支持 AVTransport 控制")
            val metadata = buildDidlMetadata(mediaUrl, title, creator)

            sendSoapAction(
                endpoint = endpoint,
                action = "SetAVTransportURI",
                actionBody = buildSetUriActionBody(endpoint.serviceType, mediaUrl, metadata)
            )
            sendSoapAction(
                endpoint = endpoint,
                action = "Play",
                actionBody = buildPlayActionBody(endpoint.serviceType)
            )
            Logger.i(TAG, "📺 [SSDP] Cast command sent to ${device.server.take(40)}")
        }
    }

    suspend fun fetchDeviceProfile(
        device: SsdpDiscovery.SsdpDevice
    ): SsdpDeviceProfile? = withContext(Dispatchers.IO) {
        fetchDeviceProfile(device.location)
    }

    private fun fetchDeviceProfile(descriptionLocation: String): SsdpDeviceProfile? {
        return runCatching {
            val request = Request.Builder()
                .url(descriptionLocation)
                // Some TV firmwares reject empty / generic clients.
                .header("User-Agent", "BiliPai/1.0 UPnP/1.0 DLNADOC/1.50")
                .header("Accept", "text/xml, application/xml, */*")
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Logger.w(TAG, "📺 [SSDP] Fetch device description failed: ${response.code}")
                    return null
                }
                val descriptionXml = response.body.string()
                val profile = parseDeviceProfile(descriptionXml, descriptionLocation)
                if (profile == null) {
                    Logger.w(TAG, "📺 [SSDP] Device description parse returned null")
                } else if (profile.avTransportEndpoint == null) {
                    Logger.w(
                        TAG,
                        "📺 [SSDP] Device has no AVTransport: name=${profile.friendlyName.take(40)}"
                    )
                }
                return profile
            }
        }.getOrElse { error ->
            Logger.w(TAG, "📺 [SSDP] Fetch device profile exception: ${error.message}")
            null
        }
    }

    internal fun parseAvTransportEndpoint(
        descriptionXml: String,
        descriptionLocation: String
    ): AvTransportEndpoint? = parseDeviceProfile(
        descriptionXml = descriptionXml,
        descriptionLocation = descriptionLocation
    )?.avTransportEndpoint

    internal fun parseDeviceProfile(
        descriptionXml: String,
        descriptionLocation: String
    ): SsdpDeviceProfile? {
        if (descriptionXml.isBlank()) return null
        return runCatching {
            val document = newDeviceDescriptionBuilder().parse(
                InputSource(StringReader(descriptionXml))
            )
            val deviceNodes = document.getElementsByTagNameNS("*", "device")
            if (deviceNodes.length == 0) return null

            // Root devices often embed MediaRenderer; prefer the node that actually owns AVTransport.
            var selectedDevice: Element? = null
            var endpoint: AvTransportEndpoint? = null
            for (i in 0 until deviceNodes.length) {
                val device = deviceNodes.item(i) as? Element ?: continue
                val candidate = findDirectAvTransportEndpoint(device, descriptionLocation) ?: continue
                val deviceType = device.getFirstChildContent("deviceType")
                val isRenderer = deviceType.contains("MediaRenderer", ignoreCase = true)
                if (endpoint == null || isRenderer) {
                    selectedDevice = device
                    endpoint = candidate
                    if (isRenderer) break
                }
            }

            val fallbackDevice = deviceNodes.item(0) as? Element
            val nameSource = selectedDevice ?: fallbackDevice ?: return null
            SsdpDeviceProfile(
                friendlyName = nameSource.getFirstChildContent("friendlyName"),
                modelName = nameSource.getFirstChildContent("modelName").ifBlank { null },
                avTransportEndpoint = endpoint
            )
        }.getOrElse { error ->
            Logger.w(TAG, "📺 [SSDP] Parse description failed: ${error.message}")
            null
        }
    }

    /**
     * Android XML parser implementations do not expose the same optional JAXP/SAX
     * feature set.  A missing hardening flag must not make a valid UPnP device
     * description undiscoverable; the EntityResolver is the portable security boundary.
     */
    private fun newDeviceDescriptionBuilder() = DocumentBuilderFactory.newInstance().apply {
        isNamespaceAware = true
        disableFeatureIfSupported("http://xml.org/sax/features/external-general-entities")
        disableFeatureIfSupported("http://xml.org/sax/features/external-parameter-entities")
        disableFeatureIfSupported("http://apache.org/xml/features/nonvalidating/load-external-dtd")
        runCatching { isXIncludeAware = false }
        runCatching { isExpandEntityReferences = false }
    }.newDocumentBuilder().apply {
        setEntityResolver { _, systemId ->
            throw SAXException("External XML entity resolution is disabled: $systemId")
        }
    }

    private fun DocumentBuilderFactory.disableFeatureIfSupported(feature: String) {
        runCatching { setFeature(feature, false) }
            .onFailure { Logger.d(TAG, "📺 [SSDP] XML feature unavailable: $feature") }
    }

    /**
     * Inspect this device node's own serviceList only (ignore nested embedded devices).
     */
    private fun findDirectAvTransportEndpoint(
        device: Element,
        descriptionLocation: String,
    ): AvTransportEndpoint? {
        val serviceList = device.directChildElement("serviceList") ?: return null
        var child = serviceList.firstChild
        while (child != null) {
            val service = child as? Element
            child = child.nextSibling
            if (service == null || !service.localOrTagName().equals("service", ignoreCase = true)) {
                continue
            }
            val serviceType = service.getFirstChildContent("serviceType")
            if (!serviceType.contains("AVTransport", ignoreCase = true)) continue
            val controlUrlRaw = service.getFirstChildContent("controlURL")
            if (controlUrlRaw.isBlank()) continue
            return AvTransportEndpoint(
                controlUrl = URI(descriptionLocation).resolve(controlUrlRaw.trim()).toString(),
                serviceType = serviceType
            )
        }
        return null
    }

    private fun Element.directChildElement(tagName: String): Element? {
        var child = firstChild
        while (child != null) {
            val element = child as? Element
            if (element != null && element.localOrTagName().equals(tagName, ignoreCase = true)) {
                return element
            }
            child = child.nextSibling
        }
        return null
    }

    private fun Element.localOrTagName(): String {
        val local = localName
        if (!local.isNullOrBlank()) return local
        return tagName.substringAfter(':')
    }

    internal fun buildSetUriActionBody(
        serviceType: String,
        mediaUrl: String,
        metadata: String
    ): String {
        val escapedMediaUrl = escapeXml(mediaUrl)
        val escapedMetadata = escapeXml(metadata)
        return """
            <u:SetAVTransportURI xmlns:u="$serviceType">
                <InstanceID>0</InstanceID>
                <CurrentURI>$escapedMediaUrl</CurrentURI>
                <CurrentURIMetaData>$escapedMetadata</CurrentURIMetaData>
            </u:SetAVTransportURI>
        """.trimIndent()
    }

    private fun buildPlayActionBody(serviceType: String): String = """
        <u:Play xmlns:u="$serviceType">
            <InstanceID>0</InstanceID>
            <Speed>1</Speed>
        </u:Play>
    """.trimIndent()

    private fun buildDidlMetadata(url: String, title: String, creator: String): String {
        val escapedUrl = escapeXml(url)
        val escapedTitle = escapeXml(title.ifBlank { "BiliPai Video" })
        val escapedCreator = escapeXml(creator.ifBlank { "BiliPai" })
        return """
            <DIDL-Lite xmlns:dc="http://purl.org/dc/elements/1.1/"
                xmlns:upnp="urn:schemas-upnp-org:metadata-1-0/upnp/"
                xmlns="urn:schemas-upnp-org:metadata-1-0/DIDL-Lite/">
                <item id="1" parentID="0" restricted="1">
                    <dc:title>$escapedTitle</dc:title>
                    <upnp:class>object.item.videoItem</upnp:class>
                    <dc:creator>$escapedCreator</dc:creator>
                    <res protocolInfo="http-get:*:video/mp4:*">$escapedUrl</res>
                </item>
            </DIDL-Lite>
        """.trimIndent()
    }

    private fun sendSoapAction(
        endpoint: AvTransportEndpoint,
        action: String,
        actionBody: String
    ) {
        val envelope = """
            <?xml version="1.0" encoding="utf-8"?>
            <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
                <s:Body>
                    $actionBody
                </s:Body>
            </s:Envelope>
        """.trimIndent()

        val request = Request.Builder()
            .url(endpoint.controlUrl)
            .header("SOAPACTION", "\"${endpoint.serviceType}#$action\"")
            .post(envelope.toRequestBody(soapContentType))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val payload = response.body.string().take(180)
                error("SOAP $action failed (${response.code}): $payload")
            }
        }
    }

    private fun escapeXml(value: String): String = buildString(value.length + 16) {
        value.forEach { ch ->
            when (ch) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                '\'' -> append("&apos;")
                else -> append(ch)
            }
        }
    }

    private fun Element.getFirstChildContent(tagName: String): String {
        val nodes = getElementsByTagNameNS("*", tagName)
        if (nodes.length == 0) return ""
        return nodes.item(0)?.textContent?.trim().orEmpty()
    }
}
