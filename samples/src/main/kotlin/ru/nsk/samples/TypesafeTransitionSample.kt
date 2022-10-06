package ru.nsk.samples

import ru.nsk.kstatemachine.*
import ru.nsk.samples.TypesafeTransitionSample.LoginData
import ru.nsk.samples.TypesafeTransitionSample.LoginEvent
import ru.nsk.samples.TypesafeTransitionSample.checkUserPassword

private object TypesafeTransitionSample {
    data class LoginData(val email: String, val password: String)

    class LoginEvent(override val data: LoginData) : DataEvent<LoginData>

    fun checkUserPassword(data: LoginData) = data.password == "qwerty"
}

/**
 * Shows API for typesafe arguments for transitions.
 * Only [DataEvent] holding special data can lead to [DataState] expecting the same data.
 */
fun main() {
    lateinit var accountFormState: DataState<LoginData>

    val machine = createStateMachine("Data states") {
        logger = StateMachine.Logger { println(it) }

        accountFormState = dataState("accountForm") {
            onEntry { println("login with: $data") }
        }

        initialState("loginForm") {
            dataTransition<LoginEvent, LoginData> {
                guard = { checkUserPassword(event.data) }
                targetState = accountFormState
            }
        }
    }

    val loginData = LoginData("test@email.com", "qwerty")
    machine.processEvent(LoginEvent(loginData))

    check(accountFormState.data === loginData)
}