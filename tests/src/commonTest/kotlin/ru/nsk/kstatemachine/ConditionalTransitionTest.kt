package ru.nsk.kstatemachine

import io.kotest.core.spec.style.StringSpec
import io.mockk.called
import io.mockk.verify
import io.mockk.verifySequence
import ru.nsk.kstatemachine.ConditionalTransitionTestData.ConditionEvent

private object ConditionalTransitionTestData {
    class ConditionEvent(val data: Boolean) : Event
}

class ConditionalTransitionTest : StringSpec({
    CoroutineStarterType.values().forEach { coroutineStarterType ->
        "conditional transition stay()" {
            val callbacks = mockkCallbacks()

            val first = object : DefaultState("first") {}

            val machine = createTestStateMachine(coroutineStarterType) {
                addInitialState(first) {
                    callbacks.listen(this)

                    transitionConditionally<SwitchEvent> {
                        direction = { stay() }
                        callbacks.listen(this)
                    }
                }
            }

            verifySequenceAndClear(callbacks) { callbacks.onStateEntry(first) }

            machine.processEventBlocking(SwitchEvent)

            verifySequence { callbacks.onTransitionTriggered(SwitchEvent) }
        }

        "conditional transition noTransition()" {
            val callbacks = mockkCallbacks()

            val first = object : DefaultState("first") {}

            val machine = createTestStateMachine(coroutineStarterType) {
                addInitialState(first) {
                    callbacks.listen(this)

                    transitionConditionally<SwitchEvent> {
                        direction = { noTransition() }
                        callbacks.listen(this)
                    }
                }
                onTransitionTriggered { callbacks.onTransitionTriggered(it.event) }
            }

            verifySequenceAndClear(callbacks) { callbacks.onStateEntry(first) }

            machine.processEventBlocking(SwitchEvent)
            verify { callbacks wasNot called }
        }

        "conditional transition targetState()" {
            val callbacks = mockkCallbacks()

            val first = object : DefaultState("first") {}
            val second = object : DefaultState("second") {}

            val machine = createTestStateMachine(coroutineStarterType) {
                addInitialState(first) {
                    callbacks.listen(this)

                    transitionConditionally<SwitchEvent> {
                        direction = { targetState(second) }
                        callbacks.listen(this)
                    }
                }
                addState(second) { callbacks.listen(this) }
            }

            verifySequenceAndClear(callbacks) { callbacks.onStateEntry(first) }

            machine.processEventBlocking(SwitchEvent)
            verifySequence {
                callbacks.onTransitionTriggered(SwitchEvent)
                callbacks.onStateExit(first)
                callbacks.onStateEntry(second)
            }
        }

        "conditional transition" {
            val callbacks = mockkCallbacks()

            val first = object : DefaultState("first") {}
            val second = object : DefaultState("second") {}

            val machine = createTestStateMachine(coroutineStarterType) {
                addInitialState(first) {
                    callbacks.listen(this)

                    transitionConditionally<SwitchEvent> {
                        direction = { targetState(second) }
                        callbacks.listen(this)
                    }
                }
                addState(second) { callbacks.listen(this) }
            }

            verifySequenceAndClear(callbacks) { callbacks.onStateEntry(first) }

            machine.processEventBlocking(SwitchEvent)
            verifySequence {
                callbacks.onTransitionTriggered(SwitchEvent)
                callbacks.onStateExit(first)
                callbacks.onStateEntry(second)
            }
        }

        "conditional transition by event data" {
            val callbacks = mockkCallbacks()

            val first = object : DefaultState("first") {}
            val second = object : DefaultState("second") {}
            val third = object : DefaultState("third") {}

            val machine = createTestStateMachine(coroutineStarterType) {
                addInitialState(first) {
                    callbacks.listen(this)

                    transitionConditionally<ConditionEvent> {
                        direction = { if (event.data) targetState(second) else targetState(third) }
                        callbacks.listen(this)
                    }
                }
                addState(second) { callbacks.listen(this) }
                addState(third) { callbacks.listen(this) }
            }

            val event = ConditionEvent(false)
            verifySequenceAndClear(callbacks) { callbacks.onStateEntry(first) }

            machine.processEventBlocking(event)
            verifySequence {
                callbacks.onTransitionTriggered(event)
                callbacks.onStateExit(first)
                callbacks.onStateEntry(third)
            }
        }

        "conditional transition by argument" {
            val callbacks = mockkCallbacks()

            val first = object : DefaultState("first") {}
            val second = object : DefaultState("second") {}
            val third = object : DefaultState("third") {}

            val machine = createTestStateMachine(coroutineStarterType) {
                addInitialState(first) {
                    callbacks.listen(this)

                    transitionConditionally<SwitchEvent> {
                        direction = { if (argument as Boolean) targetState(second) else targetState(third) }
                        callbacks.listen(this)
                    }
                }
                addState(second) { callbacks.listen(this) }
                addState(third) { callbacks.listen(this) }
            }

            verifySequenceAndClear(callbacks) { callbacks.onStateEntry(first) }

            machine.processEventBlocking(SwitchEvent, false)
            verifySequence {
                callbacks.onTransitionTriggered(SwitchEvent)
                callbacks.onStateExit(first)
                callbacks.onStateEntry(third)
            }
        }
    }
})