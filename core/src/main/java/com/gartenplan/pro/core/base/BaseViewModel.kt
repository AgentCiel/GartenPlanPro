package com.gartenplan.pro.core.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Base ViewModel class that provides common functionality
 * for all ViewModels in the app
 *
 * @param S - UI State type
 * @param E - Event type (one-time events like navigation, snackbars)
 */
abstract class BaseViewModel<S, E>(
    initialState: S,
    protected val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    // UI State
    private val _uiState = MutableStateFlow(initialState)
    val uiState: StateFlow<S> = _uiState.asStateFlow()

    // One-time events (navigation, snackbars, etc.)
    private val _events = MutableSharedFlow<E>()
    val events = _events.asSharedFlow()

    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Error state
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /**
     * Update the UI state
     */
    protected fun updateState(reducer: S.() -> S) {
        _uiState.value = _uiState.value.reducer()
    }

    /**
     * Get current state
     */
    protected fun currentState(): S = _uiState.value

    /**
     * Send a one-time event
     */
    protected fun sendEvent(event: E) {
        viewModelScope.launch {
            _events.emit(event)
        }
    }

    /**
     * Set loading state
     */
    protected fun setLoading(loading: Boolean) {
        _isLoading.value = loading
    }

    /**
     * Set error message
     */
    protected fun setError(message: String?) {
        _error.value = message
    }

    /**
     * Clear error
     */
    fun clearError() {
        _error.value = null
    }

    /**
     * Launch a coroutine with loading state management
     */
    protected fun launchWithLoading(
        onError: ((Throwable) -> Unit)? = null,
        block: suspend () -> Unit
    ) {
        viewModelScope.launch(dispatcher) {
            try {
                setLoading(true)
                setError(null)
                block()
            } catch (e: Throwable) {
                setError(e.message ?: "Ein unbekannter Fehler ist aufgetreten")
                onError?.invoke(e)
            } finally {
                setLoading(false)
            }
        }
    }

    /**
     * Launch a coroutine without loading state
     */
    protected fun launch(
        onError: ((Throwable) -> Unit)? = null,
        block: suspend () -> Unit
    ) {
        viewModelScope.launch(dispatcher) {
            try {
                block()
            } catch (e: Throwable) {
                setError(e.message ?: "Ein unbekannter Fehler ist aufgetreten")
                onError?.invoke(e)
            }
        }
    }
}

/**
 * Simple BaseViewModel without events
 */
abstract class SimpleViewModel<S>(
    initialState: S,
    dispatcher: CoroutineDispatcher = Dispatchers.IO
) : BaseViewModel<S, Unit>(initialState, dispatcher)