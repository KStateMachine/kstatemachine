import ru.nsk.kstatemachine.*

fun main(args: Array<String>) {
    class FirstEvent: Event
    class SecondEvent: Event

    val stateMachine = createStateMachine("Using States!") {
        val state1 = state("state1")

        val state2 = state("state2") {
            onEntry { println("exit state $name") }
            onEntry { println("exit state $name") }

            transition<FirstEvent> {
                targetState = state1
                onTriggered { println("triggered transition $this") }
            }
        }

        state1.apply {
            onEntry { println("enter state $name") }
            onExit { println("exit state $name") }

            transition<FirstEvent> {
                targetState = state2
                onTriggered { println("triggered transition $this") }
            }
        }

        setInitialState(state1)
    }

    stateMachine.postEvent(FirstEvent())
    stateMachine.postEvent(SecondEvent())
}