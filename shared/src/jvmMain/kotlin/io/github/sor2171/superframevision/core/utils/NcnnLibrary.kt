package io.github.sor2171.superframevision.core.utils

import com.sun.jna.Library
import com.sun.jna.Pointer
import com.sun.jna.ptr.PointerByReference

@Suppress("FunctionName", "LocalVariableName")
interface NcnnLibrary : Library {
    fun ncnn_option_create(): Pointer
    fun ncnn_option_destroy(opt: Pointer)
    fun ncnn_option_set_num_threads(opt: Pointer, numThreads: Int)
    fun ncnn_option_set_use_vulkan_compute(opt: Pointer, enable: Int)
    fun ncnn_option_set_use_fp16_packed(pointer: Pointer, enable: Int)
    fun ncnn_option_set_use_fp16_storage(pointer: Pointer, enable: Int)
    fun ncnn_option_set_use_fp16_arithmetic(pointer: Pointer, enable: Int)
    fun ncnn_option_set_use_packing_layout(pointer: Pointer, enable: Int)

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
        elemsize: Long,
        elempack: Int,
        allocator: Pointer?
    ): Pointer

    fun ncnn_mat_get_w(mat: Pointer): Int
    fun ncnn_mat_get_h(mat: Pointer): Int
    fun ncnn_mat_get_c(mat: Pointer): Int
    fun ncnn_mat_get_elemsize(mat: Pointer): Long
    fun ncnn_mat_get_data(mat: Pointer): Pointer

    fun ncnn_mat_fill_float(mat: Pointer, v: Float)

    fun ncnn_net_load_param_memory(net: Pointer, mem: ByteArray): Long
    fun ncnn_net_load_model_memory(net: Pointer, mem: ByteArray): Long

    fun ncnn_extractor_extract(ex: Pointer, name: String, out: PointerByReference): Int
    fun ncnn_mat_get_cstep(mat: Pointer): Long
    fun ncnn_mat_get_elempack(mat: Pointer): Long
    fun ncnn_net_set_vulkan_device(net: Pointer, device_index: Int)
    fun ncnn_pipelinecache_destroy(pipeline_cache: Pointer)
    fun ncnn_pipelinecache_create(device_index: Int): Pointer
    fun ncnn_option_set_pipeline_cache(opt: Pointer, pipeline_cache: Pointer)
}