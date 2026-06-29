package com.ch.middleware.router.compiler

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

/**
 * KSP 处理器提供者
 *
 * 通过 Java SPI 机制注册 [RouterInitProcessor]。
 * 从 KSP 选项中读取 `router.moduleName` 作为生成类名的模块标识。
 *
 * 使用方式（在模块的 build.gradle.kts 中配置）：
 * ```kotlin
 * ksp {
 *     arg("router.moduleName", "模块名")
 * }
 * ```
 */
class RouterInitProcessorProvider : SymbolProcessorProvider {

    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        val moduleName = environment.options["router.moduleName"] ?: "default"
        return RouterInitProcessor(
            codeGenerator = environment.codeGenerator,
            logger = environment.logger,
            moduleName = moduleName
        )
    }
}
