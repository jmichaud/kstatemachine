# KStateMachine

![Build with Gradle](https://github.com/nsk90/kstatemachine/workflows/Build%20with%20Gradle/badge.svg)

## Overview
Final State machine (FSM) library written in Kotlin.

Main features are:
* Kotlin DSL syntax for defining state machine structure;
* conditional transitions, when target state is dynamic 
and is calculated in a moment of event processing depending on application business logic;
* argument passing for events and transitions.

_The library is currently in a development phase. Your are welcome to propose useful features._

Building blocks (main classes) of this library:
* `StateMachine` is a collection of states and transitions between them, processes events when started;
* `State` states where state machine can go to;
* `Event` is a base class for events or other words actions which are processed by state machine and may trigger transitions;
* `Transition` is an operation of moving from one state to another.

Working with state machine consists of two steps:
* creation and initial setup, here you may set custom actions (side effects) via listeners
 to be performed on entering/exiting states and transitions between them;
* processing events, on which state machine can switch its states and notify about changes.
```kotlin
val stateMachine = createStateMachine {
    // setup is made in this block ...
}
// after setup it is ready to process events
stateMachine.processEvent(SwitchGreenEvent)
// ...
stateMachine.processEvent(SwitchYellowEvent)
```

## Sample state machine diagram
![Traffic light diagram](./doc/images/trafficlight.png)

## Create state machine
First of all we create a state machine with `createStateMachine()` function:
```kotlin
val stateMachine = createStateMachine(
    "Traffic lights" // name is convenient for debugging, and may be omitted
) {
    // set up state machine ...
}
```

## Setup states
In state machine setup block we define states with `state()` function and set initial one with `setInitialState()`:
```kotlin
createStateMachine {
    // use state() function to create State and add it to StateMachine
    val greenState = state()
    val yellowState = state("Yellow") // state name is optional and is useful for debugging
    
    // state machine enters this state after setup is complete
    setInitialState(greenState)
}
```

You can use State subclasses with `addState()` function:
```kotlin
object SomeState : State()

createStateMachine {
    val someState = addState(SomeState())
    //...
}
```

In state setup blocks we can add listeners for states:
```kotlin
state {
    onEntry { println("Green light is switched on") }
    onExit { println("Green light will be switched off") }
}
```

Or even shorter: 
```kotlin
state().onEntry { /*...*/ }
```

Or the same with explicit syntax:
```kotlin
val greenState = state()
greenState.addListener(object: State.Listener {
    override fun onEntry(transitionParams: TransitionParams<*>) {}
    override fun onExit(transitionParams: TransitionParams<*>) {}
})
```

## Setup transitions
When we have multiple states we should say for each one, which events will trigger transitions to another states:
```kotlin
greenState {
    // setup transition which is triggered on SwitchYellowEvent
    transition<SwitchYellowEvent> {
        // set target state where state machine go when this transition if performed
        targetState = yellowState
    }
}
```
_Note: only one transition is possible per event type. 
This means you cannot have multiple transitions parametrized with same Event subclass._

Transition may have no target state (targetState is null) which means that state machine stays 
in current state when such transition triggers:
```kotlin
greenState {
    transition<SwitchYellowEvent>()
}
```

Same as for states we can listen to transition triggering:
```kotlin
transition<SwitchYellowEvent> {
    targetState = yellowState
    onTriggered { println("Transition to $targetState is triggered by ${it.event}") }
}
```

There might be many transitions from one state to another. 
It is possible to listen to all of them in state machine setup block:
```kotlin
createStateMachine {
    //...
    onTransition { sourceState, targetState, event, argument ->
        // listen to every performed transition here
    }
}
``` 

## Conditional transitions
State machine becomes more powerful tool when you can choose target state 
depending on your business logic (some external state).

There are three options in choosing direction of next state:
* stay - in this case transition is triggered but state is not changed;
* targetState - transition is triggered and state machine goes to a specified state;
* noTransition - transition is not triggered.

To use conditional transitions you pass a lambda into `transitionConditionally()` function which makes desired decision: 
```kotlin
redState {
    // a conditional transition helps to control when a transition 
    // should be triggered and determine its target state
    transitionConditionally<SwitchGreenEvent> {
        direction = {
            // suppose you have a function returning some 
            // business logic value which may differ
            fun getCondition() = 0 
            
            when(getCondition()) {
                0 -> targetState(greenState)
                1 -> targetState(yellowState)
                2 -> stay()
                else -> noTransition()
            }
        }
    }
    // same as before you can listen when conditional transition is triggered
    onTriggered { println("Conditional transition is triggered") }
}
```
## Logging
You can enable internal state machine logging on your platform:
```kotlin
createStateMachine {
    //...
    logger = StateMachine.Logger {
        println(it)
    }
}
```
## Error handling
By default state machine simply ignores events that does not match any defined transition.
You can see those events if logging is enabled or use custom IgnoredEventHandler:
```kotlin
createStateMachine {
    //...
    ignoredEventHandler = StateMachine.IgnoredEventHandler { _, event, _ ->
        error("unexpected $event")
    }
}
```

It is not allowed to call processEvent() while state machine is already processing event.
For example from notification listener. By default state machine will throw exception in this case.
But you can set custom PendingEventHandler:
```kotlin
createStateMachine {
    //...
    pendingEventHandler = StateMachine.PendingEventHandler { pendingEvent, _ ->
        error(
            "$this can not process pending $pendingEvent " +
                    "as event processing is already running. " +
                    "Do not call processEvent() from notification listeners."
        )
    }
}
```

## Arguments
_Note: Type of arguments is `Any?`, so it is not type safe ot use them._

### Event argument
Usually if event may hold some data we define Event subclass, it is type safe. 
But sometimes if data is optional it may be simpler to use event argument. 
You can specify arbitrary argument with event in `processEvent()` function.
Then you can get this argument in state and transition listeners. 
```kotlin
val stateMachine = createStateMachine {
    state("offState").onEntry {
        println("Event ${it.event} argument: ${it.argument}")
    }
    //...
}
// pass argument with event
stateMachine.processEvent(TurnOn, 42)
```

### Transition argument
If transition listener produce some data, you can pass it to target state as a transition argument:
```kotlin
val second = state("second").onEntry {
    println("Transition argument: ${it.transition.argument}")
}
state("first") {
    transition<SwitchEvent> {
        targetState = second
        onTriggered { it.transition.argument = 42 }
    }
}
```
_Note: it is up to user to control that argument field is set from one listener.
You can use some mutable data structure and fill it from multiple listeners._

## Multi threading
State machine is designed to work in single thread.
So if you need to process events from different threads you can post them to some thread safe queue
and start single thread which will pull events from that queue in a loop and call `processEvent()` function.

## Do not
State machine is a powerful tool to control states so let it do its job, 
do not select target state by sending different event types depending on business logic state, 
let the state machine to make decision for you.

Wrong:
```kotin
if (somethingHappend)
    stateMachine.processEvent(FirstEvent)
else 
    stateMachine.processEvent(SecondEvent)
```
Correct - let the state machine to make decisions on event:
```kotin
stateMachine.processEvent(SomethingHappenedEvent)
```

## Minimal syntax sample code
```kotlin
// define your events
object TurnOn : Event
object TurnOff : Event

fun main() {
    // setup state machine
    val stateMachine = createStateMachine {
        // setup states and transitions
        val offState = state()
        val onState = state {
            transition<TurnOff> { targetState = offState }
        }
        setInitialState(offState)

        offState {
            transition<TurnOn> { targetState = onState }
        }
    }

    // process events
    stateMachine.processEvent(TurnOn)
    stateMachine.processEvent(TurnOff)
}
```

## Full syntax sample code
This sample shows different syntax variants and library possibilities in one place, so it looks messy.
```kotlin
// define your events
private object SwitchGreenEvent : Event
private object SwitchYellowEvent : Event

// events often hold some useful data
private class SwitchRedEvent(val data: String) : Event

// you can subclass State if you need
private class RedState(val data: Int) : State("Red")

fun main() {
    val stateMachine = createStateMachine(
        "Traffic lights" // optional name
    ) {
        // setup simple states
        val greenState = state("Green") // state name is optional
        val yellowState = state("Yellow")
        // or add state subclass
        val redState = addState(RedState(42))
        setInitialState(greenState)

        greenState {
            // add listeners, which are signaled on entering or exiting from the state
            onEntry { println("Green light is switched on") }
            onExit { println("Green light will be switched off") }
            // setup transition which is triggered on SwitchYellowEvent
            transition<SwitchYellowEvent> {
                targetState = yellowState
                // add listener which is signaled when transition is triggered
                onTriggered { println("Switching to $targetState") }
            }
        }

        // you can use explicit syntax for adding listeners
        greenState.addListener(object : State.Listener {
            override fun onEntry(transitionParams: TransitionParams<*>) {}
            override fun onExit(transitionParams: TransitionParams<*>) {}
        })

        yellowState {
            val transition = transition<SwitchRedEvent> {
                targetState = redState
                // you can access data from events
                onTriggered { println("Switching to $targetState, data: ${it.event.data}") }
            }
            transition.onTriggered { /* just another way for adding listeners */ }
        }
        yellowState.onEntry { /* just another way for adding listeners*/ }

        redState {
            // a conditional transition helps to control when a transition
            // should be triggered and determine its target state
            transitionConditionally<SwitchGreenEvent> {
                direction = {
                    // suppose you have a function
                    // returning some business logic value which may differ
                    fun getCondition() = 0

                    when (getCondition()) {
                        0 -> targetState(greenState)
                        1 -> targetState(yellowState)
                        2 -> stay()
                        else -> noTransition()
                    }
                }
                // you can access argument passed to processEvent() function
                // and data from state subclass
                onTriggered {
                    println("Switching state with argument: ${it.argument}, and data: ${this@redState.data}")
                }
            }
        }

        onTransition { sourceState, targetState, event, argument ->
            // it is possible to listen all transitions in one place
            // instead of listening each transition separately
        }

        // set logger to enable internal state machine logging on your platform
        logger = StateMachine.Logger { println(it) }

        // it is possible to set custom ignored event handler
        // for event that does not match any transition,
        // for example to throw exceptions on ignored events
        ignoredEventHandler = StateMachine.IgnoredEventHandler { _, _, _ ->

        }

        // you can set custom pending event handler which is triggered
        // if processEvent() is called while previous processEvent() is not complete
        pendingEventHandler = StateMachine.PendingEventHandler { pendingEvent, _ ->

            error(
                "$this can not process pending $pendingEvent " +
                        "as event processing is already running. " +
                        "Do not call processEvent() from notification listeners."
            )
        }
    }
    stateMachine.onTransition { _, _, _, _ ->
        // or add listener after state machine setup
    }

    // process events
    stateMachine.processEvent(SwitchYellowEvent)
    stateMachine.processEvent(SwitchRedEvent("Stop!"))
    // process event and pass argument, instead of adding nullable property to event class
    stateMachine.processEvent(SwitchGreenEvent, "Go!")
}
```