package com.arjanvlek.oxygenupdater.apis

import com.arjanvlek.oxygenupdater.models.Device
import com.arjanvlek.oxygenupdater.models.InstallGuidePage
import com.arjanvlek.oxygenupdater.models.NewsItem
import com.arjanvlek.oxygenupdater.models.RootInstall
import com.arjanvlek.oxygenupdater.models.ServerMessage
import com.arjanvlek.oxygenupdater.models.ServerPostResult
import com.arjanvlek.oxygenupdater.models.ServerStatus
import com.arjanvlek.oxygenupdater.models.UpdateData
import com.arjanvlek.oxygenupdater.models.UpdateMethod
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.QueryMap

/**
 * @author Adhiraj Singh Chauhan (github.com/adhirajsinghchauhan)
 */
interface ServerApi {

    @GET("devices/{filter}")
    suspend fun getDevices(
        @Path("filter") filter: String
    ): Response<List<Device>>

    @GET("updateData/{deviceId}/{updateMethodId}/{incrementalSystemVersion}")
    suspend fun getUpdateData(
        @Path("deviceId") deviceId: Long,
        @Path("updateMethodId") updateMethodId: Long,
        @Path("incrementalSystemVersion") incrementalSystemVersion: String
    ): Response<UpdateData>

    @GET("mostRecentUpdateData/{deviceId}/{updateMethodId}")
    suspend fun getMostRecentUpdateData(
        @Path("deviceId") deviceId: Long,
        @Path("updateMethodId") updateMethodId: Long
    ): Response<UpdateData>

    @GET("serverStatus")
    suspend fun getServerStatus(): Response<ServerStatus>

    @GET("serverMessages/{deviceId}/{updateMethodId}")
    suspend fun getServerMessages(
        @Path("deviceId") deviceId: Long,
        @Path("updateMethodId") updateMethodId: Long
    ): Response<List<ServerMessage>>

    @GET("news/{deviceId}/{updateMethodId}")
    suspend fun getNews(
        @Path("deviceId") deviceId: Long,
        @Path("updateMethodId") updateMethodId: Long
    ): Response<List<NewsItem>>

    @GET("news-item/{newsItemId}")
    suspend fun getNewsItem(
        @Path("newsItemId") newsItemId: Long
    ): Response<NewsItem>

    @POST("news-read")
    suspend fun markNewsItemRead(
        @Body newsItemId: Map<String, Long>
    ): Response<ServerPostResult>

    @GET("updateMethods/{deviceId}")
    suspend fun getUpdateMethodsForDevice(
        @Path("deviceId") deviceId: Long
    ): Response<List<UpdateMethod>>

    @GET("allUpdateMethods")
    suspend fun getAllUpdateMethods(): Response<List<UpdateMethod>>

    @GET("updateData/{deviceId}/{updateMethodId}/{pageNumber}")
    suspend fun getInstallGuidePage(
        @Path("deviceId") deviceId: Long,
        @Path("updateMethodId") updateMethodId: Long,
        @Path("pageNumber") pageNumber: Int
    ): Response<List<InstallGuidePage>>

    @POST("submit-update-file")
    suspend fun submitUpdateFile(
        @Body filename: Map<String, String>
    ): Response<ServerPostResult>

    @POST("log-update-installation")
    suspend fun logRootInstall(
        @Body rootInstall: RootInstall
    ): Response<ServerPostResult>

    @POST("verify-purchase")
    suspend fun verifyPurchase(
        @QueryMap queryMap: Map<String, Any?>
    ): Response<ServerPostResult>
}
