package org.sciserver.springapp.fileservice.dao;

import java.util.Collection;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface QuotaManagerService {
    @POST("createVolume")
    Call<Void> createVolume(@Body ManagerVolumeDTO volume);

    @POST("deleteVolume")
    Call<Void> deleteVolume(@Body ManagerVolumeDTO volume);

    @GET("getUsage")
    Call<Collection<QuotaFromManager>> getUsage();
}
