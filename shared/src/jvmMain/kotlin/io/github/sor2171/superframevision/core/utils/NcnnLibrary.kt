package io.github.sor2171.superframevision.core.utils

import com.sun.jna.Library
import com.sun.jna.NativeLong
import com.sun.jna.Pointer
import com.sun.jna.Structure
import com.sun.jna.ptr.PointerByReference

@Suppress("FunctionName", "PropertyName")
interface NcnnLibrary : Library {
    fun ncnn_option_create(): Pointer
    fun ncnn_option_destroy(opt: Pointer)
    fun ncnn_option_set_num_threads(opt: Pointer, numThreads: Int)
    fun ncnn_option_set_use_vulkan_compute(opt: Pointer, enable: Int)

    fun ncnn_net_create(): Pointer
    fun ncnn_net_destroy(net: Pointer)
    fun ncnn_net_set_option(net: Pointer, opt: Pointer)

    fun ncnn_extractor_create(net: Pointer): Pointer
    fun ncnn_extractor_destroy(ex: Pointer)
    fun ncnn_extractor_set_option(ex: Pointer, opt: Pointer)

    fun ncnn_extractor_input(ex: Pointer, name: String, mat: Pointer): Int

    fun ncnn_mat_destroy(mat: Pointer)

    fun ncnn_mat_create_3d_elem(
        w: Int,
        h: Int,
        c: Int,
        elemsize: NativeLong,
        elempack: Int,
        allocator: Pointer?
    ): Pointer

    fun ncnn_mat_get_w(mat: Pointer): Int
    fun ncnn_mat_get_h(mat: Pointer): Int
    fun ncnn_mat_get_c(mat: Pointer): Int
    fun ncnn_mat_get_elemsize(mat: Pointer): NativeLong
    fun ncnn_mat_get_data(mat: Pointer): Pointer

    fun ncnn_mat_fill_float(mat: Pointer, v: Float)

    fun ncnn_net_load_param_memory(net: Pointer, mem: ByteArray): Long
    fun ncnn_net_load_model_memory(net: Pointer, mem: ByteArray): Long

    fun ncnn_extractor_extract(ex: Pointer, name: String, out: PointerByReference): Int
    fun ncnn_mat_get_cstep(mat: Pointer): NativeLong
    fun ncnn_mat_get_elempack(mat: Pointer): NativeLong

    fun ncnn_get_gpu_count(): Int
    fun ncnn_get_gpu_info(device_index: Int): Pointer
    fun ncnn_net_set_vulkan_device(net: Pointer, device_index: Int)
    class NcnnGpuInfo : Structure {
        @JvmField var device_index: Int = 0
        @JvmField var device_name: String? = null
        @JvmField var vendor_id: Int = 0
        @JvmField var device_id: Int = 0
        constructor() : super()
        constructor(p: Pointer) : super(p) { useMemory(p); read() }
        override fun getFieldOrder(): List<String> = listOf(
            "device_index", "device_name", "vendor_id", "device_id"
        )
    }
}