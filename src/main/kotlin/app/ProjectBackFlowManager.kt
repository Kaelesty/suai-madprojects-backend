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
                flow = MutableSharedFlow<Action>(), scope = CoroutineScope(Dispatchers.IO)
            ).also {
                projectBackFlows[projectId] = ProjectBackFlow(
                    subscribesCount = 1, it
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
        val backFlow: BackFlow
    ) {
        class BackFlow(
            private val flow: MutableSharedFlow<Action>,
            private val scope: CoroutineScope
        ) {
            fun emit(action: Action) {
                scope.launch { flow.emit(action) }
            }
            fun close() {
                scope.cancel()
            }
            fun collect(collector: suspend (Action) -> Unit) {
                scope.launch {
                    flow.collect {
                        collector(it)
                    }
                }
            }
        }
    }
}