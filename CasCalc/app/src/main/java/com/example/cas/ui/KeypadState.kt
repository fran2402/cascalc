package com.example.cas.ui

import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.cas.engine.AngleUnit

/**
 * The keypad's state, shared by every mode (calculator, 2D, 3D, complex): the tab open, whether
 * the function keys are raised, Rad or Deg, the constants' units, the ∇ coordinates, and the
 * second pages of the trigonometry and statistics tabs. Switching modes keeps the keypad as it was.
 */
object KeypadState {
    private var prefs: SharedPreferences? = null

    var angle by mutableStateOf(AngleUnit.Radians)
        private set
    var tab by mutableIntStateOf(0)
        private set
    var panelOpen by mutableStateOf(true)
        private set
    var unitSystem by mutableStateOf(com.example.cas.engine.UnitSystem.SI)
        private set
    var coordinates by mutableStateOf(com.example.cas.cas.Coordinates(com.example.cas.cas.CoordinateKind.Cartesian, com.example.cas.cas.CoordinateKind.Cartesian.defaults))
        private set
    /** The trigonometry tab showing sec, csc, cot (the reciprocals) instead of sin, cos, tan. */
    var reciprocalTrig by mutableStateOf(false)
        private set
    /** The statistics tab showing the distributions instead. */
    var distributions by mutableStateOf(false)
        private set

    /** The graphs' signals keys (step, box, waves…) in place of the tab's, from the button beside the letters. */
    var signals by mutableStateOf(false)

    /** Reads the saved state once (every view model calls it; the first one wins). */
    fun init(p: SharedPreferences) {
        if (prefs != null) return
        prefs = p
        angle = runCatching { AngleUnit.valueOf(p.getString("angle", AngleUnit.Radians.name)!!) }.getOrDefault(AngleUnit.Radians)
        tab = p.getInt("tab", 0).coerceIn(0, FunctionTabs.lastIndex)
        panelOpen = p.getBoolean("panel", true)
        unitSystem = runCatching { com.example.cas.engine.UnitSystem.valueOf(p.getString("units", "SI")!!) }.getOrDefault(com.example.cas.engine.UnitSystem.SI)
        coordinates = loadCoordinates(p)
        reciprocalTrig = p.getBoolean("reciprocalTrig", false)
        distributions = p.getBoolean("distributions", false)
    }

    fun coordinatesOf(kind: com.example.cas.cas.CoordinateKind) = prefs?.let { coordinatesFor(it, kind) } ?: com.example.cas.cas.Coordinates(kind, kind.defaults)

    fun toggleAngle() {
        angle = if (angle == AngleUnit.Radians) AngleUnit.Degrees else AngleUnit.Radians
        prefs?.edit()?.putString("angle", angle.name)?.apply()
    }
    fun togglePanel() { panelOpen = !panelOpen; prefs?.edit()?.putBoolean("panel", panelOpen)?.apply() }
    fun selectTab(i: Int) { signals = false; tab = i; prefs?.edit()?.putInt("tab", i)?.apply() }
    fun selectUnitSystem(u: com.example.cas.engine.UnitSystem) { unitSystem = u; prefs?.edit()?.putString("units", u.name)?.apply() }
    fun selectCoordinates(c: com.example.cas.cas.Coordinates) { coordinates = c; prefs?.let { saveCoordinates(it, c) } }
    fun toggleReciprocalTrig() { reciprocalTrig = !reciprocalTrig; prefs?.edit()?.putBoolean("reciprocalTrig", reciprocalTrig)?.apply() }
    fun toggleDistributions() { distributions = !distributions; prefs?.edit()?.putBoolean("distributions", distributions)?.apply() }
}
