package io.github.sor2171.superframevision.core.service

import com.sun.jna.Callback
import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.WString
import com.sun.jna.ptr.PointerByReference

@Suppress("FunctionName", "LocalVariableName", "unused")
interface NcnnLibrary : Library {
    companion object {
        val INSTANCE: NcnnLibrary by lazy { Native.load("ncnn", NcnnLibrary::class.java) }

        /* mat pixel api */
        const val NCNN_MAT_PIXEL_RGB = 1
        const val NCNN_MAT_PIXEL_BGR = 2
        const val NCNN_MAT_PIXEL_GRAY = 3
        const val NCNN_MAT_PIXEL_RGBA = 4
        const val NCNN_MAT_PIXEL_BGRA = 5

        fun ncnnMatPixelX2Y(x: Int, y: Int): Int = x or (y shl 16)
        fun NCNN_MAT_PIXEL_X2Y(x: Int, y: Int): Int = x or (y shl 16)

        val NCNN_MAT_PIXEL_BGR2RGB = ncnnMatPixelX2Y(NCNN_MAT_PIXEL_BGR, NCNN_MAT_PIXEL_RGB)
        val NCNN_MAT_PIXEL_RGB2BGR = ncnnMatPixelX2Y(NCNN_MAT_PIXEL_RGB, NCNN_MAT_PIXEL_BGR)

        /* mat process api */
        const val NCNN_BORDER_CONSTANT = 0
        const val NCNN_BORDER_REPLICATE = 1
        const val NCNN_BORDER_REFLECT = 2
        const val NCNN_BORDER_TRANSPARENT = -233
    }

    /* layer callbacks */
    fun interface NcnnLayerCreator : Callback {
        fun invoke(userdata: Pointer?): Pointer?
    }

    fun interface NcnnLayerDestroyer : Callback {
        fun invoke(layer: Pointer?, userdata: Pointer?)
    }

    /* version api */
    fun ncnn_version(): String
    fun ncnn_version_number(): Int

    /* allocator api */
    fun ncnn_allocator_create_pool_allocator(): Pointer
    fun ncnn_allocator_create_unlocked_pool_allocator(): Pointer
    fun ncnn_allocator_destroy(allocator: Pointer?)

    /* pipelinecache api */
    fun ncnn_pipelinecache_create(device_index: Int): Pointer
    fun ncnn_pipelinecache_destroy(pipelinecache: Pointer?)
    fun ncnn_pipelinecache_clear(pipelinecache: Pointer?)
    fun ncnn_pipelinecache_get_size(pipelinecache: Pointer?): Long
    fun ncnn_pipelinecache_load_memory(pipelinecache: Pointer?, data: ByteArray, size: Long): Int
    fun ncnn_pipelinecache_load_memory(pipelinecache: Pointer?, data: Pointer?, size: Long): Int
    fun ncnn_pipelinecache_save_memory(pipelinecache: Pointer?, data: ByteArray?, size: LongArray): Int
    fun ncnn_pipelinecache_save_memory(pipelinecache: Pointer?, data: Pointer?, size: Pointer?): Int
    fun ncnn_pipelinecache_load(pipelinecache: Pointer?, path: String): Int
    fun ncnn_pipelinecache_save(pipelinecache: Pointer?, path: String): Int
    fun ncnn_pipelinecache_load_w(pipelinecache: Pointer?, path: WString): Int
    fun ncnn_pipelinecache_save_w(pipelinecache: Pointer?, path: WString): Int

    /* option api */
    fun ncnn_option_create(): Pointer
    fun ncnn_option_destroy(opt: Pointer?)

    fun ncnn_option_get_num_threads(opt: Pointer?): Int
    fun ncnn_option_set_num_threads(opt: Pointer?, numThreads: Int)

    fun ncnn_option_set_blob_allocator(opt: Pointer?, allocator: Pointer?)
    fun ncnn_option_set_workspace_allocator(opt: Pointer?, allocator: Pointer?)

    fun ncnn_option_get_use_vulkan_compute(opt: Pointer?): Int
    fun ncnn_option_get_use_local_pool_allocator(opt: Pointer?): Int
    fun ncnn_option_get_use_winograd_convolution(opt: Pointer?): Int
    fun ncnn_option_get_use_sgemm_convolution(opt: Pointer?): Int
    fun ncnn_option_get_use_packing_layout(opt: Pointer?): Int
    fun ncnn_option_get_use_fp16_packed(opt: Pointer?): Int
    fun ncnn_option_get_use_fp16_storage(opt: Pointer?): Int
    fun ncnn_option_get_use_fp16_arithmetic(opt: Pointer?): Int
    fun ncnn_option_get_use_int8_packed(opt: Pointer?): Int
    fun ncnn_option_get_use_int8_storage(opt: Pointer?): Int
    fun ncnn_option_get_use_int8_arithmetic(opt: Pointer?): Int
    fun ncnn_option_get_use_bf16_packed(opt: Pointer?): Int
    fun ncnn_option_get_use_bf16_storage(opt: Pointer?): Int
    fun ncnn_option_get_use_shader_local_memory(opt: Pointer?): Int
    fun ncnn_option_get_use_cooperative_matrix(opt: Pointer?): Int

    fun ncnn_option_set_use_vulkan_compute(opt: Pointer?, enable: Int)
    fun ncnn_option_set_use_local_pool_allocator(opt: Pointer?, enable: Int)
    fun ncnn_option_set_use_winograd_convolution(opt: Pointer?, enable: Int)
    fun ncnn_option_set_use_sgemm_convolution(opt: Pointer?, enable: Int)
    fun ncnn_option_set_use_packing_layout(opt: Pointer?, enable: Int)
    fun ncnn_option_set_use_fp16_packed(opt: Pointer?, enable: Int)
    fun ncnn_option_set_use_fp16_storage(opt: Pointer?, enable: Int)
    fun ncnn_option_set_use_fp16_arithmetic(opt: Pointer?, enable: Int)
    fun ncnn_option_set_use_int8_packed(opt: Pointer?, enable: Int)
    fun ncnn_option_set_use_int8_storage(opt: Pointer?, enable: Int)
    fun ncnn_option_set_use_int8_arithmetic(opt: Pointer?, enable: Int)
    fun ncnn_option_set_use_bf16_packed(opt: Pointer?, enable: Int)
    fun ncnn_option_set_use_bf16_storage(opt: Pointer?, enable: Int)
    fun ncnn_option_set_use_shader_local_memory(opt: Pointer?, enable: Int)
    fun ncnn_option_set_use_cooperative_matrix(opt: Pointer?, enable: Int)

    fun ncnn_option_set_pipeline_cache(opt: Pointer?, pipeline_cache: Pointer?)

    /* mat api */
    fun ncnn_mat_create(): Pointer
    fun ncnn_mat_create_1d(w: Int, allocator: Pointer?): Pointer
    fun ncnn_mat_create_2d(w: Int, h: Int, allocator: Pointer?): Pointer
    fun ncnn_mat_create_3d(w: Int, h: Int, c: Int, allocator: Pointer?): Pointer
    fun ncnn_mat_create_4d(w: Int, h: Int, d: Int, c: Int, allocator: Pointer?): Pointer

    fun ncnn_mat_create_external_1d(w: Int, data: Pointer?, allocator: Pointer?): Pointer
    fun ncnn_mat_create_external_2d(w: Int, h: Int, data: Pointer?, allocator: Pointer?): Pointer
    fun ncnn_mat_create_external_3d(w: Int, h: Int, c: Int, data: Pointer?, allocator: Pointer?): Pointer
    fun ncnn_mat_create_external_4d(w: Int, h: Int, d: Int, c: Int, data: Pointer?, allocator: Pointer?): Pointer

    fun ncnn_mat_create_1d_elem(w: Int, elemsize: Long, elempack: Int, allocator: Pointer?): Pointer
    fun ncnn_mat_create_2d_elem(w: Int, h: Int, elemsize: Long, elempack: Int, allocator: Pointer?): Pointer
    fun ncnn_mat_create_3d_elem(w: Int, h: Int, c: Int, elemsize: Long, elempack: Int, allocator: Pointer?): Pointer
    fun ncnn_mat_create_4d_elem(w: Int, h: Int, d: Int, c: Int, elemsize: Long, elempack: Int, allocator: Pointer?): Pointer

    fun ncnn_mat_create_external_1d_elem(w: Int, data: Pointer?, elemsize: Long, elempack: Int, allocator: Pointer?): Pointer
    fun ncnn_mat_create_external_2d_elem(w: Int, h: Int, data: Pointer?, elemsize: Long, elempack: Int, allocator: Pointer?): Pointer
    fun ncnn_mat_create_external_3d_elem(w: Int, h: Int, c: Int, data: Pointer?, elemsize: Long, elempack: Int, allocator: Pointer?): Pointer
    fun ncnn_mat_create_external_4d_elem(w: Int, h: Int, d: Int, c: Int, data: Pointer?, elemsize: Long, elempack: Int, allocator: Pointer?): Pointer

    fun ncnn_mat_destroy(mat: Pointer?)

    fun ncnn_mat_fill_float(mat: Pointer?, v: Float)

    fun ncnn_mat_clone(mat: Pointer?, allocator: Pointer?): Pointer
    fun ncnn_mat_reshape_1d(mat: Pointer?, w: Int, allocator: Pointer?): Pointer
    fun ncnn_mat_reshape_2d(mat: Pointer?, w: Int, h: Int, allocator: Pointer?): Pointer
    fun ncnn_mat_reshape_3d(mat: Pointer?, width: Int, height: Int, channels: Int, allocator: Pointer?): Pointer
    fun ncnn_mat_reshape_4d(mat: Pointer?, w: Int, h: Int, d: Int, c: Int, allocator: Pointer?): Pointer

    fun ncnn_mat_get_dims(mat: Pointer?): Int
    fun ncnn_mat_get_w(mat: Pointer?): Int
    fun ncnn_mat_get_h(mat: Pointer?): Int
    fun ncnn_mat_get_d(mat: Pointer?): Int
    fun ncnn_mat_get_c(mat: Pointer?): Int
    fun ncnn_mat_get_elemsize(mat: Pointer?): Long
    fun ncnn_mat_get_elempack(mat: Pointer?): Long
    fun ncnn_mat_get_cstep(mat: Pointer?): Long
    fun ncnn_mat_get_data(mat: Pointer?): Pointer

    fun ncnn_mat_get_channel_data(mat: Pointer?, c: Int): Pointer

    /* mat pixel api */
    fun ncnn_mat_from_pixels(pixels: ByteArray, type: Int, w: Int, h: Int, stride: Int, allocator: Pointer?): Pointer
    fun ncnn_mat_from_pixels(pixels: Pointer?, type: Int, w: Int, h: Int, stride: Int, allocator: Pointer?): Pointer

    fun ncnn_mat_from_pixels_resize(pixels: ByteArray, type: Int, w: Int, h: Int, stride: Int, target_width: Int, target_height: Int, allocator: Pointer?): Pointer
    fun ncnn_mat_from_pixels_resize(pixels: Pointer?, type: Int, w: Int, h: Int, stride: Int, target_width: Int, target_height: Int, allocator: Pointer?): Pointer

    fun ncnn_mat_from_pixels_roi(pixels: ByteArray, type: Int, w: Int, h: Int, stride: Int, roix: Int, roiy: Int, roiw: Int, roih: Int, allocator: Pointer?): Pointer
    fun ncnn_mat_from_pixels_roi(pixels: Pointer?, type: Int, w: Int, h: Int, stride: Int, roix: Int, roiy: Int, roiw: Int, roih: Int, allocator: Pointer?): Pointer

    fun ncnn_mat_from_pixels_roi_resize(pixels: ByteArray, type: Int, w: Int, h: Int, stride: Int, roix: Int, roiy: Int, roiw: Int, roih: Int, target_width: Int, target_height: Int, allocator: Pointer?): Pointer
    fun ncnn_mat_from_pixels_roi_resize(pixels: Pointer?, type: Int, w: Int, h: Int, stride: Int, roix: Int, roiy: Int, roiw: Int, roih: Int, target_width: Int, target_height: Int, allocator: Pointer?): Pointer

    fun ncnn_mat_to_pixels(mat: Pointer?, pixels: ByteArray, type: Int, stride: Int)
    fun ncnn_mat_to_pixels(mat: Pointer?, pixels: Pointer?, type: Int, stride: Int)

    fun ncnn_mat_to_pixels_resize(mat: Pointer?, pixels: ByteArray, type: Int, target_width: Int, target_height: Int, target_stride: Int)
    fun ncnn_mat_to_pixels_resize(mat: Pointer?, pixels: Pointer?, type: Int, target_width: Int, target_height: Int, target_stride: Int)

    /* mat ops */
    fun ncnn_mat_substract_mean_normalize(mat: Pointer?, mean_vals: FloatArray?, norm_vals: FloatArray?)
    fun ncnn_convert_packing(src: Pointer?, dst: PointerByReference, elempack: Int, opt: Pointer?)
    fun ncnn_flatten(src: Pointer?, dst: PointerByReference, opt: Pointer?)

    /* blob api */
    fun ncnn_blob_get_name(blob: Pointer?): String?
    fun ncnn_blob_get_producer(blob: Pointer?): Int
    fun ncnn_blob_get_consumer(blob: Pointer?): Int
    fun ncnn_blob_get_shape(blob: Pointer?, dims: IntArray?, w: IntArray?, h: IntArray?, c: IntArray?)
    fun ncnn_blob_get_shape(blob: Pointer?, dims: Pointer?, w: Pointer?, h: Pointer?, c: Pointer?)

    /* paramdict api */
    fun ncnn_paramdict_create(): Pointer
    fun ncnn_paramdict_destroy(pd: Pointer?)
    fun ncnn_paramdict_get_type(pd: Pointer?, id: Int): Int
    fun ncnn_paramdict_get_int(pd: Pointer?, id: Int, def: Int): Int
    fun ncnn_paramdict_get_float(pd: Pointer?, id: Int, def: Float): Float
    fun ncnn_paramdict_get_array(pd: Pointer?, id: Int, def: Pointer?): Pointer
    fun ncnn_paramdict_set_int(pd: Pointer?, id: Int, i: Int)
    fun ncnn_paramdict_set_float(pd: Pointer?, id: Int, f: Float)
    fun ncnn_paramdict_set_array(pd: Pointer?, id: Int, v: Pointer?)

    /* datareader api */
    fun ncnn_datareader_create(): Pointer
    fun ncnn_datareader_create_from_stdio(fp: Pointer?): Pointer
    fun ncnn_datareader_create_from_memory(mem: PointerByReference): Pointer
    fun ncnn_datareader_create_from_memory(mem: Pointer?): Pointer
    fun ncnn_datareader_destroy(dr: Pointer?)

    /* modelbin api */
    fun ncnn_modelbin_create_from_datareader(dr: Pointer?): Pointer
    fun ncnn_modelbin_create_from_mat_array(weights: Array<Pointer?>, n: Int): Pointer
    fun ncnn_modelbin_create_from_mat_array(weights: Pointer?, n: Int): Pointer
    fun ncnn_modelbin_destroy(mb: Pointer?)

    /* layer api */
    fun ncnn_layer_create(): Pointer
    fun ncnn_layer_create_by_typeindex(typeindex: Int): Pointer
    fun ncnn_layer_create_by_type(type: String): Pointer
    fun ncnn_layer_type_to_index(type: String): Int
    fun ncnn_layer_destroy(layer: Pointer?)

    fun ncnn_layer_get_name(layer: Pointer?): String?
    fun ncnn_layer_get_typeindex(layer: Pointer?): Int
    fun ncnn_layer_get_type(layer: Pointer?): String?

    fun ncnn_layer_get_one_blob_only(layer: Pointer?): Int
    fun ncnn_layer_get_support_inplace(layer: Pointer?): Int
    fun ncnn_layer_get_support_vulkan(layer: Pointer?): Int
    fun ncnn_layer_get_support_packing(layer: Pointer?): Int
    fun ncnn_layer_get_support_bf16_storage(layer: Pointer?): Int
    fun ncnn_layer_get_support_fp16_storage(layer: Pointer?): Int
    fun ncnn_layer_get_support_vulkan_packing(layer: Pointer?): Int
    fun ncnn_layer_get_support_any_packing(layer: Pointer?): Int
    fun ncnn_layer_get_support_vulkan_any_packing(layer: Pointer?): Int

    fun ncnn_layer_set_one_blob_only(layer: Pointer?, enable: Int)
    fun ncnn_layer_set_support_inplace(layer: Pointer?, enable: Int)
    fun ncnn_layer_set_support_vulkan(layer: Pointer?, enable: Int)
    fun ncnn_layer_set_support_packing(layer: Pointer?, enable: Int)
    fun ncnn_layer_set_support_bf16_storage(layer: Pointer?, enable: Int)
    fun ncnn_layer_set_support_fp16_storage(layer: Pointer?, enable: Int)
    fun ncnn_layer_set_support_vulkan_packing(layer: Pointer?, enable: Int)
    fun ncnn_layer_set_support_any_packing(layer: Pointer?, enable: Int)
    fun ncnn_layer_set_support_vulkan_any_packing(layer: Pointer?, enable: Int)

    fun ncnn_layer_get_bottom_count(layer: Pointer?): Int
    fun ncnn_layer_get_bottom(layer: Pointer?, i: Int): Int
    fun ncnn_layer_get_top_count(layer: Pointer?): Int
    fun ncnn_layer_get_top(layer: Pointer?, i: Int): Int

    fun ncnn_blob_get_bottom_shape(layer: Pointer?, i: Int, dims: IntArray?, w: IntArray?, h: IntArray?, c: IntArray?)
    fun ncnn_blob_get_bottom_shape(layer: Pointer?, i: Int, dims: Pointer?, w: Pointer?, h: Pointer?, c: Pointer?)
    fun ncnn_blob_get_top_shape(layer: Pointer?, i: Int, dims: IntArray?, w: IntArray?, h: IntArray?, c: IntArray?)
    fun ncnn_blob_get_top_shape(layer: Pointer?, i: Int, dims: Pointer?, w: Pointer?, h: Pointer?, c: Pointer?)

    /* net api */
    fun ncnn_net_create(): Pointer
    fun ncnn_net_destroy(net: Pointer?)

    fun ncnn_net_get_option(net: Pointer?): Pointer
    fun ncnn_net_set_option(net: Pointer?, opt: Pointer?)

    fun ncnn_net_set_vulkan_device(net: Pointer?, device_index: Int)

    fun ncnn_net_register_custom_layer_by_type(net: Pointer?, type: String, creator: Callback?, destroyer: Callback?, userdata: Pointer?)
    fun ncnn_net_register_custom_layer_by_typeindex(net: Pointer?, typeindex: Int, creator: Callback?, destroyer: Callback?, userdata: Pointer?)

    fun ncnn_net_load_param(net: Pointer?, path: String): Int
    fun ncnn_net_load_param_bin(net: Pointer?, path: String): Int
    fun ncnn_net_load_model(net: Pointer?, path: String): Int
    fun ncnn_net_load_param_w(net: Pointer?, path: WString): Int
    fun ncnn_net_load_param_bin_w(net: Pointer?, path: WString): Int
    fun ncnn_net_load_model_w(net: Pointer?, path: WString): Int

    fun ncnn_net_load_param_memory(net: Pointer?, mem: String): Int
    fun ncnn_net_load_param_memory(net: Pointer?, mem: ByteArray): Long
    fun ncnn_net_load_param_memory(net: Pointer?, mem: Pointer?): Long
    fun ncnn_net_load_param_bin_memory(net: Pointer?, mem: ByteArray): Long
    fun ncnn_net_load_param_bin_memory(net: Pointer?, mem: Pointer?): Long
    fun ncnn_net_load_model_memory(net: Pointer?, mem: ByteArray): Long
    fun ncnn_net_load_model_memory(net: Pointer?, mem: Pointer?): Long

    fun ncnn_net_load_param_datareader(net: Pointer?, dr: Pointer?): Int
    fun ncnn_net_load_param_bin_datareader(net: Pointer?, dr: Pointer?): Int
    fun ncnn_net_load_model_datareader(net: Pointer?, dr: Pointer?): Int

    fun ncnn_net_clear(net: Pointer?)

    fun ncnn_net_get_input_count(net: Pointer?): Int
    fun ncnn_net_get_output_count(net: Pointer?): Int
    fun ncnn_net_get_input_name(net: Pointer?, i: Int): String?
    fun ncnn_net_get_output_name(net: Pointer?, i: Int): String?
    fun ncnn_net_get_input_index(net: Pointer?, i: Int): Int
    fun ncnn_net_get_output_index(net: Pointer?, i: Int): Int

    /* extractor api */
    fun ncnn_extractor_create(net: Pointer?): Pointer
    fun ncnn_extractor_destroy(ex: Pointer?)

    fun ncnn_extractor_set_option(ex: Pointer?, opt: Pointer?)

    fun ncnn_extractor_input(ex: Pointer?, name: String, mat: Pointer?): Int
    fun ncnn_extractor_extract(ex: Pointer?, name: String, mat: PointerByReference): Int
    fun ncnn_extractor_input_index(ex: Pointer?, index: Int, mat: Pointer?): Int
    fun ncnn_extractor_extract_index(ex: Pointer?, index: Int, mat: PointerByReference): Int

    /* mat process api */
    fun ncnn_copy_make_border(src: Pointer?, dst: Pointer?, top: Int, bottom: Int, left: Int, right: Int, type: Int, v: Float, opt: Pointer?)
    fun ncnn_copy_make_border_3d(src: Pointer?, dst: Pointer?, top: Int, bottom: Int, left: Int, right: Int, front: Int, behind: Int, type: Int, v: Float, opt: Pointer?)
    fun ncnn_copy_cut_border(src: Pointer?, dst: Pointer?, top: Int, bottom: Int, left: Int, right: Int, opt: Pointer?)
    fun ncnn_copy_cut_border_3d(src: Pointer?, dst: Pointer?, top: Int, bottom: Int, left: Int, right: Int, front: Int, behind: Int, opt: Pointer?)

    /* mat pixel drawing api */
    fun ncnn_draw_rectangle_c1(pixels: ByteArray, w: Int, h: Int, rx: Int, ry: Int, rw: Int, rh: Int, color: Int, thickness: Int)
    fun ncnn_draw_rectangle_c1(pixels: Pointer?, w: Int, h: Int, rx: Int, ry: Int, rw: Int, rh: Int, color: Int, thickness: Int)
    fun ncnn_draw_rectangle_c2(pixels: ByteArray, w: Int, h: Int, rx: Int, ry: Int, rw: Int, rh: Int, color: Int, thickness: Int)
    fun ncnn_draw_rectangle_c2(pixels: Pointer?, w: Int, h: Int, rx: Int, ry: Int, rw: Int, rh: Int, color: Int, thickness: Int)
    fun ncnn_draw_rectangle_c3(pixels: ByteArray, w: Int, h: Int, rx: Int, ry: Int, rw: Int, rh: Int, color: Int, thickness: Int)
    fun ncnn_draw_rectangle_c3(pixels: Pointer?, w: Int, h: Int, rx: Int, ry: Int, rw: Int, rh: Int, color: Int, thickness: Int)
    fun ncnn_draw_rectangle_c4(pixels: ByteArray, w: Int, h: Int, rx: Int, ry: Int, rw: Int, rh: Int, color: Int, thickness: Int)
    fun ncnn_draw_rectangle_c4(pixels: Pointer?, w: Int, h: Int, rx: Int, ry: Int, rw: Int, rh: Int, color: Int, thickness: Int)

    fun ncnn_draw_text_c1(pixels: ByteArray, w: Int, h: Int, text: String, x: Int, y: Int, fontpixelsize: Int, color: Int)
    fun ncnn_draw_text_c1(pixels: Pointer?, w: Int, h: Int, text: String, x: Int, y: Int, fontpixelsize: Int, color: Int)
    fun ncnn_draw_text_c2(pixels: ByteArray, w: Int, h: Int, text: String, x: Int, y: Int, fontpixelsize: Int, color: Int)
    fun ncnn_draw_text_c2(pixels: Pointer?, w: Int, h: Int, text: String, x: Int, y: Int, fontpixelsize: Int, color: Int)
    fun ncnn_draw_text_c3(pixels: ByteArray, w: Int, h: Int, text: String, x: Int, y: Int, fontpixelsize: Int, color: Int)
    fun ncnn_draw_text_c3(pixels: Pointer?, w: Int, h: Int, text: String, x: Int, y: Int, fontpixelsize: Int, color: Int)
    fun ncnn_draw_text_c4(pixels: ByteArray, w: Int, h: Int, text: String, x: Int, y: Int, fontpixelsize: Int, color: Int)
    fun ncnn_draw_text_c4(pixels: Pointer?, w: Int, h: Int, text: String, x: Int, y: Int, fontpixelsize: Int, color: Int)

    fun ncnn_draw_circle_c1(pixels: ByteArray, w: Int, h: Int, cx: Int, cy: Int, radius: Int, color: Int, thickness: Int)
    fun ncnn_draw_circle_c1(pixels: Pointer?, w: Int, h: Int, cx: Int, cy: Int, radius: Int, color: Int, thickness: Int)
    fun ncnn_draw_circle_c2(pixels: ByteArray, w: Int, h: Int, cx: Int, cy: Int, radius: Int, color: Int, thickness: Int)
    fun ncnn_draw_circle_c2(pixels: Pointer?, w: Int, h: Int, cx: Int, cy: Int, radius: Int, color: Int, thickness: Int)
    fun ncnn_draw_circle_c3(pixels: ByteArray, w: Int, h: Int, cx: Int, cy: Int, radius: Int, color: Int, thickness: Int)
    fun ncnn_draw_circle_c3(pixels: Pointer?, w: Int, h: Int, cx: Int, cy: Int, radius: Int, color: Int, thickness: Int)
    fun ncnn_draw_circle_c4(pixels: ByteArray, w: Int, h: Int, cx: Int, cy: Int, radius: Int, color: Int, thickness: Int)
    fun ncnn_draw_circle_c4(pixels: Pointer?, w: Int, h: Int, cx: Int, cy: Int, radius: Int, color: Int, thickness: Int)

    fun ncnn_draw_line_c1(pixels: ByteArray, w: Int, h: Int, x0: Int, y0: Int, x1: Int, y1: Int, color: Int, thickness: Int)
    fun ncnn_draw_line_c1(pixels: Pointer?, w: Int, h: Int, x0: Int, y0: Int, x1: Int, y1: Int, color: Int, thickness: Int)
    fun ncnn_draw_line_c2(pixels: ByteArray, w: Int, h: Int, x0: Int, y0: Int, x1: Int, y1: Int, color: Int, thickness: Int)
    fun ncnn_draw_line_c2(pixels: Pointer?, w: Int, h: Int, x0: Int, y0: Int, x1: Int, y1: Int, color: Int, thickness: Int)
    fun ncnn_draw_line_c3(pixels: ByteArray, w: Int, h: Int, x0: Int, y0: Int, x1: Int, y1: Int, color: Int, thickness: Int)
    fun ncnn_draw_line_c3(pixels: Pointer?, w: Int, h: Int, x0: Int, y0: Int, x1: Int, y1: Int, color: Int, thickness: Int)
    fun ncnn_draw_line_c4(pixels: ByteArray, w: Int, h: Int, x0: Int, y0: Int, x1: Int, y1: Int, color: Int, thickness: Int)
    fun ncnn_draw_line_c4(pixels: Pointer?, w: Int, h: Int, x0: Int, y0: Int, x1: Int, y1: Int, color: Int, thickness: Int)
}