package com.mileway.feature.tracking.di

// Koin scope marker for the active-tracking session lifecycle.
// Open the scope on trip-start; close it on trip-end to release per-trip state
// (active route ID, current-track repository, live UI state) without GC pressure.
//
// Usage (Android call-site):
//   val scope = getKoin().createScope("trip:$routeId", named<TrackingScope>())
//   scope.close()  // on trip end
class TrackingScope
