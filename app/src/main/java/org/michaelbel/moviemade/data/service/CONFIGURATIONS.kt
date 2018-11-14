package org.michaelbel.moviemade.data.service

import io.reactivex.Observable
import retrofit2.http.GET

interface CONFIGURATIONS {

    @GET("configuration?")
    fun apiConfiguration(): Observable<*>

    @GET("configuration/countries?")
    fun countries(): Observable<*>

    @GET("configuration/jobs?")
    fun jobs(): Observable<*>

    @GET("configuration/languages?")
    fun languages(): Observable<*>

    @GET("configuration/primary_translations?")
    fun primaryTranslations(): Observable<*>

    @GET("configuration/timezones?")
    fun timezones(): Observable<*>
}