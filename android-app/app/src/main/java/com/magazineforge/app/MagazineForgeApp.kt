package com.magazineforge.app

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.magazineforge.app.network.ApiClient

class MagazineForgeApp : Application(), ImageLoaderFactory {
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .okHttpClient(ApiClient.okHttpClient)
            .build()
    }
}
