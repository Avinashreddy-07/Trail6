package com.example.trail6;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface NominatimService {
    @GET("reverse")
    Call<NominatimResponse> getLocationDetails(
            @Query("lat") String latitude,
            @Query("lon") String longitude,
            @Query("format") String format,
            @Query("zoom") int zoom
    );
    @GET("search")
    Call<List<NominatimResponse>> searchNearbyHospitals(
            @Query("q") String query,
            @Query("format") String format,
            @Query("limit") int limit,
            @Query("polygon_geojson") int polygonGeojson,
            @Query("dedupe") int dedupe,
            @Query("category") String category
    );
    @GET("search")
    Call<List<NominatimResponse>> searchNearbyEyeHospitals(
            @Query("q") String query,
            @Query("format") String format,
            @Query("limit") int limit,
            @Query("polygon_geojson") int polygonGeojson,
            @Query("dedupe") int dedupe,
            @Query("category") String category
    );
}
