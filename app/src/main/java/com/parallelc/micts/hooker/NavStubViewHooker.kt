package com.parallelc.micts.hooker

import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import com.parallelc.micts.module
import com.parallelc.micts.triggerCircleToSearch
import io.github.libxposed.api.XposedInterface.AfterHookCallback
import io.github.libxposed.api.XposedInterface.Hooker
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam
import io.github.libxposed.api.annotations.AfterInvocation
import io.github.libxposed.api.annotations.XposedHooker
import java.lang.reflect.Field
import kotlin.math.abs

@XposedHooker
class NavStubViewHooker {
    companion object {
        private lateinit var mCurrAction: Field
        private lateinit var mCurrX: Field
        private lateinit var mInitX: Field
        private lateinit var mCurrY: Field
        private lateinit var mInitY: Field

        fun hook(param: PackageLoadedParam) {
            val navStubView = param.classLoader.loadClass("com.miui.home.recents.NavStubView")
            runCatching { navStubView.getDeclaredField("mCheckLongPress") }
                .onSuccess { throw Exception("mCheckLongPress exists") }
                .onFailure {
                    mCurrAction = navStubView.getDeclaredField("mCurrAction")
                    mCurrAction.isAccessible = true
                    mCurrX = navStubView.getDeclaredField("mCurrX")
                    mCurrX.isAccessible = true
                    mInitX = navStubView.getDeclaredField("mInitX")
                    mInitX.isAccessible = true
                    mCurrY = navStubView.getDeclaredField("mCurrY")
                    mCurrY.isAccessible = true
                    mInitY = navStubView.getDeclaredField("mInitY")
                    mInitY.isAccessible = true
                    module.hook(navStubView.getDeclaredMethod("onTouchEvent", MotionEvent::class.java), OnTouchEventHooker::class.java)
                }
        }

        class OnTouchEventHooker : Hooker {
            companion object {
                private val mCheckLongPress = Runnable {
                    runCatching {
                        triggerCircleToSearch()
                    }.onFailure { e ->
                        module.log("NavStubViewHooker mCheckLongPress fail", e)
                    }
                }

                @JvmStatic
                @AfterInvocation
                fun after(callback: AfterHookCallback) {
                    runCatching {
                        val view = callback.thisObject as View
                        when(mCurrAction.getInt(callback.thisObject)) {
                            0 -> view.postDelayed(mCheckLongPress, ViewConfiguration.getLongPressTimeout().toLong()) // DOWN
                            2 -> { // HOLD
                                if (abs(mCurrX.getFloat(callback.thisObject) - mInitX.getFloat(callback.thisObject)) > 4 ||
                                    abs(mCurrY.getFloat(callback.thisObject) - mInitY.getFloat(callback.thisObject)) > 4)
                                    view.removeCallbacks(mCheckLongPress)
                                else {}
                            }
                            else -> view.removeCallbacks(mCheckLongPress)
                        }
                    }.onFailure { e ->
                        module.log("NavStubViewHooker onTouchEvent fail", e)
                    }
                }
            }
        }
    }
}