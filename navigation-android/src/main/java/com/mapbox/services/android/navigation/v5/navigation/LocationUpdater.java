package com.mapbox.services.android.navigation.v5.navigation;

import android.annotation.SuppressLint;
import android.location.Location;

import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineCallback;
import com.mapbox.android.core.location.LocationEngineRequest;
import com.mapbox.android.core.location.LocationEngineResult;

import java.lang.ref.WeakReference;

import androidx.annotation.NonNull;
import timber.log.Timber;

class LocationUpdater {

  private final LocationEngineCallback<LocationEngineResult> callback = new CurrentLocationEngineCallback(this);
  private final RouteProcessorBackgroundThread thread;
  private final MapboxNavigation mapboxNavigation;
  private LocationEngine locationEngine;
  private LocationEngineRequest request;

  @SuppressLint("MissingPermission")
  LocationUpdater(RouteProcessorBackgroundThread thread, MapboxNavigation mapboxNavigation,
          LocationEngine locationEngine, LocationEngineRequest request) {
    this.thread = thread;
    this.mapboxNavigation = mapboxNavigation;
    this.locationEngine = locationEngine;
    this.request = request;
    requestInitialLocationUpdates(locationEngine, request);
  }

  void updateLocationEngine(LocationEngine locationEngine) {
    requestLocationUpdates(request, locationEngine);
    this.locationEngine = locationEngine;
  }

  void updateLocationEngineRequest(LocationEngineRequest request) {
    requestLocationUpdates(request, locationEngine);
    this.request = request;
  }

  void onLocationChanged(Location location) {
    if (location != null) {
      thread.queueUpdate(NavigationLocationUpdate.create(location, mapboxNavigation));
    }
  }

  void removeLocationUpdates() {
    locationEngine.removeLocationUpdates(callback);
  }

  @SuppressLint("MissingPermission")
  private void requestInitialLocationUpdates(LocationEngine locationEngine, LocationEngineRequest request) {
    locationEngine.requestLocationUpdates(request, callback, null);
  }

  @SuppressLint("MissingPermission")
  private void requestLocationUpdates(LocationEngineRequest request, LocationEngine locationEngine) {
    this.locationEngine.removeLocationUpdates(callback);
    locationEngine.requestLocationUpdates(request, callback, null);
  }

  static class CurrentLocationEngineCallback implements LocationEngineCallback<LocationEngineResult> {

    private final WeakReference<LocationUpdater> updaterWeakReference;

    CurrentLocationEngineCallback(LocationUpdater locationUpdater) {
      this.updaterWeakReference = new WeakReference<>(locationUpdater);
    }

    @Override
    public void onSuccess(LocationEngineResult result) {
      LocationUpdater locationUpdater = updaterWeakReference.get();
      if (locationUpdater != null) {
        Location location = result.getLastLocation();
        locationUpdater.onLocationChanged(location);
      }
    }

    @Override
    public void onFailure(@NonNull Exception exception) {
      Timber.e(exception);
    }
  }
}
