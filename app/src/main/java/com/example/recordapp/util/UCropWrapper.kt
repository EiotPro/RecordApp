package com.example.recordapp.util

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import androidx.annotation.IntDef

/**
 * Wrapper class for UCrop to handle constants and methods
 * This avoids unresolved reference errors when UCrop library is not properly resolved
 */
object UCropWrapper {
    // Constants from UCrop class
    const val RESULT_ERROR = 96
    
    /**
     * Get the output Uri from the result intent
     */
    fun getOutput(intent: Intent?): Uri? {
        return if (intent == null) null else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra("com.yalantis.ucrop.OutputUri", Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra("com.yalantis.ucrop.OutputUri")
            }
        }
    }
    
    /**
     * Get error from the result intent
     */
    fun getError(intent: Intent?): Throwable? {
        return if (intent == null) null else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getSerializableExtra("com.yalantis.ucrop.Error", Throwable::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getSerializableExtra("com.yalantis.ucrop.Error") as? Throwable
            }
        }
    }
    
    /**
     * Create a UCrop builder
     */
    fun of(sourceUri: Uri, destinationUri: Uri): UCropBuilder {
        return UCropBuilder(sourceUri, destinationUri)
    }
    
    /**
     * Builder class for UCrop
     */
    class UCropBuilder(private val sourceUri: Uri, private val destinationUri: Uri) {
        private var options: Options? = null
        
        /**
         * Set options for the UCrop
         */
        fun withOptions(options: Options): UCropBuilder {
            this.options = options
            return this
        }
        
        /**
         * Get the intent for starting UCrop activity
         */
        fun getIntent(context: android.content.Context): Intent {
            val intent = Intent("com.yalantis.ucrop.UCrop")
            intent.setClassName(context, "com.yalantis.ucrop.UCropActivity")
            intent.putExtra("com.yalantis.ucrop.SourceUri", sourceUri)
            intent.putExtra("com.yalantis.ucrop.OutputUri", destinationUri)
            
            if (options != null) {
                intent.putExtra("com.yalantis.ucrop.Options", options!!.getOptionBundle())
            }
            
            return intent
        }
    }
    
    /**
     * Options class for UCrop
     */
    class Options {
        private val optionBundle = Bundle()
        
        fun setCompressionQuality(quality: Int): Options {
            optionBundle.putInt("com.yalantis.ucrop.CompressionQuality", quality)
            return this
        }
        
        fun setHideBottomControls(hide: Boolean): Options {
            optionBundle.putBoolean("com.yalantis.ucrop.HideBottomControls", hide)
            return this
        }
        
        fun setFreeStyleCropEnabled(enabled: Boolean): Options {
            optionBundle.putBoolean("com.yalantis.ucrop.FreeStyleCrop", enabled)
            return this
        }
        
        fun setToolbarTitle(title: String): Options {
            optionBundle.putString("com.yalantis.ucrop.ToolbarTitle", title)
            return this
        }
        
        fun setToolbarColor(color: Int): Options {
            optionBundle.putInt("com.yalantis.ucrop.ToolbarColor", color)
            return this
        }
        
        fun setStatusBarColor(color: Int): Options {
            optionBundle.putInt("com.yalantis.ucrop.StatusBarColor", color)
            return this
        }
        
        fun setToolbarWidgetColor(color: Int): Options {
            optionBundle.putInt("com.yalantis.ucrop.ToolbarWidgetColor", color)
            return this
        }
        
        fun setActiveControlsWidgetColor(color: Int): Options {
            optionBundle.putInt("com.yalantis.ucrop.ActiveControlsWidgetColor", color)
            return this
        }
        
        fun getOptionBundle(): Bundle {
            return optionBundle
        }
    }
} 