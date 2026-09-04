package io.github.sor2171.superframevision.core.utils

import com.sun.jna.Library
import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.Platform
import com.sun.jna.Pointer
import com.sun.jna.Structure
import com.sun.jna.ptr.PointerByReference
import org.slf4j.LoggerFactory



@Suppress("unused")
object VulkanDeviceDetector {

    private val logger = LoggerFactory.getLogger(this.javaClass)

    private const val VK_SUCCESS = 0
    private const val VK_INCOMPLETE = 5
    private const val VK_STRUCTURE_TYPE_APPLICATION_INFO = 0
    private const val VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO = 1
    private const val VK_API_VERSION_1_0 = (1 shl 22) // 4194304

    data class VulkanDeviceInfo(
        val index: Int,
        val name: String,
        val apiVersion: Int,
        val driverVersion: Int,
        val vendorId: Int,
        val deviceId: Int,
        val deviceType: Int
    ) {
        val deviceTypeName: String
            get() = when (deviceType) {
                1 -> "Integrated GPU"
                2 -> "Discrete GPU"
                3 -> "Virtual GPU"
                4 -> "CPU"
                else -> "Other"
            }

        val apiVersionString: String
            get() = "${apiVersion shr 22}.${(apiVersion shr 12) and 0x3FF}.${apiVersion and 0xFFF}"

        val driverVersionString: String
            get() = "${driverVersion shr 22}.${(driverVersion shr 12) and 0x3FF}.${driverVersion and 0xFFF}"
    }
    interface VulkanLibrary : Library {
        fun vkCreateInstance(
            pCreateInfo: Pointer?,
            pAllocator: Pointer?,
            pInstance: PointerByReference?
        ): Int

        fun vkDestroyInstance(instance: Pointer?, pAllocator: Pointer?)

        fun vkEnumeratePhysicalDevices(
            instance: Pointer?,
            pPhysicalDeviceCount: IntArray?,
            pPhysicalDevices: Pointer?
        ): Int

        fun vkGetPhysicalDeviceProperties(physicalDevice: Pointer?, pProperties: Pointer?)
    }

    class VkApplicationInfo : Structure() {
        @JvmField var sType: Int = VK_STRUCTURE_TYPE_APPLICATION_INFO
        @JvmField var pNext: Pointer? = null
        @JvmField var pApplicationName: Pointer? = null
        @JvmField var applicationVersion: Int = 1
        @JvmField var pEngineName: Pointer? = null
        @JvmField var engineVersion: Int = 1
        @JvmField var apiVersion: Int = VK_API_VERSION_1_0

        override fun getFieldOrder(): List<String> =
            listOf("sType", "pNext", "pApplicationName", "applicationVersion", "pEngineName", "engineVersion", "apiVersion")
    }

    class VkInstanceCreateInfo : Structure() {
        @JvmField var sType: Int = VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO
        @JvmField var pNext: Pointer? = null
        @JvmField var flags: Int = 0
        @JvmField var pApplicationInfo: Pointer? = null
        @JvmField var enabledLayerCount: Int = 0
        @JvmField var ppEnabledLayerNames: Pointer? = null
        @JvmField var enabledExtensionCount: Int = 0
        @JvmField var ppEnabledExtensionNames: Pointer? = null

        override fun getFieldOrder(): List<String> =
            listOf("sType", "pNext", "flags", "pApplicationInfo", "enabledLayerCount", "ppEnabledLayerNames", "enabledExtensionCount", "ppEnabledExtensionNames")
    }

    fun detect(): List<VulkanDeviceInfo> {
        val vulkan = try {
            loadVulkanLibrary()
        } catch (e: Throwable) {
            logger.warn("Vulkan loader not found: ${e.message}")
            return emptyList()
        }

        var instance: Pointer? = null
        try {
            val appName = Memory(64)
            appName.setString(0, "SuperFrameVision")

            val appInfo = VkApplicationInfo()
            appInfo.pApplicationName = appName
            appInfo.write()

            val createInfo = VkInstanceCreateInfo()
            createInfo.pApplicationInfo = appInfo.getPointer()
            createInfo.write()

            val instanceRef = PointerByReference()
            val result = vulkan.vkCreateInstance(createInfo.getPointer(), null, instanceRef)
            if (result != VK_SUCCESS) {
                logger.warn("vkCreateInstance failed: $result")
                return emptyList()
            }
            instance = instanceRef.value

            val count = IntArray(1)
            var ret = vulkan.vkEnumeratePhysicalDevices(instance, count, null)
            if (ret != VK_SUCCESS && ret != VK_INCOMPLETE) {
                logger.warn("vkEnumeratePhysicalDevices(count) failed: $ret")
                return emptyList()
            }

            val deviceCount = count[0]
            if (deviceCount == 0) return emptyList()

            val devicesMem = Memory(deviceCount * Native.POINTER_SIZE.toLong())
            ret = vulkan.vkEnumeratePhysicalDevices(instance, count, devicesMem)
            if (ret != VK_SUCCESS && ret != VK_INCOMPLETE) {
                logger.warn("vkEnumeratePhysicalDevices failed: $ret")
                return emptyList()
            }

            val resultList = mutableListOf<VulkanDeviceInfo>()
            for (i in 0 until deviceCount) {
                val device = devicesMem.getPointer(i * Native.POINTER_SIZE.toLong())
                val props = Memory(4096)
                vulkan.vkGetPhysicalDeviceProperties(device, props)

                val apiVersion = props.getInt(0)
                val driverVersion = props.getInt(4)
                val vendorId = props.getInt(8)
                val deviceId = props.getInt(12)
                val deviceType = props.getInt(16)
                val nameBytes = props.getByteArray(20, 256)
                val name = String(nameBytes, Charsets.UTF_8).trimEnd('\u0000')

                resultList += VulkanDeviceInfo(
                    index = i,
                    name = name,
                    apiVersion = apiVersion,
                    driverVersion = driverVersion,
                    vendorId = vendorId,
                    deviceId = deviceId,
                    deviceType = deviceType
                )
            }
            return resultList
        } catch (e: Throwable) {
            logger.warn("Failed to enumerate Vulkan devices", e)
            return emptyList()
        } finally {
            if (instance != null) {
                try {
                    vulkan.vkDestroyInstance(instance, null)
                } catch (_: Throwable) {
                }
            }
        }
    }

    private fun loadVulkanLibrary(): VulkanLibrary {
        val names = when {
            Platform.isWindows() -> arrayOf("vulkan-1")
            Platform.isMac() -> arrayOf("vulkan", "MoltenVK")
            else -> arrayOf("vulkan")
        }

        var lastError: Throwable? = null
        for (name in names) {
            try {
                return Native.load(name, VulkanLibrary::class.java)
            } catch (e: Throwable) {
                lastError = e
            }
        }
        throw lastError ?: UnsatisfiedLinkError("Vulkan library not found")
    }
}