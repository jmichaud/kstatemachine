package ru.nsk.samples

import ru.nsk.kstatemachine.*
import ru.nsk.samples.FinishedEventSample.States.*
import ru.nsk.samples.FinishedEventSample.SwitchEvent

private object FinishedEventSample {
    object SwitchEvent : Event

    sealed class States : DefaultState() {
        object State1 : States()
        object State2 : States()
        object State11 : States()
        object State12 : States(), FinalState
    }
}

/**
 * [FinishedEvent] is generated when state enters its child final state
 */
fun main() {
    val machine = createStateMachine {
        logger = StateMachine.Logger { println(it) }

        addInitialState(State1) {
            addInitialState(State11) {
                transition<SwitchEvent>(targetState = State12)
            }
            // State1 finished when reaches its child final state
            addFinalState(State12)
            // FinishedEvent is generated by the library when State1 finishes
            transition<FinishedEvent>(targetState = State2)
        }
        addState(State2)
    }

    machine.processEvent(SwitchEvent)

    check(State2 in machine.activeStates())
}