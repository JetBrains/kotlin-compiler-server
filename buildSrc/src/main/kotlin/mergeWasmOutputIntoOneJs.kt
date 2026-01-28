import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
fun mergeWasmOutputIntoOneJs(
    jsBuiltins: String?,
    importObjectJsContent: String,
    jsCode: String,
    wasmOutput: ByteArray,
    outputFileName: String,
    hash: String,
    staticUrl: String,
): String {

    val jsBuiltinsAlias =
        Regex("""import\s+\*\s+as\s+(\S+)\s+from\s+'\./$outputFileName\.js-builtins-$hash\.mjs';""")

    val jsBuiltInsAlias = jsBuiltinsAlias.find(importObjectJsContent)?.groupValues?.get(1)

    val replacedImportObjectContent =
        (jsBuiltins?.toByteArray()?.let { byteContent ->
            importObjectJsContent
                .replace(
                    jsBuiltinsAlias,
                    "const $jsBuiltInsAlias = await import(`data:application/javascript;base64, ${
                        Base64.encode(
                            byteContent
                        )
                    }`)"
                )
        } ?: importObjectJsContent).let {
            if (staticUrl.isNotEmpty()) {
                it.replace(
                    "from './",
                    "from '$staticUrl/",
                )
            } else it
        }

    return jsCode
        .replace(
            "import {",
            "const {"
        )
        .replace(
            "from './${outputFileName}.import-object-$hash.mjs'",
            "= await import(`data:application/javascript;base64,${
                Base64.encode(
                    replacedImportObjectContent.toByteArray()
                )
            }`) "
        )
        .replace(
            "__TAG as wasmTag",
            "__TAG: wasmTag"
        )
        .replace(
            "wasmInstance = (await WebAssembly.instantiateStreaming(fetch(new URL('./${outputFileName}-$hash.wasm',import.meta.url).href), importObject, wasmOptions)).instance;",
            "wasmInstance = await (async () => {\n" +
                    "  const wasmBase64 = await fetch(`data:application/wasm;base64,${Base64.encode(wasmOutput)}`); \n" +
                    "  const wasmBinary = new Uint8Array(await wasmBase64.arrayBuffer());\n" +
                    "  return (await WebAssembly.instantiate(wasmBinary, importObject)).instance;\n" +
                    "  })();"
        ) + "\n export const instantiate = () => Promise.resolve();"
}