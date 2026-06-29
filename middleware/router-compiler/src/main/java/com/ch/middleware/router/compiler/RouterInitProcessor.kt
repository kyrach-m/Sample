package com.ch.middleware.router.compiler

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.validate
import java.io.OutputStreamWriter

/**
 * KSP 路由初始化代码生成处理器
 *
 * 在编译期自动扫描以下注解并生成模块初始化类：
 * - [@Route]：页面路由注册
 * - [@RouteInterceptorDef]：拦截器注册（按优先级排序）
 * - [@ServiceProvider]：跨模块服务注册
 * - [@RequireLogin]：需要登录的路径自动标记
 *
 * 每个应用了 KSP 的模块会生成独立的初始化类：
 * `com.ch.middleware.router.generated.GeneratedRouterInit_<moduleName>`
 *
 * 其中 `<moduleName>` 通过 KSP 选项 `router.moduleName` 传入。
 *
 * @see RouterInitProcessorProvider
 */
class RouterInitProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val moduleName: String
) : SymbolProcessor {

    companion object {
        const val PROCESSOR_VERSION = "2.0"

        private const val TAG = "RouterInitProcessor"

        private const val ROUTE_ANNOTATION = "com.ch.middleware.router.annotation.Route"
        private const val INTERCEPTOR_ANNOTATION = "com.ch.middleware.router.annotation.RouteInterceptorDef"
        private const val SERVICE_PROVIDER_ANNOTATION = "com.ch.middleware.router.annotation.ServiceProvider"
        private const val REQUIRE_LOGIN_ANNOTATION = "com.ch.middleware.router.annotation.RequireLogin"

        private const val GENERATED_PACKAGE = "com.ch.middleware.router.generated"
    }

    /** 路由项 */
    private val routeItems = mutableListOf<RouteItem>()

    /** 服务项 */
    private val serviceItems = mutableListOf<ServiceItem>()

    /** 拦截器项 */
    private val interceptorItems = mutableListOf<InterceptorItem>()

    /** 需要登录的路径集合 */
    private val requireLoginPaths = mutableSetOf<String>()

    /** 已处理的源文件 */
    private val sourceFiles = mutableListOf<KSFile>()

    /** 是否已执行过生成 */
    private var generated = false

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val unprocessed = mutableListOf<KSAnnotated>()

        processRouteAnnotation(resolver, unprocessed)
        processServiceProviderAnnotation(resolver, unprocessed)
        processInterceptorAnnotation(resolver, unprocessed)
        processRequireLoginAnnotation(resolver, unprocessed)

        if (!generated) {
            generateInitializer()
            generated = true
        }

        return unprocessed
    }

    private fun processRouteAnnotation(resolver: Resolver, unprocessed: MutableList<KSAnnotated>) {
        val symbols = findSymbolsWithAnnotation(resolver, ROUTE_ANNOTATION)
        symbols.forEach { symbol ->
            if (symbol !is KSClassDeclaration) {
                logger.error("@Route 只能标注在类上", symbol)
                return@forEach
            }

            val annotation = findAnnotationByFqn(symbol, ROUTE_ANNOTATION)
            if (annotation == null) {
                return@forEach
            }

            val path = annotation.arguments.firstOrNull { it.name?.asString() == "path" }?.value as? String
            if (path.isNullOrBlank()) {
                logger.error("@Route 的 path 不能为空", symbol)
                return@forEach
            }

            val description = annotation.arguments.firstOrNull { it.name?.asString() == "description" }?.value as? String ?: ""
            val packageName = symbol.packageName.asString()
            val className = symbol.simpleName.asString()

            routeItems.add(RouteItem(path, className, packageName, description))
            symbol.containingFile?.let { sourceFiles.add(it) }
            logger.info("$TAG [$moduleName]: Found @Route: $path → $packageName.$className")
        }
    }

    private fun processServiceProviderAnnotation(resolver: Resolver, unprocessed: MutableList<KSAnnotated>) {
        val symbols = findSymbolsWithAnnotation(resolver, SERVICE_PROVIDER_ANNOTATION)
        symbols.forEach { symbol ->
            if (symbol !is KSClassDeclaration) {
                logger.error("@ServiceProvider 只能标注在类上", symbol)
                return@forEach
            }

            val annotation = findAnnotationByFqn(symbol, SERVICE_PROVIDER_ANNOTATION)
            if (annotation == null) {
                return@forEach
            }

            val interfaceArg = annotation.arguments.firstOrNull { it.name?.asString() == "interfaceClass" }
            val interfaceType = interfaceArg?.value as? KSType
            if (interfaceType == null) {
                logger.error("@ServiceProvider 的 interfaceClass 必须是有效的 KClass", symbol)
                return@forEach
            }

            val interfaceFqn = interfaceType.declaration.qualifiedName?.asString()
            val implFqn = symbol.qualifiedName?.asString()
            if (interfaceFqn == null || implFqn == null) {
                logger.error("无法解析接口或实现类的全限定名", symbol)
                return@forEach
            }

            val implementsInterface = try {
                symbol.superTypes.any { superType ->
                    superType.resolve().declaration.qualifiedName?.asString() == interfaceFqn
                }
            } catch (e: Exception) {
                true
            }
            if (!implementsInterface) {
                logger.error("@ServiceProvider 标注的类 $implFqn 必须实现接口 $interfaceFqn", symbol)
                return@forEach
            }

            serviceItems.add(ServiceItem(interfaceFqn, implFqn))
            symbol.containingFile?.let { sourceFiles.add(it) }
            logger.info("$TAG [$moduleName]: Found @ServiceProvider: $interfaceFqn → $implFqn")
        }
    }

    private fun processInterceptorAnnotation(resolver: Resolver, unprocessed: MutableList<KSAnnotated>) {
        val symbols = findSymbolsWithAnnotation(resolver, INTERCEPTOR_ANNOTATION)
        symbols.forEach { symbol ->
            if (symbol !is KSClassDeclaration) {
                logger.error("@RouteInterceptorDef 只能标注在类上", symbol)
                return@forEach
            }

            val annotation = findAnnotationByFqn(symbol, INTERCEPTOR_ANNOTATION)
            if (annotation == null) {
                return@forEach
            }

            val priority = annotation.arguments.firstOrNull { it.name?.asString() == "priority" }?.value as? Int ?: 0
            val className = symbol.qualifiedName?.asString()
            if (className == null) {
                logger.error("无法解析拦截器类的全限定名", symbol)
                return@forEach
            }

            interceptorItems.add(InterceptorItem(className, priority))
            symbol.containingFile?.let { sourceFiles.add(it) }
            logger.info("$TAG [$moduleName]: Found @RouteInterceptorDef: $className (priority=$priority)")
        }
    }

    private fun processRequireLoginAnnotation(resolver: Resolver, unprocessed: MutableList<KSAnnotated>) {
        val symbols = findSymbolsWithAnnotation(resolver, REQUIRE_LOGIN_ANNOTATION)
        symbols.forEach { symbol ->
            if (symbol !is KSClassDeclaration) {
                logger.error("@RequireLogin 只能标注在类上", symbol)
                return@forEach
            }

            val routeAnnotation = findAnnotationByFqn(symbol, ROUTE_ANNOTATION)

            if (routeAnnotation != null) {
                val path = routeAnnotation.arguments.firstOrNull { it.name?.asString() == "path" }?.value as? String
                if (!path.isNullOrBlank()) {
                    requireLoginPaths.add(path)
                    symbol.containingFile?.let { sourceFiles.add(it) }
                    logger.info("$TAG [$moduleName]: Found @RequireLogin: $path")
                }
            } else {
                logger.warn("@RequireLogin 标注的类 ${symbol.qualifiedName?.asString()} 没有 @Route 注解，将被忽略", symbol)
            }
        }
    }

    /**
     * 生成模块初始化代码
     *
     * 生成类名：`GeneratedRouterInit_<moduleName>`
     * 生成包名：`com.ch.middleware.router.generated`
     */
    private fun generateInitializer() {
        // 检查重复路由路径
        checkDuplicateRoutes()

        val sortedInterceptors = interceptorItems.sortedBy { it.priority }

        // 生成类名（模块特定）
        val safeModuleName = moduleName.replace(":", "_").replace("-", "_")
        val generatedClassName = "GeneratedRouterInit_$safeModuleName"

        val hasContent = routeItems.isNotEmpty() || serviceItems.isNotEmpty() ||
                interceptorItems.isNotEmpty() || requireLoginPaths.isNotEmpty()

        // 生成代码所在包
        val generatedPackage = GENERATED_PACKAGE

        val code = buildString {
            appendLine("// ============================================================")
            appendLine("// Auto-generated by KSP RouterInitProcessor. DO NOT EDIT.")
            appendLine("// Module: $moduleName")
            appendLine("// ============================================================")
            appendLine("package $generatedPackage")
            appendLine()

            if (hasContent) {
                // RouterHelper 仅在需要注册路由或拦截器时引用
                if (routeItems.isNotEmpty() || interceptorItems.isNotEmpty()) {
                    appendLine("import com.ch.middleware.router.RouterHelper")
                }
                // ServiceManager 仅在需要注册服务时引用
                if (serviceItems.isNotEmpty()) {
                    appendLine("import com.ch.core.base.service.ServiceManager")
                }
                // LoginInterceptor 仅在需要标记登录路径且不同包时引用
                if (requireLoginPaths.isNotEmpty() && generatedPackage != "com.ch.middleware.router") {
                    appendLine("import com.ch.middleware.router.LoginInterceptor")
                }
                appendLine()

                routeItems.forEach { item ->
                    val fqn = "${item.packageName}.${item.className}"
                    if (item.packageName != generatedPackage) {
                        appendLine("import $fqn")
                    }
                }
                serviceItems.forEach { item ->
                    val implPkg = item.implFqn.substringBeforeLast('.')
                    if (implPkg != generatedPackage) {
                        appendLine("import ${item.implFqn}")
                    }
                    // 也需要导入接口类
                    val interfacePkg = item.interfaceFqn.substringBeforeLast('.')
                    if (interfacePkg != generatedPackage) {
                        appendLine("import ${item.interfaceFqn}")
                    }
                }
                interceptorItems.forEach { item ->
                    val pkg = item.className.substringBeforeLast('.')
                    if (pkg != generatedPackage) {
                        appendLine("import ${item.className}")
                    }
                }
                appendLine()
            }

            appendLine("/**")
            appendLine(" * KSP 自动生成的路由初始化器（模块：$moduleName）")
            appendLine(" *")
            appendLine(" * 统计：")
            appendLine(" * - 路由：${routeItems.size} 条")
            appendLine(" * - 服务：${serviceItems.size} 条")
            appendLine(" * - 拦截器：${sortedInterceptors.size} 个")
            appendLine(" * - 需登录路径：${requireLoginPaths.size} 条")
            appendLine(" *")
            appendLine(" * **请勿手动修改此文件！**")
            appendLine(" */")
            appendLine("object $generatedClassName {")
            appendLine()
            appendLine("    fun init() {")

            if (hasContent) {
                // 仅在需要时声明 helper 变量
                if (routeItems.isNotEmpty() || interceptorItems.isNotEmpty()) {
                    appendLine("        val helper = RouterHelper")
                    appendLine()
                }

                if (routeItems.isNotEmpty()) {
                    appendLine("        // ========== 注册路由（${routeItems.size} 条） ==========")
                    routeItems.forEach { item ->
                        val desc = if (item.description.isNotEmpty()) " // ${item.description}" else ""
                        appendLine("        helper.registerRoute(\"${item.path}\", ${item.className}::class.java)$desc")
                    }
                    appendLine()
                }

                if (sortedInterceptors.isNotEmpty()) {
                    appendLine("        // ========== 注册拦截器（${sortedInterceptors.size} 个） ==========")
                    sortedInterceptors.forEach { item ->
                        val simpleName = item.className.substringAfterLast('.')
                        appendLine("        helper.addInterceptorV2($simpleName()) // priority=${item.priority}")
                    }
                    appendLine()
                }

                if (serviceItems.isNotEmpty()) {
                    appendLine("        // ========== 注册跨模块服务（${serviceItems.size} 条） ==========")
                    serviceItems.forEach { item ->
                        val interfaceSimple = item.interfaceFqn.substringAfterLast('.')
                        val implSimple = item.implFqn.substringAfterLast('.')
                        appendLine("        ServiceManager.registerService($interfaceSimple::class.java, $implSimple::class.java)")
                    }
                    appendLine()
                }

                if (requireLoginPaths.isNotEmpty()) {
                    appendLine("        // ========== 标记需要登录的路径（${requireLoginPaths.size} 条） ==========")
                    requireLoginPaths.forEach { path ->
                        appendLine("        LoginInterceptor.markRequireLogin(\"$path\")")
                    }
                    appendLine()
                }
            }

            appendLine("    }")
            appendLine("}")
        }

        val dependencies = if (sourceFiles.isNotEmpty()) {
            Dependencies(aggregating = false, *sourceFiles.distinct().toTypedArray())
        } else {
            Dependencies(aggregating = false)
        }

        val outputStream = codeGenerator.createNewFile(
            dependencies = dependencies,
            packageName = GENERATED_PACKAGE,
            fileName = generatedClassName,
            extensionName = "kt"
        )
        OutputStreamWriter(outputStream, Charsets.UTF_8).use { writer ->
            writer.write(code)
        }

        logger.info("$TAG [$moduleName]: Generated $generatedClassName with ${routeItems.size} routes, ${serviceItems.size} services, ${interceptorItems.size} interceptors, ${requireLoginPaths.size} requireLogin paths")
    }

    private fun checkDuplicateRoutes() {
        val pathCount = routeItems.groupBy { it.path }
        pathCount.filter { it.value.size > 1 }.forEach { (path, items) ->
            val classNames = items.joinToString(", ") { "${it.packageName}.${it.className}" }
            logger.error("[$moduleName] 重复的路由路径: \"$path\" 被多个类使用: $classNames。请确保每个 @Route 的 path 全局唯一。")
        }
    }

    private fun findSymbolsWithAnnotation(resolver: Resolver, annotationFqn: String): Sequence<KSAnnotated> {
        val standardSymbols = resolver.getSymbolsWithAnnotation(annotationFqn).toList()

        val fileSymbols = mutableListOf<KSAnnotated>()
        val shortName = annotationFqn.substringAfterLast('.')
        val foundQualifiedNames = standardSymbols.mapNotNull { sym ->
            (sym as? KSClassDeclaration)?.qualifiedName?.asString()
        }.toSet()

        resolver.getAllFiles().forEach { file ->
            findClassDeclarationsWithAnnotation(file.declarations, shortName, annotationFqn, foundQualifiedNames, fileSymbols)
        }

        return (standardSymbols + fileSymbols).asSequence().distinct()
    }

    private fun findClassDeclarationsWithAnnotation(
        declarations: Sequence<KSDeclaration>,
        shortName: String,
        annotationFqn: String,
        skipQualifiedNames: Set<String>,
        result: MutableList<KSAnnotated>
    ) {
        declarations.forEach { declaration ->
            if (declaration is KSClassDeclaration) {
                val qn = declaration.qualifiedName?.asString()
                if (qn != null && qn in skipQualifiedNames) {
                    findClassDeclarationsWithAnnotation(declaration.declarations, shortName, annotationFqn, skipQualifiedNames, result)
                    return@forEach
                }

                val hasAnnotation = declaration.annotations.any { anno ->
                    val annoShortName = anno.shortName.asString()
                    if (annoShortName == shortName) {
                        val fqn = try {
                            anno.annotationType.resolve().declaration.qualifiedName?.asString()
                        } catch (e: Exception) {
                            null
                        }
                        fqn == annotationFqn || fqn == null
                    } else {
                        false
                    }
                }

                if (hasAnnotation) {
                    result.add(declaration)
                }

                findClassDeclarationsWithAnnotation(declaration.declarations, shortName, annotationFqn, skipQualifiedNames, result)
            }
        }
    }

    private fun findAnnotationByFqn(symbol: KSAnnotated, annotationFqn: String): com.google.devtools.ksp.symbol.KSAnnotation? {
        val shortName = annotationFqn.substringAfterLast('.')
        return symbol.annotations.firstOrNull { anno ->
            if (anno.shortName.asString() != shortName) {
                false
            } else {
                val fqn = try {
                    anno.annotationType.resolve().declaration.qualifiedName?.asString()
                } catch (e: Exception) {
                    null
                }
                fqn == annotationFqn || fqn == null
            }
        }
    }
}

private data class RouteItem(val path: String, val className: String, val packageName: String, val description: String)
private data class ServiceItem(val interfaceFqn: String, val implFqn: String)
private data class InterceptorItem(val className: String, val priority: Int)
