package com.ch.service.startup.dag

/**
 * 循环依赖异常
 *
 * 当启动任务之间存在循环依赖时抛出此异常。
 * 例如：A → B → C → A 形成环路，无法确定执行顺序。
 *
 * @param cycle 循环依赖链路中的任务名称列表
 */
class CyclicDependencyException(
    val cycle: List<String>
) : IllegalStateException(
    "检测到循环依赖: ${cycle.joinToString(" → ")} → ${cycle.first()}"
)

/**
 * DAG 任务图
 *
 * 负责解析 [StartupTask] 之间的依赖关系，构建有向无环图（DAG），
 * 并通过拓扑排序确定执行顺序。
 *
 * 核心功能：
 * - 构建邻接表表示的有向图
 * - 检测循环依赖（DFS 三色标记法）
 * - 拓扑排序确定执行顺序（Kahn 算法）
 * - 按层级分组，支持并行调度
 *
 * 使用示例：
 * ```kotlin
 * val graph = TaskGraph()
 * graph.addTask(taskA)
 * graph.addTask(taskB) // taskB.dependencies() = [TaskA::class]
 * graph.build()        // 构建 DAG 并检测循环依赖
 * val layers = graph.getExecutionLayers() // 获取分层执行顺序
 * ```
 *
 * 算法说明：
 * - 循环检测：使用 DFS 三色标记法（WHITE=未访问, GRAY=访问中, BLACK=已完成）
 * - 拓扑排序：使用 Kahn 算法（基于入度的 BFS）
 * - 时间复杂度：O(V + E)，V 为任务数，E 为依赖边数
 */
class TaskGraph {

    /**
     * 任务注册表：类名 → 任务实例
     */
    private val taskMap = mutableMapOf<String, StartupTask>()

    /**
     * 邻接表：任务名 → 依赖它的任务名列表
     * 边方向：dependency → dependent（从被依赖方指向依赖方）
     */
    private val adjacency = mutableMapOf<String, MutableList<String>>()

    /**
     * 拓扑排序后的执行顺序
     */
    private var sortedOrder: List<String> = emptyList()

    /**
     * 分层执行顺序：每一层内的任务可以并行执行
     * 层与层之间必须串行（前一层全部完成后才能执行下一层）
     */
    private var executionLayers: List<List<String>> = emptyList()

    /**
     * 添加任务到图中
     *
     * @param task 启动任务实例
     * @throws IllegalArgumentException 如果任务名称重复
     */
    fun addTask(task: StartupTask) {
        require(task.name !in taskMap) {
            "任务名称重复: ${task.name}"
        }
        taskMap[task.name] = task
        adjacency[task.name] = mutableListOf()
    }

    /**
     * 构建 DAG
     *
     * 执行以下步骤：
     * 1. 解析所有任务的依赖关系，构建邻接表
     * 2. 检测循环依赖
     * 3. 拓扑排序确定执行顺序
     * 4. 分层分组以支持并行调度
     *
     * @throws CyclicDependencyException 如果检测到循环依赖
     * @throws IllegalStateException 如果依赖的任务未注册
     */
    fun build() {
        // Step 1: 构建邻接表和入度表
        val inDegree = mutableMapOf<String, Int>()
        taskMap.keys.forEach { inDegree[it] = 0 }

        taskMap.forEach { (name, task) ->
            task.dependencies().forEach { depClass ->
                val depName = depClass.simpleName
                    ?: throw IllegalStateException("无法获取任务类名: $depClass")

                // 检查依赖的任务是否已注册
                // 支持通过类名或 name 属性查找
                val actualDepName = taskMap.keys.find { key ->
                    key == depName || taskMap[key]?.javaClass?.name == depClass.name
                } ?: throw IllegalStateException(
                    "任务 $name 依赖的 $depName 未注册。请先通过 addTask() 注册。"
                )

                // 添加边：被依赖方 → 依赖方
                adjacency.getOrPut(actualDepName) { mutableListOf() }.add(name)
                inDegree[name] = (inDegree[name] ?: 0) + 1
            }
        }

        // Step 2: 循环依赖检测（DFS 三色标记法）
        detectCycle()

        // Step 3: 拓扑排序（Kahn 算法）
        sortedOrder = topologicalSort(inDegree)

        // Step 4: 分层分组
        executionLayers = buildLayers()
    }

    /**
     * DFS 三色标记法检测循环依赖
     *
     * 颜色定义：
     * - WHITE (0): 未访问
     * - GRAY (1): 正在访问中（在递归栈中）
     * - BLACK (2): 已完成访问
     *
     * 如果在 DFS 过程中遇到 GRAY 颜色的节点，说明存在回边，即存在环。
     *
     * @throws CyclicDependencyException 如果检测到循环依赖
     */
    private fun detectCycle() {
        val WHITE = 0
        val GRAY = 1
        val BLACK = 2
        val color = mutableMapOf<String, Int>()
        taskMap.keys.forEach { color[it] = WHITE }

        val path = mutableListOf<String>()

        fun dfs(node: String) {
            color[node] = GRAY
            path.add(node)

            for (neighbor in adjacency[node] ?: emptyList()) {
                when (color[neighbor]) {
                    GRAY -> {
                        // 找到环：从 path 中提取循环链路
                        val cycleStart = path.indexOf(neighbor)
                        val cycle = path.subList(cycleStart, path.size).toList()
                        throw CyclicDependencyException(cycle)
                    }
                    WHITE -> dfs(neighbor)
                    BLACK -> { /* 已完成，跳过 */ }
                }
            }

            path.removeAt(path.size - 1)
            color[node] = BLACK
        }

        // 从所有未访问节点开始 DFS
        taskMap.keys.forEach { node ->
            if (color[node] == WHITE) {
                dfs(node)
            }
        }
    }

    /**
     * Kahn 算法拓扑排序
     *
     * 基于入度的 BFS 算法：
     * 1. 将所有入度为 0 的节点加入队列
     * 2. 每次取出一个节点，将其邻居的入度减 1
     * 3. 入度变为 0 的节点加入队列
     * 4. 重复直到队列为空
     *
     * 排序结果保证：如果 A 依赖 B，则 B 一定在 A 之前。
     *
     * @param inDegree 入度表
     * @return 拓扑排序后的任务名列表
     */
    private fun topologicalSort(inDegree: MutableMap<String, Int>): List<String> {
        // 使用优先队列按优先级排序（高优先级先执行）
        val queue = ArrayDeque<String>()
        inDegree.filter { it.value == 0 }
            .toList()
            .sortedByDescending { taskMap[it.first]?.priority() ?: 0 }
            .forEach { queue.addLast(it.first) }

        val result = mutableListOf<String>()

        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            result.add(node)

            // 按优先级排序邻居
            val neighbors = (adjacency[node] ?: emptyList())
                .sortedByDescending { taskMap[it]?.priority() ?: 0 }

            for (neighbor in neighbors) {
                inDegree[neighbor] = (inDegree[neighbor] ?: 1) - 1
                if (inDegree[neighbor] == 0) {
                    queue.addLast(neighbor)
                }
            }
        }

        // 如果排序结果数量不等于任务数量，说明有环（理论上前面已检测）
        if (result.size != taskMap.size) {
            throw CyclicDependencyException(result)
        }

        return result
    }

    /**
     * 构建分层执行顺序
     *
     * 每一层内的任务没有相互依赖，可以并行执行。
     * 层与层之间必须串行：第 N 层全部完成后才能执行第 N+1 层。
     *
     * 分层策略：
     * - 第 0 层：入度为 0 的任务（无依赖）
     * - 第 N 层：所有依赖都在前 N-1 层中的任务
     *
     * @return 分层列表，每个子列表是一层可并行执行的任务名
     */
    private fun buildLayers(): List<List<String>> {
        val inDegree = mutableMapOf<String, Int>()
        taskMap.keys.forEach { inDegree[it] = 0 }

        taskMap.forEach { (name, task) ->
            task.dependencies().forEach { depClass ->
                val depName = depClass.simpleName ?: return@forEach
                val actualDepName = taskMap.keys.find { key ->
                    key == depName || taskMap[key]?.javaClass?.name == depClass.name
                } ?: return@forEach
                inDegree[name] = (inDegree[name] ?: 0) + 1
            }
        }

        val layers = mutableListOf<List<String>>()
        var remaining = inDegree.toMutableMap()

        while (remaining.isNotEmpty()) {
            // 当前层：入度为 0 的任务
            val currentLayer = remaining.filter { it.value == 0 }
                .toList()
                .sortedByDescending { taskMap[it.first]?.priority() ?: 0 }
                .map { it.first }

            if (currentLayer.isEmpty()) {
                throw CyclicDependencyException(remaining.keys.toList())
            }

            layers.add(currentLayer)

            // 移除当前层，更新入度
            currentLayer.forEach { name ->
                remaining.remove(name)
                (adjacency[name] ?: emptyList()).forEach { neighbor ->
                    remaining[neighbor] = (remaining[neighbor] ?: 1) - 1
                }
            }
        }

        return layers
    }

    /**
     * 获取分层执行顺序
     *
     * @return 分层列表，每个子列表是一层可并行执行的任务名
     * @throws IllegalStateException 如果尚未调用 [build]
     */
    fun getExecutionLayers(): List<List<String>> {
        check(executionLayers.isNotEmpty() || taskMap.isEmpty()) {
            "请先调用 build() 构建 DAG"
        }
        return executionLayers
    }

    /**
     * 获取拓扑排序后的执行顺序
     *
     * @return 任务名列表，按执行顺序排列
     */
    fun getSortedOrder(): List<String> {
        check(sortedOrder.isNotEmpty() || taskMap.isEmpty()) {
            "请先调用 build() 构建 DAG"
        }
        return sortedOrder
    }

    /**
     * 根据任务名获取任务实例
     *
     * @param name 任务名
     * @return 任务实例，不存在时返回 null
     */
    fun getTask(name: String): StartupTask? = taskMap[name]

    /**
     * 获取所有已注册的任务实例
     */
    fun getAllTasks(): Collection<StartupTask> = taskMap.values
}
