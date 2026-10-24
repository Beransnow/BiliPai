package com.android.purebilibili.core.plugin

import com.android.purebilibili.plugin.sdk.PluginCapabilityManifest

interface PlayerPluginApi : PlayerPlugin {
    override val capabilityManifest: PluginCapabilityManifest
}

interface DanmakuPluginApi : DanmakuPlugin {
    override val capabilityManifest: PluginCapabilityManifest
}
