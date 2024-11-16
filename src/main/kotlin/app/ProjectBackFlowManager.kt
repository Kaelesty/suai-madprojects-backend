package app

import entities.Action
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

object ProjectBackFlowManager {

    private val projectBackFlows: MutableMap<Int, ProjectBackFlow> = mutableMapOf()

    fun getProjectBackFlow(projectId: Int): ProjectBackFlow.BackFlow {
        val backflow = projectBackFlows[projectId]
        if (backflow == null) {
            return ProjectBackFlow.BackFlow(
                flow = MutableSharedFlow<Application.ActionHolder>(), scope = CoroutineScope(Dispatchers.IO)
            ).also {
                projectBackFlows[projectId] = ProjectBackFlow(
                    subscribesCount = 1, it, projectId
                )
            }
        }
        else {
            backflow.subscribesCount += 1
            return backflow.backFlow
        }
    }

    fun unsubscribe(projectId: Int) {
        val backflow = projectBackFlows[projectId]
        backflow?.let {
            backflow.subscribesCount -= 1
            if (backflow.subscribesCount == 0) {
                backflow.backFlow.close()
                projectBackFlows.remove(projectId)
            }
        }
    }

    class ProjectBackFlow(
        var subscribesCount: Int,
        val backFlow: BackFlow,
        val projectId: Int,
    ) {
        class BackFlow(
            private val flow: MutableSharedFlow<Application.ActionHolder>,
            private val scope: CoroutineScope
        ) {
            fun emit(action: Action, projectId: Int) {
                scope.launch { flow.emit(
                    Application.ActionHolder(
                        action, projectId = projectId
                    )
                ) }
            }
            fun close() {
                scope.cancel()
            }
            fun collect(collector: suspend (Application.ActionHolder) -> Unit) {
                scope.launch {
                    flow.collect {
                        collector(it)
                    }
                }
            }
        }
    }
}